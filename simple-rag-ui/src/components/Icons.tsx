import React from 'react';
import { Text, TextProps, StyleProp, TextStyle } from 'react-native';
import * as IoIcons from 'react-icons/io5';
import * as MdIcons from 'react-icons/md';

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

// Helper: try to resolve a component name from provided module map
function resolveIconFromModule(module: any, candidates: string[] | string | undefined) {
  if (!candidates) return null;
  const list = Array.isArray(candidates) ? candidates : [candidates];
  for (const name of list) {
    const cleaned = name.replace(/^md:/, '');
    if (module && (module as any)[cleaned]) return (module as any)[cleaned];
  }
  return null;
}

export const Ionicons: React.FC<IconProps> = ({ name, size = 16, color = '#000', style, ...rest }) => {
  try {
    const candidates = ionMap[name];
    // Try IoIcons first
    const IoComp = resolveIconFromModule(IoIcons, candidates as any);
    if (IoComp) return <IoComp size={size} color={color} style={style as any} {...rest} />;

    // Try MdIcons (supports entries prefixed with `md:` or fallbacks)
    const MdComp = resolveIconFromModule(MdIcons, candidates as any);
    if (MdComp) return <MdComp size={size} color={color} style={style as any} {...rest} />;
  } catch (e) {
    // ignore and fall back to glyph
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
    const candidates = mdMap[name];
    const MdComp = resolveIconFromModule(MdIcons, candidates as any);
    if (MdComp) return <MdComp size={size} color={color} style={style as any} {...rest} />;

    // If not found in material set, try ion set for similar names
    const IoComp = resolveIconFromModule(IoIcons, candidates as any);
    if (IoComp) return <IoComp size={size} color={color} style={style as any} {...rest} />;
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
