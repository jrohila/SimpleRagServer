

import React from 'react';
import { useTranslation } from 'react-i18next';
import { DrawerContentScrollView, DrawerItem } from '@react-navigation/drawer';
import { View, StyleSheet } from 'react-native';
import LanguageSwitcher from '../components/LanguageSwitcher';










export function CustomDrawerContent(props: any) {
  // Get the drawer routes in order
  const { state, navigation, descriptors } = props;
  const routes = state.routes;

  // Find indices for Home, Search, Onboarding, Chats
  const homeIdx = routes.findIndex((r: any) => r.name === 'Home');
  const searchIdx = routes.findIndex((r: any) => r.name === 'Search');
  const onboardingIdx = routes.findIndex((r: any) => r.name === 'Onboarding');
  const chatsIdx = routes.findIndex((r: any) => r.name === 'Chats');

  return (
    <DrawerContentScrollView {...props}>
      {routes.map((route: any, idx: number) => (
        <React.Fragment key={route.key}>
          <DrawerItem
            label={((): any => {
              // Prefer explicit translation for known navigation keys
              try {
                const { t } = (useTranslation as any)();
                const key = `navigation.${route.name.toLowerCase()}`;
                const translated = t(key);
                // if translation returns the key itself, fall back to descriptor title or route name
                if (translated && translated !== key) return translated;
              } catch (_) {
                // fall through
              }
              return descriptors[route.key].options.title || route.name;
            })()}
            focused={state.index === idx}
            onPress={() => navigation.navigate(route.name)}
            icon={descriptors[route.key].options.drawerIcon}
          />
          {/* Divider after Home, after Search, and after Onboarding */}
          {(
            (idx === homeIdx && searchIdx === homeIdx + 1) ||
            (idx === searchIdx && onboardingIdx === searchIdx + 1) ||
            (idx === onboardingIdx && chatsIdx === onboardingIdx + 1)
          ) && (
            <View style={styles.divider} />
          )}
        </React.Fragment>
      ))}
      <View style={styles.switcherContainer}>
        <LanguageSwitcher />
      </View>
    </DrawerContentScrollView>
  );
}





const styles = StyleSheet.create({
  divider: {
    height: 1,
    backgroundColor: '#e0e0e0',
    marginVertical: 8,
    marginHorizontal: 16,
    borderRadius: 1,
  },
  switcherContainer: {
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderTopWidth: 1,
    borderTopColor: '#eee',
  },
});




