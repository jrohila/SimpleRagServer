import React, { useCallback, useState, useEffect, useRef } from 'react';
import { View, ActivityIndicator, Alert, Text, SafeAreaView, TouchableOpacity, StyleSheet } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { Picker } from '@react-native-picker/picker';
import { Ionicons } from '@expo/vector-icons';
import { getChats } from '../../api/chats';
import { sendConversation } from '../../api/openAI';
import { ChatContainer } from '../../components/ChatContainer';
import { Message } from '../../components/ChatMessage';

type Chat = {
  id: string;
  publicName: string;
  welcomeMessage?: string;
};

export function Home() {
  // Store message histories for each chat
  const [chatHistories, setChatHistories] = useState<{ [chatId: string]: Message[] }>({});
  const [messages, setMessages] = useState<Message[]>([]);
  const [text, setText] = useState('');
  const [chats, setChats] = useState<Chat[]>([]);
  const [selectedChatId, setSelectedChatId] = useState('');
  const [loadingChats, setLoadingChats] = useState(false);
  const [isGenerating, setIsGenerating] = useState(false);
  const assistantMessageRef = useRef<Message | null>(null);

  useFocusEffect(
    useCallback(() => {
      setLoadingChats(true);
      getChats()
        .then((res) => {
          const data = (res as any).data as Chat[];
          setChats(data);
          if (data.length > 0 && !selectedChatId) {
            setSelectedChatId(data[0].id);
          }
          setLoadingChats(false);
        })
        .catch(() => setLoadingChats(false));
    }, [selectedChatId])
  );

  // Update messages when selectedChatId changes
  useEffect(() => {
    if (selectedChatId) {
      // Load chat history for the selected chat, or use default welcome message
      if (chatHistories[selectedChatId]) {
        setMessages(chatHistories[selectedChatId]);
      } else {
        // Initialize with welcome message for new chat
        const chat = chats.find(c => c.id === selectedChatId);
        const welcomeText = chat?.welcomeMessage || 'Hello! How can I help you today?';
        const welcomeMessage: Message = {
          _id: `${selectedChatId}-welcome-${Date.now()}`,
          text: welcomeText,
          createdAt: new Date(),
          user: {
            _id: 2,
            name: 'SimpleRagServer',
          },
        };
        setMessages([welcomeMessage]);
        setChatHistories(prev => ({
          ...prev,
          [selectedChatId]: [welcomeMessage]
        }));
      }
    }
  }, [selectedChatId, chats]);

  // Save messages to chat history whenever they change
  useEffect(() => {
    if (selectedChatId && messages.length > 0) {
      setChatHistories(prev => ({
        ...prev,
        [selectedChatId]: messages
      }));
    }
  }, [messages, selectedChatId]);

  const selectedChat = chats.find(c => c.id === selectedChatId);

  const handleStreamResponse = async (publicName: string, conversationMessages: any[]) => {
    try {
      // Create assistant message placeholder with loading state
      const assistantMessage: any = {
        _id: Date.now() + 1,
        text: '',
        createdAt: new Date(),
        user: {
          _id: 2,
          name: 'SimpleRagServer',
        },
        isLoading: true, // Custom property to track loading state
      };
      
      assistantMessageRef.current = assistantMessage;
      // With inverted={false}, add to the end of the array
      setMessages((prev) => [...prev, assistantMessage]);

      // Make the API call with streaming enabled
      const response = await sendConversation({
        publicName,
        messages: conversationMessages,
        stream: true,
        temperature: 0.7,
        useRag: true
      });

      // Handle streaming response from fetch API
      if (!response.body) {
        throw new Error('Response body is not available');
      }

      const reader = response.body.getReader();
      const decoder = new TextDecoder();
      let buffer = '';
      let reading = true;

      while (reading) {
        try {
          const { done, value } = await reader.read();
          
          if (done) {
            reading = false;
            break;
          }

          // Decode the chunk and add to buffer
          buffer += decoder.decode(value, { stream: true });
          
          // Split by newlines to process each SSE message
          const lines = buffer.split('\n');
          
          // Keep the last incomplete line in the buffer
          buffer = lines.pop() || '';

          for (const line of lines) {
            try {
              const trimmedLine = line.trim();
              
              // Parse SSE format: "data: {json}"
              if (trimmedLine.startsWith('data:')) {
                const data = trimmedLine.slice(5).trim();
                
                // Check for stream end signal
                if (data === '[DONE]') {
                  reading = false;
                  break;
                }
                
                try {
                  const parsed = JSON.parse(data);
                  // Extract content from: choices[0].delta.content
                  const content = parsed.choices?.[0]?.delta?.content;
                  
                  if (content) {
                    updateAssistantMessage(content, true);
                  }
                } catch (parseError) {
                  console.error('Error parsing streaming chunk:', parseError, 'Data:', data);
                  // Continue processing other lines
                }
              }
            } catch (lineError) {
              console.error('Error processing line:', lineError);
              // Continue to next line
            }
          }
        } catch (readError) {
          console.error('Error reading stream:', readError);
          // Terminate the stream gracefully on read error
          reading = false;
          break;
        }
      }

    } catch (error) {
      console.error('Error calling chat API:', error);
      Alert.alert('Error', 'Failed to get response from the server');
      // Remove the assistant message placeholder on error
      if (assistantMessageRef.current) {
        setMessages((prev) => prev.filter(m => m._id !== assistantMessageRef.current!._id));
        assistantMessageRef.current = null;
      }
    } finally {
      setIsGenerating(false);
      assistantMessageRef.current = null;
    }
  };

  const updateAssistantMessage = (content: string, append: boolean = false) => {
    try {
      if (assistantMessageRef.current) {
        setMessages((prev) => {
          try {
            const updated = prev.map(msg => {
              if (msg && msg._id === assistantMessageRef.current!._id) {
                return {
                  ...msg,
                  text: append ? (msg.text || '') + content : content,
                  isLoading: false, // Remove loading state when content arrives
                };
              }
              return msg;
            });
            return updated;
          } catch (mapError) {
            console.error('Error updating message in map:', mapError);
            return prev; // Return unchanged state on error
          }
        });
      }
    } catch (error) {
      console.error('Error in updateAssistantMessage:', error);
      // Silently fail - don't crash the app
    }
  };

  const handleClearHistory = () => {
    if (!selectedChatId) return;
    
    // Create a new welcome message
    const welcomeText = selectedChat?.welcomeMessage || 'Hello! How can I help you today?';
    const welcomeMessage: Message = {
      _id: `${selectedChatId}-welcome-${Date.now()}`,
      text: welcomeText,
      createdAt: new Date(),
      user: {
        _id: 2,
        name: 'SimpleRagServer',
      },
    };
    
    // Reset messages to just the welcome message
    setMessages([welcomeMessage]);
    
    // Update chat histories
    setChatHistories(prev => ({
      ...prev,
      [selectedChatId]: [welcomeMessage]
    }));
  };

  const handleSend = useCallback(() => {
    if (!selectedChat) {
      Alert.alert('Error', 'Please select a chat first');
      return;
    }

    if (isGenerating || !text.trim()) {
      return; // Prevent sending while generating or if text is empty
    }

    // Create user message
    const userMessage: Message = {
      _id: Date.now(),
      text: text.trim(),
      createdAt: new Date(),
      user: { _id: 1 },
    };

    // Add user message to chat
    setMessages((previousMessages) => [...previousMessages, userMessage]);
    setText('');
    setIsGenerating(true);

    // Convert messages to OpenAI format
    const conversationMessages = [...messages, userMessage]
      .map(msg => ({
        role: msg.user._id === 1 ? 'user' : 'assistant',
        content: msg.text
      }));

    // Call the API with streaming
    handleStreamResponse(selectedChat.publicName, conversationMessages);
  }, [selectedChat, messages, isGenerating, text]);

  return (
    <SafeAreaView style={styles.container}>
      {/* Chat Selection Dropdown */}
      <View style={styles.dropdownContainer}>
        {loadingChats ? (
          <ActivityIndicator color="#666" />
        ) : (
          <>
            <Ionicons name="chatbubbles" size={24} color="#007aff" style={styles.chatIcon} />
            <View style={styles.pickerWrapper}>
              <Picker
                selectedValue={selectedChatId}
                onValueChange={(value) => setSelectedChatId(value)}
                style={styles.picker}
                dropdownIconColor="#666"
              >
                <Picker.Item label="Select a chat..." value="" />
                {chats.map((chat) => (
                  <Picker.Item key={chat.id} label={chat.publicName} value={chat.id} />
                ))}
              </Picker>
            </View>
            <TouchableOpacity
              style={[
                styles.clearButton,
                (!selectedChatId || isGenerating) && styles.clearButtonDisabled
              ]}
              onPress={handleClearHistory}
              disabled={!selectedChatId || isGenerating}
            >
              <Ionicons name="trash-outline" size={20} color="#fff" />
            </TouchableOpacity>
          </>
        )}
      </View>

      <ChatContainer
        messages={messages}
        text={text}
        onTextChange={setText}
        onSend={handleSend}
        placeholder={!selectedChatId ? "Please select a chat first..." : (isGenerating ? "Generating response..." : "Type a message...")}
        disabled={!selectedChatId}
        isGenerating={isGenerating}
      />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  dropdownContainer: {
    backgroundColor: '#fff',
    paddingHorizontal: 16,
    paddingVertical: 14,
    borderBottomWidth: 1,
    borderBottomColor: '#e0e0e0',
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
  },
  chatIcon: {
    marginRight: 4,
  },
  pickerWrapper: {
    flex: 1,
    backgroundColor: '#fff',
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#ddd',
    overflow: 'hidden',
    minHeight: 48,
  },
  picker: {
    color: '#000',
    backgroundColor: 'transparent',
    borderRadius: 8,
    fontSize: 16,
    height: 48,
  },
  clearButton: {
    backgroundColor: '#ff6b6b',
    paddingHorizontal: 16,
    paddingVertical: 12,
    borderRadius: 8,
    justifyContent: 'center',
    alignItems: 'center',
    minWidth: 48,
  },
  clearButtonDisabled: {
    backgroundColor: '#ccc',
    opacity: 0.5,
  },
  clearButtonText: {
    color: '#fff',
    fontSize: 14,
    fontWeight: '600',
  },
});
