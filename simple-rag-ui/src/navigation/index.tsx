import { Search } from './screens/Search';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { createDrawerNavigator } from '@react-navigation/drawer';
import { HeaderButton, Text } from '@react-navigation/elements';
import {
  createStaticNavigation,
  StaticParamList,
} from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { Image } from 'react-native';
import bell from '../assets/bell.png';
import newspaper from '../assets/newspaper.png';


import { Home } from './screens/Home';
import { Onboarding } from './screens/Onboarding';
import { Chats } from './screens/Chats';
import { Collections } from './screens/Collections';
import { Documents } from './screens/Documents';
import { Chunks } from './screens/Chunks';
import { useTranslation } from 'react-i18next';

import { MaterialCommunityIcons } from '@expo/vector-icons';
import { CustomDrawerContent } from './CustomDrawerContent';



const Drawer = createDrawerNavigator({
  screens: {
    Home: { screen: Home, options: { drawerIcon: ({ color, size }) => <MaterialCommunityIcons name="home-outline" size={size} color={color} /> } },
    Search: { screen: Search, options: { drawerIcon: ({ color, size }) => <MaterialCommunityIcons name="magnify" size={size} color={color} /> } },
    Onboarding: { screen: Onboarding, options: { drawerIcon: ({ color, size }) => <MaterialCommunityIcons name="rocket-launch-outline" size={size} color={color} /> } },
    Chats: { screen: Chats, options: { drawerIcon: ({ color, size }) => <MaterialCommunityIcons name="chat-outline" size={size} color={color} /> } },
    Collections: { screen: Collections, options: { drawerIcon: ({ color, size }) => <MaterialCommunityIcons name="folder-outline" size={size} color={color} /> } },
    Documents: { screen: Documents, options: { drawerIcon: ({ color, size }) => <MaterialCommunityIcons name="file-document-outline" size={size} color={color} /> } },
    Chunks: { screen: Chunks, options: { drawerIcon: ({ color, size }) => <MaterialCommunityIcons name="view-list-outline" size={size} color={color} /> } },
  },
  drawerContent: (props) => <CustomDrawerContent {...props} />,
});

export const Navigation = createStaticNavigation(Drawer);

type DrawerParamList = StaticParamList<typeof Drawer>;

declare global {
  namespace ReactNavigation {
    interface RootParamList extends DrawerParamList {}
  }
}
