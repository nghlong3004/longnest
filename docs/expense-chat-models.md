# Thiết kế ghi nhận thu chi qua chat

Trạng thái: thiết kế từ buổi brainstorm ngày 2026-11-01; triển khai schema bước đầu.
Trước mắt phục vụ một người qua Telegram; có định danh người dùng để mở rộng sau.

Công nghệ hiện có: Java 25, Spring Boot 4.1.1, Spring Data JPA, PostgreSQL,
Flyway, Spring AI DeepSeek và Telegram Bots SDK. Phạm vi lần này là tài liệu,
migration, entity JPA và kiểm tra lưu/đọc dữ liệu.

Package Java:

- `me.nghlong3004.longnest.model.enums`: các enum dùng trong model.
- `me.nghlong3004.longnest.model.entity`: `User`, `UserIdentity`, `Transaction`,
  `TransactionDraft`, tương ứng bốn bảng bên dưới.

Entity dùng Lombok getter/builder, constructor mặc định protected cho JPA và
constructor đầy đủ private. Builder không cho truyền ID, timestamp hoặc trạng
thái kết thúc của draft. Quan hệ tới user/giao dịch dùng lazy, không cascade xóa.
Draft có các thao tác hỏi lại, xác nhận và hủy; chỉ draft `PENDING` được đổi.

## 1. `users` — chủ sở hữu dữ liệu

| Trường | Kiểu đề xuất | Ý nghĩa |
|---|---|---|
| `id` | UUID | ID nội bộ, không dùng trực tiếp ID tài khoản Telegram |
| `created_at` | Timestamp | Thời điểm tạo người dùng |
| `updated_at` | Timestamp | Thời điểm cập nhật gần nhất |

## 2. `user_identities` — tài khoản người dùng trên từng kênh

| Trường | Kiểu đề xuất | Ý nghĩa |
|---|---|---|
| `id` | UUID | Mã liên kết |
| `user_id` | UUID, FK → users | Người dùng nội bộ sở hữu tài khoản |
| `channel` | Enum | Kênh cung cấp tài khoản, trước mắt là `TELEGRAM` |
| `external_user_id` | String | ID người gửi do kênh cung cấp, không phải ID cuộc trò chuyện |
| `created_at` | Timestamp | Thời điểm tạo liên kết |
| `updated_at` | Timestamp | Thời điểm sửa liên kết gần nhất |

Cặp `channel + external_user_id` là duy nhất. Khi nhận tin, tra cặp này để lấy
`user_id`, rồi dùng ID nội bộ đó cho giao dịch và draft. Một người có thể có nhiều
identity; việc ghép identity mới vào người dùng có sẵn cần xác minh, không tự ghép
dựa trên tên hiển thị hoặc `chat_id`. Luồng `READY` hiện tạo user cùng identity
cho người gửi đã được Telegram adapter cho phép; chưa có luồng ghép thêm tài khoản.

`channel` có enum rõ ràng trong Java và PostgreSQL. Chưa cần bảng `channels` vì
chưa có cấu hình hay vòng đời riêng cho từng kênh. Khi thêm kênh, bổ sung giá trị
enum bằng migration mới và cập nhật enum Java tương ứng.

## 3. `transactions` — giao dịch đã ghi nhận

| Trường | Kiểu đề xuất | Bắt buộc | Ý nghĩa |
|---|---|---|---|
| `id` | UUID | Có | Mã giao dịch |
| `user_id` | UUID, FK → users | Có | Người sở hữu giao dịch |
| `type` | Enum | Có | `INCOME` hoặc `EXPENSE` |
| `amount` | Decimal | Có | Số tiền dương, trước mắt dùng VNĐ |
| `category` | Enum | Không | Nhóm mục đích thu/chi |
| `description` | Text | Không | Nội dung, ví dụ “ăn trưa” |
| `location` | Text | Không | Nơi phát sinh |
| `date` | Date | Có | Ngày thực tế thu/chi |
| `time_of_day` | Enum | Không | Sáng, trưa, chiều hoặc tối |
| `channel` | Enum | Có | Kênh gửi, trước mắt là `TELEGRAM` |
| `chat_id` | String | Có | ID cuộc trò chuyện chứa tin nhắn gốc |
| `message_id` | String | Có | ID tin nhắn gốc trong cuộc trò chuyện |
| `created_at` | Timestamp | Có | Thời điểm tạo bản ghi |
| `updated_at` | Timestamp | Có | Thời điểm sửa gần nhất |

Nhóm chi đã bàn: ăn uống, nhà ở, đi lại, sức khỏe, học tập, mua sắm, giải trí, khác.
Chưa biết mục đích thì để `category` trống; “Khác” dành cho mục đích đã rõ nhưng
không thuộc nhóm có sẵn. Nhóm thu chưa chốt.

## 4. `transaction_drafts` — giao dịch đang chờ làm rõ

| Trường | Kiểu đề xuất | Bắt buộc | Ý nghĩa |
|---|---|---|---|
| `id` | UUID | Có | Mã bản nháp |
| `user_id` | UUID, FK → users | Có | Người sở hữu bản nháp |
| `type` | Enum | Không | Loại thu/chi đã hiểu được |
| `amount` | Decimal | Không | Số tiền dương nếu đã xác định được |
| `category` | Enum | Không | Nhóm mục đích đã hiểu được |
| `description` | Text | Không | Nội dung đã hiểu được |
| `location` | Text | Không | Nơi phát sinh đã hiểu được |
| `date` | Date | Không | Ngày thu/chi đã xác định được |
| `time_of_day` | Enum | Không | Buổi đã xác định được |
| `channel` | Enum | Có | Kênh gửi |
| `chat_id` | String | Có | ID cuộc trò chuyện |
| `message_id` | String | Có | ID tin nhắn gốc |
| `original_text` | Text | Có | Nguyên văn tin nhắn gốc để giữ ngữ cảnh |
| `original_sent_at` | Timestamp | Có | Timestamp gửi tin gốc, giữ ngày mặc định qua các lượt hỏi |
| `version` | Bigint | Có | Phiên bản trạng thái dùng kiểm tra context cũ |
| `question_text` | Text | Không | Câu hỏi hiện tại của bot |
| `question_message_id` | String | Không | ID tin nhắn chứa câu hỏi hiện tại |
| `status` | Enum | Có | `PENDING`, `CONFIRMED` hoặc `CANCELLED` |
| `transaction_id` | UUID, FK → transactions | Không | Giao dịch được tạo từ bản nháp; có khi đã xác nhận |
| `created_at` | Timestamp | Có | Thời điểm tạo bản nháp |
| `updated_at` | Timestamp | Có | Thời điểm bổ sung hoặc đổi trạng thái |

## Luồng xử lý

- Tin đã rõ: tạo thẳng giao dịch, không cần tạo draft.
- Tin còn mơ hồ: lưu draft trong DB rồi hỏi lại.
- Anh reply câu hỏi của bot: ghép theo kênh, cuộc trò chuyện và ID câu hỏi;
  kiểm tra chủ sở hữu trước khi xử lý.
- Hỏi nhiều lượt vẫn cập nhật cùng draft: giữ thông tin đã rõ, bổ sung hoặc sửa
  theo câu trả lời; cập nhật câu hỏi hiện tại và `updated_at`.
- Đủ rõ: tạo giao dịch, gắn `transaction_id` và chuyển draft từ `PENDING` sang
  `CONFIRMED` trong cùng một database transaction. Một draft chỉ được xác nhận một lần.
- Nút “Hủy”: chuyển sang `CANCELLED`, giữ dữ liệu trong DB, ngừng hỏi.
- Trả lời câu hỏi cũ của draft đã hủy: báo đã hủy, không tự mở lại.
- Chỉ tạo draft mới khi cần làm rõ một khoản khác.

Ví dụ: “Ăn trưa hôm qua” → lưu draft → hỏi số tiền → anh reply “45k” → bổ sung
45.000 VNĐ → tạo giao dịch → draft thành `CONFIRMED`.

## Quy tắc chung và đánh đổi

- Giao dịch cần rõ loại thu/chi và số tiền; thiếu nội dung, nhóm, nơi hoặc buổi
  không ngăn ghi nhận. Chưa chốt việc mặc định loại chi khi anh chỉ gửi số tiền.
- Không nói ngày: lấy ngày của tin nhắn gốc theo múi giờ người dùng, không lấy
  ngày trả lời bổ sung. Giữ ngày mặc định đó qua các lượt hỏi.
- `date` là ngày giao dịch; các timestamp là thời điểm xử lý dữ liệu.
- Tra nguồn tin bằng `channel + chat_id + message_id`; các ID bên ngoài là String.
- `chat_id` nhận diện cuộc trò chuyện; `external_user_id` nhận diện người gửi.
- Đọc, sửa và hủy dữ liệu đều kiểm tra `user_id`.
- Giữ thông tin nguồn trong hai bảng: truy vấn đơn giản, chấp nhận lặp nguồn khi
  draft thành giao dịch. Chưa tách bảng `messages`.
- Ngữ cảnh LLM trước mắt gồm tin mới, dữ liệu draft và câu hỏi hiện tại. Nếu cần
  hội thoại tự do hoặc lưu đầy đủ lịch sử, xem xét thêm `messages`.

## Migration ban đầu

- File: `apps/server/src/main/resources/db/migration/V1__create_expense_chat_schema.sql`.
- Bốn bảng: `users`, `user_identities`, `transactions`, `transaction_drafts`.
- ID dùng UUID, mặc định `gen_random_uuid()`; tiền dùng `NUMERIC(19,2)`, phải dương
  và không nhận `NaN`; timestamp dùng `TIMESTAMPTZ`.
- `created_at`, `updated_at` mặc định thời điểm tạo. Entity dùng
  `@CreationTimestamp` / `@UpdateTimestamp` với đồng hồ DB để tự cập nhật audit khi
  ghi qua Hibernate; chưa thêm trigger. Câu lệnh SQL trực tiếp phải tự cập nhật
  `updated_at`.
- Các trường phân loại dùng enum PostgreSQL có tên riêng, tương ứng enum Java
  trong package `me.nghlong3004.longnest.model.enums`:

| Enum Java | Enum PostgreSQL | Giá trị |
|---|---|---|
| `TransactionType` | `transaction_type` | `INCOME`, `EXPENSE` |
| `TransactionCategory` | `transaction_category` | `FOOD`, `HOUSING`, `TRANSPORT`, `HEALTH`, `EDUCATION`, `SHOPPING`, `ENTERTAINMENT`, `OTHER` |
| `TimeOfDay` | `time_of_day` | `MORNING`, `NOON`, `AFTERNOON`, `EVENING` |
| `Channel` | `channel_type` | `TELEGRAM` |
| `DraftStatus` | `draft_status` | `PENDING`, `CONFIRMED`, `CANCELLED` |

Nhóm thu chưa chốt; trước mắt có thể để `category` trống. Khi chốt nhóm mới,
thêm giá trị bằng migration và cập nhật enum Java, không sửa migration đã áp dụng.

- Draft `CONFIRMED` phải có `transaction_id`; các trạng thái khác không được có.
  Giao dịch liên kết phải cùng chủ sở hữu và chỉ gắn với tối đa một draft.
- Có index cho truy vấn giao dịch theo người/ngày, draft theo người/trạng thái
  và tra câu hỏi của bot. Không đặt UNIQUE trên nguồn giao dịch vì một tin có thể
  chứa nhiều khoản. Luồng ghi hiện khóa identity rồi kiểm tra nguồn theo owner
  trước khi insert để chống xử lý lại tin đã ghi.
- Khóa ngoại không xóa dây chuyền dữ liệu tài chính.
- Giữ `ddl-auto: validate`, `sql.init.mode: never`; không dùng Hibernate tạo bảng.

Các bước thực hiện: cập nhật thiết kế → viết test schema chạy với PostgreSQL
Testcontainers → tạo V1 → thêm entity và kiểm tra JPA → chạy toàn bộ Maven test.
Giữ `ddl-auto: validate` khi kiểm tra entity với schema Flyway. Test dùng database tạm,
không chạy launcher hoặc đọc cấu hình bí mật của môi trường thật.

## Liên kết tin nhắn của draft (V2)

`draft_message_links`: `id`, `draft_id` (FK), `channel`, `chat_id`, `message_id`.
Nguồn `channel + chat_id + message_id` duy nhất. Bảng nhỏ này liên kết tin gốc,
các câu hỏi và các lượt trả lời đã xử lý với draft; không lưu đầy đủ lịch sử text.
Nhờ đó Reply câu hỏi cũ và tin được giao lại sau restart vẫn tra đúng trạng thái,
kể cả draft đã hủy hoặc confirmed. Xóa câu hỏi trên Telegram không xóa liên kết DB.

Luồng hỏi lại nhiều lượt, nút Hủy và xác nhận cùng transaction đã được nối trong
service; xem [luồng chatbot](telegram-chatbot.md). V2 giữ nguyên V1 đã áp dụng.

## Những phần chưa triển khai

- Luồng đăng ký và xác minh liên kết thêm tài khoản theo kênh.
- Bộ nhóm thu.
- Quy tắc khi anh chỉ gửi một số tiền; cách xử lý một tin có nhiều khoản.
- Luồng sửa hoặc hoàn tác giao dịch đã ghi nhận.
