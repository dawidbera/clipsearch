# ClipSearch 📎🔍

ClipSearch is a modern, asynchronous document search engine built with a microservices architecture. It allows uploading files (text, PDF, and images), automatically extracting their content, and generating **AI-powered summaries** using a local LLM.

## 🌟 Key Features
- **AI Summarization:** Get concise summaries of your documents automatically, powered by **local Phi-3 model via Ollama**.
- **OCR Support:** Extract text from images (PNG, JPG) using **Tesseract**.
- **Advanced File Processing:** High-performance text extraction from PDF and TXT documents via **Apache Tika**.
- **Full-Text Search:** Scalable search engine powered by **Elasticsearch**.
- **Microservices Architecture:** Built with Quarkus (API & Worker), S3 (LocalStack), and SQS (LocalStack).
- **Deployment Ready:** Supports both Docker Compose for local dev and OpenShift/Kubernetes for production.

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

1.  **Frontend (Angular):** Responsive UI for document management and search.
2.  **API (Quarkus):** RESTful interface for file uploads and search queries.
3.  **Worker (Quarkus):** Background processor for OCR, Tika extraction, and AI summarization.
4.  **Elasticsearch:** Core search engine with persistent storage.
5.  **LocalStack:** Emulates AWS S3 and SQS for development.
6.  **Ollama:** Local LLM engine providing privacy-focused AI summaries.

## ☁️ Cloud Deployment

### OpenShift
A helper script is provided for deploying to OpenShift using Kustomize:
```bash
./deploy-openshift.sh
```
Ensure you are logged in (`oc login`) and have the necessary permissions in your target namespace.

## 🛠 CI/CD
The project uses GitHub Actions for continuous integration.
- **Images:** Automatically built and pushed to **GitHub Container Registry (GHCR)**.
- **Registry:** `ghcr.io/dawidbera/clipsearch-*`

---
Created by [dawidbera](https://github.com/dawidbera)