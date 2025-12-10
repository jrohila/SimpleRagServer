# simple-rag-ui

Frontend for the SimpleRagServer project — a React Native Web application
that provides RAG (Retrieval-Augmented Generation) features with support for
both remote and local WebGPU-based LLM inference.

This README focuses on how to develop, build and integrate the frontend
in this mono-repo.

**Contents**
- **Project**: brief overview
- **Quick Start**: dev commands
- **Features**: key capabilities including WebGPU support
- **Production Build**: build & package steps
- **Backend Integration**: how to repackage Spring Boot with new assets
- **WASM & Performance Notes**: large asset handling and recommendations
- **Troubleshooting**: common runtime warnings and how to resolve them

---

**Project**: Frontend description
- **Location**: `simple-rag-ui`
- Source: written with React Native components (for cross-platform code).
- Target: built to run on the web using `react-native-web` and
  `vite-plugin-react-native-web` (Vite is used as the dev/build tool).
- Built with: TypeScript, React (web bridge via `react-native-web`), Vite
- Key runtime: optionally uses an on-device transformer runtime which
  fetches an ONNX WASM binary for local inference (this asset can be large).

---

**Quick Start (local dev)**
- Install dependencies:

```powershell
cd simple-rag-ui
npm install
```

- Start the Vite dev server (the project uses `vite` and `vite-plugin-react-native-web`):

```powershell
npm run dev
```

- Open the app in your browser at the URL shown by Vite (usually `http://localhost:5173`).

Notes:
- The UI source uses React Native components and runs on the web via `react-native-web`.
- Use the language switcher in the UI to toggle i18n strings (app uses `i18next`).
- If you ran a previous build that included Expo-related artifacts, clear any
  stale backend static assets (see Backend Integration below) before testing.

---

**Features**

**Core Capabilities**
- **Chat Management**: Create, edit, and delete chat configurations with support
  for multiple simultaneous conversations. Each chat maintains its own settings,
  collections, and conversation history.
- **Multi-Language Support**: Built-in internationalization with English and Finnish
  translations (i18next). Language can be switched from the UI header.
- **Markdown & HTML Rendering**: Chat messages support full markdown rendering with
  automatic HTML-to-markdown conversion for proper display of formatted content.

**LLM Integration**
- **Remote LLM Mode**: Connect to remote OpenAI-compatible backends or custom
  LLM providers. Supports streaming responses and configurable model parameters.
- **Local WebGPU LLM Mode**: Run LLM models entirely in the browser using
  `transformers.js` with WebGPU acceleration (when available). This mode:
  - Loads ONNX-format models optimized for web (e.g., from HuggingFace)
  - Downloads models on-demand with progress tracking
  - Runs inference locally with no API costs or data privacy concerns
  - Supports per-chat WebGPU configuration including model selection and size limits
  - Allows custom prompt templates and system prompts per chat

**WebGPU Configuration**
Each chat can have its own WebGPU settings:
- **Model Selection**: Search and select from HuggingFace ONNX web-compatible models
  with multi-term case-insensitive search
- **Size Limits**: Configure maximum model size (in MB) to control downloads
- **Prompt Customization**: Override system prompts, context prompts, memory prompts,
  and extractor prompts at the chat level
- **Prompt Rewriting**: Enable automatic user prompt enhancement with custom templates
- **Independent Settings**: Each chat maintains separate WebGPU config without
  affecting other chats

**Retrieval-Augmented Generation (RAG)**
- **Document Collections**: Organize documents into collections for targeted retrieval
- **Context Integration**: Both remote and local modes use retrieved document chunks
  to augment LLM context and improve answer relevance
- **Document Onboarding**: Upload and process documents through the backend for
  indexing and retrieval

**Additional Features**
- **Chat Entity Settings**: Configure model parameters (temperature, max tokens, top-p)
  per chat
- **Streaming Responses**: Real-time token-by-token response display
- **Mode Switching**: Toggle between remote and local LLM modes from the home screen
- **Responsive UI**: Works on desktop and mobile browsers with React Native Web

Notes:
- Local WebGPU mode requires a modern browser with WebGPU support (Chrome 113+,
  Edge 113+) and downloads models on first use
- Remote mode requires configured backend endpoints; see backend documentation
  for setup
- WebGPU models can be 1-10GB+; ensure adequate bandwidth and storage

---

**Build (production)**
- Produce a production build (outputs to `dist`):

```powershell
cd simple-rag-ui
npm run build
```

- After a successful build the optimized files are available in `simple-rag-ui/dist`.
- A bundle visualizer may be present at `simple-rag-ui/dist/stats.html` if enabled
  during the last build — open it to inspect module sizes and chunking.

---

**Backend integration (Spring Boot)**
- The backend serves the built frontend from `simple-rag-server/target/classes/static`.
  To update the packaged frontend that the server serves:

```powershell
# 1) Build frontend
cd simple-rag-ui; npm run build

# 2) Repackage backend so the new `dist` is copied into the jar
cd ..\simple-rag-server
mvn -DskipTests package
```

- Alternatively, copy the contents of `simple-rag-ui/dist` into
  `simple-rag-server/src/main/resources/static` (or `target/classes/static` during development)
  before building the backend.

---

**Maven integration (monorepo build)**
- This project is configured so the frontend can be built and packaged by Maven:
  - `simple-rag-ui/pom.xml` uses the `frontend-maven-plugin` to install Node/npm,
    run `npm install`, and run `npm run build` during the Maven build lifecycle.
  - The `maven-resources-plugin` copies the generated `dist` output into
    the module's `outputDirectory/static` so it becomes part of the module
    resources.
- As a convenience, building the complete project from the repository root
  will build the frontend and include its assets in the final server artifact.

Example (build entire repository and package the server JAR):

```powershell
# from the repository root
mvn -DskipTests package
```

Notes:
- The first Maven build may take longer because `frontend-maven-plugin` downloads
  and installs a Node runtime and runs `npm install` for the frontend.
- If you prefer to iterate quickly on the frontend, run `npm run build` inside
  `simple-rag-ui` locally and then build the backend — this avoids re-running
  npm install during each Maven run.


---

**WASM & Performance Notes**
- The on-device/in-browser transformer uses an ONNX WASM runtime which places
  a `.wasm` asset in the build (e.g. `ort-wasm-*.wasm`). That file is often
  the largest network payload (~tens of MB).
- Recommendations:
  - Host the WASM via a CDN or separate static route so the main page
    doesn't need to download it on first load.
  - Lazy-load the transformer runtime only when the user begins an action
    that requires local inference (use dynamic `import()` or React.lazy).
  - Use Vite/Rollup `manualChunks` to separate large runtime modules into
    their own chunks (this project already includes manual chunking rules).

---

**Troubleshooting**
- "props.pointerEvents is deprecated. Use style.pointerEvents":
  - This warning is commonly emitted by leftover React Native / Expo web
    artifacts in previously-built bundles. Build the frontend fresh and
    repackage the backend so it serves the new assets.

- If the browser still attempts to connect to `ws://localhost:8081` or
  loads `expo.js` after you updated the source, make sure there are no
  stale files in `simple-rag-server/target/classes/static/assets` —
  clean the backend build and re-run `mvn package`.

- Source map tools (e.g. source-map-explorer) may report warnings for very large
  bundles; use the `vite-plugin-visualizer` HTML report for more robust
  interactive analysis.

---

**Development tips**
- Icons: the project migrated from `@expo/vector-icons` to `react-icons`.
  If you see missing glyphs in web builds, ensure components import the
  local icon wrapper (`src/components/Icons.tsx`).
- i18n: translations live under `src/i18n` and language switching is available
  from the header UI.
- To reduce initial bundle size, prefer dynamic imports for heavy
  components (transformers, PDF renderer, large modals).

---

**Technology Stack**
- **React Native Web**: Write once, run on web with native-like components
- **Vite**: Fast build tool with HMR for development
- **TypeScript**: Type-safe development
- **React Native Paper**: Material Design UI components
- **Transformers.js**: WebGPU-accelerated LLM inference in browser
- **i18next**: Internationalization framework
- **Axios**: HTTP client for backend API communication
- **React Navigation**: Navigation structure (stack + tab navigators)

---

**Contributing**
- Fork, create a topic branch, and open a PR. Keep changes focused and
  run the frontend build locally before opening the PR.
- Ensure translations are updated for both English and Finnish when adding new UI text
- Test both remote and local WebGPU modes before submitting

---

**License & Contact**
- See the repository root `LICENSE` file for project licensing.
- For questions or issues, open an issue in the repository or contact the maintainer.
