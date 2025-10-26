# SimpleRagServer

SimpleRagServer is a self-hosted, OpenAI-compatible Retrieval-Augmented Generation (RAG) backend built with Spring Boot, OpenSearch, and Ollama. It enables private, local, or enterprise-grade LLM pipelines with hybrid search, local LLM/embedding support, and document chunking—all fully dockerized for easy deployment.

## Features
- OpenAI-compatible `/v1/chat/completions` API (works with OpenAI clients/SDKs)
- Hybrid search: combines lexical and kNN vector retrieval in OpenSearch
- Automatic context injection and system prompt handling
- Docling-powered document parsing and chunking
- Local LLM and embedding model support (Ollama, with model auto-pulling)
- Streaming and non-streaming chat support
- Modular, production-grade Spring Boot architecture
- Docker Compose for one-command startup of all services

## Docker Compose Services
- `opensearch`: Hybrid search backend (9200)
- `dashboards`: OpenSearch Dashboards UI (15601)
- `ollama`: Local LLM/embedding server (11434, optional profile)
- `ollama-pull-models`: Auto-pulls required Ollama models
- `docling-serve`: Document chunking and parsing (5001)

## Architecture Overview
```
 ┌──────────────┐       ┌──────────────────┐       ┌────────────┐
 │   Client     │──────▶│  SimpleRagServer │──────▶│   Ollama   │
 │ (OpenAI API) │       │   (Spring Boot)  │◀──────│ (LLM Host) │
 └──────────────┘       └──────┬───────────┘       └────────────┘
        │
        ▼
      ┌───────────────┐
      │  OpenSearch   │  ← hybrid retrieval (lexical + vector)
      └───────────────┘
        │
        ▼
      ┌───────────────┐
      │ Docling Serve │  ← document parsing and chunking
      └───────────────┘
```

## Prerequisites
- Docker and Docker Compose v2
- Java 21+ and Maven (to run the Spring Boot app)
- Optional: Ollama installed on the host if not using the container

## Quickstart

1) Start all services:
```bash
docker compose up -d
```
- With Ollama container:
```bash
docker compose --profile ollama up -d
```

2) Ensure Ollama models
- The puller tries the Ollama container first, then the host (`http://host.docker.internal:11434`). It skips quietly if neither is reachable.
```bash
docker compose run --rm ollama-pull-models
```
- Customize which models to pull (space-separated):
```bash
export OLLAMA_MODELS="granite3.3:2b embeddinggemma:300m"
```

3) Run the server
```bash
cd simple-rag-server
mvn -q spring-boot:run
```
- App: http://localhost:8080

## API Usage

Non-streaming:
```bash
curl -s http://localhost:8080/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "granite3.3:2b",
    "stream": false,
    "messages": [{"role":"user","content":"Give a short summary of SimpleRagServer."}]
  }'
```

Streaming:
```bash
curl -N http://localhost:8080/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "granite3.3:2b",
    "stream": true,
    "messages": [{"role":"user","content":"Summarize the project in one paragraph."}]
  }'
```

## Configuration

In `simple-rag-server/src/main/resources/application.properties`:
- `processing.chat.system.append=...`
- `processing.chat.rag.context-prompt=...`
- `logging.level.io.github.jrohila.simpleragserver.chat=DEBUG` (to log messages sent to the LLM)

## Behavior Summary
- RAG retrieval: hybrid query; boosted terms are capped to avoid oversized queries
- System prompts: append to client system messages; insert a default system message if none provided
- Title request bypass: first user message detected as a title request is sent directly (no RAG/system append)
- Streaming and non-streaming parity: same message assembly and RAG logic
- Docling integration: always on via REST

## Build

Package:
```bash
cd simple-rag-server && mvn -q -DskipTests package
```

Run JAR:
```bash
java -jar simple-rag-server/target/simple-rag-server-0.0.1-SNAPSHOT.jar
```
