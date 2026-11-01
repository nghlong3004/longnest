# Telegram → chatbot → Telegram

Phạm vi hiện tại: DeepSeek đề xuất thông tin thu/chi theo JSON; backend parse,
kiểm tra và lưu `READY` vào DB trước khi xác nhận về Telegram. Chưa lưu hoặc ghép draft.

Luồng: `TelegramMessageHandler → ChatService → ChatClient → DeepSeek` và trả ngược
qua Telegram. Giữ kiểm tra người gửi trước khi gọi model.

`service/ChatService` là interface có Javadoc cho contract của từng method;
`service/impl/ChatServiceImpl` là bean `@Service` thực hiện lời gọi chatbot.
`TransactionService` thực hiện liên kết user và lưu giao dịch trong một transaction DB;
`TransactionMapper` dùng MapStruct, không đặt quy tắc ngày hay quyền sở hữu trong mapper.

- Dùng Spring AI DeepSeek 2.0.1 có sẵn; không thêm thư viện hay tự viết HTTP API.
- Cấu hình lấy từ các biến `LONGNEST_DEEPSEEK_ENABLED`, `LONGNEST_DEEPSEEK_API_KEY`,
  `LONGNEST_DEEPSEEK_BASE_URL`, `LONGNEST_DEEPSEEK_MODEL` trong `.env.dev`.
- Khi DeepSeek tắt, giữ hành vi echo trước đây. Khi bật mà thiếu cấu hình hợp lệ,
  ứng dụng dừng khởi động thay vì âm thầm chuyển về echo.
- Dùng model đã cấu hình, tắt thinking cho bước trò chuyện đơn giản. Không đặt
  giới hạn token đầu ra riêng; HTTP kết nối tối đa 10 giây, đọc tối đa 60 giây và
  không tự retry yêu cầu model.
- Mỗi yêu cầu gửi tin nhắn mới cùng ngày gửi tin gốc và múi giờ; chưa có bộ nhớ hội thoại.
- Sau khi kiểm tra người gửi, hiển thị `typing` bằng
  [sendChatAction](https://core.telegram.org/bots/api#sendchataction), làm mới mỗi
  4 giây trên scheduler riêng trong lúc xử lý. Hủy lịch trước khi gửi câu trả lời,
  kể cả khi xử lý lỗi; lỗi hiển thị typing không chặn việc trả lời. Telegram tự
  xóa trạng thái khi nhận tin từ bot hoặc sau tối đa 5 giây nếu không làm mới.
- Bật DeepSeek JSON mode (`response_format.type=json_object`). Các record DTO
  `ChatResponse`, `TransactionData` và enum `ChatAction` mô tả cấu trúc đề xuất;
  Spring AI sinh hướng dẫn schema, không dùng schema JPA làm đầu ra.
- Prompt nằm ở `prompts/transaction-chat-system.txt`. JSON có `action`, `transaction`,
  `reply`; loại hành động là `READY`, `ASK_CLARIFICATION`, `CHAT`.
- Jackson 3 parse JSON thành `ChatResponse`; từ chối JSON hỏng, trường lạ/trùng, enum lạ,
  dữ liệu sau JSON, reply rỗng và dữ liệu không hợp lệ theo Jakarta Validation.
  `READY` cần rõ type và amount dương trong NUMERIC(19,2). Income không có category chi.
- `READY`: backend tìm `UserIdentity` theo channel + external user ID; chưa có thì
  tạo user và identity cùng giao dịch trong một transaction. Metadata nguồn chỉ lấy
  từ Telegram, không lấy ID hoặc owner do model đề xuất.
- Sau commit thành công, backend tự tạo một câu từ dữ liệu đã lưu, ví dụ:
  `Đã ghi khoản chi 45.000đ — Ăn trưa.` Không gửi `reply` của LLM cho `READY`.
- `ASK_CLARIFICATION` và `CHAT`: chỉ gửi `reply`, không tạo user hoặc ghi ledger.
  Câu trả lời bổ sung chưa được nối với câu hỏi cũ; cần gửi lại nội dung khoản đầy đủ.
- Khi transaction date là null, lấy ngày gửi tin gốc theo `LONGNEST_LEDGER_TIME_ZONE`
  (mặc định `Asia/Ho_Chi_Minh`), không lấy ngày chạy xử lý. Cùng ngày tham chiếu đó
  được gửi vào system message để model hiểu ngày tương đối.
- Chống replay trong luồng hiện tại: khóa identity trong transaction, kiểm tra
  owner + channel + chat ID + message ID rồi trả giao dịch cũ nếu đã ghi. Các tin
  riêng biệt cùng nội dung vẫn là các khoản riêng. Giữ schema V1 cho phép nhiều
  giao dịch chung nguồn; chưa triển khai nhiều khoản trong một tin.
- Tạo identity lần đầu đồng thời có thể gặp unique constraint; transaction thua
  rollback và báo lỗi, không tạo user mồ côi. Bot hiện chỉ chạy một polling consumer.
- Một tin nhiều khoản sẽ hỏi gửi riêng, không âm thầm cộng gộp hoặc bỏ khoản.
- Đưa nội dung người dùng vào `UserMessage`, không coi đó là template.
- Model trả rỗng hoặc lỗi: bot báo thử lại; lỗi một tin không chặn tin tiếp theo.
- Câu trả lời gửi dạng văn bản, chia phần tối đa 4096 UTF-16 code units, giữ nguyên
  nội dung và không tách đôi emoji. Không ghi nội dung hoặc API key vào log ứng dụng.
- Token, API key không nằm trong code hoặc tài liệu; không đổi schema DB.

Câu thử: `Ăn trưa 45k`, `Nhận lương 15 triệu`, `Ăn trưa`, `Xin chào`. Hai câu đầu
sẽ được ghi nếu dữ liệu hợp lệ; hai câu sau chỉ nhận câu hỏi hoặc lời chào. JSON mode
giúp giữ cú pháp JSON; backend vẫn kiểm tra cấu trúc và dữ liệu trước khi ghi.

Kiểm tra: service với ChatModel giả; native DeepSeek client với HTTP giả; handler
Telegram với client giả; toàn bộ Maven test; cuối cùng chạy launcher và nhắn thử
trên bot thật. Không dùng credential thật trong test tự động.

## Xử lý lỗi

`ResourceException` giữ `ErrorCode` và nguyên nhân gốc. `ErrorCode` là enum Java
trong `model/enums`, không phải kiểu dữ liệu lưu DB.

| Mã lỗi | Khi nào dùng | Message key |
|---|---|---|
| `CHATBOT_UNAVAILABLE` | Lỗi kết nối hoặc yêu cầu bị provider từ chối | `error.chatbot.unavailable` |
| `CHATBOT_EMPTY_RESPONSE` | Model trả null hoặc chỉ có khoảng trắng | `error.chatbot.empty-response` |
| `CHATBOT_INVALID_RESPONSE` | JSON hoặc dữ liệu không hợp lệ; chưa ghi ledger | `error.chatbot.invalid-response` |
| `TRANSACTION_SAVE_FAILED` | Thao tác ghi DB thất bại | `error.transaction.save-failed` |
| `INTERNAL_ERROR` | Lỗi không dự kiến | `error.internal` |

`ChatServiceImpl` chuyển các lỗi provider, parse/validation và ghi DB đã biết thành
`ResourceException`; lỗi lập trình vẫn giữ nguyên loại lỗi. Telegram bắt lỗi tại
biên polling và gọi bean `GlobalExceptionHandler` để chọn thông báo qua
`MessageSource`. Nội dung tiếng Việt nằm trong `messages_vi.properties`, bản gốc
trong `messages.properties`. Log ghi mã lỗi, loại lỗi và vị trí khi cần; không in
exception message hoặc nguyên nhân chứa thông tin riêng tư.
Tắt logger `org.hibernate.orm.jdbc.error` vì PostgreSQL có thể đưa toàn bộ dòng
tài chính bị lỗi vào chi tiết exception; handler vẫn ghi mã lỗi và loại nguyên nhân.

`@RestControllerAdvice` / `@ExceptionHandler` tự động xử lý lỗi trong HTTP
controller, không tự bắt lỗi từ Telegram polling. Hiện ứng dụng chạy non-web,
nên dùng handler chung do Spring quản lý và gọi trực tiếp ở Telegram. Khi thêm
HTTP controller, advice có thể dùng lại phần chọn mã và thông báo này.
