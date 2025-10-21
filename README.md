# SimpleRagServer

An OpenAI-compatible RAG server built with Spring Boot, OpenSearch, and Ollama

SimpleRagServer is a lightweight, fully open-source Retrieval-Augmented Generation (RAG) backend designed for developers who want to run private, local, or enterprise-grade LLM pipelines without cloud dependencies.

It provides an OpenAI-compatible API (`/v1/chat/completions`) that can be dropped into any client or SDK that supports OpenAI, while running everything locally using:
- OpenSearch for hybrid retrieval (lexical + vector search)
- Ollama for hosting local LLMs and embeddings
- Docling Serve for document parsing and chunking

The project’s goal is simplicity and transparency: a self-hosted RAG system with production-level architecture (Spring Boot microservice, Docker Compose environment, and modular processing pipelines) that can evolve into a scalable, enterprise-ready “SimpleAgentServer.”

## Features
- OpenAI-compatible API: `/v1/chat/completions`
- Hybrid RAG retrieval: OpenSearch lexical + kNN vector search
- Automatic context injection: context prefixed via a system message before LLM call
- Docling-powered chunking: always-on document parsing via Docling Serve
- Local LLM hosting: Ollama (host or container) for chat and embeddings
- Streaming support: OpenAI-style server-sent events
- Configurable prompts: via `application.properties`
- Dockerized stack: start the whole environment with one command

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

Everything runs locally via Docker Compose.

## Prerequisites
- Docker and Docker Compose v2
- Java 21+ and Maven (to run the Spring Boot app)
- Optional: Ollama installed on the host if not using the container

## Services (Docker Compose)
- OpenSearch (9200): hybrid retrieval backend
- OpenSearch Dashboards (15601): UI for queries and monitoring
- Ollama (11434): local LLMs (optional via profile)
- Model puller: pulls Ollama models automatically (container or host)
- Docling Serve (5001): document parsing and chunking (always used)

## Quickstart

1) Start backing services
- Default (uses host Ollama if available):
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
