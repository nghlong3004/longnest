# Telegram → chatbot → Telegram

Phạm vi: nối nội dung văn bản từ Telegram tới DeepSeek và trả câu trả lời về đúng
cuộc trò chuyện riêng của chủ bot. Chưa phân loại, lưu giao dịch hay lưu draft.

Luồng: `TelegramMessageHandler → ChatService → ChatClient → DeepSeek` và trả ngược
qua Telegram. Giữ kiểm tra người gửi trước khi gọi model.

- Dùng Spring AI DeepSeek 2.0.1 có sẵn; không thêm thư viện hay tự viết HTTP API.
- Cấu hình lấy từ các biến `LONGNEST_DEEPSEEK_ENABLED`, `LONGNEST_DEEPSEEK_API_KEY`,
  `LONGNEST_DEEPSEEK_BASE_URL`, `LONGNEST_DEEPSEEK_MODEL` trong `.env.dev`.
- Khi DeepSeek tắt, giữ hành vi echo trước đây. Khi bật mà thiếu cấu hình hợp lệ,
  ứng dụng dừng khởi động thay vì âm thầm chuyển về echo.
- Dùng model đã cấu hình, tắt thinking cho bước trò chuyện đơn giản. Không đặt
  giới hạn token đầu ra riêng; HTTP kết nối tối đa 10 giây, đọc tối đa 60 giây và
  không tự retry yêu cầu model.
- Mỗi yêu cầu hiện chỉ gửi tin nhắn mới; chưa thêm bộ nhớ hội thoại.
- Đưa nội dung người dùng vào `UserMessage`, không coi đó là template.
- Model trả rỗng hoặc lỗi: bot báo thử lại; lỗi một tin không chặn tin tiếp theo.
- Câu trả lời gửi dạng văn bản, chia phần tối đa 4096 UTF-16 code units, giữ nguyên
  nội dung và không tách đôi emoji. Không ghi nội dung hoặc API key vào log ứng dụng.
- Token, API key không nằm trong code hoặc tài liệu; không đổi schema DB.

Kiểm tra: service với ChatModel giả; native DeepSeek client với HTTP giả; handler
Telegram với client giả; toàn bộ Maven test; cuối cùng chạy launcher và nhắn thử
trên bot thật. Không dùng credential thật trong test tự động.
