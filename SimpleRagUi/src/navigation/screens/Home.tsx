import { GiftedChat, IMessage, Bubble, Send } from 'react-native-gifted-chat';
import React, { useCallback, useState, useEffect, useRef } from 'react';
import { View, TextInput, Platform, ActivityIndicator, Alert } from 'react-native';
import { Picker } from '@react-native-picker/picker';
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
      // Create assistant message placeholder
      const assistantMessage: IMessage = {
        _id: Date.now() + 1,
        text: '',
        createdAt: new Date(),
        user: {
          _id: 2,
          name: 'SimpleRagServer',
        },
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
            } catch (e) {
              console.error('Error parsing streaming chunk:', e, 'Data:', data);
            }
          }
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
    if (assistantMessageRef.current) {
      setMessages((prev) => {
        const updated = prev.map(msg => {
          if (msg._id === assistantMessageRef.current!._id) {
            return {
              ...msg,
              text: append ? msg.text + content : content
            };
          }
          return msg;
        });
        return updated;
      });
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
    const conversationMessages = [...messages, ...newMessages]
      .reverse() // GiftedChat stores in reverse chronological order
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
          <ActivityIndicator color="#fff" />
        ) : (
          <View style={HomeStyles.pickerWrapper}>
            <Picker
              selectedValue={selectedChatId}
              onValueChange={(value) => setSelectedChatId(value)}
              style={HomeStyles.picker}
              dropdownIconColor="#fff"
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
        placeholder={isGenerating ? "Generating response..." : "Type a message..."}
        showUserAvatar
        alwaysShowSend
  messagesContainerStyle={HomeStyles.messagesContainer}
        text={text}
        onInputTextChanged={setText}
        renderComposer={props => (
          <TextInput
            {...props.textInputProps}
            style={[HomeStyles.input, props.textInputStyle]}
            value={text}
            onChangeText={setText}
            placeholder={isGenerating ? "Generating response..." : "Type a message..."}
            placeholderTextColor="#888"
            multiline={false}
            autoComplete="off"
            autoCorrect={false}
            autoCapitalize="none"
            editable={!isGenerating}
            onSubmitEditing={() => {
              if (text.trim() && !isGenerating) {
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
        renderSend={props => (
          <Send {...props} containerStyle={HomeStyles.sendContainer} disabled={isGenerating || !text.trim()}>
            {props.children}
          </Send>
        )}
        renderDay={() => null}
        renderTime={() => null}
      />
    </View>
  );
}


