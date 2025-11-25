import React, { useRef, useEffect } from 'react';
import { 
  View, 
  FlatList, 
  TextInput, 
  TouchableOpacity, 
  KeyboardAvoidingView, 
  Platform,
  Text 
} from 'react-native';
import styles from '../styles/ChatContainerStyles';
import { Ionicons } from './Icons';
import { ChatMessage, Message } from './ChatMessage';

interface ChatContainerProps {
  messages: Message[];
  text: string;
  onTextChange: (text: string) => void;
  onSend: () => void;
  placeholder?: string;
  disabled?: boolean;
  isGenerating?: boolean;
}

export const ChatContainer: React.FC<ChatContainerProps> = ({
  messages,
  text,
  onTextChange,
  onSend,
  placeholder = 'Type a message...',
  disabled = false,
  isGenerating = false,
}) => {
  const flatListRef = useRef<FlatList>(null);

  useEffect(() => {
    // Auto-scroll to bottom when new messages arrive
    if (messages.length > 0) {
      setTimeout(() => {
        flatListRef.current?.scrollToOffset({ offset: 0, animated: true });
      }, 100);
    }
  }, [messages.length]);

  const handleSend = () => {
    if (text?.trim() && !disabled) {
      onSend();
    }
  };

  return (
    <KeyboardAvoidingView
      style={styles.container}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      keyboardVerticalOffset={Platform.OS === 'ios' ? 90 : 0}
    >
      <FlatList
        ref={flatListRef}
        data={[...messages].reverse()}
        keyExtractor={(item) => item._id.toString()}
        renderItem={({ item }) => <ChatMessage message={item} />}
        contentContainerStyle={styles.messageList}
        showsVerticalScrollIndicator={true}
        inverted={true}
      />

      <View style={styles.inputContainer}>
        <TextInput
          style={[styles.input, disabled && styles.inputDisabled]}
          value={text}
          onChangeText={onTextChange}
          placeholder={placeholder}
          placeholderTextColor="#999"
          multiline={false}
          autoComplete="off"
          autoCorrect={false}
          autoCapitalize="none"
          editable={!isGenerating && !disabled}
          onSubmitEditing={handleSend}
          blurOnSubmit={Platform.OS !== 'ios'}
          returnKeyType="send"
        />
        <TouchableOpacity
          style={[
            styles.sendButton,
            (!text?.trim() || isGenerating || disabled) && styles.sendButtonDisabled
          ]}
          onPress={handleSend}
          disabled={!text?.trim() || isGenerating || disabled}
        >
          <Ionicons 
            name="send" 
            size={20} 
            color="#fff" 
          />
        </TouchableOpacity>
      </View>
    </KeyboardAvoidingView>
  );
};

// styles moved to `src/styles/ChatContainerStyles.ts`
