# BÀI 3: Đọc hiểu & Dò lỗi - Khắc phục lỗi chia nhỏ văn bản sai cách (Bad Chunking Strategy)

## Phân tích lỗi sai & Hiện tượng

Cấu hình hiện tại:

`TokenTextSplitter(100, 0, 1, 1000, true)`

Lỗi cốt lõi nằm ở việc **`chunkSize = 100` quá nhỏ và `overlap = 0`**.

### 1. Chunk Size = 100 quá nhỏ

Tài liệu CRM Guideline dài khoảng 10.000 từ nhưng lại được chia thành các chunk rất nhỏ chỉ khoảng 100 tokens.

Điều này khiến một quy trình nghiệp vụ có thể bị chia thành nhiều chunk khác nhau.

Ví dụ, tài liệu gốc:

    Nếu thiết bị bị sọc màn hình trong vòng 7 ngày kể từ ngày mua,
    khách hàng được đổi mới sản phẩm nếu đáp ứng đầy đủ điều kiện bảo hành.
    Sau 7 ngày, sản phẩm sẽ được chuyển sang quy trình sửa chữa...

Có thể bị chia thành:

    Chunk 1:
    Nếu thiết bị bị sọc màn hình trong vòng 7 ngày kể từ ngày mua...

    Chunk 2:
    khách hàng được đổi mới sản phẩm nếu đáp ứng đầy đủ điều kiện bảo hành...

    Chunk 3:
    Sau 7 ngày, sản phẩm sẽ được chuyển sang quy trình sửa chữa...

Khi người dùng hỏi:

> "Nếu thiết bị bị sọc màn hình sau 7 ngày mua thì được đổi mới hay sửa chữa?"

Thông tin cần thiết để trả lời có thể nằm ở nhiều chunk khác nhau. Nếu quá trình retrieval chỉ lấy một số chunk có điểm similarity cao nhất, LLM có thể chỉ nhận được một phần thông tin và đưa ra câu trả lời sai hoặc kết luận rằng tài liệu không đề cập.

### 2. Overlap = 0 là vấn đề nghiêm trọng

`overlap = 0` nghĩa là **hai chunk hoàn toàn không có phần nội dung chồng lấn**.

Khi một câu hoặc một quy tắc nghiệp vụ nằm ngay tại ranh giới giữa hai chunk, ngữ nghĩa của câu có thể bị tách ra.

Ví dụ:

    Chunk 1:
    Thiết bị bị sọc màn hình trong vòng 7 ngày kể từ ngày mua sẽ được

    Chunk 2:
    đổi mới nếu đáp ứng đầy đủ điều kiện bảo hành.

Nếu hệ thống chỉ retrieve Chunk 1 thì LLM không biết hành động tiếp theo là **"đổi mới"**.

Nếu chỉ retrieve Chunk 2 thì LLM không biết **điều kiện áp dụng** là thiết bị bị sọc màn hình trong vòng 7 ngày.

Đây chính là hiện tượng **Semantic Loss tại chunk boundary**.

---

## Giải pháp cấu hình Chunking tối ưu

Đối với tài liệu CRM Guideline, nên sử dụng chunk có kích thước vừa đủ lớn để giữ trọn một đoạn quy trình nghiệp vụ và có overlap để bảo toàn ngữ cảnh giữa các chunk.

Một cấu hình đề xuất:

    TokenTextSplitter(
        800,   // chunkSize
        200,   // overlap
        5,
        1000,
        true
    );

### Lựa chọn `chunkSize = 800`

`800 tokens` phù hợp hơn `100 tokens` vì:

- Giữ được nhiều thông tin trong cùng một chunk.
- Có khả năng chứa trọn một quy trình hoặc một đoạn hướng dẫn nghiệp vụ.
- Giảm khả năng một câu hỏi nghiệp vụ bị phân tán thành quá nhiều chunk.
- Vẫn đủ nhỏ để embedding và retrieval tập trung vào nội dung liên quan.

### Lựa chọn `overlap = 200`

`200 tokens` tương đương khoảng 25% chunk size.

Overlap giúp nội dung ở cuối chunk trước được lặp lại ở đầu chunk sau.

Ví dụ:

    Chunk 1:
    [---------------- 800 tokens ----------------]
                             ↓
                        200 tokens
                             ↓
    Chunk 2:
                 [200 tokens][--------- 600 tokens ---------]

Nhờ đó, nếu một quy tắc nghiệp vụ nằm tại ranh giới chunk, chunk tiếp theo vẫn giữ lại một phần ngữ cảnh của chunk trước.

Điều này đặc biệt quan trọng với các câu hỏi về:

- Điều kiện bảo hành.
- Thời hạn đổi trả.
- Điều kiện áp dụng.
- Quy trình xử lý.
- Ngoại lệ.
- Các bước thực hiện.

### Chiến lược phân mảnh tài liệu

Không nên chỉ dựa vào việc cắt tài liệu theo số token. Với tài liệu Markdown có cấu trúc rõ ràng, nên ưu tiên giữ nguyên cấu trúc:

    # Tiêu đề
    ## Quy trình bảo hành
    ### Điều kiện đổi mới
    ### Điều kiện sửa chữa
    ### Các trường hợp ngoại lệ

Mục tiêu là tránh chia cắt một quy trình nghiệp vụ ở giữa câu hoặc giữa các bước xử lý.

Chiến lược đề xuất:

    Markdown Document
           ↓
    Giữ cấu trúc heading / section
           ↓
    Chunk khoảng 800 tokens
           ↓
    Overlap khoảng 200 tokens
           ↓
    Embedding
           ↓
    Vector Store

---

## Mã nguồn Java đề xuất sau khi refactor

    @Service
    public class DocumentIngestionService {

        @Autowired
        private VectorStore vectorStore;

        public void ingest(Resource resource) {

            // Đọc tài liệu Markdown
            MarkdownDocumentReader reader =
                    new MarkdownDocumentReader(resource);

            List<Document> rawDocs = reader.read();

            // Chia nhỏ tài liệu với kích thước phù hợp
            // và có overlap để bảo toàn ngữ cảnh tại ranh giới chunk.
            TokenTextSplitter splitter = new TokenTextSplitter(
                    800,   // chunkSize: 800 tokens
                    200,   // overlap: 200 tokens
                    5,
                    1000,
                    true
            );

            List<Document> splitDocs = splitter.apply(rawDocs);

            // Lưu các chunk đã embedding vào Vector Store
            vectorStore.accept(splitDocs);
        }
    }

---

## Kết luận

Cấu hình ban đầu:

`TokenTextSplitter(100, 0, 1, 1000, true)`

không phù hợp với tài liệu CRM Guideline vì:

- `chunkSize = 100` quá nhỏ.
- `overlap = 0` làm mất ngữ cảnh giữa các chunk.
- Quy trình nghiệp vụ có thể bị chia cắt tại ranh giới chunk.
- Retrieval có thể chỉ lấy được một phần thông tin cần thiết.

Cấu hình đề xuất:

`TokenTextSplitter(800, 200, 5, 1000, true)`

Trong đó:

| Tham số | Giá trị | Mục đích |
|---|---:|---|
| `chunkSize` | **800** | Giữ đủ ngữ cảnh cho một quy trình nghiệp vụ |
| `overlap` | **200** | Bảo toàn thông tin tại ranh giới chunk |
| `minChunkSizeChars` | **5** | Loại bỏ các chunk quá nhỏ |
| `maxNumChunks` | **1000** | Giới hạn số chunk được tạo |
| `keepSeparator` | **true** | Giữ separator khi chia văn bản |

**Đáp án cốt lõi:** `chunkSize` không nên quá nhỏ và đặc biệt **không nên để `overlap = 0` đối với tài liệu nghiệp vụ có các quy trình liên kết với nhau**. Việc sử dụng chunk khoảng **800 tokens với overlap 200 tokens** giúp giảm Semantic Loss và cải thiện khả năng Retrieval/RAG của CRM Ticket Assistant.