import React from 'react';
import { View, Text, ActivityIndicator, Platform } from 'react-native';
import styles, { markdownStyles } from '../styles/ChatMessageStyles';
import { Ionicons } from './Icons';
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

  // Convert common HTML tags to markdown equivalents
  const convertHtmlToMarkdown = (text: string): string => {
    return text
      // Line breaks - use two spaces + newline for soft breaks (works in tables)
      .replace(/<br\s*\/?>/gi, '  \n')
      .replace(/<\/p>/gi, '\n\n')
      .replace(/<p>/gi, '')
      // Bold
      .replace(/<strong>(.*?)<\/strong>/gi, '**$1**')
      .replace(/<b>(.*?)<\/b>/gi, '**$1**')
      // Italic
      .replace(/<em>(.*?)<\/em>/gi, '*$1*')
      .replace(/<i>(.*?)<\/i>/gi, '*$1*')
      // Code
      .replace(/<code>(.*?)<\/code>/gi, '`$1`')
      // Links
      .replace(/<a\s+href="([^"]+)"[^>]*>(.*?)<\/a>/gi, '[$2]($1)')
      // Lists
      .replace(/<li>(.*?)<\/li>/gi, '- $1\n')
      .replace(/<ul>|<\/ul>/gi, '\n')
      .replace(/<ol>|<\/ol>/gi, '\n')
      // Headers
      .replace(/<h1>(.*?)<\/h1>/gi, '# $1\n')
      .replace(/<h2>(.*?)<\/h2>/gi, '## $1\n')
      .replace(/<h3>(.*?)<\/h3>/gi, '### $1\n')
      .replace(/<h4>(.*?)<\/h4>/gi, '#### $1\n')
      // Remove other HTML tags
      .replace(/<[^>]+>/g, '');
  };

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
            {convertHtmlToMarkdown(message.text || '')}
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
