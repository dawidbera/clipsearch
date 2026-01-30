# ClipSearch 📎🔍

ClipSearch is a modern, asynchronous document search engine built with a microservices architecture. It allows uploading files (text, PDF, and images), automatically extracting their content using OCR, and generating **AI-powered summaries** using a local LLM.

## 🌟 Key Features
- **AI Summarization:** Get concise, 2-3 bullet point summaries of your documents automatically, powered by **local Phi-3 model via Ollama**.
- **OCR Support:** Extract text from images (PNG, JPG) in English and German using **Tesseract**.
- **Full-Text Search:** High-performance search for TXT and PDF documents via **Elasticsearch**.
- **Data Persistence:** Persistent storage for the search index using Kubernetes PVCs.
- **Microservices Architecture:** Scalable design with Quarkus (API & Worker), S3, and SQS.

## 🚀 Quick Start (Local Dev)

### Prerequisites
- Docker & Docker Compose
- **Ollama** installed on your host machine (for AI features)
  - Run `ollama pull phi3:mini`

### Running the App
1. Clone the repository:
   ```bash
   git clone https://github.com/dawidbera/clipsearch.git
   cd clipsearch
   ```
2. Start the application:
   ```bash
   docker-compose up --build
   ```
3. Open in your browser: [http://localhost:4200](http://localhost:4200)

## 🏗️ Architecture

1.  **Frontend (Angular):** Clean UI with pagination, filtering, and file downloads.
2.  **API (Quarkus):** Handles S3 uploads and pre-signed download URLs.
3.  **Worker (Quarkus):** Processes documents, performs OCR, and calls **local AI** for summarization.
4.  **Elasticsearch:** Search engine with persistent volumes.
5.  **LocalStack:** AWS S3/SQS emulator.
6.  **Ollama (Host):** Local LLM engine for document summarization.

---
Created by [dawidbera](https://github.com/dawidbera)