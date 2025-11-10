import { GiftedChat, IMessage, Bubble, Send } from 'react-native-gifted-chat';
import React, { useCallback, useState, useEffect, useRef } from 'react';
import { View, TextInput, Platform, ActivityIndicator, Alert } from 'react-native';
import { Picker } from '@react-native-picker/picker';
import Markdown from 'react-native-markdown-display';
import { HomeStyles } from '../../styles/HomeStyles';
import { getChats } from '../../api/chats';
import { sendConversation } from '../../api/openAI';

type Chat = {
  id: string;
  publicName: string;
};

export function Home() {
  const [messages, setMessages] = useState<IMessage[]>([{
    _id: 1,
    text: 'Hello! How can I help you today?',
    createdAt: new Date(),
    user: {
      _id: 2,
      name: 'SimpleRagServer',
    },
  }]);
  const [text, setText] = useState('');
  const [chats, setChats] = useState<Chat[]>([]);
  const [selectedChatId, setSelectedChatId] = useState('');
  const [loadingChats, setLoadingChats] = useState(false);
  const [isGenerating, setIsGenerating] = useState(false);
  const assistantMessageRef = useRef<IMessage | null>(null);

  useEffect(() => {
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
  }, []);

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
      setMessages((prev) => GiftedChat.append(prev, [assistantMessage]));

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

  const onSend = useCallback((newMessages: IMessage[] = []) => {
    if (!selectedChat) {
      Alert.alert('Error', 'Please select a chat first');
      return;
    }

    if (isGenerating) {
      return; // Prevent sending while generating
    }

    // Add user message to chat
    setMessages((previousMessages) => GiftedChat.append(previousMessages, newMessages));
    setText('');
    setIsGenerating(true);

    // Convert GiftedChat messages to OpenAI format
    // GiftedChat stores messages in reverse chronological order (newest first)
    // Combine current messages with the new user message
    const allMessages = GiftedChat.append(messages, newMessages);
    
    // Reverse to get chronological order (oldest first) for the API
    const conversationMessages = allMessages
      .reverse()
      .map(msg => ({
        role: msg.user._id === 1 ? 'user' : 'assistant',
        content: msg.text
      }));

    // Call the API with streaming
    handleStreamResponse(selectedChat.publicName, conversationMessages);
  }, [selectedChat, messages, isGenerating]);

  return (
  <View style={HomeStyles.container}>
      {/* Chat Selection Dropdown */}
      <View style={HomeStyles.dropdownContainer}>
        {loadingChats ? (
          <ActivityIndicator color="#666" />
        ) : (
          <View style={HomeStyles.pickerWrapper}>
            <Picker
              selectedValue={selectedChatId}
              onValueChange={(value) => setSelectedChatId(value)}
              style={HomeStyles.picker}
              dropdownIconColor="#666"
            >
              <Picker.Item label="Select a chat..." value="" />
              {chats.map((chat) => (
                <Picker.Item key={chat.id} label={chat.publicName} value={chat.id} />
              ))}
            </Picker>
          </View>
        )}
      </View>

      <GiftedChat
        messages={messages}
        onSend={onSend}
        user={{ _id: 1 }}
        placeholder={!selectedChatId ? "Please select a chat first..." : (isGenerating ? "Generating response..." : "Type a message...")}
        showUserAvatar
        alwaysShowSend
  messagesContainerStyle={HomeStyles.messagesContainer}
        text={text}
        onInputTextChanged={setText}
        renderComposer={props => (
          <TextInput
            {...props.textInputProps}
            style={[HomeStyles.input, props.textInputStyle, !selectedChatId && { opacity: 0.5 }]}
            value={text}
            onChangeText={setText}
            placeholder={!selectedChatId ? "Please select a chat first..." : (isGenerating ? "Generating response..." : "Type a message...")}
            placeholderTextColor="#999"
            multiline={false}
            autoComplete="off"
            autoCorrect={false}
            autoCapitalize="none"
            editable={!isGenerating && !!selectedChatId}
            onSubmitEditing={() => {
              if (text.trim() && !isGenerating && selectedChatId) {
                onSend([{
                  _id: Date.now(),
                  text: text.trim(),
                  createdAt: new Date(),
                  user: { _id: 1 },
                }]);
              }
            }}
            blurOnSubmit={Platform.OS !== 'ios'}
            returnKeyType="send"
          />
        )}
        renderBubble={props => (
          <Bubble
            {...props}
            wrapperStyle={{
              left: {
                backgroundColor: '#fff', // Assistant bubble (host)
              },
              right: {
                backgroundColor: '#007aff', // User bubble
              },
            }}
            textStyle={{
              left: { color: '#222' },
              right: { color: '#fff' },
            }}
          />
        )}
        renderMessageText={props => {
          const isAssistant = props.currentMessage?.user._id === 2;
          const isLoading = (props.currentMessage as any)?.isLoading;
          
          // Show loading animation for assistant messages before content arrives
          if (isAssistant && isLoading && !props.currentMessage?.text) {
            return (
              <View style={{ padding: 8, flexDirection: 'row', alignItems: 'center' }}>
                <ActivityIndicator size="small" color="#666" />
                <View style={{ marginLeft: 8 }}>
                  <View style={{ 
                    width: 40, 
                    height: 8, 
                    backgroundColor: '#e0e0e0', 
                    borderRadius: 4,
                    marginBottom: 4 
                  }} />
                  <View style={{ 
                    width: 60, 
                    height: 8, 
                    backgroundColor: '#e0e0e0', 
                    borderRadius: 4 
                  }} />
                </View>
              </View>
            );
          }
          
          // Render markdown for assistant messages, plain text for user messages
          if (isAssistant) {
            return (
              <View style={{ padding: 8 }}>
                <Markdown
                  style={{
                    body: { color: '#222', fontSize: 16 },
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
                    },
                    fence: { 
                      backgroundColor: '#f6f8fa', 
                      color: '#24292e',
                      padding: 10,
                      borderRadius: 6,
                      fontFamily: Platform.OS === 'ios' ? 'Menlo' : 'monospace',
                    },
                    strong: { fontWeight: 'bold' },
                    em: { fontStyle: 'italic' },
                    link: { color: '#0366d6', textDecorationLine: 'underline' },
                    blockquote: { 
                      backgroundColor: '#f6f8fa',
                      borderLeftColor: '#dfe2e5',
                      borderLeftWidth: 4,
                      paddingLeft: 10,
                      marginLeft: 0,
                    },
                    list_item: { flexDirection: 'row', marginBottom: 4 },
                    bullet_list: { marginBottom: 8 },
                    ordered_list: { marginBottom: 8 },
                    heading1: { fontSize: 24, fontWeight: 'bold', marginBottom: 8 },
                    heading2: { fontSize: 20, fontWeight: 'bold', marginBottom: 6 },
                    heading3: { fontSize: 18, fontWeight: 'bold', marginBottom: 4 },
                  }}
                >
                  {props.currentMessage?.text || ''}
                </Markdown>
              </View>
            );
          }
          
          // Default rendering for user messages
          return (
            <View style={{ padding: 8 }}>
              <Markdown
                style={{
                  body: { color: '#fff', fontSize: 16 },
                  code_inline: { 
                    backgroundColor: 'rgba(255, 255, 255, 0.2)', 
                    color: '#fff',
                    paddingHorizontal: 4,
                    paddingVertical: 2,
                    borderRadius: 3,
                  },
                }}
              >
                {props.currentMessage?.text || ''}
              </Markdown>
            </View>
          );
        }}
        renderSend={props => (
          <Send {...props} containerStyle={HomeStyles.sendContainer} disabled={isGenerating || !text.trim() || !selectedChatId}>
            {props.children}
          </Send>
        )}
        renderDay={() => null}
        renderTime={() => null}
      />
    </View>
  );
}


