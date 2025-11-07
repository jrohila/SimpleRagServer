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
import { Profile } from './screens/Profile';
import { Settings } from './screens/Settings';
import { Updates } from './screens/Updates';
import { NotFound } from './screens/NotFound';

const HomeTabs = createBottomTabNavigator({
  screens: {
    Home: {
      screen: Home,
      options: {
        title: 'Feed',
        tabBarIcon: ({ color, size }) => (
          <Image
            source={newspaper}
            tintColor={color}
            style={{ width: size, height: size }}
          />
        ),
      },
    },
    Updates: {
      screen: Updates,
      options: {
        tabBarIcon: ({ color, size }) => (
          <Image
            source={bell}
            tintColor={color}
            style={{ width: size, height: size }}
          />
        ),
      },
    },
  },
});

const Drawer = createDrawerNavigator({
  screens: {
    HomeTabs: {
      screen: HomeTabs,
      options: {
        title: 'Home',
      },
    },
    Profile: {
      screen: Profile,
      options: {
        title: 'Profile',
      },
    },
    Settings: {
      screen: Settings,
      options: {
        title: 'Settings',
      },
    },
  },
});


// Drawer as the main navigator
export const Navigation = createStaticNavigation(Drawer);

type DrawerParamList = StaticParamList<typeof Drawer>;

declare global {
  namespace ReactNavigation {
    interface RootParamList extends DrawerParamList {}
  }
}
