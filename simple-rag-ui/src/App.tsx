import { Assets as NavigationAssets } from '@react-navigation/elements';
import { DarkTheme, DefaultTheme } from '@react-navigation/native';
import * as React from 'react';
import { useColorScheme } from 'react-native';
import { Navigation } from './navigation';
import newspaperImage from './assets/newspaper.png';
import bellImage from './assets/bell.png';
// Initialize i18n (loads translations and sets language)
import './i18n';
import useHealthCheck from './hooks/useHealthCheck';
import BackendDownModal from './components/BackendDownModal';

// Optionally preload images for web (no-op if not needed)
try {
  if (typeof window !== 'undefined' && window.document) {
    const imgs = [newspaperImage, bellImage];
    imgs.forEach((src) => {
      const img = new Image();
      // @ts-ignore
      img.src = src;
    });
  }
} catch (e) {
  // ignore preload errors
}

const prefix = typeof window !== 'undefined' ? window.location.origin + '/' : '/';

export function App() {
  // Use the default theme for the app
  const theme = DefaultTheme;
  const { up, forceCheck } = useHealthCheck();

  return (
    <>
      <Navigation
        theme={theme}
        linking={{
          enabled: 'auto',
          prefixes: [prefix],
        }}
        onReady={() => {
          // no-op for web
        }}
      />
      {/* backend modal overlays the app when backend is down */}
      <BackendDownModal visible={!up} onRetry={forceCheck} />
    </>
  );
}
