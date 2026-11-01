# Telegram → chatbot → ghi thu chi

`TelegramMessageHandler → ChatService → ChatClient/DeepSeek` để hiểu tin nhắn;
`TransactionService` và `DraftService` lưu dữ liệu bằng Spring Data JPA. Các service
có interface/Javadoc và implementation riêng, dùng chung `UserIdentityService`.
MapStruct chỉ ánh xạ dữ liệu; ownership, ngày mặc định và lifecycle nằm ở service/entity.

## Luồng hiện tại

- Chỉ tin nhắn riêng của Telegram user cấu hình mới được xử lý. Group, bot sender,
  media, callback của người khác hoặc chat khác không đi tới service/model.
- `ChatMessage` mang ID người gửi, kênh, chat, message, timestamp và reply-to do
  adapter cung cấp. LLM không được quyết định owner hoặc ID nguồn.
- DeepSeek JSON mode trả `ChatResponse`: `action`, `transaction`, `reply`.
  Jackson 3 và Jakarta Validation kiểm tra JSON, enum, số tiền và dữ liệu liên quan.
  Từ chối trường lạ/trùng, enum dạng số và nội dung dư sau JSON.
- `READY` mới: lưu giao dịch rồi backend tạo câu từ giá trị đã commit, ví dụ
  `Đã ghi khoản chi 45.000đ — Ăn trưa.` Không gửi lời xác nhận sẵn của LLM.
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
- `CHAT` chỉ trả lời, giữ draft. Khi anh muốn ghi một khoản khác trong lúc draft
  còn chờ, prompt nhắc hủy draft hiện tại trước để tránh ghép hai khoản.
- Câu trả lời trùng, Reply tới câu hỏi cũ và nút cũ đều tra trạng thái DB; draft
  `CONFIRMED`/`CANCELLED` không được mở lại. Tin riêng cùng nội dung vẫn là khoản riêng.

Ví dụ: `Ăn trưa` → bot hỏi số tiền + nút Hủy → `45` còn mơ hồ → bot hỏi tiếp →
`45k` → lưu 45.000đ cho ăn trưa và xác nhận. `45k` có thể là tin thường hoặc Reply.

## Hủy và xóa câu hỏi

Nút inline dùng `cancel_draft:<UUID>`; backend kiểm tra owner/channel/chat của
UUID đó, không tin callback data như bằng chứng quyền sở hữu. Hủy thành công giữ
bản ghi `CANCELLED`, trả một thông báo nhỏ qua
[answerCallbackQuery](https://core.telegram.org/bots/api#answercallbackquery) và gọi
[deleteMessage](https://core.telegram.org/bots/api#deletemessage) cho đúng câu hỏi
được bấm. Không gửi thêm tin chat. Lỗi xóa không hoàn tác trạng thái đã hủy.
Telegram giới hạn việc xóa tin quá 48 giờ; trạng thái DB vẫn là nguồn xác định.
Draft đã confirmed chỉ nhận thông báo đã ghi, không xóa giao dịch bằng nút Hủy.
Lệnh `/cancel`, `hủy` hoặc `huỷ` cũng hủy draft hiện tại mà không gọi LLM.

## Ngữ cảnh, ngày và tính nhất quán

- Context gồm tin gốc, các trường đang biết và câu hỏi hiện tại, không phải toàn
  bộ lịch sử chat. Nội dung người dùng luôn ở `UserMessage`; không nâng nội dung
  đó thành system instructions. Prompt ở `prompts/transaction-chat-system.txt`.
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
mỗi phần và không tách đôi emoji. Câu hỏi chỉ có một nút Hủy ở phần đầu.

| ErrorCode | Ý nghĩa |
|---|---|
| `CHATBOT_UNAVAILABLE` | Lỗi kết nối/provider |
| `CHATBOT_EMPTY_RESPONSE` | Model không có câu trả lời |
| `CHATBOT_INVALID_RESPONSE` | JSON hoặc dữ liệu chưa hợp lệ, không ghi |
| `TRANSACTION_SAVE_FAILED` | Ghi dữ liệu thất bại, không báo thành công |
| `DRAFT_NOT_FOUND` | Không có draft phù hợp với owner/chat |
| `DRAFT_UPDATE_FAILED` | Không cập nhật/hủy được draft |
| `DRAFT_CONTEXT_CHANGED` | Context đổi trong lúc model chạy; nạp lại trạng thái đã commit |
| `INTERNAL_ERROR` | Lỗi ngoài dự kiến |

`ResourceException` giữ mã enum và cause; `GlobalExceptionHandler` chọn thông báo
qua `MessageSource`, chỉ log mã/loại lỗi và vị trí cần thiết. Không log nội dung
chat, JSON model hay key. Tắt logger `org.hibernate.orm.jdbc.error` vì lỗi PostgreSQL
có thể chứa toàn bộ dòng tài chính. Telegram polling gọi handler trực tiếp;
`@RestControllerAdvice` chỉ tự hoạt động ở HTTP controller.

Kiểm tra tự động dùng credential giả, HTTP provider giả, TelegramClient giả và
PostgreSQL Testcontainers; không gọi model/Telegram thật. Sau đó chạy launcher để
chủ bot thử nhiều lượt, Reply và nút Hủy trực tiếp.
