import React from 'react';
import { Modal, View, Text, Pressable, StyleSheet } from 'react-native';
import { useTranslation } from 'react-i18next';
import { GlobalStyles, Colors, BorderRadius, Spacing } from '../styles/GlobalStyles';

type Props = {
  visible: boolean;
  onRetry?: () => void;
};

const styles = StyleSheet.create({
  webOverlay: {
    position: 'fixed' as any,
    left: 0,
    top: 0,
    right: 0,
    bottom: 0,
    backgroundColor: 'rgba(0,0,0,0.6)',
    display: 'flex' as any,
    justifyContent: 'center' as any,
    alignItems: 'center' as any,
    padding: Spacing.xl,
    zIndex: 2000,
  },
  webContent: {
    width: '100%' as any,
    maxWidth: 560,
    backgroundColor: Colors.white,
    borderRadius: BorderRadius.lg,
    padding: Spacing.xl,
    textAlign: 'center' as any,
  },
  webButton: {
    padding: `${Spacing.sm}px ${Spacing.md}px`,
    backgroundColor: Colors.primary,
    color: Colors.white,
    border: 'none',
    borderRadius: BorderRadius.md,
  },
  modalOverlay: {
    ...GlobalStyles.modalOverlay,
    backgroundColor: 'rgba(0,0,0,0.6)',
  },
  modalContent: {
    ...GlobalStyles.modalContent,
    maxWidth: 560,
  },
  retryButton: {
    ...GlobalStyles.button,
    ...GlobalStyles.buttonPrimary,
    borderRadius: BorderRadius.md,
  },
});

export default function BackendDownModal({ visible, onRetry }: Props) {
  const { t } = useTranslation();
  // If running on the web, render a simple HTML overlay instead of relying solely on
  // the React Native `Modal` implementation (which can behave inconsistently on web builds).
  const isWeb = typeof window !== 'undefined' && typeof window.document !== 'undefined';

  if (isWeb) {
    if (!visible) return null;
    return (
      <div style={styles.webOverlay as any}>
        <div style={styles.webContent as any}>
          <h3 style={{ margin: '0 0 8px 0' }}>{t('backend.unavailable.title')}</h3>
          <p style={{ marginBottom: 18 }}>{t('backend.unavailable.message')}</p>
          <div style={{ display: 'flex', justifyContent: 'center' }}>
            <button onClick={onRetry} style={styles.webButton as any}>
              {t('backend.unavailable.retry')}
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <Modal visible={visible} transparent animationType="fade" onRequestClose={() => {}}>
      <View style={styles.modalOverlay}>
        <View style={styles.modalContent}>
          <Text style={GlobalStyles.modalTitle}>{t('backend.unavailable.title')}</Text>
          <Text style={GlobalStyles.modalMessage}>{t('backend.unavailable.message')}</Text>
          <View style={{ flexDirection: 'row' }}>
            <Pressable onPress={onRetry} style={styles.retryButton}>
              <Text style={GlobalStyles.buttonText}>{t('backend.unavailable.retry')}</Text>
            </Pressable>
          </View>
        </View>
      </View>
    </Modal>
  );
}
