# ClipSearch 📎🔍

ClipSearch is a modern, asynchronous document search engine built with a microservices architecture. It allows uploading files (text and PDF), automatically extracting their content, and performing lightning-fast full-text searches.

## 🚀 Quick Start (Local Dev)

The project uses **Docker Compose** to spin up the entire infrastructure with a single command.

### Prerequisites
- Docker & Docker Compose
- A web browser

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
3. Open in your browser:
   - **Frontend:** [http://localhost:4200](http://localhost:4200)
   - **API (Swagger/Health):** [http://localhost:8080/q/dev](http://localhost:8080/q/dev)

## 🏗️ Architecture

The application consists of the following components:

1.  **Frontend (Angular):** A clean user interface for uploading and searching.
2.  **API Service (Quarkus):** Accepts files, saves them to S3, and sends notifications to an SQS queue.
3.  **Worker Service (Quarkus):** Asynchronously retrieves tasks from SQS, extracts text from documents (Apache Tika), and indexes them in Elasticsearch.
4.  **LocalStack:** Emulates AWS services (S3, SQS) locally.
5.  **Elasticsearch:** A powerful full-text search engine.

## 🛠️ Technologies

- **Backend:** Java 17, Quarkus, Apache Tika
- **Frontend:** Angular 17, Bootstrap
- **Infrastructure:** LocalStack (S3, SQS), Elasticsearch 8.x, Docker
- **CI/CD:** GitHub Actions (automatic build and push to GHCR)

## 📁 Project Structure

```text
clipsearch/
├── backend/            # API and Worker source code (Maven)
├── frontend/           # Angular application
├── deploy/             # Kubernetes / OpenShift manifests
├── scripts/            # Helper scripts (AWS initialization)
└── docker-compose.yml  # Local environment definition
```

## 🌐 Deployment on OpenShift

The project is ready for deployment on the **OpenShift Developer Sandbox**:
```bash
oc apply -k deploy/overlays/openshift-sandbox
```

---
Created by [dawidbera](https://github.com/dawidbera)