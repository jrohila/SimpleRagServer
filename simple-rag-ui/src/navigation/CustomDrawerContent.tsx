

import React from 'react';
import { DrawerContentScrollView, DrawerItem } from '@react-navigation/drawer';
import { View, StyleSheet } from 'react-native';










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
            label={descriptors[route.key].options.title || route.name}
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
});




