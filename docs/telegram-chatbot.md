# Telegram → chatbot → Telegram

Phạm vi hiện tại: cho DeepSeek đề xuất thông tin thu/chi theo JSON và gửi nguyên
JSON về cuộc trò chuyện riêng của chủ bot để kiểm tra. Chưa lưu giao dịch hay draft.

Luồng: `TelegramMessageHandler → ChatService → ChatClient → DeepSeek` và trả ngược
qua Telegram. Giữ kiểm tra người gửi trước khi gọi model.

`service/ChatService` là interface có Javadoc cho contract của từng method;
`service/impl/ChatServiceImpl` là bean `@Service` thực hiện lời gọi chatbot.

- Dùng Spring AI DeepSeek 2.0.1 có sẵn; không thêm thư viện hay tự viết HTTP API.
- Cấu hình lấy từ các biến `LONGNEST_DEEPSEEK_ENABLED`, `LONGNEST_DEEPSEEK_API_KEY`,
  `LONGNEST_DEEPSEEK_BASE_URL`, `LONGNEST_DEEPSEEK_MODEL` trong `.env.dev`.
- Khi DeepSeek tắt, giữ hành vi echo trước đây. Khi bật mà thiếu cấu hình hợp lệ,
  ứng dụng dừng khởi động thay vì âm thầm chuyển về echo.
- Dùng model đã cấu hình, tắt thinking cho bước trò chuyện đơn giản. Không đặt
  giới hạn token đầu ra riêng; HTTP kết nối tối đa 10 giây, đọc tối đa 60 giây và
  không tự retry yêu cầu model.
- Mỗi yêu cầu hiện chỉ gửi tin nhắn mới; chưa thêm bộ nhớ hội thoại.
- Bật DeepSeek JSON mode (`response_format.type=json_object`). Các record DTO
  `ChatResponse`, `TransactionData` và enum `ChatAction` mô tả cấu trúc đề xuất;
  Spring AI sinh hướng dẫn schema, không dùng schema JPA làm đầu ra.
- Prompt nằm ở `prompts/transaction-chat-system.txt`. JSON có `action`, `transaction`,
  `reply`; loại hành động là `READY`, `ASK_CLARIFICATION`, `CHAT`.
- Telegram hiển thị toàn bộ JSON gốc của model, chưa chỉ lấy trường `reply`.
  Code không parse, sửa hoặc tự bổ sung kết quả trong bước kiểm tra này.
- Với `READY`, `reply` nói rõ trạng thái chưa lưu, ví dụ:
  `Khoản chi 45.000đ cho ăn trưa — chưa lưu.` Chỉ dùng lời xác nhận đã ghi sau khi
  triển khai và lưu DB thành công.
- Luồng chính sau khi nối DB: đủ thông tin thì kiểm tra, lưu thành công rồi gửi
  một tin xác nhận; không gửi thêm tin "Em hiểu" trước đó. Thiếu thông tin thì hỏi
  lại trước khi lưu. Luồng lưu này chưa được triển khai trong bản kiểm tra JSON.
- Không có ngày cụ thể thì `date=null`. Ngày tương đối chưa có mốc tham chiếu sẽ
  hỏi lại; chưa truyền ngày gửi tin vào prompt ở bước này.
- Một tin nhiều khoản sẽ hỏi gửi riêng, không âm thầm cộng gộp hoặc bỏ khoản.
- Đưa nội dung người dùng vào `UserMessage`, không coi đó là template.
- Model trả rỗng hoặc lỗi: bot báo thử lại; lỗi một tin không chặn tin tiếp theo.
- Câu trả lời gửi dạng văn bản, chia phần tối đa 4096 UTF-16 code units, giữ nguyên
  nội dung và không tách đôi emoji. Không ghi nội dung hoặc API key vào log ứng dụng.
- Token, API key không nằm trong code hoặc tài liệu; không đổi schema DB.

Câu thử: `Ăn trưa 45k`, `Nhận lương 15 triệu`, `Ăn trưa`, `Xin chào`. Kết quả lần
lượt nên là `READY/EXPENSE`, `READY/INCOME`, `ASK_CLARIFICATION`, `CHAT`. JSON mode
giúp giữ cú pháp JSON; ý nghĩa và sự đầy đủ của dữ liệu vẫn cần kiểm tra.

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
| `INTERNAL_ERROR` | Lỗi không dự kiến | `error.internal` |

`ChatServiceImpl` chỉ chuyển các lỗi HTTP/Spring AI đã biết thành
`ResourceException`; lỗi lập trình vẫn giữ nguyên loại lỗi. Telegram bắt lỗi tại
biên polling và gọi bean `GlobalExceptionHandler` để chọn thông báo qua
`MessageSource`. Nội dung tiếng Việt nằm trong `messages_vi.properties`, bản gốc
trong `messages.properties`. Log ghi mã lỗi, loại lỗi và vị trí khi cần; không in
exception message hoặc nguyên nhân chứa thông tin riêng tư.

`@RestControllerAdvice` / `@ExceptionHandler` tự động xử lý lỗi trong HTTP
controller, không tự bắt lỗi từ Telegram polling. Hiện ứng dụng chạy non-web,
nên dùng handler chung do Spring quản lý và gọi trực tiếp ở Telegram. Khi thêm
HTTP controller, advice có thể dùng lại phần chọn mã và thông báo này.
