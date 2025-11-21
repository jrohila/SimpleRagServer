import React from 'react';
import { View, Text, ActivityIndicator, StyleSheet, Platform } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import Markdown from 'react-native-markdown-display';

export interface Message {
  _id: string | number;
  text: string;
  createdAt: Date;
  user: {
    _id: number;
    name?: string;
  };
  isLoading?: boolean;
}

interface ChatMessageProps {
  message: Message;
  showUsername?: boolean;
}

export const ChatMessage: React.FC<ChatMessageProps> = ({ message, showUsername = true }) => {
  const isUser = message.user._id === 1;
  const isAssistant = message.user._id === 2;

  return (
    <View style={styles.messageContainer}>
      {showUsername && (
        <View style={styles.usernameContainer}>
          {isAssistant && (
            <Ionicons name="sparkles" size={14} color="#007aff" style={styles.usernameIcon} />
          )}
          {isUser && (
            <Ionicons name="person-circle" size={14} color="#666" style={styles.usernameIcon} />
          )}
          <Text style={styles.usernameText}>
            {isUser ? 'You' : 'Assistant'}
          </Text>
        </View>
      )}
      
      <View style={[
        styles.bubble,
        isUser ? styles.userBubble : styles.assistantBubble
      ]}>
        {message.isLoading && !message.text ? (
          <View style={styles.loadingContainer}>
            <ActivityIndicator size="small" color="#666" />
            <View style={styles.loadingBarsContainer}>
              <View style={styles.loadingBar1} />
              <View style={styles.loadingBar2} />
            </View>
          </View>
        ) : (
          <Markdown style={isAssistant ? markdownStyles.assistant : markdownStyles.user}>
            {message.text || ''}
          </Markdown>
        )}
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  messageContainer: {
    marginBottom: 20,
    alignItems: 'flex-start',
    paddingHorizontal: 12,
  },
  usernameContainer: {
    marginBottom: 4,
    marginLeft: 10,
    flexDirection: 'row',
    alignItems: 'center',
  },
  usernameIcon: {
    marginRight: 4,
  },
  usernameText: {
    color: '#666',
    fontSize: 12,
    fontWeight: '600',
  },
  bubble: {
    maxWidth: '85%',
    paddingHorizontal: 12,
    paddingVertical: 10,
    borderRadius: 12,
    borderWidth: 1,
  },
  userBubble: {
    backgroundColor: '#fff',
    borderColor: '#e0e0e0',
  },
  assistantBubble: {
    backgroundColor: '#e3f2fd',
    borderColor: '#2196f3',
  },
  loadingContainer: {
    padding: 8,
    flexDirection: 'row',
    alignItems: 'center',
  },
  loadingBarsContainer: {
    marginLeft: 8,
  },
  loadingBar1: {
    width: 40,
    height: 8,
    backgroundColor: '#e0e0e0',
    borderRadius: 4,
    marginBottom: 4,
  },
  loadingBar2: {
    width: 60,
    height: 8,
    backgroundColor: '#e0e0e0',
    borderRadius: 4,
  },
});

const markdownStyles = {
  assistant: {
    body: { color: '#222', fontSize: 16 },
    heading1: { fontSize: 24, fontWeight: 'bold' as const, marginBottom: 8 },
    heading2: { fontSize: 20, fontWeight: 'bold' as const, marginBottom: 6 },
    heading3: { fontSize: 18, fontWeight: 'bold' as const, marginBottom: 4 },
    paragraph: { marginBottom: 8 },
    strong: { fontWeight: 'bold' as const },
    em: { fontStyle: 'italic' as const },
    s: { textDecorationLine: 'line-through' as const },
    code_inline: {
      backgroundColor: '#f0f0f0',
      color: '#d63384',
      paddingHorizontal: 4,
      paddingVertical: 2,
      borderRadius: 3,
      fontFamily: Platform.OS === 'ios' ? 'Menlo' : 'monospace',
    },
    code_block: {
      backgroundColor: '#f6f8fa',
      color: '#24292e',
      padding: 10,
      borderRadius: 6,
      fontFamily: Platform.OS === 'ios' ? 'Menlo' : 'monospace',
      marginBottom: 8,
    },
    fence: {
      backgroundColor: '#f6f8fa',
      color: '#24292e',
      padding: 10,
      borderRadius: 6,
      fontFamily: Platform.OS === 'ios' ? 'Menlo' : 'monospace',
      marginBottom: 8,
    },
    link: { color: '#0366d6', textDecorationLine: 'underline' as const },
    blockquote: {
      backgroundColor: '#f6f8fa',
      borderLeftColor: '#dfe2e5',
      borderLeftWidth: 4,
      paddingLeft: 10,
      marginLeft: 0,
      marginBottom: 8,
    },
    list_item: { flexDirection: 'row' as const, marginBottom: 4 },
    bullet_list: { marginBottom: 8 },
    ordered_list: { marginBottom: 8 },
    hr: { backgroundColor: '#e0e0e0', height: 1, marginVertical: 8 },
    table: {
      borderWidth: 1,
      borderColor: '#dfe2e5',
      borderRadius: 6,
      marginBottom: 8,
      overflow: 'hidden' as const,
    },
    thead: {
      backgroundColor: '#0d47a1',
    },
    tbody: {
      backgroundColor: '#ffffff',
    },
    th: {
      backgroundColor: '#0d47a1',
      color: '#ffffff',
      fontWeight: 'bold' as const,
      padding: 8,
      borderWidth: 1,
      borderColor: '#1565c0',
    },
    tr: {
      borderBottomWidth: 1,
      borderBottomColor: '#e0e0e0',
    },
    td: {
      backgroundColor: '#ffffff',
      color: '#222',
      padding: 8,
      borderWidth: 1,
      borderColor: '#e0e0e0',
    },
  },
  user: {
    body: { color: '#222', fontSize: 16 },
    paragraph: { marginBottom: 8 },
    strong: { fontWeight: 'bold' as const },
    em: { fontStyle: 'italic' as const },
    code_inline: {
      backgroundColor: '#f0f0f0',
      color: '#d63384',
      paddingHorizontal: 4,
      paddingVertical: 2,
      borderRadius: 3,
      fontFamily: Platform.OS === 'ios' ? 'Menlo' : 'monospace',
    },
    table: {
      borderWidth: 1,
      borderColor: '#dfe2e5',
      borderRadius: 6,
      marginBottom: 8,
      overflow: 'hidden' as const,
    },
    thead: {
      backgroundColor: '#666666',
    },
    tbody: {
      backgroundColor: '#f9f9f9',
    },
    th: {
      backgroundColor: '#666666',
      color: '#ffffff',
      fontWeight: 'bold' as const,
      padding: 8,
      borderWidth: 1,
      borderColor: '#888888',
    },
    tr: {
      borderBottomWidth: 1,
      borderBottomColor: '#e0e0e0',
    },
    td: {
      backgroundColor: '#f9f9f9',
      color: '#222',
      padding: 8,
      borderWidth: 1,
      borderColor: '#e0e0e0',
    },
  },
};
