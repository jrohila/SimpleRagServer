import React, { useRef, useEffect } from 'react';
import { 
  View, 
  FlatList, 
  TextInput, 
  TouchableOpacity, 
  StyleSheet, 
  KeyboardAvoidingView, 
  Platform,
  Text 
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
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

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  messageList: {
    paddingHorizontal: 12,
    paddingVertical: 12,
  },
  inputContainer: {
    flexDirection: 'row',
    padding: 12,
    backgroundColor: '#fff',
    borderTopWidth: 1,
    borderTopColor: '#e0e0e0',
    alignItems: 'flex-end',
  },
  input: {
    flex: 1,
    color: '#000',
    backgroundColor: '#fff',
    borderRadius: 20,
    paddingHorizontal: 16,
    paddingVertical: 10,
    fontSize: 16,
    borderWidth: 1,
    borderColor: '#ddd',
    marginRight: 8,
  },
  inputDisabled: {
    opacity: 0.5,
  },
  sendButton: {
    backgroundColor: '#007aff',
    width: 44,
    height: 44,
    borderRadius: 22,
    justifyContent: 'center',
    alignItems: 'center',
  },
  sendButtonDisabled: {
    backgroundColor: '#ccc',
    opacity: 0.5,
  },
  sendButtonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
  },
});
