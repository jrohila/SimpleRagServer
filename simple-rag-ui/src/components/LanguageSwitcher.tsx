import React, { useState, useEffect } from 'react';
import { View, Text, Pressable, StyleSheet, Platform } from 'react-native';
import { MaterialCommunityIcons } from '@expo/vector-icons';
import i18n from '../i18n';

const storeLang = async (lng: string) => {
  try {
    // Try AsyncStorage (native)
    // eslint-disable-next-line @typescript-eslint/no-var-requires
    const AsyncStorage = require('@react-native-async-storage/async-storage').default;
    if (AsyncStorage && AsyncStorage.setItem) {
      await AsyncStorage.setItem('user-lang', lng);
      return;
    }
  } catch (_) {
    // ignore
  }

  try {
    if (typeof window !== 'undefined' && window.localStorage) {
      window.localStorage.setItem('user-lang', lng);
    }
  } catch (_) {
    // ignore
  }
};

const loadStoredLang = async (): Promise<string | null> => {
  try {
    const AsyncStorage = require('@react-native-async-storage/async-storage').default;
    if (AsyncStorage && AsyncStorage.getItem) return await AsyncStorage.getItem('user-lang');
  } catch (_) {
    // ignore
  }
  try {
    if (typeof window !== 'undefined' && window.localStorage) return window.localStorage.getItem('user-lang');
  } catch (_) {
    // ignore
  }
  return null;
};

export default function LanguageSwitcher() {
  const [open, setOpen] = useState(false);
  const [current, setCurrent] = useState(i18n.language || 'en');

  useEffect(() => {
    // try to restore stored language
    (async () => {
      const stored = await loadStoredLang();
      if (stored && stored !== i18n.language) {
        try {
          await i18n.changeLanguage(stored);
          setCurrent(stored);
        } catch (_) {
          // ignore
        }
      }
    })();
  }, []);

  const change = async (lng: string) => {
    try {
      await i18n.changeLanguage(lng);
      setCurrent(lng);
      await storeLang(lng);
    } catch (_) {
      // ignore errors
    } finally {
      setOpen(false);
    }
  };

  const languages = [
    { key: 'en', label: 'English' },
    { key: 'fi', label: 'Suomi' },
  ];

  return (
    <View style={styles.row}>
      <MaterialCommunityIcons name="earth" size={20} style={styles.globeOutside} />
      <View style={styles.selectorWrapper}>
        <Pressable onPress={() => setOpen((s) => !s)} style={styles.selector} accessibilityRole="button" accessibilityLabel="Change language">
          <Text style={styles.selectorText}>{languages.find((l) => l.key === current)?.label || current}</Text>
          <MaterialCommunityIcons name={open ? 'chevron-up' : 'chevron-down'} size={18} style={styles.caretIcon} />
        </Pressable>

        {open && (
          <View style={[styles.dropdown, Platform.OS === 'web' ? styles.elevated : null]}>
            {languages.map((l) => (
              <Pressable key={l.key} onPress={() => change(l.key)} style={styles.item}>
                <Text style={[styles.itemText, l.key === current ? styles.itemCurrent : null]}>{l.label}</Text>
              </Pressable>
            ))}
          </View>
        )}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { width: '100%' },
  row: { flexDirection: 'row', alignItems: 'center', width: '100%' },
  globeOutside: { marginRight: 12, color: '#333', marginLeft: 2 },
  selectorWrapper: { flex: 1, position: 'relative' },
  selector: {
    paddingVertical: 8,
    paddingHorizontal: 12,
    borderRadius: 8,
    backgroundColor: 'transparent',
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    borderWidth: 1,
    borderColor: '#eee',
  },
  selectorText: { fontSize: 14, color: '#222' },
  caretIcon: { color: '#666' },
  dropdown: {
    position: 'absolute',
    top: '100%',
    left: 0,
    right: 0,
    marginTop: 6,
    backgroundColor: '#fff',
    borderRadius: 6,
    overflow: 'hidden',
    borderWidth: 1,
    borderColor: '#eee',
    zIndex: 999,
  },
  elevated: { boxShadow: '0 2px 6px rgba(0,0,0,0.15)' as any },
  item: { paddingVertical: 8, paddingHorizontal: 12 },
  itemText: { fontSize: 14 },
  itemCurrent: { fontWeight: '700' },
});
