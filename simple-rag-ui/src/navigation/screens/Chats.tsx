import React, { useEffect, useState } from 'react';
import { View, ScrollView, Text, TextInput, Button, ActivityIndicator } from 'react-native';
import styles from '../../styles/ChatsStyles';
import { Picker } from '@react-native-picker/picker';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Window } from '../../components/Window';
import { getChats, getChatById, updateChat, deleteChat } from '../../api/chats';
import { getCollections } from '../../api/collections';
import { DeleteModal, DeleteResult } from '../../components/DeleteModal';
import { UpdateModal, UpdateResult } from '../../components/UpdateModal';

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
  const [deleteModalVisible, setDeleteModalVisible] = useState(false);
  const [deleteResult, setDeleteResult] = useState<DeleteResult | null>(null);
  const [deleting, setDeleting] = useState(false);
  const [confirmModalVisible, setConfirmModalVisible] = useState(false);
  
  // Update modal state
  const [updateConfirmVisible, setUpdateConfirmVisible] = useState(false);
  const [updateResultVisible, setUpdateResultVisible] = useState(false);
  const [updateResult, setUpdateResult] = useState<UpdateResult | null>(null);

  // Form fields
  const [publicName, setPublicName] = useState('');
  const [internalName, setInternalName] = useState('');
  const [internalDescription, setInternalDescription] = useState('');
  const [defaultLanguage, setDefaultLanguage] = useState('');
  const [defaultSystemPrompt, setDefaultSystemPrompt] = useState('');
  const [defaultSystemPromptAppend, setDefaultSystemPromptAppend] = useState('');
  const [defaultOutOfScopeMessage, setDefaultOutOfScopeMessage] = useState('');
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
          setDefaultOutOfScopeMessage(data.defaultOutOfScopeMessage || '');
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
      setDefaultOutOfScopeMessage('');
      setDefaultContextPrompt('');
      setDefaultMemoryPrompt('');
      setDefaultExtractorPrompt('');
      setOverrideSystemMessage(false);
      setOverrideAssistantMessage(false);
      setDefaultCollectionId('');
    }
  }, [selectedChatId]);

  // Check if any field has changed
  const hasChanges = () => {
    if (!chatDetails) return false;
    return (
      publicName !== (chatDetails.publicName || '') ||
      internalName !== (chatDetails.internalName || '') ||
      internalDescription !== (chatDetails.internalDescription || '') ||
      defaultLanguage !== (chatDetails.defaultLanguage || '') ||
      defaultSystemPrompt !== (chatDetails.defaultSystemPrompt || '') ||
      defaultSystemPromptAppend !== (chatDetails.defaultSystemPromptAppend || '') ||
      defaultOutOfScopeMessage !== (chatDetails.defaultOutOfScopeMessage || '') ||
      defaultContextPrompt !== (chatDetails.defaultContextPrompt || '') ||
      defaultMemoryPrompt !== (chatDetails.defaultMemoryPrompt || '') ||
      defaultExtractorPrompt !== (chatDetails.defaultExtractorPrompt || '') ||
      overrideSystemMessage !== (chatDetails.overrideSystemMessage || false) ||
      overrideAssistantMessage !== (chatDetails.overrideAssistantMessage || false) ||
      defaultCollectionId !== (chatDetails.defaultCollectionId || '')
    );
  };

  const handleUpdate = () => {
    if (!selectedChatId || !chatDetails || !hasChanges()) return;
    setUpdateConfirmVisible(true);
  };

  const handleConfirmUpdate = () => {
    if (!selectedChatId || !chatDetails) return;
    setUpdateConfirmVisible(false);
    setUpdateResultVisible(true);
    setUpdating(true);
    setUpdateResult(null);
    
    const updatedChat: Chat = {
      ...chatDetails,
      publicName,
      internalName,
      internalDescription,
      defaultLanguage,
      defaultSystemPrompt,
      defaultSystemPromptAppend,
      defaultOutOfScopeMessage,
      defaultContextPrompt,
      defaultMemoryPrompt,
      defaultExtractorPrompt,
      overrideSystemMessage,
      overrideAssistantMessage,
      defaultCollectionId,
    };
    updateChat(selectedChatId, updatedChat)
      .then(() => {
        setUpdating(false);
        setUpdateResult({
          success: true,
          message: 'Chat updated successfully',
        });
      })
      .catch((error) => {
        const errorMessage = error?.response?.data?.message || error?.message || 'Unknown error occurred';
        setUpdating(false);
        setUpdateResult({
          success: false,
          message: `Failed to update chat: ${errorMessage}`,
        });
      });
  };

  const handleCancelUpdate = () => {
    setUpdateConfirmVisible(false);
  };

  const handleCloseUpdateModal = () => {
    setUpdateResultVisible(false);
    const wasSuccessful = updateResult?.success;
    if (wasSuccessful && selectedChatId) {
      // Reload the chats dropdown list
      getChats()
        .then((res) => {
          const data = (res as any).data as Chat[];
          setChats(data);
        })
        .catch(() => {
          console.error('Failed to reload chats list');
        });
      
      // Reload the chat details after successful update
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
          setDefaultOutOfScopeMessage(data.defaultOutOfScopeMessage || '');
          setDefaultContextPrompt(data.defaultContextPrompt || '');
          setDefaultMemoryPrompt(data.defaultMemoryPrompt || '');
          setDefaultExtractorPrompt(data.defaultExtractorPrompt || '');
          setOverrideSystemMessage(data.overrideSystemMessage || false);
          setOverrideAssistantMessage(data.overrideAssistantMessage || false);
          setDefaultCollectionId(data.defaultCollectionId || '');
        })
        .catch(() => {
          console.error('Failed to reload chat');
        });
    }
    setUpdateResult(null);
  };


  const handleDelete = () => {
    if (!selectedChatId) return;
    setConfirmModalVisible(true);
  };

  const handleConfirmDelete = () => {
    setConfirmModalVisible(false);
    setDeleting(true);
    setDeleteModalVisible(true);
    setDeleteResult(null);
    deleteChat(selectedChatId)
      .then(() => {
        setDeleteResult({
          success: true,
          message: `Chat "${publicName}" has been successfully deleted.`
        });
        setDeleting(false);
        setSelectedChatId('');
        setChatDetails(null);
        // Refresh chat list
        getChats().then((res) => {
          const data = (res as any).data as Chat[];
          setChats(data);
        });
      })
      .catch((error) => {
        const errorMessage = error?.response?.data?.message || error?.message || 'Unknown error occurred';
        setDeleteResult({
          success: false,
          message: `Failed to delete chat: ${errorMessage}`
        });
        setDeleting(false);
      });
  };

  const handleCancelDelete = () => {
    setConfirmModalVisible(false);
  };

  const handleCloseDeleteModal = () => {
    setDeleteModalVisible(false);
    const wasSuccessful = deleteResult?.success;
    setDeleteResult(null);
    if (wasSuccessful) {
      // Reload chat list after successful delete
      getChats().then((res) => {
        const data = (res as any).data as Chat[];
        setChats(data);
      });
    }
  };

  return (
      <SafeAreaView style={{ flex: 1 }}>
              <ScrollView>
    <Window>
      {loading && <ActivityIndicator />}
      <View style={styles.container}>
          {/* Chat Dropdown */}
          <View style={styles.dropdownContainer}>
            <Text style={styles.dropdownLabel}>Chat:</Text>
            <View style={styles.pickerWrapper}>
              <Picker
                selectedValue={selectedChatId}
                onValueChange={(value) => setSelectedChatId(value)}
                style={styles.picker}
              >
                <Picker.Item label="Select a chat..." value="" />
                {chats.map((chat) => (
                  <Picker.Item key={chat.id} label={chat.publicName || chat.id} value={chat.id} />
                ))}
              </Picker>
            </View>
          </View>
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
        <Text style={styles.label}>Default Out of Scope Message</Text>
        <TextInput
          style={[styles.input, styles.textarea]}
          value={defaultOutOfScopeMessage}
          onChangeText={setDefaultOutOfScopeMessage}
          placeholder="Default Out of Scope Message"
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
        <Button title="Update" onPress={handleUpdate} disabled={updating || !chatDetails || !hasChanges()} />
        <View style={{ height: 10 }} />
        <Button title="Delete" onPress={handleDelete} color="red" disabled={updating || !chatDetails} />
      </View>
      </View>
    </Window>
      </ScrollView>

      <DeleteModal
        confirmVisible={confirmModalVisible}
        itemName={publicName}
        onConfirm={handleConfirmDelete}
        onCancel={handleCancelDelete}
        resultVisible={deleteModalVisible}
        deleting={deleting}
        deleteResult={deleteResult}
        onClose={handleCloseDeleteModal}
        confirmMessage={`Are you sure you want to delete the chat \"${publicName}\"?`}
        deletingMessage="Deleting chat..."
      />
      
      <UpdateModal
        confirmVisible={updateConfirmVisible}
        itemName={publicName}
        onConfirm={handleConfirmUpdate}
        onCancel={handleCancelUpdate}
        resultVisible={updateResultVisible}
        updating={updating}
        updateResult={updateResult}
        onClose={handleCloseUpdateModal}
        confirmMessage={`Are you sure you want to update the chat \"${publicName}\"?`}
        updatingMessage="Updating chat..."
      />
    </SafeAreaView>
  );
}

