## 1. File `build.gradle`

Ví dụ sử dụng **Gradle + Spring Boot + Spring AI BOM**:

```gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.4.0'
    id 'io.spring.dependency-management' version '1.1.6'
}

group = 'com.example'
version = '0.0.1-SNAPSHOT'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

ext {
    set('springAiVersion', "1.0.0")
}

dependencyManagement {
    imports {
        mavenBom "org.springframework.ai:spring-ai-bom:${springAiVersion}"
    }
}

dependencies {

    // Spring Boot Web
    implementation 'org.springframework.boot:spring-boot-starter-web'

    // Spring AI Ollama Starter (Local AI)
    implementation 'org.springframework.ai:spring-ai-starter-model-ollama'

    // Spring AI OpenAI Starter (Dùng cho OpenRouter Cloud)
    implementation 'org.springframework.ai:spring-ai-starter-model-openai'

    // Lombok (tuỳ chọn)
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'

    // Test
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

---

# 2. File `application-local.properties`

Cấu hình chạy AI local bằng Ollama:

```properties
spring.application.name=hybrid-ai-runtime


# Ollama local configuration
spring.ai.ollama.base-url=http://localhost:11434

spring.ai.ollama.chat.options.model=qwen2.5-coder:7b

spring.ai.ollama.chat.options.temperature=0.7
```

Giải thích:

* Ollama mặc định chạy tại:

```text
http://localhost:11434
```

* Model sử dụng:

```text
qwen2.5-coder:7b
```

Kiểm tra model:

```bash
ollama list
```

Nếu chưa có:

```bash
ollama pull qwen2.5-coder:7b
```

---

# 3. File `application-cloud.properties`

Cấu hình OpenRouter thông qua chuẩn OpenAI API:

```properties
spring.application.name=hybrid-ai-runtime


# OpenRouter API configuration
spring.ai.openai.base-url=https://openrouter.ai/api

spring.ai.openai.api-key=${OPENROUTER_API_KEY}

spring.ai.openai.chat.options.model=google/gemini-2.5-flash

spring.ai.openai.chat.options.temperature=0.7
```

Trong đó:

```properties
${OPENROUTER_API_KEY}
```

không lưu trực tiếp API Key trong source code.

API Key được lấy từ biến môi trường:

```bash
OPENROUTER_API_KEY=your_api_key
```

Điều này giúp:

* Không lộ khóa API trên GitHub.
* Dễ triển khai Docker/Cloud.
* Có thể thay đổi key mà không sửa code.

---

# 4. File `application.properties`

Cấu hình profile mặc định:

```properties
spring.application.name=hybrid-ai-runtime

spring.profiles.active=local
```

Khi chạy mặc định:

```text
application.properties
        |
        |
        +--> application-local.properties
```

Ứng dụng sẽ kết nối:

```text
Spring Boot
      |
      |
      Ollama localhost:11434
      |
      |
      qwen2.5-coder:7b
```

---

# 5. Chạy ứng dụng với profile Cloud

## Bước 1: Thiết lập API Key

Linux/Mac:

```bash
export OPENROUTER_API_KEY="your_api_key"
```

Windows PowerShell:

```powershell
$env:OPENROUTER_API_KEY="your_api_key"
```

---

## Bước 2: Chạy Spring Boot bằng profile cloud

Dùng Gradle:

```bash
./gradlew bootRun --args='--spring.profiles.active=cloud'
```

Windows:

```cmd
gradlew bootRun --args="--spring.profiles.active=cloud"
```

---

Khi chạy, Spring Boot sẽ ưu tiên:

```text
application.properties

        ↓

application-cloud.properties
```

và thay đổi provider:

Từ:

```
Ollama Local
localhost:11434
qwen2.5-coder:7b
```

sang:

```
OpenRouter Cloud
google/gemini-2.5-flash
```

---

## Mô hình Hybrid AI Runtime

```
                  Spring Boot
                       |
                    Spring AI
                       |
        --------------------------------
        |                              |
 application-local            application-cloud
        |                              |
        ↓                              ↓
 Ollama Runtime              OpenAI Compatible API
        |                              |
 qwen2.5-coder:7b          OpenRouter
                               |
                         Gemini 2.5 Flash
```

Nhờ cơ chế **Spring Profile**, cùng một code Java có thể chuyển đổi giữa AI chạy local và AI cloud mà không cần sửa source code.
