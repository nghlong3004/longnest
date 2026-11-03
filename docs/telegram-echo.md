# Telegram echo

Mục tiêu: anh nhắn văn bản cho bot, bot trả lại nguyên văn trong cuộc trò chuyện
riêng. Giữ khoảng trắng, tiếng Việt, emoji và xuống dòng; không bật Markdown/HTML.

- Dùng Telegram Bots SDK 10.3.0 có sẵn, nhận tin bằng long polling vì ứng dụng hiện
  không chạy web server. Webhook sẽ cần endpoint HTTPS nên chưa phù hợp bước này.
- `LONGNEST_TELEGRAM_ENABLED=true` bật tích hợp; mặc định tắt.
- `LONGNEST_TELEGRAM_TOKEN` là token bot; `LONGNEST_TELEGRAM_USERS` là danh sách ID người gửi
  được phép, ngăn cách bằng dấu phẩy (fallback tên cũ `LONGNEST_TELEGRAM_USER`).
  Chỉ phản hồi tin riêng từ chính người trong danh sách; bỏ qua nhóm, bot và media.
- `/start` cũng được trả lại nguyên văn như các tin văn bản khác.
- Cấu hình, HTTP client và polling executor do Spring quản lý; đăng ký bot qua
  lifecycle sau khi các bean đã khởi tạo và dọn tài nguyên khi tắt ứng dụng.
- Không ghi nội dung tin hoặc token vào log. Lỗi gửi một tin không chặn tin sau.
- Bước này chỉ echo, chưa gọi LLM, tạo user hay ghi giao dịch/draft.

Triển khai theo thứ tự: test lọc người gửi/echo → handler và cấu hình → lifecycle
→ test đầy đủ → chạy `bash scripts/dev-server.sh` với `.env.dev` để kiểm tra live.
Test tự động dùng token giả, không kết nối Telegram thật.
