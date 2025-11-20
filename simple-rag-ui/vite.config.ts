import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import reactNativeWeb from 'vite-plugin-react-native-web';

export default defineConfig({
  plugins: [
    react(),
    reactNativeWeb(),
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
    sourcemap: true,
  },
});
