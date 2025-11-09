

import React, { useEffect, useState } from 'react';
import { View, ScrollView, Text, TextInput, Button, Alert, ActivityIndicator, TouchableOpacity } from 'react-native';
import styles from '../../styles/ChatsStyles';
import { Picker } from '@react-native-picker/picker';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Window } from '../../components/Window';
import { getChats, getChatById, updateChat, deleteChat } from '../../api/chats';
import { getCollections } from '../../api/collections';

type Chat = {
  id: string;
  publicName: string;
  defaultCollectionId?: string;
  [key: string]: any;
};

type Collection = {
  id: string;
  name: string;
  [key: string]: any;
};

export function Chats() {
  const [chats, setChats] = useState<Chat[]>([]);
  const [selectedChatId, setSelectedChatId] = useState('');
  const [chatDetails, setChatDetails] = useState<Chat | null>(null);
  const [loading, setLoading] = useState(false);
  const [updating, setUpdating] = useState(false);
  const [collections, setCollections] = useState<Collection[]>([]);

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
  const [defaultCollectionId, setDefaultCollectionId] = useState<string>('');
  // Fetch collections on mount
  useEffect(() => {
    getCollections()
      .then((res) => {
        const data = (res as any).data as Collection[];
        setCollections(data);
      })
      .catch(() => {});
  }, []);


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
          setDefaultCollectionId(data.defaultCollectionId || '');
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
      setDefaultCollectionId('');
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
      defaultCollectionId,
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
      <SafeAreaView style={{ flex: 1 }}>
              <ScrollView>
    <Window>
      {loading && <ActivityIndicator />}
      <View style={styles.row}>
        <ScrollView style={styles.sidebar} contentContainerStyle={styles.sidebarContent}>
          {chats.map((chat) => (
            <TouchableOpacity
              key={chat.id}
              style={[styles.chatItem, selectedChatId === chat.id && styles.chatItemSelected]}
              onPress={() => setSelectedChatId(chat.id)}
            >
              <Text style={styles.chatItemText}>{chat.publicName || chat.id}</Text>
            </TouchableOpacity>
          ))}
        </ScrollView>
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

        <Text style={styles.label}>Default Collection</Text>
        <View style={styles.pickerWrapper}>
          <Picker
            selectedValue={defaultCollectionId}
            onValueChange={(itemValue) => chatDetails && setDefaultCollectionId(itemValue)}
            enabled={!!chatDetails}
            style={styles.picker}
          >
            <Picker.Item label="Select a collection..." value="" />
            {collections.map((col) => (
              <Picker.Item key={col.id} label={col.name} value={col.id} />
            ))}
          </Picker>
        </View>

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
      </View>
    </Window>
      </ScrollView>

    </SafeAreaView>
  );
}

