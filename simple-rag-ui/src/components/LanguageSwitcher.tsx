import React, { useState, useEffect } from 'react';
import { View, Text, Pressable, Platform } from 'react-native';
import styles from '../styles/LanguageSwitcherStyles';
import { MaterialCommunityIcons } from './Icons';
import i18n from '../i18n';

const storeLang = async (lng: string) => {
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

// styles moved to `src/styles/LanguageSwitcherStyles.ts`
