import React from 'react';
import { View, Text, ActivityIndicator, Platform } from 'react-native';
import styles, { markdownStyles } from '../styles/ChatMessageStyles';
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

  // Format timestamp
  const formatTimestamp = (date: Date): string => {
    const messageDate = new Date(date);
    
    const dateStr = messageDate.toLocaleDateString('en-US', { 
      month: 'short', 
      day: 'numeric' 
    });
    
    const timeStr = messageDate.toLocaleTimeString('en-US', { 
      hour: '2-digit', 
      minute: '2-digit',
      hour12: false 
    });
    
    return `${dateStr}, ${timeStr}`;
  };

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
      {!message.isLoading && message.text && (
        <Text style={styles.timestamp}>
          {formatTimestamp(message.createdAt)}
        </Text>
      )}
    </View>
  );
};

// styles moved to `src/styles/ChatMessageStyles.ts` (markdownStyles exported)
