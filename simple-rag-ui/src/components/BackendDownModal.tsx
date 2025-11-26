import React from 'react';
import { Modal, View, Text, Pressable } from 'react-native';
import { useTranslation } from 'react-i18next';

type Props = {
  visible: boolean;
  onRetry?: () => void;
};

export default function BackendDownModal({ visible, onRetry }: Props) {
  const { t } = useTranslation();
  // If running on the web, render a simple HTML overlay instead of relying solely on
  // the React Native `Modal` implementation (which can behave inconsistently on web builds).
  const isWeb = typeof window !== 'undefined' && typeof window.document !== 'undefined';

  if (isWeb) {
    if (!visible) return null;
    return (
      <div style={{
        position: 'fixed',
        left: 0,
        top: 0,
        right: 0,
        bottom: 0,
        backgroundColor: 'rgba(0,0,0,0.6)',
        display: 'flex',
        justifyContent: 'center',
        alignItems: 'center',
        padding: 20,
        zIndex: 2000,
      }}>
        <div style={{
          width: '100%',
          maxWidth: 560,
          backgroundColor: '#fff',
          borderRadius: 8,
          padding: 20,
          textAlign: 'center',
        }}>
          <h3 style={{ margin: '0 0 8px 0' }}>{t('backend.unavailable.title')}</h3>
          <p style={{ marginBottom: 18 }}>{t('backend.unavailable.message')}</p>
          <div style={{ display: 'flex', justifyContent: 'center' }}>
            <button onClick={onRetry} style={{ padding: '8px 12px', backgroundColor: '#1976D2', color: '#fff', border: 'none', borderRadius: 6 }}>
              {t('backend.unavailable.retry')}
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <Modal visible={visible} transparent animationType="fade" onRequestClose={() => {}}>
      <View style={{
        flex: 1,
        backgroundColor: 'rgba(0,0,0,0.6)',
        justifyContent: 'center',
        alignItems: 'center',
        padding: 20,
      }}>
        <View style={{
          width: '100%',
          maxWidth: 560,
          backgroundColor: '#fff',
          borderRadius: 8,
          padding: 20,
          alignItems: 'center',
        }}>
          <Text style={{ fontSize: 18, fontWeight: '600', marginBottom: 8 }}>{t('backend.unavailable.title')}</Text>
          <Text style={{ marginBottom: 18, textAlign: 'center' }}>{t('backend.unavailable.message')}</Text>
          <View style={{ flexDirection: 'row' }}>
            <Pressable onPress={onRetry} style={{ padding: 10, backgroundColor: '#1976D2', borderRadius: 6 }}>
              <Text style={{ color: '#fff' }}>{t('backend.unavailable.retry')}</Text>
            </Pressable>
          </View>
        </View>
      </View>
    </Modal>
  );
}
