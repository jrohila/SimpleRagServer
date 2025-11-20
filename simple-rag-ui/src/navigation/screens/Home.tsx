import React, { useCallback, useState, useEffect, useRef } from 'react';
import { View, ActivityIndicator, Alert, Text, SafeAreaView, TouchableOpacity, StyleSheet, Switch } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { Picker } from '@react-native-picker/picker';
import { Ionicons } from '@expo/vector-icons';
import { getChats } from '../../api/chats';
import { ChatContainer } from '../../components/ChatContainer';
import { ErrorBoundary } from '../../components/ErrorBoundary';
import { Message } from '../../components/ChatMessage';
import { LLMServiceFactory, LLMMode } from '../../services/LLMServiceFactory';
import { LLMMessage } from '../../services/RemoteLLMService';

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
  const [llmMode, setLlmMode] = useState<LLMMode>('remote');
  const [isLocalLLMAvailable, setIsLocalLLMAvailable] = useState(false);
  const [isInitializingLocalLLM, setIsInitializingLocalLLM] = useState(false);
  const assistantMessageRef = useRef<Message | null>(null);

  // Check WebGPU availability on mount
  useEffect(() => {
    LLMServiceFactory.checkLocalAvailability().then(setIsLocalLLMAvailable);
  }, []);

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
        // Filter out any null/undefined messages when loading from history
        const validMessages = chatHistories[selectedChatId].filter(msg => msg != null);
        setMessages(validMessages);
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
      // Filter out any null/undefined messages before saving
      const validMessages = messages.filter(msg => msg != null);
      if (validMessages.length > 0) {
        setChatHistories(prev => ({
          ...prev,
          [selectedChatId]: validMessages
        }));
      }
    }
  }, [messages, selectedChatId]);

  const selectedChat = chats.find(c => c.id === selectedChatId);

  const handleStreamResponse = async (conversationMessages: LLMMessage[]) => {
    try {
      // Create assistant message placeholder with loading state
      const assistantMessage: Message = {
        _id: Date.now() + 1,
        text: '',
        createdAt: new Date(),
        user: {
          _id: 2,
          name: llmMode === 'local' ? 'Local AI' : 'SimpleRagServer',
        },
        isLoading: true,
      };
      
      assistantMessageRef.current = assistantMessage;
      setMessages((prev) => [...prev, assistantMessage]);

      // Get appropriate service
      const service = LLMServiceFactory.getService(llmMode);

      // For local mode, initialize if needed
      if (llmMode === 'local') {
        const localLLM = LLMServiceFactory.getLocalLLM();
        if (!localLLM.isModelLoaded()) {
          setIsInitializingLocalLLM(true);
          updateAssistantMessage('Initializing local AI model... This may take a moment on first run.', false);
        }
      }

      await service.sendMessage(
        {
          publicName: selectedChat?.publicName || '',
          temperature: 0.7,
          useRag: llmMode === 'remote', // Only use RAG for remote mode
        },
        conversationMessages,
        {
          onContent: (content: string) => {
            setIsInitializingLocalLLM(false);
            updateAssistantMessage(content, true);
          },
          onComplete: () => {
            setIsGenerating(false);
            setIsInitializingLocalLLM(false);
            assistantMessageRef.current = null;
          },
          onError: (error: Error) => {
            console.error('Error from LLM service:', error);
            Alert.alert('Error', error.message || 'Failed to get response');
            if (assistantMessageRef.current) {
              setMessages((prev) => prev.filter(m => m != null && m._id !== assistantMessageRef.current!._id));
              assistantMessageRef.current = null;
            }
            setIsGenerating(false);
            setIsInitializingLocalLLM(false);
          }
        }
      );

    } catch (error) {
      console.error('Error calling LLM:', error);
      Alert.alert('Error', 'Failed to get response from the LLM');
      if (assistantMessageRef.current) {
        setMessages((prev) => prev.filter(m => m != null && m._id !== assistantMessageRef.current!._id));
        assistantMessageRef.current = null;
      }
      setIsGenerating(false);
      setIsInitializingLocalLLM(false);
    }
  };

  const updateAssistantMessage = (content: string, append: boolean = false) => {
    try {
      if (assistantMessageRef.current) {
        setMessages((prev) => {
          // Safety check - ensure prev is an array and filter out any null/undefined messages immediately
          const safeMessages = Array.isArray(prev) ? prev.filter(msg => msg != null && msg._id != null) : [];
          
          return safeMessages.map(msg => {
            if (msg._id === assistantMessageRef.current!._id) {
              return {
                ...msg,
                text: append ? (msg.text || '') + content : content,
                isLoading: false,
              };
            }
            return msg;
          });
        });
      }
    } catch (error) {
      console.error('Error in updateAssistantMessage:', error);
    }
  };

  const handleErrorReset = () => {
    // Clean up any corrupted state
    setMessages(prevMessages => prevMessages.filter(msg => msg != null && msg._id != null));
    assistantMessageRef.current = null;
    setIsGenerating(false);
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

  const handleToggleLLMMode = async () => {
    if (isGenerating) {
      Alert.alert('Please Wait', 'Cannot switch mode while generating a response');
      return;
    }

    const newMode: LLMMode = llmMode === 'remote' ? 'local' : 'remote';
    
    if (newMode === 'local' && !isLocalLLMAvailable) {
      Alert.alert(
        'WebGPU Not Available',
        'Your browser does not support WebGPU, which is required for local AI models. Please use Chrome, Edge, or another WebGPU-compatible browser.'
      );
      return;
    }

    setLlmMode(newMode);
  };

  const handleSend = useCallback(() => {
    if (!selectedChat && llmMode === 'remote') {
      Alert.alert('Error', 'Please select a chat first');
      return;
    }

    if (isGenerating || !text.trim()) {
      return;
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

    // Convert messages to LLM format
    const conversationMessages: LLMMessage[] = [...messages, userMessage]
      .filter(msg => msg != null) // Filter out any null/undefined messages
      .map(msg => ({
        role: msg.user._id === 1 ? 'user' : 'assistant',
        content: msg.text
      })) as LLMMessage[];

    // Add system message for local mode
    if (llmMode === 'local') {
      conversationMessages.unshift({
        role: 'system',
        content: 'You are a helpful assistant.'
      });
    }

    handleStreamResponse(conversationMessages);
  }, [selectedChat, messages, isGenerating, text, llmMode]);

  const getPlaceholder = () => {
    if (llmMode === 'local') {
      if (isInitializingLocalLLM) return 'Initializing local AI...';
      if (isGenerating) return 'Generating locally...';
      return 'Type a message (using local AI)...';
    }
    if (!selectedChatId) return 'Please select a chat first...';
    if (isGenerating) return 'Generating response...';
    return 'Type a message...';
  };

  return (
    <SafeAreaView style={styles.container}>
      {/* Chat Selection and LLM Mode Toggle */}
      <View style={styles.dropdownContainer}>
        {loadingChats ? (
          <ActivityIndicator color="#666" />
        ) : (
          <>
            <Ionicons name="chatbubbles" size={24} color="#007aff" style={styles.chatIcon} />
            
            {/* Chat Selector - Only show in remote mode */}
            {llmMode === 'remote' && (
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
            )}

            {/* Local AI indicator */}
            {llmMode === 'local' && (
              <View style={styles.localModeIndicator}>
                <Ionicons name="hardware-chip" size={20} color="#4CAF50" />
                <Text style={styles.localModeText}>Local AI (WebGPU)</Text>
              </View>
            )}

            {/* LLM Mode Toggle */}
            <View style={styles.toggleContainer}>
              <Text style={styles.toggleLabel}>
                {llmMode === 'remote' ? 'Remote' : 'Local'}
              </Text>
              <Switch
                value={llmMode === 'local'}
                onValueChange={handleToggleLLMMode}
                disabled={!isLocalLLMAvailable || isGenerating}
                trackColor={{ false: '#767577', true: '#4CAF50' }}
                thumbColor={llmMode === 'local' ? '#fff' : '#f4f3f4'}
              />
            </View>

            <TouchableOpacity
              style={[
                styles.clearButton,
                ((!selectedChatId && llmMode === 'remote') || isGenerating) && styles.clearButtonDisabled
              ]}
              onPress={handleClearHistory}
              disabled={(!selectedChatId && llmMode === 'remote') || isGenerating}
            >
              <Ionicons name="trash-outline" size={20} color="#fff" />
            </TouchableOpacity>
          </>
        )}
      </View>

      {/* Warning for WebGPU not available */}
      {!isLocalLLMAvailable && (
        <View style={styles.warningBanner}>
          <Ionicons name="warning" size={16} color="#ff9800" />
          <Text style={styles.warningText}>
            WebGPU not available. Local AI mode disabled.
          </Text>
        </View>
      )}

      <ErrorBoundary onReset={handleErrorReset}>
        <ChatContainer
          messages={messages.filter(msg => msg != null && msg._id != null)}
          text={text}
          onTextChange={setText}
          onSend={handleSend}
          placeholder={getPlaceholder()}
          disabled={(llmMode === 'remote' && !selectedChatId) || isInitializingLocalLLM}
          isGenerating={isGenerating}
        />
      </ErrorBoundary>
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
  localModeIndicator: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    backgroundColor: '#e8f5e9',
    paddingHorizontal: 12,
    paddingVertical: 12,
    borderRadius: 8,
  },
  localModeText: {
    color: '#2e7d32',
    fontSize: 14,
    fontWeight: '600',
  },
  toggleContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    paddingHorizontal: 8,
  },
  toggleLabel: {
    fontSize: 14,
    color: '#666',
    fontWeight: '500',
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
  warningBanner: {
    backgroundColor: '#fff3cd',
    paddingHorizontal: 16,
    paddingVertical: 10,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    borderBottomWidth: 1,
    borderBottomColor: '#ffc107',
  },
  warningText: {
    color: '#856404',
    fontSize: 13,
    flex: 1,
  },
});
