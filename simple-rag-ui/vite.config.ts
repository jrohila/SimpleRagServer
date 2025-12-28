import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import reactNativeWeb from 'vite-plugin-react-native-web';
import { visualizer } from 'rollup-plugin-visualizer';
import path from 'path';

// Custom plugin to resolve @huggingface/transformers v4
function transformersV4Plugin() {
  const transformersPath = path.resolve(__dirname, 'node_modules/@huggingface/transformers/src/transformers.js');
  const emptyModule = path.resolve(__dirname, 'empty-module.js');
  
  return {
    name: 'transformers-v4-resolver',
    enforce: 'pre', // Run before other plugins
    resolveId(id) {
      if (id === '@huggingface/transformers') {
        return transformersPath;
      }
      // Handle Node.js built-in modules - override Vite's default externalization
      if (id.startsWith('node:') || ['fs', 'path', 'url', 'stream', 'events', 'os', 'util', 'child_process', 'crypto'].includes(id)) {
        return { id: emptyModule, external: false };
      }
      // Exclude Node.js native addons from browser build
      if (id.includes('onnxruntime-node') || id.includes('sharp')) {
        return { id: emptyModule, external: false };
      }
    }
  };
}

export default defineConfig({
  plugins: [
    transformersV4Plugin(),
    react(),
    reactNativeWeb(),
    // visualizer generates an interactive `dist/stats.html` for bundle analysis
    visualizer({ filename: 'dist/stats.html', open: false, gzipSize: true }),
  ],
  resolve: {
    alias: {
      'react-native': 'react-native-web',
    },
    extensions: ['.web.tsx', '.web.ts', '.web.jsx', '.web.js', '.tsx', '.ts', '.jsx', '.js'],
  },
  optimizeDeps: {
    include: [
      'react-native-web',
      '@react-navigation/native',
      '@react-navigation/native-stack',
      'react-native-gesture-handler',
      'react-native-screens',
      'react-native-safe-area-context',
    ],
    exclude: [
      'onnxruntime-node', // Exclude Node.js native addon from browser build
      'sharp', // Exclude Node.js native image processing library
    ],
    esbuildOptions: {
      resolveExtensions: ['.web.tsx', '.web.ts', '.web.jsx', '.web.js', '.tsx', '.ts', '.jsx', '.js'],
      loader: {
        '.js': 'jsx',
      },
    },
  },
  server: {
    port: 8081,
    host: true,
  },
  build: {
    outDir: 'dist',
    // disable source maps for the production visualizer run to avoid map parsing issues
    sourcemap: false,
    // use terser for production minification which can produce
    // source maps more compatible with source-map-explorer
    minify: 'terser',
    terserOptions: {
      compress: {
        passes: 2,
      },
      format: {
        comments: false,
      },
      // keep source map generation stable
      mangle: true,
    },
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id) return;
          // Put heavy transformer / ONNX runtime related modules into their own chunk
          if (id.includes('transformers.web') || id.includes('@huggingface/transformers') || id.includes('onnxruntime-web') || id.includes('onnxruntime-common')) {
            return 'transformers';
          }
          // Group react and react-dom into vendor
          if (id.includes('node_modules') && (id.includes('react') || id.includes('react-dom'))) {
            return 'vendor-react';
          }
          // Group react-icons separately
          if (id.includes('node_modules') && id.includes('react-icons')) {
            return 'vendor-icons';
          }
        },
      },
    },
  },
});
