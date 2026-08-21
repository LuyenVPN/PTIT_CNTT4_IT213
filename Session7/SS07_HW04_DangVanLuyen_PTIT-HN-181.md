# BÀI 4: Tối ưu Prompt - Bóc tách dữ liệu khiếu nại khách hàng chống ảo tưởng

## Prompt đã thiết kế

```text
# ROLE

Bạn là AI chuyên gia phân tích và trích xuất dữ liệu khiếu nại khách hàng cho hệ thống CRM.

Nhiệm vụ của bạn là phân tích nội dung khiếu nại tự do của khách hàng và chuyển đổi thành dữ liệu có cấu trúc theo đúng schema được cung cấp bởi hệ thống.

Bạn phải ưu tiên tính chính xác của dữ liệu, không được tự bịa thông tin và phải tuân thủ nghiêm ngặt các quy tắc bên dưới.

# INPUT

Nội dung khiếu nại của khách hàng:

Đơn hàng công nghệ mã #APP-9981 giao chậm quá. Tôi đặt từ thứ Hai mà tới giờ là thứ Sáu rồi vẫn chưa thấy tăm hơi đâu.
Lúc mua nhân viên cam kết giao trong vòng 24 giờ. Trễ hẹn tới tận 4 ngày rồi! Tôi yêu cầu kiểm tra ngay,
nếu do lỗi vận chuyển thì hoàn tiền lại cho tôi.

# OUTPUT SCHEMA

Hãy trả về một JSON object phù hợp chính xác với cấu trúc:

{
  "orderCode": "string",
  "issueType": "LATE_DELIVERY | DAMAGED_GOODS | WRONG_ITEM | REFUND_REQUEST | OTHER",
  "delayHours": "integer",
  "customerDemand": "string",
  "requiresUrgentEscalation": "boolean"
}

# EXTRACTION RULES

## 1. orderCode

Trích xuất chính xác mã đơn hàng xuất hiện trong nội dung khiếu nại.

Trong trường hợp này:

orderCode = "APP-9981"

Không tự tạo hoặc thay đổi mã đơn hàng.

## 2. issueType

Chuẩn hóa loại sự cố về đúng một trong năm giá trị sau:

- LATE_DELIVERY
- DAMAGED_GOODS
- WRONG_ITEM
- REFUND_REQUEST
- OTHER

Quy tắc ưu tiên:

- Nếu vấn đề chính của khiếu nại là giao hàng trễ → LATE_DELIVERY.
- Nếu hàng hóa bị hư hỏng → DAMAGED_GOODS.
- Nếu giao sai sản phẩm → WRONG_ITEM.
- Nếu vấn đề chính là yêu cầu hoàn tiền → REFUND_REQUEST.
- Nếu không thuộc các trường hợp trên → OTHER.

Trong trường hợp có nhiều thông tin, hãy xác định vấn đề chính mà khách hàng đang khiếu nại.

Trong dữ liệu hiện tại, vấn đề chính là giao hàng chậm, vì vậy:

issueType = "LATE_DELIVERY"

Lưu ý: Việc khách hàng đề cập đến "hoàn tiền" là yêu cầu xử lý, không làm thay đổi issueType chính nếu nguyên nhân khiếu nại vẫn là giao hàng trễ.

## 3. delayHours

Chuyển thời gian giao hàng bị trễ sang số giờ.

Quy tắc:

- 1 ngày = 24 giờ.
- 2 ngày = 48 giờ.
- 3 ngày = 72 giờ.
- 4 ngày = 96 giờ.

Cụm từ "Trễ hẹn tới tận 4 ngày rồi" phải được chuyển thành:

delayHours = 96

Không trả về chuỗi "4 ngày".

Nếu có cả thời gian cam kết và thời gian thực tế, ưu tiên sử dụng thông tin trễ được khách hàng mô tả trực tiếp.

## 4. customerDemand

Trích xuất yêu cầu mà khách hàng muốn doanh nghiệp thực hiện.

Không được biến yêu cầu có điều kiện thành một yêu cầu chắc chắn.

Trong trường hợp này, khách hàng:

- Yêu cầu kiểm tra ngay.
- Nếu nguyên nhân do lỗi vận chuyển thì yêu cầu hoàn tiền.

Do đó customerDemand phải phản ánh đầy đủ hai nội dung trên.

## 5. requiresUrgentEscalation

Giá trị phải là boolean true hoặc false.

Trả về true nếu:

- Khách hàng yêu cầu hoàn tiền; hoặc
- Khách hàng thể hiện thái độ rất bức xúc, gay gắt hoặc có dấu hiệu cần xử lý khẩn cấp.

Trả về false nếu không có các dấu hiệu trên.

Trong trường hợp này, khách hàng yêu cầu hoàn tiền nếu lỗi do vận chuyển và thể hiện sự bức xúc vì đã trễ 4 ngày.

Do đó:

requiresUrgentEscalation = true

# ANTI-HALLUCINATION RULES

1. Chỉ sử dụng thông tin có trong nội dung khiếu nại.
2. Không tự tạo mã đơn hàng, thời gian, nguyên nhân hoặc yêu cầu không được đề cập.
3. Không suy diễn rằng lỗi chắc chắn thuộc về đơn vị vận chuyển. Khách hàng chỉ nói "nếu do lỗi vận chuyển".
4. Không biến yêu cầu có điều kiện thành kết luận chắc chắn.
5. Không thêm bất kỳ field nào ngoài schema được yêu cầu.
6. issueType phải sử dụng chính xác một trong năm giá trị được cho phép.
7. delayHours phải là số nguyên, không phải chuỗi.
8. requiresUrgentEscalation phải là boolean true hoặc false.
9. Chỉ trả về JSON hợp lệ theo Format Instructions được cung cấp bởi BeanOutputConverter.
10. Không thêm Markdown code fence, lời giải thích hoặc văn bản bên ngoài JSON.
```

## Cấu trúc JSON Schema / Format Instructions giả định

`BeanOutputConverter` sẽ cung cấp Format Instructions cho AI dựa trên Java Record:

```java
public record ComplaintExtraction(
    String orderCode,
    String issueType,
    int delayHours,
    String customerDemand,
    boolean requiresUrgentEscalation
) {}
```

AI phải tạo JSON có đúng 5 field:

```text
orderCode                  → String
issueType                  → String
delayHours                 → Integer
customerDemand             → String
requiresUrgentEscalation   → Boolean
```

Trong đó `issueType` chỉ được phép nhận:

```text
LATE_DELIVERY
DAMAGED_GOODS
WRONG_ITEM
REFUND_REQUEST
OTHER
```

Format Instructions của `BeanOutputConverter` cần được chèn vào prompt thực tế, ví dụ:

```text
# FORMAT INSTRUCTIONS

{format_instructions}
```

Trong Spring AI, có thể xây dựng prompt theo hướng:

```java
BeanOutputConverter<ComplaintExtraction> converter =
        new BeanOutputConverter<>(ComplaintExtraction.class);

String formatInstructions = converter.getFormatInstructions();

String prompt = """
    ...
    
    # FORMAT INSTRUCTIONS
    %s
    """.formatted(formatInstructions);
```

Sau khi AI trả về JSON, `BeanOutputConverter` sẽ chịu trách nhiệm chuyển JSON đó thành:

```java
ComplaintExtraction
```

## Kết quả JSON AI sinh ra khớp với yêu cầu

```json
{
  "orderCode": "APP-9981",
  "issueType": "LATE_DELIVERY",
  "delayHours": 96,
  "customerDemand": "Yêu cầu kiểm tra ngay; nếu nguyên nhân do lỗi vận chuyển thì hoàn tiền.",
  "requiresUrgentEscalation": true
}
```

## Giải thích các bẫy dữ liệu đã được xử lý

### Bẫy 1: Tính toán thời gian trễ

Khách hàng nói:

> "Trễ hẹn tới tận 4 ngày rồi!"

Prompt quy định:

`1 ngày = 24 giờ`

Do đó:

`4 × 24 = 96 giờ`

Kết quả:

```text
delayHours = 96
```

Điều này giúp dữ liệu đầu ra có dạng số nguyên và dễ dàng sử dụng cho việc phân loại, thống kê hoặc thiết lập rule trong hệ thống CRM.

### Bẫy 2: Phân loại issueType

Mặc dù khách hàng có đề cập đến "hoàn tiền", nguyên nhân chính của khiếu nại vẫn là **giao hàng chậm**.

Vì vậy:

```text
issueType = LATE_DELIVERY
```

Không chọn `REFUND_REQUEST` vì hoàn tiền chỉ là yêu cầu có điều kiện:

> "nếu do lỗi vận chuyển thì hoàn tiền lại cho tôi."

Điều này cho thấy prompt cần phân biệt **loại sự cố** và **yêu cầu của khách hàng**.

### Bẫy 3: Đánh giá độ khẩn cấp

Khách hàng vừa yêu cầu kiểm tra ngay, vừa đề cập đến việc hoàn tiền và thể hiện sự bức xúc vì đơn hàng đã trễ 4 ngày.

Theo rule:

```text
requiresUrgentEscalation = true
```

### Bẫy 4: Không tự suy diễn nguyên nhân

Một điểm quan trọng là AI **không được kết luận rằng lỗi chắc chắn do đơn vị vận chuyển**.

Khách hàng chỉ nói:

> "nếu do lỗi vận chuyển thì hoàn tiền lại cho tôi."

Do đó:

- Không được ghi `shipping_error = true`.
- Không được khẳng định đơn vị vận chuyển có lỗi.
- Chỉ ghi nhận yêu cầu hoàn tiền có điều kiện.

Đây là một constraint quan trọng để ngăn AI tạo ra thông tin không có trong dữ liệu gốc.

## Kết luận

Prompt được thiết kế theo hướng **Role + Structured Output + Explicit Rules + Anti-Hallucination Constraints**.

Luồng xử lý:

```text
Customer Complaint
       ↓
Extract orderCode
       ↓
Classify issueType
       ↓
Calculate delayHours
       ↓
Extract customerDemand
       ↓
Evaluate urgent escalation
       ↓
Validate JSON Schema
       ↓
BeanOutputConverter
       ↓
ComplaintExtraction Record
```

Kết quả cuối cùng:

```json
{
  "orderCode": "APP-9981",
  "issueType": "LATE_DELIVERY",
  "delayHours": 96,
  "customerDemand": "Yêu cầu kiểm tra ngay; nếu nguyên nhân do lỗi vận chuyển thì hoàn tiền.",
  "requiresUrgentEscalation": true
}
```

**Đáp án cốt lõi:** Prompt phải tách biệt rõ **nguyên nhân/sự cố (`issueType`)**, **yêu cầu khách hàng (`customerDemand`)**, **dữ liệu tính toán (`delayHours`)** và **mức độ khẩn cấp (`requiresUrgentEscalation`)**, đồng thời cấm AI suy diễn các thông tin không xuất hiện trong nội dung khiếu nại.