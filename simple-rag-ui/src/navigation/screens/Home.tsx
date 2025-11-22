import React, { useCallback, useState, useEffect, useRef } from 'react';
import { View, ActivityIndicator, Alert, Text, SafeAreaView, TouchableOpacity, StyleSheet, Switch, Modal, Linking } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { Picker } from '@react-native-picker/picker';
import { Ionicons } from '@expo/vector-icons';
import { getChats, getChatById } from '../../api/chats';
import { ChatContainer } from '../../components/ChatContainer';
import { ErrorBoundary } from '../../components/ErrorBoundary';
import { Message } from '../../components/ChatMessage';
import { LLMServiceFactory, LLMMode } from '../../services/LLMServiceFactory';
import { LLMMessage } from '../../services/RemoteLLMService';

type Chat = {
  id: string;
  publicName: string;
  welcomeMessage?: string;
  useUserPromptRewriting?: boolean;
  userPromptRewritingPrompt?: string;
};

export function Home() {
  // Store message histories for each chat
  const [chatHistories, setChatHistories] = useState<{ [chatId: string]: Message[] }>({});
  const [messages, setMessages] = useState<Message[]>([]);
  const [text, setText] = useState('');
  const [chats, setChats] = useState<Chat[]>([]);
  const [selectedChatId, setSelectedChatId] = useState('');
  const [selectedChatDetails, setSelectedChatDetails] = useState<Chat | null>(null);
  const [loadingChats, setLoadingChats] = useState(false);
  const [isGenerating, setIsGenerating] = useState(false);
  const [llmMode, setLlmMode] = useState<LLMMode>('remote');
  const [isLocalLLMAvailable, setIsLocalLLMAvailable] = useState(false);
  const [isInitializingLocalLLM, setIsInitializingLocalLLM] = useState(false);
  const [downloadProgress, setDownloadProgress] = useState<{ percentage: number; loaded: number; total: number } | null>(null);
  const [showUpgradeModal, setShowUpgradeModal] = useState(false);
  const assistantMessageRef = useRef<Message | null>(null);

  const isChromeBrowser = typeof navigator !== 'undefined' && /Chrome\//.test(navigator.userAgent) && !/Edg\//.test(navigator.userAgent);

  // Check WebGPU availability on mount
  useEffect(() => {
    LLMServiceFactory.checkLocalAvailability().then(setIsLocalLLMAvailable);
    
    // Set up download progress callback
    const localLLM = LLMServiceFactory.getLocalLLM();
    if (localLLM) {
      localLLM.setDownloadProgressCallback((progress) => {
        setDownloadProgress(progress);
      });
    }
  }, []);

  // When initializing local model, poll for completion to close the modal when ready
  useEffect(() => {
    if (!isInitializingLocalLLM) return;
    const localLLM = LLMServiceFactory.getLocalLLM();
    if (!localLLM) return;

    let cancelled = false;
    const start = Date.now();

    const check = async () => {
      try {
        if (localLLM.isModelLoaded && localLLM.isModelLoaded()) {
          if (!cancelled) setIsInitializingLocalLLM(false);
          return;
        }
        // Poll until model is loaded or timeout (2 minutes)
        if (Date.now() - start > 2 * 60 * 1000) {
          console.warn('Local model init timeout');
          if (!cancelled) setIsInitializingLocalLLM(false);
          return;
        }
        setTimeout(check, 500);
      } catch (err) {
        console.warn('Error checking local model load state', err);
        if (!cancelled) setIsInitializingLocalLLM(false);
      }
    };

    check();

    return () => { cancelled = true; };
  }, [isInitializingLocalLLM]);

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
      // Fetch full chat details to get prompt rewriting configuration
      getChatById(selectedChatId)
        .then((res) => {
          const data = (res as any).data as Chat;
          setSelectedChatDetails(data);
        })
        .catch((error) => {
          console.warn('Failed to fetch chat details:', error);
          setSelectedChatDetails(null);
        });
      
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

      let isFirstContent = true;
      
      // Prepare config with prompt rewriting settings
      const config: any = {
        publicName: selectedChat?.publicName || '',
        temperature: 0.7,
        useRag: llmMode === 'local' ? !!selectedChatId : llmMode === 'remote', // Enable RAG for local mode if chat is selected
      };
      
      // Add prompt rewriting configuration if available from full chat details
      if (selectedChatDetails) {
        config.useUserPromptRewriting = selectedChatDetails.useUserPromptRewriting;
        config.userPromptRewritingPrompt = selectedChatDetails.userPromptRewritingPrompt;
      }
      
      await service.sendMessage(
        config,
        conversationMessages,
        {
          onContent: (content: string) => {
            setIsInitializingLocalLLM(false);
            // Clear initialization message on first content, then append
            updateAssistantMessage(content, !isFirstContent);
            isFirstContent = false;
          },
          onComplete: () => {
            // Ensure final message state is set before clearing ref
            if (assistantMessageRef.current) {
              setMessages((prev) => {
                const safeMessages = Array.isArray(prev) ? prev.filter(msg => msg != null && msg._id != null) : [];
                return safeMessages.map(msg => {
                  if (!msg || !msg._id) return msg;
                  if (msg._id === assistantMessageRef.current!._id) {
                    return { 
                      ...msg, 
                      isLoading: false,
                      createdAt: new Date() // Update timestamp when response is complete
                    };
                  }
                  return msg;
                }).filter((msg): msg is Message => msg != null && msg._id != null);
              });
            }
            setIsGenerating(false);
            setIsInitializingLocalLLM(false);
            // Defer nulling the ref to avoid race conditions
            setTimeout(() => {
              assistantMessageRef.current = null;
            }, 0);
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
          
          const updatedMessages = safeMessages.map(msg => {
            // Double check msg is valid before accessing properties
            if (!msg || !msg._id) return null;
            
            if (msg._id === assistantMessageRef.current!._id) {
              return {
                ...msg,
                text: append ? (msg.text || '') + content : content,
                isLoading: false,
              };
            }
            return msg;
          });
          
          // Filter out any null values that might have been created
          return updatedMessages.filter((msg): msg is Message => msg != null && msg._id != null);
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
    // If switching to local, start model initialization in background and show progress
    if (newMode === 'local') {
      const localLLM = LLMServiceFactory.getLocalLLM();
      if (localLLM) {
        // Ensure progress callback is set (in case it wasn't already)
        localLLM.setDownloadProgressCallback((progress) => {
          setDownloadProgress(progress);
        });

        // If already loaded, clear initializing flag; otherwise start init
        if (localLLM.isModelLoaded && localLLM.isModelLoaded()) {
          setIsInitializingLocalLLM(false);
        } else {
          setIsInitializingLocalLLM(true);
          localLLM.startInitialization();
        }
      }
    } else {
      // switching away from local - clear download progress
      setDownloadProgress(null);
      setIsInitializingLocalLLM(false);
    }
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
                {isChromeBrowser && (
                  <TouchableOpacity onPress={() => setShowUpgradeModal(true)} style={styles.upgradeButton}>
                    <Ionicons name="rocket-outline" size={18} color="#2e7d32" />
                  </TouchableOpacity>
                )}
              </View>
            )}

            <View style={styles.toggleContainer}>
              <Text style={styles.toggleLabel}>{llmMode === 'remote' ? 'Remote' : 'Local'}</Text>
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
                ((!selectedChatId && llmMode === 'remote') || isGenerating) && styles.clearButtonDisabled,
              ]}
              onPress={handleClearHistory}
              disabled={(!selectedChatId && llmMode === 'remote') || isGenerating}
            >
              <Ionicons name="trash-outline" size={20} color="#fff" />
            </TouchableOpacity>
          </>
        )}
      </View>

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

      {/* Initialization modal for local model */}
      <Modal
        visible={isInitializingLocalLLM}
        transparent={true}
        animationType="fade"
        onRequestClose={() => { /* no-op: modal is not dismissible while initializing */ }}
      >
        <View style={styles.modalOverlay}>
          <View style={styles.modalContent}>
            <ActivityIndicator size="large" color="#2196F3" />
            <Text style={styles.modalTitle}>Preparing Local AI Model</Text>
            <Text style={styles.modalMessage}>The local model is downloading and initializing. This may take a few minutes on first run.</Text>

            {downloadProgress ? (
              <View style={{ width: '100%' }}>
                <Text style={styles.modalProgressText}>{downloadProgress.percentage}% ({Math.round(downloadProgress.loaded / 1024 / 1024)}MB / {Math.round(downloadProgress.total / 1024 / 1024)}MB)</Text>
                <View style={styles.progressBar}>
                  <View style={[styles.progressFill, { width: `${Math.max(0, Math.min(100, downloadProgress.percentage))}%` }]} />
                </View>
              </View>
            ) : (
              <Text style={styles.modalProgressText}>Starting...</Text>
            )}
          </View>
        </View>
      </Modal>

      {/* Upgrade modal for Chrome users */}
      <Modal
        visible={showUpgradeModal}
        transparent={true}
        animationType="slide"
        onRequestClose={() => setShowUpgradeModal(false)}
      >
        <View style={styles.modalOverlay}>
          <View style={styles.modalContent}>
            <Text style={styles.modalTitle}>Upgrade Browser Settings</Text>
            <Text style={styles.modalMessage}>To improve local model performance on Chrome, enable the following flags and ensure the high-performance GPU is selected.</Text>
            <View style={{ width: '100%' }}>
              <Text style={styles.modalProgressText}>1) Enable Unsafe WebGPU: open Chrome flags and enable <Text style={{ fontWeight: '700' }}>#enable-unsafe-webgpu</Text></Text>
              <Text style={styles.modalProgressText}>2) Force high-performance GPU: enable your system/browser GPU preferences and try launching Chrome with high-performance GPU.</Text>
            </View>

            <View style={styles.modalButtonsRow}>
              <TouchableOpacity
                style={styles.modalButton}
                onPress={async () => {
                  // Best-effort: try to open chrome://flags; many browsers block this from pages.
                  // If that fails, open a helpful search page and copy the flag token to the clipboard.
                  const flagFragment = '#enable-unsafe-webgpu';
                  try {
                    // Try direct open first (may be blocked)
                    // eslint-disable-next-line @typescript-eslint/ban-ts-comment
                    // @ts-ignore
                    const opened = window.open('chrome://flags/' + flagFragment, '_blank');
                    if (opened) return;
                  } catch (err) {
                    // fallthrough to fallback behavior
                  }

                  // Fallback: attempt to open a web search with instructions
                  const searchUrl = 'https://www.google.com/search?q=enable+unsafe+webgpu+chrome';
                  try {
                    await Linking.openURL(searchUrl);
                  } catch (linkErr) {
                    // If opening the search fails (webview restrictions), copy the flag token and show instructions
                    try {
                      if (navigator.clipboard && navigator.clipboard.writeText) {
                        await navigator.clipboard.writeText(flagFragment);
                        Alert.alert('Flags', 'Could not open chrome://flags. The flag token "#enable-unsafe-webgpu" has been copied to your clipboard. Open chrome://flags/ and paste it into the search box.');
                        return;
                      }
                    } catch (clipErr) {
                      // ignore
                    }

                    Alert.alert('Open Flags', 'Please open chrome://flags/ in your Chrome address bar and search for "#enable-unsafe-webgpu" to enable Unsafe WebGPU.');
                  }
                }}
              >
                <Text style={styles.modalButtonText}>Open Flags</Text>
              </TouchableOpacity>

              <TouchableOpacity style={[styles.modalButton, { minWidth: 140 }]} onPress={() => setShowUpgradeModal(false)}>
                <Text style={styles.modalButtonText}>Close</Text>
              </TouchableOpacity>
            </View>
          </View>
        </View>
      </Modal>
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
  downloadBanner: {
    backgroundColor: '#e3f2fd',
    paddingHorizontal: 16,
    paddingVertical: 12,
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    borderBottomWidth: 1,
    borderBottomColor: '#2196F3',
    flexWrap: 'wrap',
  },
  downloadText: {
    color: '#0d47a1',
    fontSize: 13,
    flex: 1,
  },
  progressBar: {
    width: '100%',
    height: 4,
    backgroundColor: '#bbdefb',
    borderRadius: 2,
    marginTop: 8,
    overflow: 'hidden',
  },
  progressFill: {
    height: '100%',
    backgroundColor: '#2196F3',
    borderRadius: 2,
  },
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.5)',
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
  },
  modalContent: {
    width: '100%',
    maxWidth: 640,
    backgroundColor: '#fff',
    padding: 20,
    borderRadius: 12,
    alignItems: 'center',
  },
  modalTitle: {
    fontSize: 18,
    fontWeight: '700',
    marginTop: 12,
    color: '#0d47a1',
  },
  modalMessage: {
    fontSize: 14,
    color: '#333',
    textAlign: 'center',
    marginTop: 8,
    marginBottom: 12,
  },
  modalProgressText: {
    fontSize: 13,
    color: '#0d47a1',
    marginTop: 8,
    textAlign: 'center',
  },
  upgradeButton: {
    marginLeft: 12,
    backgroundColor: 'transparent',
    borderWidth: 1,
    borderColor: 'rgba(0,0,0,0.06)',
    paddingHorizontal: 8,
    paddingVertical: 6,
    borderRadius: 8,
  },
  upgradeButtonText: {
    display: 'none',
  },
  modalButtonsRow: {
    width: '100%',
    flexDirection: 'row',
    justifyContent: 'space-between',
    gap: 12,
    marginTop: 12,
  },
  modalButton: {
    backgroundColor: '#2196F3',
    paddingHorizontal: 14,
    paddingVertical: 10,
    borderRadius: 8,
    minWidth: 110,
    alignItems: 'center',
  },
  modalButtonText: {
    color: '#fff',
    fontWeight: '600',
  },
  
});
