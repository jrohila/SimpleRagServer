# Transformers.js v4 Setup for Browser

This project uses transformers.js v4 (alpha) from the GitHub v4 branch. Since v4 is still in development and doesn't have pre-built browser bundles, we've implemented a custom setup to make it work.

## What was done

### 1. Package Installation
- Installed directly from GitHub: `huggingface/transformers.js#v4`
- This gives us version `4.0.0-alpha.0`

### 2. Custom Vite Configuration
Created a custom plugin in `vite.config.ts` that:
- Resolves `@huggingface/transformers` to the source files
- Intercepts Node.js built-in modules (`node:fs`, `node:stream`, etc.)
- Replaces them with our empty stub module (`empty-module.js`)

### 3. Empty Module Stub
Created `empty-module.js` that exports:
- Empty implementations of Node.js APIs (fs, stream, path, etc.)
- These are no-ops that prevent errors when the v4 code tries to use Node.js features

### 4. Post-Install Patch
Created `patch-transformers.js` that automatically runs after `npm install`:
- Modifies the transformers package.json
- Changes exports to point to `./src/transformers.js` instead of missing dist files
- This is necessary because the v4 branch doesn't include build scripts in the npm package

## Files Modified/Created

1. **vite.config.ts** - Added custom plugin for module resolution
2. **empty-module.js** - Stub for Node.js modules  
3. **patch-transformers.js** - Post-install script
4. **package.json** - Added postinstall script
5. **node_modules/@huggingface/transformers/package.json** - Patched exports (auto-applied)

## Important Notes

⚠️ **This is an alpha version** - v4 is still under development

✅ **Build works** - The project successfully builds with v4

🔄 **After npm install** - The patch script automatically runs to fix the package

## API Changes in v4

The LocalLLMService.ts code didn't require changes because:
- The `pipeline`, `env`, and `TextStreamer` APIs remain the same
- Environment configuration (`env.useBrowserCache`, etc.) works without `@ts-ignore`

## If You Want to Go Back to v3

Simply change package.json:
```json
"@huggingface/transformers": "^3.8.1"
```

And remove the v4-specific files:
- empty-module.js
- patch-transformers.js  
- The custom plugin from vite.config.ts

## Build Output

Successfully builds with:
- transformers chunk: ~603 KB (gzipped: ~173 KB)
- All features working including WebGPU support
