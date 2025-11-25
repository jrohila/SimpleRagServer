# simple-rag-server

Spring Boot backend for SimpleRagServer — an OpenAI-compatible Retrieval-Augmented Generation (RAG) server that provides document-grounded LLM responses with hybrid search, intelligent out-of-scope detection, and multi-tenant collection management.

This README focuses on the backend architecture, REST APIs, configuration, and development setup.

---

## Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Key Features](#key-features)
- [REST API Endpoints](#rest-api-endpoints)
- [Configuration](#configuration)
- [Development Setup](#development-setup)
- [Testing](#testing)
- [Deployment](#deployment)

---

## Overview

**simple-rag-server** is a production-grade Spring Boot 3.5.5 application that:
- Exposes an **OpenAI-compatible** `/v1/chat/completions` API endpoint
- Performs **hybrid search** (BM25 + kNN vector) using OpenSearch
- Implements **intelligent out-of-scope detection** to prevent hallucinations
- Manages **multi-tenant collections** with isolated document storage
- Supports **streaming and non-streaming** chat completions
- Integrates with **Ollama** for local LLM and embedding models
- Uses **Docling** for advanced document parsing and chunking
- Provides **memory-augmented RAG** with user profile extraction

The backend is designed to work with the **simple-rag-ui** React Native web frontend but can also be used standalone via API clients.

---

## Architecture

### Module Structure

```
simple-rag-server/
├── src/main/java/io/github/jrohila/simpleragserver/
│   ├── chat/                    # Chat completions and streaming
│   │   ├── ChatController.java
│   │   ├── ChatService.java
│   │   ├── ChatStreamConsumer.java
│   │   ├── pipeline/            # RAG processing pipeline
│   │   │   ├── BoostTermDetectionPipe.java
│   │   │   ├── ContextAdditionPipe.java
│   │   │   ├── MessageListPreProcessPipe.java
│   │   │   └── OutOfScopeDetectionPipe.java
│   │   └── util/                # Utilities (Granite helper, etc.)
│   ├── controller/              # REST controllers
│   │   ├── OnboardingController.java
│   │   ├── ChatManagerController.java
│   │   ├── CollectionController.java
│   │   ├── DocumentController.java
│   │   ├── ChunkController.java
│   │   ├── SearchController.java
│   │   └── ...
│   ├── repository/              # Service layer and data access
│   │   ├── ChatManagerService.java
│   │   ├── CollectionService.java
│   │   ├── DocumentService.java
│   │   ├── ChunkService.java
│   │   ├── ChunkSearchService.java
│   │   └── IndicesManager.java
│   ├── domain/                  # Domain entities and DTOs
│   │   ├── ChatEntity.java
│   │   ├── CollectionEntity.java
│   │   ├── DocumentEntity.java
│   │   └── ...
│   ├── config/                  # Spring configuration
│   ├── startup/                 # Application startup tasks
│   └── SimpleRagServerApplication.java
└── src/main/resources/
    └── application.properties   # Configuration properties
```

### Processing Pipeline

The RAG pipeline processes chat requests through several stages:

1. **MessageListPreProcessPipe**: Prepares message history and system prompts
2. **BoostTermDetectionPipe**: Extracts important terms from conversation history
3. **Hybrid Search**: Executes BM25 (keyword) + kNN (semantic) search in OpenSearch
4. **OutOfScopeDetectionPipe**: Validates query relevance using dual similarity analysis
5. **ContextAdditionPipe**: Injects relevant document chunks within token budget
6. **LLM Generation**: Streams or returns response from Ollama

---

## Key Features

### OpenAI-Compatible API
- `/v1/chat/completions` endpoint works with OpenAI SDKs and clients
- Supports both streaming (SSE) and non-streaming responses
- Per-chat configuration with model, temperature, and prompt overrides

### Hybrid Search & RAG
- **BM25 (lexical)**: Keyword matching with term boosting from conversation history
- **kNN (semantic)**: Vector similarity using embedding models
- **RRF (Reciprocal Rank Fusion)**: Combines both ranking methods for optimal results
- **Dynamic context injection**: Fills token budget with highest-ranked chunks

### Out-of-Scope Detection
- **Dual similarity analysis**: Combines embedding similarity and BM25 score distribution
- **Statistical validation**: Uses mean and standard deviation thresholds
- **Graceful handling**: Returns friendly message instead of hallucinated responses
- **Configurable**: Adjust sensitivity via properties

### Multi-Tenant Collections
- **Isolated indices**: Each collection gets its own OpenSearch index
- **Document segregation**: Prevents cross-collection data leakage
- **Per-chat association**: Each chat entity links to exactly one collection
- **Flexible organization**: Organize documents by topic, project, or tenant

### Document Processing
- **Docling integration**: Advanced parsing with heading detection and semantic chunking
- **Async processing**: Non-blocking document upload and chunking
- **Multiple formats**: PDF, DOCX, TXT, HTML, Markdown via Apache Tika
- **Chunk management**: CRUD operations on document chunks with metadata

### Memory & User Profiles
- **Fact extraction**: Automatically extracts user attributes from conversations
- **Memory-augmented RAG**: Combines document context with user profile facts
- **JSON-based memory**: Structured fact storage with confidence and merge strategies

### Chat Management
- **Chat entities**: Each chat has its own settings, prompts, and collection scope
- **LLM configuration**: Per-chat model, temperature, and parameter overrides
- **Prompt customization**: System, context, memory, and out-of-scope prompts
- **Override control**: Server-side enforcement of LLM settings

---

## REST API Endpoints

### Chat Completion API (OpenAI-Compatible)

**POST** `/{publicName}/v1/chat/completions`  
**POST** `/{publicName}/api/chat`

OpenAI-compatible chat completions endpoint.

**Path Parameters:**
- `publicName` - The public name/identifier of the chat

**Query Parameters:**
- `useRag` (optional, default: `true`) - Enable/disable RAG context injection

**Request Body:**
```json
{
  "model": "ibm/granite4:micro-h",
  "stream": false,
  "messages": [
    {"role": "user", "content": "What is SimpleRagServer?"}
  ],
  "temperature": 0.7,
  "max_tokens": 2048
}
```

**Response (non-streaming):**
```json
{
  "id": "chatcmpl-123",
  "object": "chat.completion",
  "created": 1677652288,
  "model": "ibm/granite4:micro-h",
  "choices": [{
    "index": 0,
    "message": {
      "role": "assistant",
      "content": "SimpleRagServer is a self-hosted RAG backend..."
    },
    "finish_reason": "stop"
  }],
  "usage": {
    "prompt_tokens": 50,
    "completion_tokens": 100,
    "total_tokens": 150
  }
}
```

**Response (streaming):** Server-Sent Events (SSE) with JSON chunks

---

### Onboarding API

**POST** `/api/onboarding/createNewChat`  
Content-Type: `multipart/form-data`

Creates a new chat with associated collection and optional document uploads.

**Form Parameters:**
- `publicName` - User-facing chat name
- `internalName` - Internal identifier (unique)
- `internalDescription` - Chat description
- `defaultLanguage` - Default language (e.g., "en")
- `defaultSystemPrompt` - System prompt override
- `defaultSystemPromptAppend` - Additional system instructions
- `defaultContextPrompt` - RAG context prompt
- `defaultMemoryPrompt` - Memory injection prompt
- `defaultExtractorPrompt` - Fact extraction prompt
- `defaultOutOfScopeContext` - Out-of-scope detection prompt
- `collectionName` - Collection identifier
- `collectionDescription` - Collection description
- `overrideSystemMessage` (boolean) - Allow system message override
- `overrideAssistantMessage` (boolean) - Allow assistant message override
- `files` (optional) - MultipartFile array of documents to upload

**Response:**
```json
{
  "chat": { ... },
  "collection": { ... },
  "documents": [ ... ]
}
```

---

### Chat Management

**GET** `/api/chats` - List all chats  
**GET** `/api/chats/{id}` - Get chat by ID  
**GET** `/api/chats/publicName/{publicName}` - Get chat by public name  
**PUT** `/api/chats/{id}` - Update chat configuration  
**DELETE** `/api/chats/{id}` - Delete chat

---

### Collection Management

**GET** `/api/collections` - List all collections  
**GET** `/api/collections/{id}` - Get collection by ID  
**POST** `/api/collections` - Create new collection  
**PUT** `/api/collections/{id}` - Update collection  
**DELETE** `/api/collections/{id}` - Delete collection and its index

---

### Document Management

**GET** `/api/documents` - List all documents  
**GET** `/api/documents/{id}` - Get document by ID  
**GET** `/api/documents/collection/{collectionId}` - Get documents by collection  
**POST** `/api/documents` - Upload new document  
**DELETE** `/api/documents/{id}` - Delete document and its chunks

---

### Chunk Management

**GET** `/api/chunks` - List all chunks  
**GET** `/api/chunks/{id}` - Get chunk by ID  
**GET** `/api/chunks/document/{documentId}` - Get chunks by document  
**PUT** `/api/chunks/{id}` - Update chunk content  
**DELETE** `/api/chunks/{id}` - Delete chunk

---

### Search API

**POST** `/api/search/hybrid` - Hybrid search (BM25 + kNN)  
**POST** `/api/search/semantic` - Semantic (kNN) search only  
**POST** `/api/search/keyword` - Keyword (BM25) search only

**Request Body:**
```json
{
  "query": "What is RAG?",
  "collectionId": "col-123",
  "topK": 5,
  "boostTerms": ["retrieval", "augmented"]
}
```

---

### WebGPU Support (Frontend Integration)

**POST** `/api/webgpu/search` - Search endpoint for WebGPU local LLM mode

When the frontend uses local WebGPU inference, it still calls backend search APIs to retrieve RAG context. The backend returns document chunks which the frontend then injects into the local LLM prompt.

---

## Configuration

### Application Properties

Located in `src/main/resources/application.properties`:

#### Basic Settings
```properties
spring.application.name=SimpleRagServer
server.port=8080
server.max-http-request-header-size=64KB
```

#### File Upload
```properties
spring.content.storage.type.default=fs
spring.content.fs.filesystemRoot=C:/upload
spring.servlet.multipart.max-file-size=50MB
spring.servlet.multipart.max-request-size=50MB
```

#### OpenSearch
```properties
opensearch.uris=http://localhost:9200
opensearch.username=admin
opensearch.password=MyAdm1n_Passw0rd!
```

#### Ollama (LLM & Embeddings)
```properties
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.embedding.options.model=embeddinggemma:300m
spring.ai.ollama.chat.options.model=ibm/granite4:micro-h
spring.ai.ollama.chat.options.num-ctx=32768
```

#### Docling (Document Parsing)
```properties
docling-serve.url=http://localhost:5001
docling.timeout.connect=10000
docling.timeout.read=600000
```

#### RAG Prompts
```properties
processing.chat.system.prompt=You are a highly capable and professional AI assistant...
processing.chat.system.append=Follow these universal behavioral and compliance rules...
processing.chat.rag.context-prompt=You are given two information sources...
processing.chat.rag.memory-prompt=You are provided with a JSON object...
processing.chat.rag.out-of-scope-prompt=Before attempting to answer...
processing.chat.out-of-scope-message=Thank you for your query...
```

#### Chunking
```properties
processing.chunking=async
chunks.dimension-size=768
chunks.similarity-function=cosinesimil
```

### Environment Variables

Override properties using environment variables:
```bash
export OPENSEARCH_URIS=http://my-opensearch:9200
export OLLAMA_BASE_URL=http://my-ollama:11434
export DOCLING_SERVE_URL=http://my-docling:5001
```

---

## Development Setup

### Prerequisites
- Java 21+
- Maven 3.6+
- Docker and Docker Compose (for dependencies)

### 1. Start Dependencies

Start OpenSearch, Ollama, and Docling services:
```bash
# From repository root
docker compose up -d
```

With Ollama container:
```bash
docker compose --profile ollama up -d
```

Pull required Ollama models:
```bash
docker compose run --rm ollama-pull-models
```

### 2. Build the Backend

From repository root (builds all modules including UI):
```bash
mvn clean package -DskipTests
```

Build backend only:
```bash
cd simple-rag-server
mvn clean package -DskipTests
```

### 3. Run the Application

Run with Maven (hot-reload enabled):
```bash
cd simple-rag-server
mvn spring-boot:run
```

Or run the JAR directly:
```bash
java -jar target/simple-rag-server-0.0.1-SNAPSHOT.jar
```

### 4. Access the Application

- **Web UI**: http://localhost:8080/
- **API Docs**: http://localhost:8080/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8080/v3/api-docs

---

## Testing

### Run Tests

Run all tests:
```bash
mvn test
```

Skip tests during build:
```bash
mvn clean package -DskipTests
```

### Manual API Testing

Using curl:
```bash
# Create a new chat
curl -X POST http://localhost:8080/api/onboarding/createNewChat \
  -F "publicName=Test Chat" \
  -F "internalName=test-chat" \
  -F "internalDescription=Test" \
  -F "defaultLanguage=en" \
  -F "collectionName=test-collection" \
  -F "collectionDescription=Test Collection"

# Chat completion
curl http://localhost:8080/test-chat/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "ibm/granite4:micro-h",
    "stream": false,
    "messages": [{"role":"user","content":"Hello!"}]
  }'
```

---

## Deployment

### Production Build

Build with production optimizations:
```bash
mvn clean package -Pprod
```

### Docker Deployment

The backend can be containerized and deployed alongside other services:

```dockerfile
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/simple-rag-server-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Build and run:
```bash
docker build -t simple-rag-server .
docker run -p 8080:8080 \
  -e OPENSEARCH_URIS=http://opensearch:9200 \
  -e OLLAMA_BASE_URL=http://ollama:11434 \
  simple-rag-server
```

### Health Checks

The application exposes Spring Boot Actuator health endpoints (if configured):
- `/actuator/health` - Overall application health
- `/actuator/info` - Application info

---

## Key Dependencies

- **Spring Boot 3.5.5** - Application framework
- **Spring AI 1.0.1** - LLM integration with Ollama
- **OpenSearch Java Client 3.2.0** - Search and vector storage
- **Apache Tika 3.2.2** - Document text extraction
- **Apache PDFBox 2.0.30** - PDF processing
- **Apache OpenNLP 2.5.6** - NLP utilities (via simple-rag-nlp module)
- **Lombok** - Boilerplate code reduction
- **SpringDoc OpenAPI 2.3.0** - API documentation

---

## Architecture Highlights

### Dependency Injection
- Constructor-based injection for testability
- Service layer abstraction for business logic
- Repository pattern for data access

### Async Processing
- Document chunking runs asynchronously
- Non-blocking file uploads
- CompletableFuture for background tasks

### Error Handling
- Global exception handlers
- Graceful degradation when services unavailable
- Validation with Jakarta Bean Validation

### Logging
- SLF4J with Logback
- Configurable log levels per package
- Request/response logging for debugging

---

## Troubleshooting

### OpenSearch Connection Issues
- Ensure OpenSearch is running: `docker ps`
- Check connection settings in `application.properties`
- Verify network connectivity: `curl http://localhost:9200`

### Ollama Model Errors
- Pull required models: `docker compose run --rm ollama-pull-models`
- Check model availability: `curl http://localhost:11434/api/tags`
- Verify base URL configuration

### Document Processing Failures
- Ensure Docling service is running: `curl http://localhost:5001/health`
- Check timeout settings if processing large documents
- Review logs for parsing errors

### Out-of-Memory Errors
- Increase JVM heap: `java -Xmx4g -jar app.jar`
- Reduce `num-ctx` in Ollama settings for smaller context windows
- Limit document chunk sizes

---

## Contributing

See the repository root README for contribution guidelines.

---

## License

See the repository root `LICENSE` file for project licensing.

---

## Support

- **Issues**: Open an issue on GitHub
- **API Docs**: http://localhost:8080/swagger-ui.html
- **Debug Logging**: Set `logging.level.io.github.jrohila.simpleragserver=DEBUG`
