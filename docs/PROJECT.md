# ClipSearch Technical Documentation

## 🏗 System Architecture

ClipSearch is a distributed system consisting of several microservices coordinated via asynchronous messaging.

### 🧩 Components

1.  **Frontend (`/frontend`)**
    *   **Technology:** Angular
    *   **Role:** User interface for file management and searching.
    *   **Config:** Managed via `frontend-config.json` and `public/config.json`.

2.  **API Service (`/backend/api`)**
    *   **Technology:** Quarkus (Java)
    *   **Responsibilities:**
        *   Manages file uploads to S3.
        *   Generates pre-signed URLs for downloads.
        *   Proxies search requests to Elasticsearch.
        *   Handles metadata storage and retrieval.

3.  **Worker Service (`/backend/worker`)**
    *   **Technology:** Quarkus (Java)
    *   **Responsibilities:**
        *   Listens for SQS events (triggered by S3 uploads).
        *   **OCR:** Tesseract for images.
        *   **Text Extraction:** Apache Tika for PDFs and TXT files.
        *   **AI Summarization:** Communicates with Ollama (Phi-3) for document summaries.
        *   **Indexing:** Pushes processed content to Elasticsearch.

4.  **Infrastrucure**
    *   **LocalStack:** Provides S3 and SQS in local development.
    *   **Elasticsearch:** Stores the document index.
    *   **Ollama:** Local LLM engine.

## 🔄 Data Flow

1.  **Upload:** User uploads a file via Frontend -> API -> S3 (LocalStack).
2.  **Notification:** S3 triggers an event -> SQS Queue.
3.  **Processing:** Worker picks up message from SQS -> Downloads file from S3.
4.  **Analysis:** Worker runs OCR/Tika -> Worker calls Ollama for summary.
5.  **Indexing:** Worker sends text and summary to Elasticsearch.
6.  **Search:** User searches via Frontend -> API -> Elasticsearch.

## 🚀 CI/CD Pipeline

The project uses GitHub Actions (`.github/workflows/ci.yml`) to:
1.  Build Java components with Maven.
2.  Build Docker images for API, Worker, and Frontend.
3.  Push images to `ghcr.io/dawidbera/clipsearch-*`.

## 🛠 Troubleshooting

### Common Issues
*   **405 on Uploads:** Consistently use `UploadResource` for all upload-related logic.
*   **Single Search Result:** Ensure `uploadId` is used as the Elasticsearch `_id` to avoid duplicates or overwrites.
*   **Ollama Connection:** Ensure Ollama is running on the host and accessible from the container (usually via `host.docker.internal`).
