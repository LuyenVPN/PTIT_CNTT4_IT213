# Bài 3: Phân tích Tài chính — Tính toán chi phí AI

## 1. Dữ liệu đầu vào

Hệ thống xử lý trung bình 10.000 hóa đơn mỗi ngày.

- Input trung bình: 1.500 tokens/hóa đơn.
- Output trung bình: 500 tokens/hóa đơn.
- Thời gian tính chi phí: 30 ngày/tháng.

### Tổng Input tokens mỗi ngày

10.000 × 1.500
= 15.000.000 tokens
= 15M tokens

### Tổng Output tokens mỗi ngày

10.000 × 500
= 5.000.000 tokens
= 5M tokens

### Tổng số tokens mỗi ngày

15M + 5M
= 20M tokens


## 2. Mô hình A — Direct API: DeepSeek Chat

Giá:

- Input: $0.14 / 1M tokens.
- Output: $0.28 / 1M tokens.

### Chi phí Input

15M × $0.14
= $2.10/ngày

### Chi phí Output

5M × $0.28
= $1.40/ngày

### Tổng chi phí mỗi ngày

$2.10 + $1.40
= $3.50/ngày

### Tổng chi phí 30 ngày

$3.50 × 30
= $105/tháng

### Kết quả Mô hình A

Input:  $2.10/ngày
Output: $1.40/ngày
Tổng:   $3.50/ngày

30 ngày:
$3.50 × 30 = $105


## 3. Mô hình B — OpenRouter + Gemini 2.5 Flash

Giá:

- Input: $0.075 / 1M tokens.
- Output: $0.30 / 1M tokens.

### Chi phí Input

15M × $0.075
= $1.125/ngày

### Chi phí Output

5M × $0.30
= $1.50/ngày

### Tổng chi phí mỗi ngày

$1.125 + $1.50
= $2.625/ngày

### Tổng chi phí 30 ngày

$2.625 × 30
= $78.75/tháng

### Kết quả Mô hình B

Input:  $1.125/ngày
Output: $1.50/ngày
Tổng:   $2.625/ngày

30 ngày:
$2.625 × 30 = $78.75


## 4. So sánh chi phí Mô hình A và B

| Chi phí | Mô hình A — DeepSeek | Mô hình B — Gemini |
|---|---:|---:|
| Input/ngày | $2.10 | $1.125 |
| Output/ngày | $1.40 | $1.50 |
| Tổng/ngày | $3.50 | $2.625 |
| Tổng/30 ngày | $105.00 | $78.75 |

Mô hình B rẻ hơn:

$105 - $78.75
= $26.25/tháng

Tỷ lệ tiết kiệm:

($105 - $78.75) / $105 × 100
= 25%

Như vậy, nếu chưa tính retry, Mô hình B tiết kiệm 25% chi phí API so với Mô hình A.


## 5. Tính chi phí Retry của Mô hình B

Đề bài cho:

- Tỷ lệ lỗi/mất kết nối API Aggregator: 0.5%.
- Retry làm tăng khoảng 5% tổng số Input tokens/ngày.

Số Input tokens phát sinh thêm do retry:

15M × 5%
= 0.75M tokens/ngày

### Input tokens thực tế

15M + 0.75M
= 15.75M tokens/ngày

Output vẫn là:

5M tokens/ngày


## 6. Chi phí Mô hình B sau khi tính Retry

### Chi phí Input mới

15.75 × $0.075
= $1.18125/ngày

### Chi phí Output

5 × $0.30
= $1.50/ngày

### Tổng chi phí thực tế mỗi ngày

$1.18125 + $1.50
= $2.68125/ngày

### Tổng chi phí 30 ngày

$2.68125 × 30
= $80.4375
≈ $80.44/tháng

### Kết quả Mô hình B sau Retry

Input:
15.75M tokens/ngày
→ $1.18125/ngày

Output:
5M tokens/ngày
→ $1.50/ngày

Tổng:
$2.68125/ngày

30 ngày:
≈ $80.44/tháng


## 7. So sánh cuối cùng

| Phương án | Chi phí/ngày | Chi phí/30 ngày |
|---|---:|---:|
| Mô hình A — Direct DeepSeek | $3.50 | $105.00 |
| Mô hình B — OpenRouter + Gemini | $2.625 | $78.75 |
| Mô hình B + Retry | $2.68125 | $80.44 |

Mặc dù có retry, Mô hình B vẫn rẻ hơn Mô hình A:

$105 - $80.4375
= $24.5625/tháng

Tỷ lệ tiết kiệm thực tế:

$24.5625 / $105 × 100
≈ 23.39%

Như vậy, sau khi tính retry, Mô hình B vẫn tiết kiệm khoảng 23,39% so với Mô hình A.


## 8. Phân tích các yếu tố phi tài chính

Việc lựa chọn API không nên chỉ dựa vào giá. Với hệ thống xử lý 10.000 hóa đơn/ngày, cần xem xét thêm các yếu tố kiến trúc như Vendor Lock-in, Latency, SLA, bảo mật và khả năng mở rộng.


## 9. Vendor Lock-in

### Mô hình A — Direct API

Kiến trúc:

Application
↓
DeepSeek API

Ưu điểm:

- Kiến trúc đơn giản.
- Ít một lớp trung gian.
- Dễ kiểm soát request/response.
- Dễ xác định nguyên nhân khi xảy ra lỗi.

Nhược điểm:

- Phụ thuộc trực tiếp vào một nhà cung cấp.
- Nếu muốn chuyển sang Gemini, OpenAI hoặc model khác có thể phải thay đổi integration.
- Tăng mức độ phụ thuộc vào API và chính sách của DeepSeek.

### Mô hình B — API Aggregator

Kiến trúc:

Application
↓
OpenRouter
↓
Gemini

Ưu điểm:

Có thể thay đổi model/provider dễ dàng hơn.

Ví dụ:

             OpenRouter
                 │
        ┌────────┼────────┐
        ↓        ↓        ↓
     Gemini   DeepSeek   Model khác

Ứng dụng có thể xây dựng chiến lược fallback hoặc lựa chọn model dựa trên chi phí, chất lượng và tình trạng hệ thống.

Nhược điểm:

- Phụ thuộc thêm vào một dịch vụ trung gian.
- Có thêm dependency trong kiến trúc.
- Có thể phát sinh thêm rủi ro về quota, policy và availability của aggregator.


## 10. Latency

### Direct API

Application
↓
DeepSeek

Chỉ có một lớp API trung gian nên về lý thuyết có thể giảm latency.

### Aggregator

Application
↓
OpenRouter
↓
Gemini

Có thêm một network hop và một lớp xử lý trung gian nên latency có thể cao hơn.

Tuy nhiên latency thực tế còn phụ thuộc vào:

- Vị trí server.
- Network.
- Tải của provider.
- Model được sử dụng.
- Thời gian inference.
- Cơ chế routing của aggregator.

Do đó không nên kết luận chỉ dựa trên số lượng network hop mà cần benchmark thực tế.


## 11. SLA và độ ổn định

### Direct API

Ưu điểm:

- Ít thành phần hơn.
- Dễ xác định nguyên nhân lỗi.
- Dependency chain ngắn hơn.

Nếu DeepSeek gặp sự cố thì hệ thống trực tiếp bị ảnh hưởng.

### Aggregator

Kiến trúc:

Application
↓
OpenRouter
↓
Gemini

Có thêm một dependency nên hệ thống có thể bị ảnh hưởng nếu:

- OpenRouter gặp sự cố.
- Gemini gặp sự cố.
- API key hoặc quota gặp vấn đề.
- Routing giữa OpenRouter và provider xảy ra lỗi.

Tuy nhiên Aggregator có lợi thế là có thể hỗ trợ nhiều provider/model, từ đó có thể xây dựng chiến lược fallback nếu nền tảng hỗ trợ.

Ví dụ:

Request
↓
Gemini
↓
Failure?
↓
DeepSeek
↓
Failure?
↓
Model dự phòng

Điều này có thể tăng khả năng phục hồi của hệ thống.


## 12. Bảo mật và dữ liệu hóa đơn

Đây là yếu tố đặc biệt quan trọng đối với hệ thống xử lý hóa đơn.

Hóa đơn có thể chứa:

- Tên khách hàng.
- Địa chỉ.
- Số điện thoại.
- Mã số thuế.
- Thông tin giao dịch.
- Thông tin tài chính.

Nếu sử dụng Aggregator:

Application
↓
OpenRouter
↓
Gemini

Dữ liệu phải đi qua thêm một bên thứ ba.

Do đó cần kiểm tra:

- Chính sách lưu trữ dữ liệu.
- Chính sách sử dụng dữ liệu để training.
- Data retention.
- Compliance.
- Encryption.
- Quy định về dữ liệu cá nhân.
- Hợp đồng/SLA doanh nghiệp.

Nếu hệ thống có yêu cầu bảo mật nghiêm ngặt, Direct API hoặc giải pháp Enterprise có chính sách dữ liệu rõ ràng có thể phù hợp hơn.


## 13. Khả năng mở rộng và chiến lược dài hạn

Mô hình B có lợi thế nếu doanh nghiệp muốn xây dựng kiến trúc Multi-model.

Thay vì:

Application → DeepSeek

có thể xây dựng:

                 ┌── Gemini
                 │
Application → Aggregator
│
├── DeepSeek
│
└── Model dự phòng

Điều này cho phép lựa chọn model dựa trên:

- Chi phí.
- Latency.
- Chất lượng.
- Tình trạng hệ thống.
- Loại tài liệu.

Ví dụ, hóa đơn đơn giản có thể dùng model rẻ hơn, trong khi hóa đơn phức tạp có thể chuyển sang model có khả năng xử lý tốt hơn.


## 14. Quyết định dưới góc độ Kỹ sư giải pháp

Nếu chỉ xét chi phí, nên chọn:

Mô hình B — OpenRouter + Gemini 2.5 Flash

Vì:

Mô hình A = $105/tháng
Mô hình B = $78.75/tháng
Mô hình B + Retry ≈ $80.44/tháng

Ngay cả sau khi tính retry, B vẫn rẻ hơn khoảng 23,39%.

Tuy nhiên, đối với hệ thống Production lâu dài, quyết định nên dựa trên cả chi phí và kiến trúc.

### Có thể chọn Mô hình A nếu:

- Ưu tiên kiến trúc đơn giản.
- Muốn giảm dependency trung gian.
- Yêu cầu latency thấp.
- Muốn kiểm soát trực tiếp provider.
- Có yêu cầu compliance/bảo mật nghiêm ngặt.

### Có thể chọn Mô hình B nếu:

- Muốn tối ưu chi phí.
- Muốn dễ dàng thay đổi model.
- Muốn xây dựng multi-model/fallback.
- Chấp nhận thêm một lớp dependency.
- Aggregator đáp ứng tốt yêu cầu SLA, bảo mật và compliance.


## 15. Kết luận

Về mặt tài chính:

Mô hình A:
$3.50/ngày
$105/tháng

Mô hình B:
$2.625/ngày
$78.75/tháng

Mô hình B + Retry:
$2.68125/ngày
≈ $80.44/tháng

Mô hình B vẫn tiết kiệm khoảng 23,39% so với Mô hình A sau khi tính retry.

Đứng dưới góc độ Kỹ sư giải pháp, nếu OpenRouter đáp ứng đầy đủ yêu cầu về SLA, bảo mật, compliance và latency, nên ưu tiên Mô hình B vì vừa có chi phí thấp hơn vừa mang lại khả năng linh hoạt khi thay đổi model/provider.

Tuy nhiên, không nên phụ thuộc hoàn toàn vào một Aggregator. Đối với hệ thống Production lâu dài, kiến trúc tốt hơn là xây dựng một AI Abstraction Layer trong ứng dụng:

                    AI Service
                        ↓
                 Model Abstraction
                        ↓
              ┌─────────┴─────────┐
              ↓                   ↓
        Direct Provider       Aggregator
              ↓                   ↓
          DeepSeek           Gemini/Other

Nhờ đó hệ thống có thể chuyển đổi giữa Direct API và Aggregator mà không ảnh hưởng đến business logic. Đây là hướng tiếp cận cân bằng tốt giữa chi phí, tính linh hoạt, khả năng mở rộng và khả năng phục hồi.