import { Assets as NavigationAssets } from '@react-navigation/elements';
import { DarkTheme, DefaultTheme } from '@react-navigation/native';
import { Asset } from 'expo-asset';
import { createURL } from 'expo-linking';
import * as SplashScreen from 'expo-splash-screen';
import * as React from 'react';
import { useColorScheme } from 'react-native';
import { Navigation } from './navigation';
import newspaperImage from './assets/newspaper.png';
import bellImage from './assets/bell.png';

Asset.loadAsync([
  ...NavigationAssets,
  newspaperImage,
  bellImage,
]);

SplashScreen.preventAutoHideAsync();

const prefix = createURL('/');

export function App() {
  // Use the default theme for the app
  const theme = DefaultTheme;

  return (
    <Navigation
      theme={theme}
      linking={{
        enabled: 'auto',
        prefixes: [prefix],
      }}
      onReady={() => {
        SplashScreen.hideAsync();
      }}
    />
  );
}
