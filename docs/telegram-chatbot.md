# Telegram → chatbot → ghi thu chi

`TelegramMessageHandler → ChatService → ChatClient/DeepSeek` để hiểu tin nhắn;
`TransactionService` và `DraftService` lưu dữ liệu bằng Spring Data JPA. Các service
có interface/Javadoc và implementation riêng, dùng chung `UserIdentityService`.
MapStruct chỉ ánh xạ dữ liệu; ownership, ngày mặc định và lifecycle nằm ở service/entity.

## Luồng hiện tại

- `LONGNEST_TELEGRAM_USERS` là danh sách ID được phép, ngăn cách bằng dấu phẩy.
  Mỗi người chỉ dùng chat riêng của chính mình; dữ liệu tra theo identity của người gửi.
  Giữ fallback `LONGNEST_TELEGRAM_USER` cho cấu hình cũ.
- Chỉ tin nhắn riêng của Telegram user trong danh sách mới được xử lý. Group, bot sender,
  media, callback của người khác hoặc chat khác không đi tới service/model.
- `ChatMessage` mang ID người gửi, kênh, chat, message, timestamp và reply-to do
  adapter cung cấp. Tên hiển thị first/last cũng chỉ lấy từ sender Telegram đã được
  xác thực; bỏ control/bidi/format, gộp khoảng trắng và giới hạn 80 Unicode code point.
  Tên là nhãn hiển thị tùy chọn, không phải identity hay chỉ dẫn. LLM không được
  quyết định owner hoặc ID nguồn; không lấy tên từ body/model để thay metadata.
- DeepSeek JSON mode trả `ChatResponse`: `action`, `transaction`, `reply`, `query`.
  Jackson 3 và Jakarta Validation kiểm tra JSON, enum, số tiền và dữ liệu liên quan.
  Từ chối trường lạ/trùng, enum dạng số và nội dung dư sau JSON.
- `READY` mới: lưu giao dịch rồi backend tạo câu từ giá trị đã commit, ví dụ
  `Em đã ghi khoản chi 45.000đ — Ăn trưa rồi nhé, Long.` khi có tên đáng tin dùng được;
  thiếu tên thì bỏ phần gọi tên. Không gửi lời xác nhận sẵn của LLM.
  Số tiền/nội dung lấy nguyên giá trị đã commit, không thay chữ hàng loạt trong dữ liệu.
- `ASK_CLARIFICATION`: lưu hoặc cập nhật một draft `PENDING`, rồi trả `ChatReply`
  với text và draft ID. Telegram gắn nút **Hủy** dưới câu hỏi và đăng ký ID tin
  câu hỏi sau khi gửi thành công.
- Mỗi owner/channel/chat có một draft đang chờ trong luồng service. Tin thường
  bổ sung draft đó; native Reply có thể trỏ tới bất kỳ tin đã liên kết với draft.
  Nếu Reply không có liên kết hợp lệ, không tự tạo giao dịch mới.
- Hỏi nhiều lượt vẫn dùng cùng draft, giữ trường đã biết khi output là null;
  thông tin bổ sung/đính chính khác null có thể thay thế trường cũ. Đổi loại sang
  income sẽ bỏ category chi. Chưa có thao tác xóa riêng một trường qua giá trị null.
- Khi đủ rõ, tạo giao dịch và chuyển draft sang `CONFIRMED` cùng một transaction
  DB. Giữ message ID gốc làm nguồn giao dịch. Lưu lỗi rollback cả việc bổ sung và
  xác nhận; draft đang chờ vẫn có thể được trả lời lại.
- `QUERY` chỉ đọc giao dịch đã ghi, giữ nguyên draft; backend tạo tổng tiền và danh sách từ DB.
- `CHAT` chỉ trả lời, giữ draft. Khi anh muốn ghi một khoản khác trong lúc draft
  còn chờ, prompt nhắc hủy draft hiện tại trước để tránh ghép hai khoản.
- Lời chào thuần cả tin (`Xin chào`, `Chào`, `Chào em`, `Xin chào em`, `hello`/`hi`
  có thể thêm `em`, `/start`,
  có thể thêm dấu câu cuối) được trả lời xác định bằng tên metadata: `Em chào Long,
  cần ghi hay xem thu chi cứ nhắn em nhé.` Không có tên dùng được thì bỏ địa chỉ,
  không máy móc “Chào bạn”. Kiểm tra receipt đã xử lý/đã ghi/draft replay vẫn đi
  trước lời chào; lời chào không bổ sung hay hủy draft. Tin chào kèm ghi/xem giao
  dịch không bị bắt vào nhánh này và vẫn gọi model như trước. Nhánh lời chào không
  thêm model call; chế độ echo khi không có ChatClient giữ nguyên.
- Câu trả lời trùng, Reply tới câu hỏi cũ và nút cũ đều tra trạng thái DB; draft
  `CONFIRMED`/`CANCELLED` không được mở lại. Tin riêng cùng nội dung vẫn là khoản riêng.

Ví dụ: `Ăn trưa` → bot hỏi số tiền + nút Hủy → `45` còn mơ hồ → bot hỏi tiếp →
`45k` → lưu 45.000đ cho ăn trưa và xác nhận. `45k` có thể là tin thường hoặc Reply.

## Xem khoản thu/chi

Ví dụ tin nhắn:

- `Xem khoản chi hôm nay` → `TODAY`.
- `Xem khoản chi hôm qua` → `YESTERDAY`, chỉ riêng ngày hôm qua.
- `Xem khoản chi từ hôm qua tới giờ` → `SINCE_YESTERDAY`, gồm hôm qua và hôm nay.
- `Xem chi tiêu từ đầu tháng tới giờ` → `THIS_MONTH`, đầu tháng đến ngày gửi tin.
- `Xem khoản thu tháng trước` → `LAST_MONTH`, cả tháng lịch trước đó.
- `Anh muốn xem tất cả khoản chi từ tháng trước tới tháng này` → `SINCE_LAST_MONTH`,
  đầu tháng trước đến ngày gửi tin mới; ngày 02/11/2026 cho 01/10/2026 → 02/11/2026.
- `Xem khoản chi từ 20/10/2026 tới giờ` → `SINCE_DATE`.
- `Xem khoản chi từ 01/10/2026 đến 15/10/2026` → `CUSTOM`.

QUERY có `transaction=null` và `query={type, period, from, to}`. Type bắt buộc thu
hoặc chi; các kỳ tương đối để from/to null. Backend dùng ngày **tin yêu cầu xem**
theo múi giờ sổ, không dùng ngày gốc của draft. CUSTOM có cả hai ngày, SINCE_DATE
chỉ có from. Khoảng ngày bao gồm hai đầu; ngày đảo ngược bị từ chối. Chỉ hỏi trường
thật sự còn thiếu, không bắt gửi lại cả yêu cầu. `chi tiêu` đã rõ là EXPENSE;
`gần đây` chỉ cần hỏi khoảng thời gian và nhớ loại chi. Lượt sau `từ tháng trước
tới tháng này` tiếp tục yêu cầu xem, không bổ sung draft tài chính. Backend có
recovery chỉ đọc hẹp cho câu đầy đủ trên (kể cả typo `Anh muốn hết tất cả khoản chi ...`)
và câu tiếp theo chỉ có kỳ tương đối, tránh vòng lặp CHAT của model. Không dùng
recovery này để ghi dữ liệu hoặc đoán ngày cụ thể/ngôn ngữ ngoài các mẫu hỗ trợ.

`TransactionQueryService` tra identity từ người gửi đã xác thực rồi lọc user ID,
loại và ngày. Không tạo user/identity/draft/receipt mới. Được xem trong lúc draft
đang chờ hoặc Reply vào câu hỏi cũ; các guard chống ghi vào draft đã đóng vẫn giữ.
Receipt của chính tin đã xử lý tiếp tục được replay trước khi gọi model.

Một lượt có tin tổng tiền/số khoản cho **toàn khoảng**, sau đó tối đa 5 tin giao
dịch riêng, đều native Reply vào tin yêu cầu xem. Nội dung/địa điểm dài được rút gọn
khi hiển thị, không thay DB. Mỗi thẻ có nút **⋯** cho Sửa/Xóa. Nếu còn dữ liệu, tin cuối có
**Xem thêm**; nút gọi DB trực tiếp, không gọi LLM, giữ ngày và tin Reply gốc.

## Sửa và xóa giao dịch đã ghi

- **⋯ → Sửa → chọn trường → nhập giá trị → Trước/Sau → Lưu hoặc Hủy**.
  Trường được hỗ trợ: số tiền, ngày, loại thu/chi, danh mục, nội dung, địa điểm, buổi.
  Số tiền nhận số dương tối đa hai chữ số lẻ và hậu tố `k`/`K` không mơ hồ:
  `50k` → bản xem trước `50.000đ`; không nhận dấu phân cách hàng nghìn hay NLP.
  Ngày nhập `YYYY-MM-DD`; enum có danh sách lựa chọn trong hướng dẫn nhập.
  Nhập `-` để bỏ trường tùy chọn. Nội dung/địa điểm mới tối đa 500 ký tự;
  dữ liệu dài đã lưu ở trường không sửa được giữ nguyên. Đổi sang thu sẽ bỏ
  danh mục chi, thể hiện trong bản xem trước. Không dùng LLM cho thao tác này.
  Bản xem trước chỉ dùng ngữ cảnh nhận diện gọn, hiện đầy đủ giá trị cũ/mới của
  trường đang sửa (kể cả text cũ dài), và ghi rõ trường khác giữ nguyên cùng
  thay đổi danh mục suy ra khi đổi loại. Không dùng phần rút gọn của thẻ để xác nhận.
  Nếu dài, chia thành tin tối đa 4096 UTF-16 units, không cắt đôi emoji; chỉ tin
  cuối có Lưu/Hủy, token gắn đúng tin cuối và chỉ có hiệu lực khi gửi đủ bản xem trước.
  Token bước trước bị vô hiệu hóa. Nếu gửi dở thất bại, không bật Lưu; có thể
  nhập lại hoặc Hủy (kể cả `/cancel`), không đổi sổ hay draft tạo khoản.
- **⋯ → Xóa** nêu ID khoản, số tiền, ngày và việc xóa vĩnh viễn, không hoàn tác.
  Chỉ nút **Xóa vĩnh viễn** mới xóa dòng `transactions`. Hủy và `/cancel` khi
  đang nhập/xem trước chỉ đóng thao tác sửa; không đổi sổ hay draft/receipt tạo khoản.
  Tin nhập đã xử lý chỉ thêm receipt định danh riêng cho thao tác sửa/xóa.
- Token ngẫu nhiên chỉ tham chiếu state phía server, gắn ID giao dịch ổn định,
  chat/owner và đúng tin bot. Mỗi bước kiểm tra lại owner/channel/chat và version
  DB; token bước trước và confirmation đã dùng không còn hiệu lực. Session thao
  tác hết hạn sau 15 phút, độc lập hoàn toàn với draft tạo khoản.
  `transaction_action_inputs` chỉ lưu định danh các tin đã nhận trong thao tác,
  không lưu nội dung. Replay của giá trị nhập hay `/cancel` không được chạy lại
  luồng tạo khoản, kể cả sau restart.
- Flyway V3 thêm version, trạng thái draft `DELETED` và bảng
  `deleted_transaction_sources` chỉ giữ định danh nguồn chống replay. Draft đã
  confirmed được bỏ FK và chuyển `DELETED` trước khi xóa ledger row trong cùng
  transaction; xóa payload tài chính/text của draft, giữ link nguồn để tin cũ
  không tạo lại khoản. V1/V2 không sửa.
- Sau DB commit, bot yêu cầu xóa native mọi thẻ của khoản vừa xóa đã theo dõi.
  Nếu Telegram từ chối (ví dụ tin quá cũ), bot sửa thẻ thành “đã xóa khỏi sổ” và
  bỏ nút. Đây là best effort: nếu Telegram từ chối cả sửa/gửi thì không thể cập
  nhật chữ trên client, nhưng token cũ đã bị vô hiệu hóa trên server. Lỗi Telegram
  không rollback DB và không báo sai thành lỗi tài chính.
- Các query view còn được theo dõi (kể cả thẻ trùng/lượt Xem thêm)
  được đánh dấu không còn hiện hành và bỏ nút, rồi gửi lại trang đầu/tổng mới theo
  đúng filter và tin yêu cầu gốc. Việc reset trang tránh sót/lặp do ngày hoặc loại
  đổi làm khoản rời filter/thứ tự. Callback phân trang gắn cả token lẫn ID tin thẻ: nút của view cũ không dùng lại được
  kể cả khi cursor trang mới trùng hệt. Không sửa dữ liệu của người khác.

State giao diện nằm trong bộ nhớ của một bot instance; restart bỏ session/token
sửa cũ (bấm lại nhận yêu cầu xem danh sách mới), không mất thay đổi đã commit hay
receipt chống replay trong DB. Thẻ/query đã gửi trước restart không có registry để
làm mới tự động. Tin mới chưa từng được nhận trước restart đi theo luồng chat
thường, không tự khôi phục bước nhập sửa; cần mở query mới để tiếp tục sửa.
Registry giữ tối đa 128 chat theo LRU; mỗi chat có tối đa 20 trang query còn hiệu lực
(trong một giờ) và 200 tham chiếu thẻ gần nhất để hỗ trợ gỡ thẻ trùng khi xóa.
Refresh thu hồi các view cũ rồi đăng ký các trang mới, không duyệt lại lịch sử vô hạn.
Nút phân trang/thao tác của view hết hạn hoặc bị thu hồi không còn dùng được; cần
gửi yêu cầu xem mới. Xóa chỉ có thể tìm/gỡ những thẻ còn trong giới hạn theo dõi,
nhưng quyền thao tác và dữ liệu đã commit luôn được kiểm tra trên server. Evict chat
cũng bỏ phiên sửa trong bộ nhớ; tin mới chưa từng được xử lý sau eviction đi theo
luồng chat thường. Không có job nền hay retry bền vững cho Telegram. Nếu giao diện cập nhật thất bại, yêu cầu
xem lại danh sách để đọc DB hiện hành. Kiểm tra tự động dùng TelegramClient giả;
chưa xác nhận UX trong Telegram thật. Copy nói rõ khi sổ đã đổi nhưng tin hiển thị
chưa làm mới/gỡ được; không trộn lỗi hiển thị sau commit với lỗi thay đổi dữ liệu.

Dùng chung `TransactionPage`/`TransactionCursor`; sắp ngày giảm dần rồi UUID giảm
dần để phân trang keyset ổn định khi cùng ngày. UUID không thể hiện thứ tự thời gian
trong ngày. Tổng và danh sách mỗi lượt đọc trong cùng transaction REPEATABLE_READ;
giữa hai lượt, giao dịch mới có thể làm đổi tổng. Callback `q:` mang type, ba ngày,
UUID dạng Base64URL và ID tin yêu cầu; tối đa 64 byte, không chứa owner. Callback
được kiểm tra lại người gửi/chat, phạm vi, ID tin thẻ và việc trang vẫn còn trong registry trước khi
truy vấn. Token cũ sau restart/eviction/hết hạn yêu cầu xem lại danh sách. Không cần migration.

## Hủy và xóa câu hỏi

Nút inline dùng `cancel_draft:<UUID>`; backend kiểm tra owner/channel/chat của
UUID đó, không tin callback data như bằng chứng quyền sở hữu. Hủy thành công giữ
bản ghi `CANCELLED`, trả lời callback để dừng trạng thái chờ qua
[answerCallbackQuery](https://core.telegram.org/bots/api#answercallbackquery) và gọi
[deleteMessage](https://core.telegram.org/bots/api#deletemessage) cho đúng câu hỏi
được bấm. Sau khi hủy lần đầu, gửi một tin xác nhận có nội dung và số tiền đã biết,
ví dụ `Em hủy khoản “Ăn trưa” (45.000đ) rồi nhé.` Tin này Reply vào tin thu/chi gốc
của người dùng. Bấm lại nút đã hủy chỉ hiện thông báo nhỏ, không gửi lặp tin chat.
Lỗi xóa không hoàn tác trạng thái đã hủy và không chặn tin xác nhận. Tên quá dài
được rút gọn trong thông báo để giữ một tin; dữ liệu draft trong DB giữ nguyên.
Telegram giới hạn việc xóa tin quá 48 giờ; trạng thái DB vẫn là nguồn xác định.
Draft đã confirmed chỉ nhận thông báo đã ghi, không xóa giao dịch bằng nút Hủy.
Lệnh `/cancel`, `hủy` hoặc `huỷ` cũng hủy draft hiện tại mà không gọi LLM.

## Ngữ cảnh, ngày và tính nhất quán

- Model nhận tối đa 6 cặp user/bot đã hoàn tất, theo role và thứ tự thời gian, mỗi
  phía lịch sử tối đa 1000 Unicode code point; tin mới giữ nguyên. Lưu cả câu trả lời
  thực do backend tạo (xác nhận/tổng/danh sách), không chỉ ví dụ few-shot. Scope là
  channel + sender đáng tin + private chat; trùng source không thêm lượt. Tin đến
  muộn không đẩy context lùi về trước hay đưa lượt tương lai vào prompt. Các lượt
  cùng scope được tuần tự hóa khi diễn giải; khóa stripe và tối đa 128 conversation
  theo LRU chặn tăng bộ nhớ không giới hạn. History mất khi restart/evict, draft
  và receipt DB không mất. Nút/edit input có luồng riêng, không trở thành chỉ dẫn
  ghi khoản mới.
- Draft đang chờ vẫn cung cấp tin gốc, trường đã biết và câu hỏi hiện tại riêng.
  History không phải quyền ghi mới: backend chặn READY cũ trên lời cảm ơn, câu xem,
  câu chỉ có số tiền dạng số không có draft hoặc câu tiếp nối mơ hồ đã nhận diện. Chỉ draft
  đang chờ được dùng để hoàn thiện phần tài chính. Giữ nguyên source receipt,
  owner/date gate, /cancel và receipt của tin sửa khoản.
  Guard chỉ từ chối các dạng tiếp nối mơ hồ đã biết, không dùng whitelist động từ/
  mục đích và không bắt số tiền phải viết bằng chữ số. READY hợp lệ cho `Cà phê 35k`,
  `Tiền điện 500k`, `Taxi 50k` hoặc số tiền viết bằng chữ vẫn đi theo luồng hiện có,
  dù có hay không có history. Draft có thể nhận câu trả lời số tiền viết bằng chữ;
  `hôm nay` trả lời câu hỏi ngày của draft khi chưa có ngữ cảnh query thực sự.
  Chỉ một lần gọi model mỗi tin cần diễn giải; không thêm lần kiểm tra tách history.
  Ngoài các dạng từ chối hẹp, vẫn tin bước trích xuất hiện có từ tin mới/draft;
  đây không phải bằng chứng độc lập cho ngữ nghĩa của mọi trường model đề xuất.
  READY ghi mới không được bỏ ngày slash/ISO đã nêu rõ với `ngày` rồi âm thầm dùng
  ngày gửi tin. Câu trả lời số tiền dạng `50/2` không bị coi thành câu trả lời ngày.
- Nội dung người dùng luôn ở `UserMessage`; tên hiển thị nằm trong block JSON
  user-role riêng, không đưa vào system instructions hay đổi owner. Bot xưng em,
  ưu tiên tên dùng được khi chào/gọi trực tiếp thay vì “bạn”, không suy giới tính.
  Xác nhận ghi có thể gọi tên một lần; thẻ giao dịch, tổng và các lỗi/hướng dẫn lặp
  bỏ cách gọi không cần thiết, không ép tên vào mỗi dòng. Thiếu tên dùng câu trung tính
  tự nhiên không có địa chỉ. Lớp trình bày bỏ surrogate UTF-16 lẻ ở tên, kể cả trước
  khi chuyển metadata tới model; không sửa body/nội dung giao dịch hay quy tắc identity.
  Plain text/no parseMode giữ tên dạng markup
  vô hại về định dạng. Prompt ở `prompts/transaction-chat-system.txt`.
- Không nói ngày: dùng ngày gửi tin gốc theo `LONGNEST_LEDGER_TIME_ZONE` (mặc định
  `Asia/Ho_Chi_Minh`). Draft giữ timestamp gốc qua restart và các lượt hỏi.
- Ngày tương đối trong câu trả lời mới dùng ngày gửi câu trả lời đó; ngày đã biết
  của draft không đổi nếu người dùng chỉ bổ sung số tiền.
- Service khóa identity/draft trong các transaction ngắn. Ghi READY không có
  context sẽ quyết định ghi mới hay tiếp tục draft và lưu receipt trong cùng
  transaction; thông báo đã gửi câu hỏi không làm đổi version diễn giải.
  Version chỉ bảo vệ dữ liệu/câu hỏi/trạng thái có ý nghĩa với lượt trả lời. Không giữ transaction
  DB trong lúc chờ model. Context cũ không được ghi đè trạng thái mới.
- `draft_message_links` chỉ lưu ID nguồn → draft để định tuyến và chống replay,
  gồm tin gốc, các câu hỏi đã gửi và các câu trả lời đã xử lý. Không lưu body đầy
  đủ hoặc thay thế bằng một bảng lịch sử hội thoại.
- Flyway V2 thêm timestamp gốc, version và bảng liên kết; không sửa V1. Draft cũ
  dùng `created_at` làm mốc backfill khi trước đây chưa có timestamp nguồn.
- Không đặt UNIQUE trên nguồn trong `transactions`: schema vẫn cho phép nhiều
  khoản chung nguồn. Luồng hiện tại chỉ nhận một khoản mỗi tin; prompt hỏi gửi riêng
  nếu có nhiều khoản. Replay và trạng thái draft được kiểm tra trước khi ghi.

## Cấu hình và lỗi

Dùng Spring AI DeepSeek 2.0.1 hiện có; cấu hình từ `LONGNEST_DEEPSEEK_ENABLED`,
`LONGNEST_DEEPSEEK_API_KEY`, `LONGNEST_DEEPSEEK_BASE_URL`, `LONGNEST_DEEPSEEK_MODEL`.
Khi tắt model, giữ echo; các lệnh hủy vẫn hoạt động. Khi bật mà thiếu key hợp lệ,
ứng dụng dừng khởi động. Không thêm retry model; connect timeout 10s, read timeout 60s.

Sau authorization, bot làm mới [typing](https://core.telegram.org/bots/api#sendchataction)
mỗi 4s trên scheduler riêng; hủy lịch trước khi gửi câu trả lời, kể cả khi lỗi.
Lỗi hiển thị typing không chặn trả lời. Text gửi plain, tối đa 4096 UTF-16 units
mỗi phần và không tách đôi emoji. Mọi phần của câu trả lời, câu hỏi và thông báo
lỗi đều dùng `ReplyParameters` trỏ đúng tin người dùng đang được xử lý. Tin xác
nhận hủy qua button dùng ID tin gốc từ draft đã kiểm tra owner/chat. Nếu người dùng
đã xóa tin nguồn, cho phép Telegram vẫn gửi thông báo thay vì mất xác nhận.
Câu hỏi chỉ có một nút Hủy ở phần đầu.

| ErrorCode | Ý nghĩa |
|---|---|
| `CHATBOT_UNAVAILABLE` | Lỗi kết nối/provider |
| `CHATBOT_EMPTY_RESPONSE` | Model không có câu trả lời |
| `CHATBOT_INVALID_RESPONSE` | JSON hoặc dữ liệu chưa hợp lệ, không ghi |
| `TRANSACTION_SAVE_FAILED` | Ghi dữ liệu thất bại, không báo thành công |
| `DRAFT_NOT_FOUND` | Không có draft phù hợp với owner/chat |
| `DRAFT_UPDATE_FAILED` | Không cập nhật/hủy được draft |
| `DRAFT_CONTEXT_CHANGED` | Context đổi trong lúc model chạy; nạp lại trạng thái đã commit |
| `QUERY_INVALID_PARAMETERS` | Loại, khoảng ngày hoặc cursor xem chưa hợp lệ |
| `QUERY_FAILED` | Không lấy được danh sách giao dịch |
| `TRANSACTION_DELETED` | Nguồn cũ của khoản đã xóa, không tạo lại ledger row |
| `TRANSACTION_INPUT_PROCESSED` | Tin thuộc thao tác sửa/xóa đã xử lý, không diễn giải thành khoản mới |
| `INTERNAL_ERROR` | Lỗi ngoài dự kiến |

`ResourceException` giữ mã enum và cause; `GlobalExceptionHandler` chọn thông báo
qua `MessageSource`, chỉ log mã/loại lỗi và vị trí cần thiết. Không log nội dung
chat, JSON model hay key. Tắt logger `org.hibernate.orm.jdbc.error` vì lỗi PostgreSQL
có thể chứa toàn bộ dòng tài chính. Telegram polling gọi handler trực tiếp;
`@RestControllerAdvice` chỉ tự hoạt động ở HTTP controller.

Kiểm tra tự động dùng credential giả, HTTP provider giả, TelegramClient giả và
PostgreSQL Testcontainers; không gọi model/Telegram thật. Sau đó chạy launcher để
chủ bot thử nhiều lượt, Reply và nút Hủy trực tiếp.

## Tổ chức code Telegram

`TelegramMessageHandler` xác thực update, chuyển text và định tuyến callback qua
`TelegramCallbackRouter`. Các handler riêng xử lý xem thêm, hủy draft và sửa/xóa.
`TelegramMessageSender` là adapter duy nhất gọi SDK để gửi/sửa/xóa/ack/typing,
dùng chung cách chia text không cắt đôi surrogate pair. Keyboard cho sửa/xóa nằm ở
`TelegramTransactionKeyboard`; text thuần nằm ở `presentation.TransactionActionPresentation`.

`TelegramEditSession` kiểm soát các bước chuyển trạng thái và dữ liệu cần có của
từng bước. `TelegramTransactionRegistry` quản lý token, phiên nhập, view và tham chiếu
thẻ theo chat. Khóa stripe theo chat giữ thứ tự xử lý mà không giữ khóa toàn bộ
coordinator khi gọi DB/Telegram. Không thêm xử lý nền hoặc thay đổi transaction của sổ.
Menu/chọn trường chỉ đổi token khi cập nhật giao diện thành công. Giá trị nhập mới
thu hồi nút Lưu trước đó, kể cả khi giá trị không hợp lệ; preview chỉ bật Lưu sau
khi gửi đủ. `TransactionField` định kiểu các trường sửa, còn parsing/validation và
quy tắc danh mục vẫn thuộc service mutation.
