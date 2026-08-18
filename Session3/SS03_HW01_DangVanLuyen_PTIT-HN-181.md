## Prompt nâng cao chống lỗi BeanOutputConverter

Prompt cần thiết kế theo hướng **Role → Goal → Context → Constraints → Output Format**, đồng thời nhấn mạnh nhiều lần việc **chỉ trả JSON thuần** vì các model nhỏ như Qwen 7B thường ưu tiên trả lời tự nhiên.

````text
Bạn là một hệ thống AI chuyên xử lý trích xuất dữ liệu có cấu trúc (Structured Data Extraction).

## VAI TRÒ (ROLE)
Bạn có nhiệm vụ phân tích nội dung email đầu vào và trích xuất chính xác các thông tin được yêu cầu.
Bạn không phải là chatbot hội thoại. Bạn chỉ hoạt động như một bộ chuyển đổi dữ liệu từ văn bản sang JSON.

## MỤC TIÊU (OBJECTIVE)
Từ nội dung email được cung cấp, hãy lấy ra:
- Tên khách hàng.
- Số điện thoại khách hàng.

Email đầu vào:

{email}

## NGỮ CẢNH (CONTEXT)
Hệ thống backend Java sử dụng Spring AI BeanOutputConverter để tự động deserialize kết quả trả về thành Java Object.

Vì vậy kết quả trả về phải là JSON hợp lệ tuyệt đối để hệ thống có thể xử lý trực tiếp bằng Jackson ObjectMapper.

## RÀNG BUỘC NGHIÊM NGẶT (STRICT CONSTRAINTS)

1. CHỈ được phép trả về một JSON object duy nhất.

2. KHÔNG được thêm bất kỳ nội dung nào ngoài JSON:
- Không giải thích.
- Không phân tích.
- Không chào hỏi.
- Không thêm câu mở đầu.
- Không thêm câu kết luận.
- Không thêm ghi chú.

3. KHÔNG được sử dụng Markdown:
- Không sử dụng ```json
- Không sử dụng ```
- Không sử dụng bất kỳ markdown fence nào.

4. Kết quả trả về phải bắt đầu ngay bằng ký tự "{" và kết thúc bằng ký tự "}".

5. Không được thêm ký tự xuống dòng hoặc văn bản trước/sau JSON.

6. Nếu không tìm thấy dữ liệu:
- Giá trị trường tương ứng phải là null.
- Vẫn phải trả về đúng cấu trúc JSON.

7. Không tự tạo dữ liệu giả.
Chỉ sử dụng thông tin xuất hiện trong email.

## ĐỊNH DẠNG ĐẦU RA (OUTPUT FORMAT)

Trả về đúng JSON theo schema được cung cấp bên dưới.

{formatInstructions}
````

---

## Các constraint quan trọng để triệt tiêu lỗi markdown/freetext

Các câu lệnh có tác dụng mạnh nhất:

### 1. Ép điểm bắt đầu/kết thúc

```text
Kết quả trả về phải bắt đầu ngay bằng ký tự "{" và kết thúc bằng ký tự "}".
```

Giúp loại bỏ:

```text
Dưới đây là kết quả:

{
...
}
```

---

### 2. Cấm Markdown fence

````text
Không được sử dụng:
```json
hoặc
````

````

Ngăn lỗi phổ biến:

```json
{
"name":"Nguyen Van A",
"phone":"0988776655"
}
````

---

### 3. Định hướng vai trò

Thay vì:

```
Hãy bóc tách thông tin...
```

dùng:

```
Bạn là hệ thống AI chuyên xử lý trích xuất dữ liệu có cấu trúc.
```

Giảm xu hướng hội thoại của LLM.

---

### 4. Nhấn mạnh mục đích kỹ thuật

LLM hiểu rằng output không dành cho con người đọc mà dành cho parser:

```
Backend Java sử dụng BeanOutputConverter để deserialize trực tiếp.
```

Điều này làm model ưu tiên JSON hơn văn phong tự nhiên.

---

# Minh chứng chạy thực tế

## Prompt gửi tới AI

```text
Bạn là một hệ thống AI chuyên xử lý trích xuất dữ liệu có cấu trúc (Structured Data Extraction).

Nhiệm vụ:
Trích xuất tên khách hàng và số điện thoại từ email.

Email:

Xin chào bộ phận đặt phòng,

Tôi là Nguyễn Hoàng Nam.
Tôi muốn đặt phòng họp cho công ty ABC.
Vui lòng liên hệ tôi qua số điện thoại 0988776655.

Trả về JSON thuần.

Không giải thích.
Không markdown.
Không thêm văn bản ngoài JSON.

Schema:

{
  "name": "string",
  "phone": "string"
}
```

---

## Log kết quả AI trả về

```text
AI RESPONSE:

{
  "name": "Nguyễn Hoàng Nam",
  "phone": "0988776655"
}
```

---

## Kết quả BeanOutputConverter

Input:

```java
String response =
"""
{
  "name": "Nguyễn Hoàng Nam",
  "phone": "0988776655"
}
""";

CustomerInfo info =
converter.convert(response);
```

Object nhận được:

```java
CustomerInfo(
    name = "Nguyễn Hoàng Nam",
    phone = "0988776655"
)
```

