# SimpleRagServer

SimpleRagServer is a self-hosted, OpenAI-compatible Retrieval-Augmented Generation (RAG) backend built with Spring Boot, OpenSearch, and Ollama. It enables private, local, or enterprise-grade LLM pipelines with hybrid search, local LLM/embedding support, document chunking, intelligent out-of-scope detection, and a modern React Native web UI—all fully dockerized for easy deployment.

## Features

### Core Capabilities
- **OpenAI-compatible API**: `/v1/chat/completions` endpoint (works with OpenAI clients/SDKs)
- **Hybrid search**: Combines lexical (BM25) and kNN vector retrieval in OpenSearch with RRF (Reciprocal Rank Fusion)
- **Out-of-scope detection**: Intelligent prompt validation using dual similarity analysis (BM25 + embeddings) to prevent hallucinations
- **Automatic context injection**: Smart document retrieval and system prompt handling
- **Docling-powered document parsing**: Advanced document chunking and parsing with heading detection
- **Local LLM support**: Full integration with Ollama for local/private LLM and embedding models
- **Streaming & non-streaming**: Real-time streaming chat and traditional request/response modes
- **Modern web UI**: React Native (Expo) web application with real-time chat, markdown rendering, and search capabilities

### Architecture Features
- **Multi-tenant collections**: Segregate documents by collection, each with its own OpenSearch index
- **Chat entity abstraction**: Each chat has its own settings linked to a collection, controlling context, prompts, and document scope
- **Modular Spring Boot architecture**: Clean separation of concerns with pipeline-based processing
- **Production-grade**: Built with enterprise patterns and best practices

## Multi-Module Project Structure

```
simple-rag-parent/
├── simple-rag-server/     # Spring Boot backend (API + business logic)
├── simple-rag-ui/         # React Native (Expo) web UI
└── simple-rag-nlp/        # NLP utilities (summarization, term extraction)
```

## Docker Compose Services

- **opensearch**: Hybrid search backend with BM25 + kNN (port 9200)
- **dashboards**: OpenSearch Dashboards UI (port 15601, **opt-in**; start with `--profile dashboards`)
- **ollama**: Local LLM/embedding server (port 11434, optional profile)
- **ollama-pull-models**: Auto-pulls required Ollama models on startup
- **docling-serve**: Document chunking and parsing service (port 5001)

## Architecture Overview

```
 ┌──────────────────┐       ┌──────────────────────────┐       ┌────────────┐
 │   Web UI         │       │                          │       │   Ollama   │
 │ (React Native)   │──────▶│  SimpleRagServer         │──────▶│ (LLM Host) │
 │                  │       │   (Spring Boot)          │◀──────│            │
 └──────────────────┘       │                          │       └────────────┘
        │                   │  ┌────────────────────┐  │
        │                   │  │ Out-of-Scope       │  │
        │                   │  │ Detection Pipeline │  │
        │                   │  └────────────────────┘  │
        │                   │  ┌────────────────────┐  │
        │                   │  │ Context Addition   │  │
        │                   │  │ Pipeline           │  │
        │                   │  └────────────────────┘  │
        │                   └──────┬───────────────────┘
        │                          │
        ▼                          ▼
   ┌─────────────────────────────────────┐
   │         OpenSearch                  │
   │  ┌──────────────┐  ┌──────────────┐│
   │  │  BM25        │  │  kNN Vector  ││
   │  │  (Keyword)   │  │  (Semantic)  ││
   │  └──────────────┘  └──────────────┘│
   │         Hybrid Search + RRF         │
   └─────────────────────────────────────┘
                │
                ▼
      ┌───────────────────┐
      │  Docling Serve    │
      │  (Doc Parser)     │
      └───────────────────┘
```

### Key Processing Pipeline

1. **User Query** → Web UI or API client
2. **Boost Term Detection** → Extract and boost important terms from conversation history
3. **Hybrid Search** → Execute BM25 (keyword) + kNN (semantic) search with RRF ranking
4. **Out-of-Scope Detection** → Dual similarity analysis:
   - Compare prompt embedding to result embeddings (semantic similarity)
   - Analyze BM25 score distribution (keyword similarity)
   - Calculate mean, std deviation, and similarity thresholds
   - Return out-of-scope message if prompt doesn't match retrieved context
5. **Context Addition** → Inject relevant document chunks within token budget
6. **LLM Generation** → Stream or batch response from Ollama
7. **Response** → Return with source citations

## Prerequisites
- Docker and Docker Compose v2
- Java 21+ and Maven (to run the Spring Boot app)
- Optional: Ollama installed on the host if not using the container

## Quickstart

### 1. Start All Services

Basic startup (without Ollama container):
```bash
docker compose up -d
```

With Ollama container:
```bash
docker compose --profile ollama up -d
```

With OpenSearch Dashboards:
```bash
docker compose --profile dashboards up -d
```

### 2. Pull Ollama Models

The puller tries the Ollama container first, then the host (`http://host.docker.internal:11434`):
```bash
docker compose run --rm ollama-pull-models
```

Customize which models to pull (space-separated):
```bash
export OLLAMA_MODELS="granite3.3:2b embeddinggemma:300m"
docker compose run --rm ollama-pull-models
```

### 3. Build and Run the Application

Build all modules (server + UI):
```bash
mvn clean package
```

Run the Spring Boot application:
```bash
cd simple-rag-server
mvn spring-boot:run
```

Or run the JAR directly:
```bash
java -jar simple-rag-server/target/simple-rag-server-0.0.1-SNAPSHOT.jar
```

### 4. Access the Application

- **Web UI**: http://localhost:8080/
- **API Docs**: http://localhost:8080/swagger-ui.html
- **OpenSearch**: http://localhost:9200
- **OpenSearch Dashboards** (if enabled): http://localhost:15601
- **Ollama API**: http://localhost:11434 (if using container profile)

## API Usage

### Web UI

The React Native web UI provides:
- **Chat Interface**: Real-time streaming chat with markdown rendering
- **Chat Selection**: Dropdown to switch between different chat sessions
- **Search Interface**: Hybrid, semantic, and keyword search capabilities
- **Markdown Support**: Code syntax highlighting, tables, lists, and formatting
- **Responsive Design**: Works on desktop and mobile browsers

Access at: http://localhost:8080/

### Onboarding API (Create New Chat)

The onboarding endpoint creates a new chat and collection with customizable settings:

```bash
curl -X POST http://localhost:8080/onboarding \
  -H "Content-Type: application/json" \
  -d '{
    "publicName": "My Assistant",
    "internalName": "my-assistant",
    "internalDescription": "Personal knowledge assistant",
    "defaultLanguage": "en",
    "defaultSystemPrompt": "You are a helpful assistant.",
    "defaultSystemPromptAppend": "Always be concise and accurate.",
    "defaultContextPrompt": "Use the following context to answer:",
    "defaultMemoryPrompt": "Previous conversation:",
    "defaultExtractorPrompt": "Extract key facts:",
    "collectionName": "my-docs",
    "collectionDescription": "My personal documents",
    "overrideSystemMessage": true,
    "overrideAssistantMessage": true
  }'
```

**Key Parameters:**
- `overrideSystemMessage` (default: `true`): Override system message for this chat
- `overrideAssistantMessage` (default: `true`): Override assistant message for this chat
- Chat settings control RAG behavior, prompts, and document access scope

### Chat Completion API

Non-streaming:
```bash
curl -s http://localhost:8080/my-chat/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "granite3.3:2b",
    "stream": false,
    "messages": [
      {"role":"user","content":"Give a short summary of SimpleRagServer."}
    ]
  }'
```

Streaming (Server-Sent Events):
```bash
curl -N http://localhost:8080/my-chat/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "granite3.3:2b",
    "stream": true,
    "messages": [
      {"role":"user","content":"Summarize the project in one paragraph."}
    ]
  }'
```

**Optional Query Parameters:**
- `?useRag=true` (default: true): Enable RAG context injection
- `?useRag=false`: Direct LLM completion without document retrieval

### Out-of-Scope Detection

The system automatically detects when a user's query is outside the scope of available documents:

**How It Works:**
1. Performs hybrid search (BM25 + kNN) for the query
2. Calculates **embedding similarity**: Compares prompt embedding to retrieved document embeddings
3. Calculates **keyword similarity**: Analyzes BM25 score distribution
4. Uses statistical analysis (mean, standard deviation) to determine if query matches available content
5. Returns a friendly out-of-scope message if query doesn't align with documents

**Example Out-of-Scope Response:**
```
Thank you for your query. However, I'm currently unable to assist with your request 
because it falls outside the scope of my expertise and the information I have access to. 
My role is to provide clear and accurate guidance based strictly on the given context 
and my defined knowledge domain. Please feel free to ask questions related to topics 
within my expertise, and I will be glad to help you.
```

**Benefits:**
- ✅ Prevents hallucinations when relevant context isn't available
- ✅ Maintains response accuracy and user trust
- ✅ Provides clear feedback on system capabilities
- ✅ Configurable sensitivity via similarity thresholds
  -H "Content-Type: application/json" \
  -d '{
    "model": "granite3.3:2b",
    "stream": true,
    "messages": [{"role":"user","content":"Summarize the project in one paragraph."}]
  }'
```

## Configuration

### Application Properties

Located in `simple-rag-server/src/main/resources/application.properties`:

#### Chat Processing
```properties
# System prompts
processing.chat.system.prompt=You are a helpful assistant.
processing.chat.system.append=Be concise and accurate.

# RAG prompts
processing.chat.rag.context-prompt=Use the following context to answer the question:
processing.chat.rag.memory-prompt=Previous conversation:
processing.chat.rag.out-of-scope-prompt=Compare the user's prompt to the context...

# Out-of-scope message
processing.chat.out-of-scope-message=Thank you for your query...

# Post-processing
processing.post.chat.fact.extractor.append=Extract key facts from the conversation.
```

#### Logging
```properties
# Enable debug logging
logging.level.io.github.jrohila.simpleragserver=DEBUG
```

#### OpenSearch
```properties
# Connection settings
opensearch.host=localhost
opensearch.port=9200
opensearch.scheme=http
```

#### Ollama
```properties
# LLM and embedding endpoints
spring.ai.ollama.base-url=http://localhost:11434
spring.ai.ollama.embedding.model=nomic-embed-text
spring.ai.ollama.chat.model=granite3.3:2b
```

### Environment Variables

Override properties using environment variables:
```bash
export OPENSEARCH_HOST=my-opensearch-host
export OPENSEARCH_PORT=9200
export OLLAMA_BASE_URL=http://my-ollama:11434
```

## Key Features & Behavior

### Hybrid Search with RRF
- **BM25 (Keyword)**: Lexical matching for exact terms and phrases
- **kNN (Semantic)**: Vector similarity for conceptual matching
- **RRF (Reciprocal Rank Fusion)**: Combines both approaches for optimal results
- **Boost terms**: Automatically extracted from conversation history
- **Term capping**: Limits boosted terms to prevent oversized queries (max 12 terms)

### Out-of-Scope Detection
- **Dual similarity analysis**: Combines embedding similarity and BM25 score distribution
- **Statistical validation**: Uses mean and standard deviation to determine relevance
- **Automatic activation**: Triggered after 4 messages in conversation
- **Configurable threshold**: 50% error buffer for similarity matching
- **Graceful handling**: Returns friendly message instead of hallucinated answers

### Context Management
- **Token budget**: Dynamically calculated based on model limits
- **Smart truncation**: Fills context within budget constraints
- **Priority ranking**: Uses RRF scores to prioritize best-matching chunks
- **Source tracking**: Maintains document and chunk references for citations

### System Prompts
- **Append mode**: Adds system instructions to client-provided prompts
- **Default injection**: Inserts system message if none provided
- **Title request bypass**: First message detected as title request sent directly (no RAG)
- **Chat-specific**: Each chat entity can override default prompts

### Collections & Multi-Tenancy
- **Isolated indices**: Each collection creates a separate OpenSearch index
- **Document segregation**: Prevents cross-collection data leakage
- **Per-chat association**: Each chat links to exactly one collection
- **Flexible organization**: Organize documents by topic, project, or tenant

### Startup & Reliability
- **Startup wait**: Waits up to 5 minutes for OpenSearch availability
- **Graceful degradation**: Continues without RAG if OpenSearch unavailable
- **Health checks**: Monitors external service connectivity
- **Error handling**: Comprehensive error boundaries throughout pipeline

### Web UI Features
- **Real-time streaming**: Server-Sent Events (SSE) for live chat responses
- **Markdown rendering**: Full markdown support with code syntax highlighting
- **Chat management**: Switch between multiple chat sessions
- **Search interface**: Hybrid, semantic, and keyword search capabilities
- **Responsive design**: Works on desktop and mobile browsers
- **Loading states**: Skeleton animations and progress indicators
- **Error handling**: User-friendly error messages and retry logic

## Build & Development

### Full Build (All Modules)

Build server, UI, and NLP modules:
```bash
mvn clean package
```

Skip tests:
```bash
mvn clean package -DskipTests
```

### Build Individual Modules

UI only:
```bash
cd simple-rag-ui
mvn clean package
```

Server only:
```bash
cd simple-rag-server
mvn clean package
```

### Run from JAR

After building:
```bash
java -jar simple-rag-server/target/simple-rag-server-0.0.1-SNAPSHOT.jar
```

### Development Mode

Run with Maven (auto-reload):
```bash
cd simple-rag-server
mvn spring-boot:run
```

### UI Development

For UI-only development:
```bash
cd simple-rag-ui
npm install
npm start
# or
npm run web
```

The UI is built automatically during Maven build and bundled into the server JAR.

## Technology Stack

### Backend
- **Java 21** - Modern Java LTS
- **Spring Boot 3.5.5** - Application framework
- **Spring AI 1.0.1** - LLM integration
- **OpenSearch 3.x** - Hybrid search (BM25 + kNN)
- **Apache Tika 3.2.2** - Document parsing
- **Apache PDFBox 2.0.30** - PDF processing
- **Apache OpenNLP 2.5.6** - NLP utilities

### Frontend
- **React Native 0.81.4** - Cross-platform UI framework
- **Expo 54** - Development platform
- **TypeScript 5.9** - Type-safe JavaScript
- **React Navigation 7.x** - Routing
- **GiftedChat 2.8.1** - Chat UI components
- **Markdown Display 7.0.2** - Markdown rendering

### Infrastructure
- **Docker Compose** - Service orchestration
- **Ollama** - Local LLM hosting
- **Docling** - Document processing service
- **Maven 3.x** - Build tool
- **Node.js 25.1.0** - Frontend tooling
- **npm 11.6.2** - Package management

## Project Requirements

- **Java**: 21 or higher
- **Maven**: 3.6 or higher
- **Docker**: 20.10 or higher
- **Docker Compose**: v2.0 or higher
- **Node.js**: 25.1.0 (auto-installed by Maven)
- **npm**: 11.6.2 (auto-installed by Maven)

## License

[Specify your license here]

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Support

For issues and questions:
- Open an issue on GitHub
- Check the OpenAPI docs at http://localhost:8080/swagger-ui.html
- Review logs with `logging.level.io.github.jrohila.simpleragserver=DEBUG`
