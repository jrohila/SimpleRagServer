# simple-rag-ui

Frontend for the SimpleRagServer project — a lightweight React web UI
that works with the SimpleRagServer backend to provide RAG (Retrieval-Augmented
Generation) features, local inference options, and document onboarding.

This README focuses on how to develop, build and integrate the frontend
in this mono-repo.

**Contents**
- **Project**: brief overview
- **Quick Start**: dev commands
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
- **Admin & Power User UI**: the interface exposes lightweight admin and
  power-user capabilities such as managing chats, selecting active chat
  conversations, viewing chat metadata, and performing administrative
  actions (delete, export, or archive chats).
- **Chat interaction**: users can select a chat, enter prompts, and receive
  LLM-generated responses. The chat UI supports streaming responses and
  markdown rendering where applicable.
- **Remote LLM mode**: by default the UI connects to a remote, OpenAI-compatible
  backend API (configurable via the server). In this mode the UI sends prompt
  + context to the backend, which performs inference and returns responses.
- **Local WebGPU LLM mode**: users can enable a local LLM mode where the UI
  uses `transformers.js` with WebGPU (when available) to load and run an
  LLM model in the browser. This mode downloads a model runtime and a
  potentially large WASM/WebGPU asset — see the "WASM & Performance Notes"
  section for guidance on offloading or lazy-loading these files.
- **Retrieval-Augmented Generation (RAG)**: in both remote and local modes the
  UI integrates RAG: documents and uploaded content expand the knowledge base
  used to construct retrieval context for prompts. The client collects
  retrieval results and sends them to the LLM (remote or local) as context
  to improve answer relevance.

Notes:
- Local (transformers.js) mode is opt-in and can be toggled from the UI; when
  enabled the large runtime/model assets are fetched on demand.
- Remote mode requires a compatible backend endpoint; the project supports
  OpenAI-compatible APIs and custom backend implementations.

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

**Contributing**
- Fork, create a topic branch, and open a PR. Keep changes focused and
  run the frontend build locally before opening the PR.

---

**License & Contact**
- See the repository root `LICENSE` file for project licensing.
- For questions or issues, open an issue in the repository or contact the maintainer.
# Starter Template with React Navigation

This is a minimal starter template for React Native apps using Expo and React Navigation.

It includes the following:

- Example [Native Stack](https://reactnavigation.org/docs/native-stack-navigator) with a nested [Bottom Tab](https://reactnavigation.org/docs/bottom-tab-navigator)
- Web support with [React Native for Web](https://necolas.github.io/react-native-web/)
- TypeScript support and configured for React Navigation
- Automatic [deep link](https://reactnavigation.org/docs/deep-linking) and [URL handling configuration](https://reactnavigation.org/docs/configuring-links)
- Theme support [based on system appearance](https://reactnavigation.org/docs/themes/#using-the-operating-system-preferences)
- Expo [Development Build](https://docs.expo.dev/develop/development-builds/introduction/) with [Continuous Native Generation](https://docs.expo.dev/workflow/continuous-native-generation/)

## Getting Started

1. Create a new project using this template:

   ```sh
   npx create-expo-app@latest --template react-navigation/template
   ```

2. Edit the `app.json` file to configure the `name`, `slug`, `scheme` and bundle identifiers (`ios.bundleIdentifier` and `android.bundleIdentifier`) for your app.

3. Edit the `src/App.tsx` file to start working on your app.

## Running the app

- Install the dependencies:

  ```sh
  npm install
  ```

- Start the development server:

  ```sh
  npm start
  ```

- Build and run iOS and Android development builds:

  ```sh
  npm run ios
  # or
  npm run android
  ```

- In the terminal running the development server, press `i` to open the iOS simulator, `a` to open the Android device or emulator, or `w` to open the web browser.

## Notes

This project uses a [development build](https://docs.expo.dev/develop/development-builds/introduction/) and cannot be run with [Expo Go](https://expo.dev/go). To run the app with Expo Go, edit the `package.json` file, remove the `expo-dev-client` package and `--dev-client` flag from the `start` script.

We highly recommend using the development builds for normal development and testing.

The `ios` and `android` folder are gitignored in the project by default as they are automatically generated during the build process ([Continuous Native Generation](https://docs.expo.dev/workflow/continuous-native-generation/)). This means that you should not edit these folders directly and use [config plugins](https://docs.expo.dev/config-plugins/) instead. However, if you need to edit these folders, you can remove them from the `.gitignore` file so that they are tracked by git.

## Resources

- [React Navigation documentation](https://reactnavigation.org/)
- [Expo documentation](https://docs.expo.dev/)

---

Demo assets are from [lucide.dev](https://lucide.dev/)
