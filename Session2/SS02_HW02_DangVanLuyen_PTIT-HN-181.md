### 1. Cơ chế Tokenization và đặc thù xử lý Tiếng Việt

**Tokenization** là quá trình mà mô hình ngôn ngữ lớn (LLM) sử dụng để chia nhỏ văn bản đầu vào thành các đơn vị gọi là **token** trước khi xử lý. Token không hoàn toàn tương đương với một từ; nó có thể là:

* Một ký tự.
* Một phần của từ.
* Một từ hoàn chỉnh.
* Một nhóm ký tự phổ biến.

Ví dụ:

```
"Hello world"
```

Có thể được tokenizer chia thành:

```
["Hello", " world"]
```

Trong khi một từ dài hoặc ít phổ biến có thể bị tách thành nhiều token hơn.

Các mô hình như **Llama, Qwen, GPT** sử dụng những bộ tokenizer dựa trên các thuật toán như **BPE (Byte Pair Encoding)** hoặc các biến thể của nó. Tokenizer xây dựng một bộ từ vựng (vocabulary) gồm các chuỗi ký tự xuất hiện thường xuyên trong dữ liệu huấn luyện. Khi gặp văn bản mới, tokenizer tìm cách biểu diễn văn bản bằng số lượng token ít nhất có thể.

---

### Vì sao Tiếng Việt thường tốn nhiều token hơn Tiếng Anh?

Tiếng Việt có một số đặc điểm khiến quá trình tokenization kém hiệu quả hơn so với Tiếng Anh:

#### a. Đặc điểm dấu và ký tự Unicode

Tiếng Việt sử dụng nhiều ký tự có dấu:

Ví dụ:

```
học viên phát triển hệ thống
```

Các ký tự như:

* ọ
* ế
* ệ
* ố

có thể làm tokenizer phải chia nhỏ hơn so với các ký tự Latin thông thường trong Tiếng Anh.

---

#### b. Từ Tiếng Việt thường được ghép bởi nhiều âm tiết

Trong Tiếng Anh:

```
computer
```

có thể được lưu trong vocabulary như một token hoặc vài token.

Nhưng Tiếng Việt:

```
máy tính
```

gồm hai âm tiết có khoảng trắng, tokenizer có thể xử lý thành:

```
["máy", " tính"]
```

hoặc thậm chí:

```
["m", "áy", " t", "ính"]
```

nếu cụm đó không phổ biến trong dữ liệu huấn luyện.

---

#### c. Dữ liệu huấn luyện Tiếng Anh chiếm ưu thế

Phần lớn dữ liệu huấn luyện LLM hiện nay là Tiếng Anh. Vì vậy tokenizer thường có nhiều token đại diện cho các từ/cụm từ Tiếng Anh phổ biến.

Ví dụ:

Tiếng Anh:

```
artificial intelligence
```

có thể chỉ mất vài token.

Trong khi:

```
trí tuệ nhân tạo
```

có thể cần nhiều token hơn vì ít xuất hiện hơn trong vocabulary.

---

Vì vậy cùng một lượng thông tin, văn bản Tiếng Việt thường có tỷ lệ:

```
1 từ Tiếng Việt ≈ 1.3 - 1.8 tokens
```

trong nhiều trường hợp, trong khi Tiếng Anh thường gần:

```
1 từ Tiếng Anh ≈ 1 - 1.3 tokens
```

Do đó tài liệu 8.000 từ Tiếng Việt có thể tăng lên khoảng 12.000 tokens như đề bài đưa ra.

---

# 2. Phân tích lỗi tràn Context Window khi gửi tài liệu

## Khái niệm Context Window

**Context Window** là giới hạn số lượng token tối đa mà mô hình có thể nhận và xử lý trong một lần request.

Context Window bao gồm toàn bộ:

* Nội dung tài liệu gửi vào.
* Prompt của người dùng.
* System prompt.
* Lịch sử hội thoại (nếu có).
* Phần output mà mô hình dự kiến sinh ra.

Công thức đơn giản:

```
Tổng token sử dụng =
Input tokens + Output tokens
```

---

## Thông số trong bài toán

Tài liệu:

```
8.000 từ Tiếng Việt
≈ 12.000 tokens
```

Mô hình:

```
Qwen2.5-Coder:7B
Context Window = 8.192 tokens
```

Request gửi lên:

```
12.000 tokens tài liệu
+
Prompt yêu cầu tóm tắt
```

Như vậy:

```
12.000 > 8.192
```

Input đã vượt quá giới hạn context window.

---

## Hiện tượng xảy ra khi gửi request đến Ollama

Khi gửi toàn bộ tài liệu, Ollama/model có thể xảy ra các trường hợp:

### Trường hợp 1: Báo lỗi vượt Context Window

Model có thể trả về lỗi dạng:

```
context length exceeded
```

hoặc:

```
input length exceeds maximum context length
```

Nguyên nhân:

Số lượng token đầu vào lớn hơn khả năng lưu trữ của vùng context.

---

### Trường hợp 2: Tự cắt bớt nội dung đầu vào

Một số hệ thống có cơ chế truncate:

Ví dụ:

```
Tài liệu ban đầu:
12.000 tokens

Context cho phép:
8.192 tokens

=> Cắt bỏ khoảng 3.800 tokens
```

Hậu quả:

* Một phần nội dung tài liệu bị mất.
* Các đoạn quan trọng có thể bị loại bỏ.
* Tóm tắt không còn đầy đủ.

---

### Trường hợp 3: Chất lượng trả lời giảm mạnh

Ngay cả khi hệ thống cố xử lý bằng cách nén dữ liệu, mô hình có thể:

* Bỏ sót thông tin quan trọng.
* Hiểu sai mối liên hệ giữa các phần tài liệu.
* Tạo bản tóm tắt thiếu chính xác.

Nguyên nhân là LLM chỉ có thể "nhìn thấy" dữ liệu nằm trong context window hiện tại.

---

# 3. Giải pháp khắc phục giới hạn Context Window

## Giải pháp 1: Chunking + MapReduce Summarization

Đây là phương pháp phổ biến trong các hệ thống RAG và xử lý tài liệu dài.

### Bước 1: Chia nhỏ tài liệu (Chunking)

Thay vì gửi:

```
12.000 tokens
```

một lần, chia thành nhiều đoạn:

Ví dụ:

```
Chunk 1: 2.000 tokens
Chunk 2: 2.000 tokens
Chunk 3: 2.000 tokens
Chunk 4: 2.000 tokens
Chunk 5: 2.000 tokens
Chunk 6: 2.000 tokens
```

Mỗi chunk nằm trong giới hạn 8.192 tokens.

---

### Bước 2: Map - Tóm tắt từng phần

Gửi từng chunk cho model:

```
Chunk 1 → Summary 1

Chunk 2 → Summary 2

Chunk 3 → Summary 3
```

---

### Bước 3: Reduce - Tổng hợp

Sau khi có các bản tóm tắt nhỏ:

```
Summary 1
Summary 2
Summary 3
...
```

Tiếp tục gửi cho model để tạo:

```
Final Summary
```

---

Ưu điểm:

* Xử lý được tài liệu rất dài.
* Không cần đổi model.
* Tiết kiệm RAM GPU.

Nhược điểm:

* Có thể mất một số liên kết giữa các phần xa nhau trong tài liệu.

---

# Giải pháp 2: Tăng Context Window trong Ollama bằng Modelfile

Ollama cho phép tùy chỉnh model thông qua **Modelfile**.

Ví dụ:

```
FROM qwen2.5-coder:7b

PARAMETER num_ctx 16384
```

Sau đó tạo model mới:

```
ollama create qwen-custom -f Modelfile
```

Model mới sẽ có:

```
Context Window = 16.384 tokens
```

Khi đó:

```
12.000 tokens tài liệu
+
Prompt
```

có thể vừa nằm trong giới hạn.

---

Tuy nhiên cần lưu ý:

Tăng context window không chỉ là thay đổi thông số. Nó làm tăng:

* RAM sử dụng.
* VRAM GPU.
* Thời gian suy luận.

Ví dụ:

```
8K context → 16K context
```

có thể yêu cầu lượng bộ nhớ lớn hơn đáng kể.

Nếu máy chạy local yếu, model có thể:

* Chạy chậm hơn.
* Bị thiếu bộ nhớ.
* Tự giảm hiệu năng.

---

# Kết luận

Trong bài toán này, nguyên nhân chính là sự khác biệt giữa **kích thước tài liệu theo số từ** và **kích thước thực tế theo token**. Tài liệu Tiếng Việt 8.000 từ sau tokenization trở thành khoảng 12.000 tokens, vượt quá Context Window 8.192 tokens của Qwen2.5-Coder:7B trên Ollama.

Khi gửi toàn bộ tài liệu trong một request, hệ thống có thể gặp lỗi vượt context hoặc phải cắt bớt dữ liệu dẫn đến bản tóm tắt thiếu chính xác.

Hai hướng xử lý phù hợp:

1. **Chunking + MapReduce**: chia nhỏ tài liệu, xử lý từng phần rồi tổng hợp kết quả.
2. **Tăng Context Window trong Ollama bằng Modelfile**: cấu hình `num_ctx` lớn hơn nếu phần cứng đáp ứng được.

Trong thực tế triển khai chatbot tài liệu kỹ thuật, phương án kết hợp **Chunking + Retrieval (RAG)** thường được ưu tiên vì có khả năng mở rộng tốt hơn so với chỉ tăng Context Window.
