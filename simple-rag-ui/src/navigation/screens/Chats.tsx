import React, { useEffect, useState, useCallback } from 'react';
import { View, ScrollView, Text, Button, ActivityIndicator } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import styles from '../../styles/ChatsStyles';
import { Picker } from '@react-native-picker/picker';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Window } from '../../components/Window';
import { getChats, getChatById, updateChat, deleteChat } from '../../api/chats';
import { getCollections } from '../../api/collections';
import { DeleteModal, DeleteResult } from '../../components/DeleteModal';
import { UpdateModal, UpdateResult } from '../../components/UpdateModal';
import { ChatForm, ChatFormData, Collection } from '../../components/ChatForm';
import { useTranslation } from 'react-i18next';
import { useNavigation } from '@react-navigation/native';

type Chat = {
  id: string;
  publicName: string;
  defaultCollectionId?: string;
  llmConfig?: {
    useCase?: string;
    maxNewTokens?: number;
    temperature?: number;
    doSample?: boolean;
    topK?: number;
    topP?: number;
    repetitionPenalty?: number;
    minNewTokens?: number;
  };
  [key: string]: any;
};

export function Chats() {
  const { t } = useTranslation();
  const navigation = useNavigation();

  React.useEffect(() => {
    try {
      navigation.setOptions({ title: t('navigation.chats') as any });
    } catch (e) {
      // ignore when navigation not available
    }
  }, [t, navigation]);
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

  // Form data - consolidated into ChatFormData structure
  const [formData, setFormData] = useState<ChatFormData>({
    publicName: '',
    internalName: '',
    internalDescription: '',
    defaultLanguage: '',
    defaultCollectionId: '',
    welcomeMessage: '',
    defaultSystemPrompt: '',
    defaultSystemPromptAppend: '',
    defaultOutOfScopeMessage: '',
    defaultContextPrompt: '',
    defaultMemoryPrompt: '',
    defaultExtractorPrompt: '',
    overrideSystemMessage: false,
    overrideAssistantMessage: false,
    useUserPromptRewriting: false,
    userPromptRewritingPrompt: '',
  });
  
  // Accordion state - Basic is expanded by default
  const [expandedAccordion, setExpandedAccordion] = useState<string | null>('basic');
  
  // Fetch collections and chats whenever screen comes into focus
  useFocusEffect(
    useCallback(() => {
      getCollections()
        .then((res) => {
          const data = (res as any).data as Collection[];
          setCollections(data);
        })
        .catch(() => {});
      
      setLoading(true);
      getChats()
        .then((res) => {
          const data = (res as any).data as Chat[];
          setChats(data);
          setLoading(false);
        })
        .catch(() => setLoading(false));
    }, [])
  );


  useEffect(() => {
    if (selectedChatId) {
      setLoading(true);
      setExpandedAccordion('basic'); // Open Basic accordion by default when chat is selected
      getChatById(selectedChatId)
        .then((res) => {
          const data = (res as any).data as Chat;
          setChatDetails(data);
          setFormData({
            publicName: data.publicName || '',
            internalName: data.internalName || '',
            internalDescription: data.internalDescription || '',
            defaultLanguage: data.defaultLanguage || '',
            defaultCollectionId: data.defaultCollectionId || '',
            welcomeMessage: data.welcomeMessage || '',
            defaultSystemPrompt: data.defaultSystemPrompt || '',
            defaultSystemPromptAppend: data.defaultSystemPromptAppend || '',
            defaultOutOfScopeMessage: data.defaultOutOfScopeMessage || '',
            defaultContextPrompt: data.defaultContextPrompt || '',
            defaultMemoryPrompt: data.defaultMemoryPrompt || '',
            defaultExtractorPrompt: data.defaultExtractorPrompt || '',
            overrideSystemMessage: !!data.overrideSystemMessage,
            overrideAssistantMessage: !!data.overrideAssistantMessage,
            useUserPromptRewriting: !!data.useUserPromptRewriting,
            userPromptRewritingPrompt: data.userPromptRewritingPrompt || '',
          });
          setLoading(false);
        })
        .catch(() => setLoading(false));
    } else {
      setExpandedAccordion('basic'); // Reset to Basic when no chat selected
      setChatDetails(null);
      setFormData({
        publicName: '',
        internalName: '',
        internalDescription: '',
        defaultLanguage: '',
        defaultCollectionId: '',
        welcomeMessage: '',
        defaultSystemPrompt: '',
        defaultSystemPromptAppend: '',
        defaultOutOfScopeMessage: '',
        defaultContextPrompt: '',
        defaultMemoryPrompt: '',
        defaultExtractorPrompt: '',
        overrideSystemMessage: false,
        overrideAssistantMessage: false,
        useUserPromptRewriting: false,
        userPromptRewritingPrompt: '',
      });
    }
  }, [selectedChatId]);

  // Check if any field has changed
  const hasChanges = () => {
    if (!chatDetails) return false;
    return (
      formData.publicName !== (chatDetails.publicName || '') ||
      formData.internalName !== (chatDetails.internalName || '') ||
      formData.internalDescription !== (chatDetails.internalDescription || '') ||
      formData.defaultLanguage !== (chatDetails.defaultLanguage || '') ||
      formData.defaultSystemPrompt !== (chatDetails.defaultSystemPrompt || '') ||
      formData.defaultSystemPromptAppend !== (chatDetails.defaultSystemPromptAppend || '') ||
      formData.welcomeMessage !== (chatDetails.welcomeMessage || '') ||
      formData.defaultOutOfScopeMessage !== (chatDetails.defaultOutOfScopeMessage || '') ||
      formData.defaultContextPrompt !== (chatDetails.defaultContextPrompt || '') ||
      formData.defaultMemoryPrompt !== (chatDetails.defaultMemoryPrompt || '') ||
      formData.defaultExtractorPrompt !== (chatDetails.defaultExtractorPrompt || '') ||
      formData.overrideSystemMessage !== (chatDetails.overrideSystemMessage || false) ||
      formData.overrideAssistantMessage !== (chatDetails.overrideAssistantMessage || false) ||
      formData.useUserPromptRewriting !== (chatDetails.useUserPromptRewriting || false) ||
      formData.userPromptRewritingPrompt !== (chatDetails.userPromptRewritingPrompt || '') ||
      formData.defaultCollectionId !== (chatDetails.defaultCollectionId || '')
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
      publicName: formData.publicName,
      internalName: formData.internalName,
      internalDescription: formData.internalDescription,
      defaultLanguage: formData.defaultLanguage,
      defaultSystemPrompt: formData.defaultSystemPrompt,
      defaultSystemPromptAppend: formData.defaultSystemPromptAppend,
      welcomeMessage: formData.welcomeMessage,
      defaultOutOfScopeMessage: formData.defaultOutOfScopeMessage,
      defaultContextPrompt: formData.defaultContextPrompt,
      defaultMemoryPrompt: formData.defaultMemoryPrompt,
      defaultExtractorPrompt: formData.defaultExtractorPrompt,
      overrideSystemMessage: formData.overrideSystemMessage,
      overrideAssistantMessage: formData.overrideAssistantMessage,
      useUserPromptRewriting: formData.useUserPromptRewriting,
      userPromptRewritingPrompt: formData.userPromptRewritingPrompt,
      defaultCollectionId: formData.defaultCollectionId,
    };
    updateChat(selectedChatId, updatedChat)
      .then(() => {
        setUpdating(false);
        setUpdateResult({
          success: true,
          message: t('messages.chatUpdateSuccess'),
        });
      })
      .catch((error) => {
        const errorMessage = error?.response?.data?.message || error?.message || 'Unknown error occurred';
        setUpdating(false);
        setUpdateResult({
          success: false,
          message: t('messages.chatUpdateFailed', { error: errorMessage }),
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
          setFormData({
            publicName: data.publicName || '',
            internalName: data.internalName || '',
            internalDescription: data.internalDescription || '',
            defaultLanguage: data.defaultLanguage || '',
            defaultCollectionId: data.defaultCollectionId || '',
            welcomeMessage: data.welcomeMessage || '',
            defaultSystemPrompt: data.defaultSystemPrompt || '',
            defaultSystemPromptAppend: data.defaultSystemPromptAppend || '',
            defaultOutOfScopeMessage: data.defaultOutOfScopeMessage || '',
            defaultContextPrompt: data.defaultContextPrompt || '',
            defaultMemoryPrompt: data.defaultMemoryPrompt || '',
            defaultExtractorPrompt: data.defaultExtractorPrompt || '',
            overrideSystemMessage: data.overrideSystemMessage || false,
            overrideAssistantMessage: data.overrideAssistantMessage || false,
            useUserPromptRewriting: !!data.useUserPromptRewriting,
            userPromptRewritingPrompt: data.userPromptRewritingPrompt || '',
          });
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
          message: t('messages.chatDeleted', { name: formData.publicName })
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
          message: t('messages.chatDeleteFailed', { error: errorMessage })
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
      <SafeAreaView style={styles.safeArea}>
              <ScrollView>
    <Window>
      {loading && <ActivityIndicator />}
      <View style={styles.container}>
          {/* Chat Dropdown */}
          <View style={styles.dropdownContainer}>
            <Text style={styles.dropdownLabel}>{t('labels.chatLabel')}</Text>
            <View style={styles.pickerWrapper}>
              <Picker
                selectedValue={selectedChatId}
                onValueChange={(value) => setSelectedChatId(value)}
                style={styles.picker}
              >
                <Picker.Item label={t('basic.selectChat')} value="" />
                {chats.map((chat) => (
                  <Picker.Item key={chat.id} label={chat.publicName || chat.id} value={chat.id} />
                ))}
              </Picker>
            </View>
          </View>
        <View style={styles.form}>
          <ChatForm
            data={formData}
            onFieldChange={(field, value) => {
              setFormData((prev) => ({ ...prev, [field]: value }));
            }}
            collections={collections}
            disabled={!chatDetails}
            expandedAccordion={expandedAccordion}
            onAccordionChange={setExpandedAccordion}
          />

          <Button title={t('actions.update')} onPress={handleUpdate} disabled={updating || !chatDetails || !hasChanges()} />
          <View style={styles.spacer} />
          <Button title={t('actions.delete')} onPress={handleDelete} color="red" disabled={updating || !chatDetails} />
        </View>
      </View>
    </Window>
      </ScrollView>

      <DeleteModal
        confirmVisible={confirmModalVisible}
        itemName={formData.publicName}
        onConfirm={handleConfirmDelete}
        onCancel={handleCancelDelete}
        resultVisible={deleteModalVisible}
        deleting={deleting}
        deleteResult={deleteResult}
        onClose={handleCloseDeleteModal}
        confirmMessage={t('messages.confirmDelete', { name: formData.publicName })}
        deletingMessage={t('messages.deleting')}
      />
      
      <UpdateModal
        confirmVisible={updateConfirmVisible}
        itemName={formData.publicName}
        onConfirm={handleConfirmUpdate}
        onCancel={handleCancelUpdate}
        resultVisible={updateResultVisible}
        updating={updating}
        updateResult={updateResult}
        onClose={handleCloseUpdateModal}
        confirmMessage={t('messages.confirmUpdate', { name: formData.publicName })}
        updatingMessage={t('messages.updating')}
      />
    </SafeAreaView>
  );
}

