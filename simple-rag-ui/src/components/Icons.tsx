import React from 'react';
import { Text, TextProps, StyleProp, TextStyle } from 'react-native';
import {
  IoSparkles,
  IoPersonCircle,
  IoSend,
  IoChatbubbles,
  IoPerson,
  IoChatbubble,
} from 'react-icons/io5';
import {
  MdMemory,
  MdDeveloperBoard,
  MdRocketLaunch,
  MdRocket,
  MdDeleteOutline,
  MdDelete,
  MdHome,
  MdHomeFilled,
  MdSearch,
  MdOutlineSearch,
  MdChatBubbleOutline,
  MdChatBubble,
  MdFolder,
  MdOutlineFolder,
  MdDescription,
  MdInsertDriveFile,
  MdViewList,
  MdList,
  MdPublic,
  MdLanguage,
  MdKeyboardArrowUp,
  MdArrowUpward,
  MdKeyboardArrowDown,
  MdArrowDownward,
  MdInfo,
  MdOutlineInfo,
  MdSettings,
  MdOutlineSettings,
} from 'react-icons/md';

type IconProps = {
  name: string;
  size?: number;
  color?: string;
  style?: StyleProp<TextStyle>;
} & TextProps;

// Friendly mappings from logical icon names to react-icons components.
// We attempt multiple candidate names when possible so small name differences
// between icon sets won't break rendering (helps during migration off Expo).
const ionMap: Record<string, string | string[]> = {
  sparkles: ['IoSparkles', 'IoSparklesOutline'],
  'person-circle': ['IoPersonCircle', 'IoPersonCircleOutline', 'IoPerson'],
  send: ['IoSend', 'IoSendSharp'],
  chatbubbles: ['IoChatbubbles', 'IoChatbubble'],
  'hardware-chip': ['md:MdMemory', 'md:MdDeveloperBoard'],
  'rocket-outline': ['md:MdRocketLaunch', 'md:MdRocket'],
  'trash-outline': ['md:MdDeleteOutline', 'md:MdDelete'],
  trash: ['md:MdDelete', 'MdDelete'],
};

const mdMap: Record<string, string | string[]> = {
  'home-outline': ['MdHome', 'MdHomeFilled'],
  magnify: ['MdSearch', 'MdOutlineSearch'],
  'rocket-launch-outline': ['MdRocketLaunch', 'MdRocket'],
  'chat-outline': ['MdChatBubbleOutline', 'MdChatBubble'],
  'folder-outline': ['MdFolder', 'MdOutlineFolder'],
  'file-document-outline': ['MdDescription', 'MdInsertDriveFile'],
  'view-list-outline': ['MdViewList', 'MdList'],
  earth: ['MdPublic', 'MdLanguage'],
  'chevron-up': ['MdKeyboardArrowUp', 'MdArrowUpward'],
  'chevron-down': ['MdKeyboardArrowDown', 'MdArrowDownward'],
  information: ['MdInfo', 'MdOutlineInfo'],
  cog: ['MdSettings', 'MdOutlineSettings'],
  'file-document': ['MdDescription', 'MdInsertDriveFile'],
};

// Map logical icon names to specific react-icons components (named imports)
const ICON_COMPONENTS: Record<string, any> = {
  // Ionicons
  sparkles: IoSparkles,
  'person-circle': IoPersonCircle,
  person: IoPerson,
  send: IoSend,
  chatbubbles: IoChatbubbles,
  chatbubble: IoChatbubble,

  // Material icons (used via md: or directly)
  'hardware-chip': MdMemory,
  'hardware-developer-board': MdDeveloperBoard,
  'rocket-outline': MdRocketLaunch,
  'rocket': MdRocket,
  'trash-outline': MdDeleteOutline,
  trash: MdDelete,
  information: MdInfo,
  cog: MdSettings,
  'file-document': MdDescription,
  'home-outline': MdHome,
  magnify: MdSearch,
  'rocket-launch-outline': MdRocketLaunch,
  'chat-outline': MdChatBubbleOutline,
  'folder-outline': MdFolder,
  'file-document-outline': MdDescription,
  'view-list-outline': MdViewList,
  earth: MdPublic,
  'chevron-up': MdKeyboardArrowUp,
  'chevron-down': MdKeyboardArrowDown,
};

export const Ionicons: React.FC<IconProps> = ({ name, size = 16, color = '#000', style, ...rest }) => {
  try {
    const Comp = ICON_COMPONENTS[name];
    if (Comp) return <Comp size={size} color={color} style={style as any} {...rest} />;
  } catch (e) {
    // eslint-disable-next-line no-console
    console.debug('Icon lookup error for', name, e);
  }
  // fallback glyphs
  const glyphs: Record<string, string> = {
    sparkles: '✨',
    'person-circle': '👤',
    send: '➤',
    chatbubbles: '💬',
    'hardware-chip': '💽',
    'rocket-outline': '🚀',
    'trash-outline': '🗑️',
    trash: '🗑️',
  };
  const glyph = glyphs[name] || '●';
  // eslint-disable-next-line no-console
  console.debug('Icon fallback used for', name, '->', glyph);
  return (
    <Text {...rest} style={[{ fontSize: size, color, lineHeight: size }, style]}>
      {glyph}
    </Text>
  );
};

export const MaterialCommunityIcons: React.FC<IconProps> = ({ name, size = 16, color = '#000', style, ...rest }) => {
  try {
    const Comp = ICON_COMPONENTS[name];
    if (Comp) return <Comp size={size} color={color} style={style as any} {...rest} />;
  } catch (_) {
    // ignore and fall back
  }
  const glyphs: Record<string, string> = {
    'home-outline': '🏠',
    magnify: '🔍',
    'rocket-launch-outline': '🚀',
    'chat-outline': '💬',
    'folder-outline': '📁',
    'file-document-outline': '📄',
    'view-list-outline': '📋',
    earth: '🌐',
    'chevron-up': '▲',
    'chevron-down': '▼',
  };
  const glyph = glyphs[name] || '■';
  // eslint-disable-next-line no-console
  console.debug('Material icon fallback used for', name, '->', glyph);
  return (
    <Text {...rest} style={[{ fontSize: size, color, lineHeight: size }, style]}>
      {glyph}
    </Text>
  );
};

export default {
  Ionicons,
  MaterialCommunityIcons,
};
