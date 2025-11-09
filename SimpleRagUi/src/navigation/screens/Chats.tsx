

import React, { useEffect, useState } from 'react';
import { View, Text, TextInput, Button, Alert, ActivityIndicator, StyleSheet } from 'react-native';
import { Picker } from '@react-native-picker/picker';
import { Window } from '../../components/Window';
import { getChats, getChatById, updateChat, deleteChat } from '../../api/chats';

type Chat = {
  id: string;
  publicName: string;
  [key: string]: any;
};

export function Chats() {
  const [chats, setChats] = useState<Chat[]>([]);
  const [selectedChatId, setSelectedChatId] = useState('');
  const [chatDetails, setChatDetails] = useState<Chat | null>(null);
  const [loading, setLoading] = useState(false);
  const [updating, setUpdating] = useState(false);

  // Form fields
  const [publicName, setPublicName] = useState('');
  const [internalName, setInternalName] = useState('');
  const [internalDescription, setInternalDescription] = useState('');
  const [defaultLanguage, setDefaultLanguage] = useState('');
  const [defaultSystemPrompt, setDefaultSystemPrompt] = useState('');
  const [defaultSystemPromptAppend, setDefaultSystemPromptAppend] = useState('');
  const [defaultContextPrompt, setDefaultContextPrompt] = useState('');
  const [defaultMemoryPrompt, setDefaultMemoryPrompt] = useState('');
  const [defaultExtractorPrompt, setDefaultExtractorPrompt] = useState('');
  const [overrideSystemMessage, setOverrideSystemMessage] = useState(false);
  const [overrideAssistantMessage, setOverrideAssistantMessage] = useState(false);


  useEffect(() => {
    setLoading(true);
    getChats()
      .then((res) => {
        const data = (res as any).data as Chat[];
        setChats(data);
        setLoading(false);
      })
      .catch(() => setLoading(false));
  }, []);


  useEffect(() => {
    if (selectedChatId) {
      setLoading(true);
      getChatById(selectedChatId)
        .then((res) => {
          const data = (res as any).data as Chat;
          setChatDetails(data);
          setPublicName(data.publicName || '');
          setInternalName(data.internalName || '');
          setInternalDescription(data.internalDescription || '');
          setDefaultLanguage(data.defaultLanguage || '');
          setDefaultSystemPrompt(data.defaultSystemPrompt || '');
          setDefaultSystemPromptAppend(data.defaultSystemPromptAppend || '');
          setDefaultContextPrompt(data.defaultContextPrompt || '');
          setDefaultMemoryPrompt(data.defaultMemoryPrompt || '');
          setDefaultExtractorPrompt(data.defaultExtractorPrompt || '');
          setOverrideSystemMessage(!!data.overrideSystemMessage);
          setOverrideAssistantMessage(!!data.overrideAssistantMessage);
          setLoading(false);
        })
        .catch(() => setLoading(false));
    } else {
      setChatDetails(null);
      setPublicName('');
      setInternalName('');
      setInternalDescription('');
      setDefaultLanguage('');
      setDefaultSystemPrompt('');
      setDefaultSystemPromptAppend('');
      setDefaultContextPrompt('');
      setDefaultMemoryPrompt('');
      setDefaultExtractorPrompt('');
      setOverrideSystemMessage(false);
      setOverrideAssistantMessage(false);
    }
  }, [selectedChatId]);


  const handleUpdate = () => {
    if (!selectedChatId || !chatDetails) return;
    setUpdating(true);
    const updatedChat: Chat = {
      ...chatDetails,
      publicName,
      internalName,
      internalDescription,
      defaultLanguage,
      defaultSystemPrompt,
      defaultSystemPromptAppend,
      defaultContextPrompt,
      defaultMemoryPrompt,
      defaultExtractorPrompt,
      overrideSystemMessage,
      overrideAssistantMessage,
    };
    updateChat(selectedChatId, updatedChat)
      .then(() => {
        Alert.alert('Success', 'Chat updated');
        setUpdating(false);
      })
      .catch(() => {
        Alert.alert('Error', 'Failed to update chat');
        setUpdating(false);
      });
  };


  const handleDelete = () => {
    if (!selectedChatId) return;
    Alert.alert('Confirm', 'Delete this chat?', [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Delete', style: 'destructive', onPress: () => {
          setUpdating(true);
          deleteChat(selectedChatId)
            .then(() => {
              Alert.alert('Deleted', 'Chat deleted');
              setSelectedChatId('');
              setUpdating(false);
              // Refresh chat list
              getChats().then((res) => {
                const data = (res as any).data as Chat[];
                setChats(data);
              });
            })
            .catch(() => {
              Alert.alert('Error', 'Failed to delete chat');
              setUpdating(false);
            });
        }
      }
    ]);
  };

  return (
    <Window>
      <Text style={styles.title}>Chats</Text>
      {loading && <ActivityIndicator />}
      <Picker
        selectedValue={selectedChatId}
        onValueChange={setSelectedChatId}
        style={styles.picker}
      >
        <Picker.Item label="Select a chat..." value="" />
        {chats.map((chat) => (
          <Picker.Item key={chat.id} label={chat.publicName || chat.id} value={chat.id} />
        ))}
      </Picker>
      <View style={styles.form}>
        <Text style={styles.label}>Public Name</Text>
        <TextInput
          style={styles.input}
          value={publicName}
          onChangeText={setPublicName}
          placeholder="Public Name"
          editable={!!chatDetails}
        />
        <Text style={styles.label}>Internal Name</Text>
        <TextInput
          style={styles.input}
          value={internalName}
          onChangeText={setInternalName}
          placeholder="Internal Name"
          editable={!!chatDetails}
        />
        <Text style={styles.label}>Internal Description</Text>
        <TextInput
          style={styles.input}
          value={internalDescription}
          onChangeText={setInternalDescription}
          placeholder="Internal Description"
          editable={!!chatDetails}
        />
        <Text style={styles.label}>Default Language</Text>
        <TextInput
          style={styles.input}
          value={defaultLanguage}
          onChangeText={setDefaultLanguage}
          placeholder="Default Language"
          editable={!!chatDetails}
        />
        <Text style={styles.label}>Default System Prompt</Text>
        <TextInput
          style={[styles.input, styles.textarea]}
          value={defaultSystemPrompt}
          onChangeText={setDefaultSystemPrompt}
          placeholder="Default System Prompt"
          editable={!!chatDetails}
          multiline
          numberOfLines={5}
        />
        <Text style={styles.label}>Default System Prompt Append</Text>
        <TextInput
          style={[styles.input, styles.textarea]}
          value={defaultSystemPromptAppend}
          onChangeText={setDefaultSystemPromptAppend}
          placeholder="Default System Prompt Append"
          editable={!!chatDetails}
          multiline
          numberOfLines={5}
        />
        <Text style={styles.label}>Default Context Prompt</Text>
        <TextInput
          style={[styles.input, styles.textarea]}
          value={defaultContextPrompt}
          onChangeText={setDefaultContextPrompt}
          placeholder="Default Context Prompt"
          editable={!!chatDetails}
          multiline
          numberOfLines={5}
        />
        <Text style={styles.label}>Default Memory Prompt</Text>
        <TextInput
          style={[styles.input, styles.textarea]}
          value={defaultMemoryPrompt}
          onChangeText={setDefaultMemoryPrompt}
          placeholder="Default Memory Prompt"
          editable={!!chatDetails}
          multiline
          numberOfLines={5}
        />
        <Text style={styles.label}>Default Extractor Prompt</Text>
        <TextInput
          style={[styles.input, styles.textarea]}
          value={defaultExtractorPrompt}
          onChangeText={setDefaultExtractorPrompt}
          placeholder="Default Extractor Prompt"
          editable={!!chatDetails}
          multiline
          numberOfLines={5}
        />
        <View style={styles.checkboxRow}>
          <Text style={styles.label}>Override System Message</Text>
          <input
            type="checkbox"
            checked={overrideSystemMessage}
            onChange={() => !!chatDetails && setOverrideSystemMessage(!overrideSystemMessage)}
            style={styles.checkbox}
            disabled={!chatDetails}
          />
        </View>
        <View style={styles.checkboxRow}>
          <Text style={styles.label}>Override Assistant Message</Text>
          <input
            type="checkbox"
            checked={overrideAssistantMessage}
            onChange={() => !!chatDetails && setOverrideAssistantMessage(!overrideAssistantMessage)}
            style={styles.checkbox}
            disabled={!chatDetails}
          />
        </View>
        <Button title="Update" onPress={handleUpdate} disabled={updating || !chatDetails} />
        <View style={{ height: 10 }} />
        <Button title="Delete" onPress={handleDelete} color="red" disabled={updating || !chatDetails} />
      </View>
    </Window>
  );
}

const styles = StyleSheet.create({
  title: {
    fontSize: 22,
    fontWeight: 'bold',
    marginBottom: 16,
    textAlign: 'center',
  },
  picker: {
    marginVertical: 12,
  },
  form: {
    marginTop: 20,
  },
  input: {
    borderWidth: 1,
    borderColor: '#ccc',
    borderRadius: 6,
    padding: 8,
    marginVertical: 8,
  },
  label: {
    alignSelf: 'flex-start',
    fontWeight: 'bold',
    marginBottom: 2,
    marginTop: 6,
  },
  checkboxRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginVertical: 8,
    gap: 8,
  },
  checkbox: {
    marginLeft: 8,
    width: 20,
    height: 20,
  },
  textarea: {
    minHeight: 100,
    textAlignVertical: 'top',
  },
});
