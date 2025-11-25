import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import reactNativeWeb from 'vite-plugin-react-native-web';
import { visualizer } from 'rollup-plugin-visualizer';

export default defineConfig({
  plugins: [
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
