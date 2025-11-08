import { GiftedChat, IMessage, Bubble, Send } from 'react-native-gifted-chat';
import React, { useCallback, useState } from 'react';
import { View, TextInput, Platform } from 'react-native';
import { HomeStyles } from '../../styles/HomeStyles';

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

  const onSend = useCallback((newMessages: IMessage[] = []) => {
    setMessages((previousMessages) => GiftedChat.append(previousMessages, newMessages));
    setText('');
    // Here you would call your LLM backend and stream/append the response
  }, []);

  return (
  <View style={HomeStyles.container}>
      <GiftedChat
        messages={messages}
        onSend={onSend}
        user={{ _id: 1 }}
        placeholder="Type a message..."
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
            placeholder="Type a message..."
            placeholderTextColor="#888"
            multiline={false}
            autoComplete="off"
            autoCorrect={false}
            autoCapitalize="none"
            onSubmitEditing={() => {
              if (text.trim()) {
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
          <Send {...props} containerStyle={HomeStyles.sendContainer}>
            {props.children}
          </Send>
        )}
        renderDay={() => null}
        renderTime={() => null}
      />
    </View>
  );
}


