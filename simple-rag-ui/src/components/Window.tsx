import React from 'react';
import { View, ViewProps } from 'react-native';
import styles from '../styles/WindowStyles';

interface WindowProps extends ViewProps {
  children: React.ReactNode;
}

export const Window: React.FC<WindowProps> = ({ children, style, ...rest }) => (
  <View style={[styles.window, style]} {...rest}>
    {children}
  </View>
);

// styles moved to `src/styles/WindowStyles.ts`
