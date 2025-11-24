
import React, { useState } from 'react';
import { View, Text, TextInput, Button, ScrollView, StyleSheet, Alert, TouchableOpacity } from 'react-native';
import { useTranslation } from 'react-i18next';
import { useNavigation } from '@react-navigation/native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Window } from '../../components/Window';
import styles from '../../styles/OnboardingStyles';
import { CreateModal, CreateResult } from '../../components/CreateModal';
import { ChatForm, ChatFormData, Collection } from '../../components/ChatForm';
import { getCollections } from '../../api/collections';
import { DEFAULT_PROMPTS, DEFAULT_CHAT_CONFIG, DEFAULT_ONBOARDING_COLLECTION } from '../../constants/defaultPrompts';

interface SelectedFile {
  uri: string;
  name: string;
  size?: number;
  mimeType?: string;
  file?: File; // For web compatibility
}

export function Onboarding() {
  const { t } = useTranslation();
  const navigation = useNavigation();

  React.useEffect(() => {
    try {
      navigation.setOptions({ title: t('navigation.onboarding') as any });
    } catch (e) {
      // ignore when navigation not available
    }
  }, [t, navigation]);
  // Chat form data - using ChatFormData interface
  const [formData, setFormData] = useState<ChatFormData>({
    publicName: DEFAULT_CHAT_CONFIG.publicName,
    internalName: DEFAULT_CHAT_CONFIG.internalName,
    internalDescription: DEFAULT_CHAT_CONFIG.internalDescription,
    defaultLanguage: DEFAULT_CHAT_CONFIG.defaultLanguage,
    defaultCollectionId: '', // Not used in onboarding
    defaultSystemPrompt: DEFAULT_PROMPTS.systemPrompt,
    defaultSystemPromptAppend: DEFAULT_PROMPTS.systemPromptAppend,
    defaultContextPrompt: DEFAULT_PROMPTS.contextPrompt,
    defaultMemoryPrompt: DEFAULT_PROMPTS.memoryPrompt,
    defaultExtractorPrompt: DEFAULT_PROMPTS.extractorPrompt,
    welcomeMessage: DEFAULT_PROMPTS.welcomeMessage,
    defaultOutOfScopeMessage: DEFAULT_PROMPTS.outOfScopeMessage,
    overrideSystemMessage: true,
    overrideAssistantMessage: true,
    useUserPromptRewriting: true,
    userPromptRewritingPrompt: DEFAULT_PROMPTS.userPromptRewriting,
  });

  // Collection data - separate from chat form data
  const [collectionName, setCollectionName] = useState(DEFAULT_ONBOARDING_COLLECTION.name);
  const [collectionDescription, setCollectionDescription] = useState(DEFAULT_ONBOARDING_COLLECTION.description);
  
  const [selectedFiles, setSelectedFiles] = useState<SelectedFile[]>([]);
  const [isUploading, setIsUploading] = useState(false);
  const [collections, setCollections] = useState<Collection[]>([]);
  
  // Accordion state - Basic is expanded by default
  const [expandedAccordion, setExpandedAccordion] = useState<string | null>('basic');
  
  // Create modal state
  const [createConfirmVisible, setCreateConfirmVisible] = useState(false);
  const [createResultVisible, setCreateResultVisible] = useState(false);
  const [createResult, setCreateResult] = useState<CreateResult | null>(null);

  const handleFieldChange = (field: keyof ChatFormData, value: string | boolean) => {
    setFormData((prev) => ({ ...prev, [field]: value }));
  };

  const handleClear = () => {
    setFormData({
      publicName: DEFAULT_CHAT_CONFIG.publicName,
      internalName: DEFAULT_CHAT_CONFIG.internalName,
      internalDescription: DEFAULT_CHAT_CONFIG.internalDescription,
      defaultLanguage: DEFAULT_CHAT_CONFIG.defaultLanguage,
      defaultCollectionId: '',
      defaultSystemPrompt: DEFAULT_PROMPTS.systemPrompt,
      defaultSystemPromptAppend: DEFAULT_PROMPTS.systemPromptAppend,
      defaultContextPrompt: DEFAULT_PROMPTS.contextPrompt,
      defaultMemoryPrompt: DEFAULT_PROMPTS.memoryPrompt,
      defaultExtractorPrompt: DEFAULT_PROMPTS.extractorPrompt,
      welcomeMessage: DEFAULT_PROMPTS.welcomeMessage,
      defaultOutOfScopeMessage: DEFAULT_PROMPTS.outOfScopeMessage,
      collectionName: DEFAULT_ONBOARDING_COLLECTION.name,
      collectionDescription: DEFAULT_ONBOARDING_COLLECTION.description,
      overrideAssistantMessage: true,
      userPromptRewritingPrompt: DEFAULT_PROMPTS.userPromptRewriting,
    });
    setSelectedFiles([]);
  };

  const handlePickFiles = async () => {
    try {
      if (typeof window !== 'undefined') {
        // Web: create a hidden file input to allow multiple selection
        const input = document.createElement('input');
        input.type = 'file';
        input.multiple = true;
        input.accept = '*/*';
        input.onchange = (e: any) => {
          const files: FileList = e.target.files;
          if (files && files.length > 0) {
            const newFiles: SelectedFile[] = Array.from(files).map((f: File) => ({
              uri: URL.createObjectURL(f),
              name: f.name,
              size: f.size,
              mimeType: f.type,
              file: f,
            }));
            setSelectedFiles((prev) => [...prev, ...newFiles]);
            console.log('Files selected:', newFiles.map((ff) => ff.name).join(', '));
          }
        };
        input.click();
      } else {
        // Native: document picker not available in this web build
        Alert.alert(t('messages.errorTitle'), t('onboarding.errors.pickFiles'));
      }
    } catch (error) {
      Alert.alert(t('messages.errorTitle'), t('onboarding.errors.pickFiles'));
      console.error('File picker error:', error);
    }
  };

  const handleRemoveFile = (index: number) => {
    setSelectedFiles((prev) => prev.filter((_, i) => i !== index));
  };

  const handleCreate = async () => {
    setCreateConfirmVisible(true);
  };

  const handleConfirmCreate = async () => {
    setCreateConfirmVisible(false);
    setCreateResultVisible(true);
    setIsUploading(true);
    setCreateResult(null);
    
    try {
      const uploadFormData = new FormData();
      
      // Append all form fields
      uploadFormData.append('publicName', formData.publicName);
      uploadFormData.append('internalName', formData.internalName);
      uploadFormData.append('internalDescription', formData.internalDescription);
      uploadFormData.append('defaultLanguage', formData.defaultLanguage);
      uploadFormData.append('defaultSystemPrompt', formData.defaultSystemPrompt);
      uploadFormData.append('defaultSystemPromptAppend', formData.defaultSystemPromptAppend);
      uploadFormData.append('defaultOutOfScopeMessage', formData.defaultOutOfScopeMessage);
      uploadFormData.append('defaultContextPrompt', formData.defaultContextPrompt);
      uploadFormData.append('defaultMemoryPrompt', formData.defaultMemoryPrompt);
      uploadFormData.append('welcomeMessage', formData.welcomeMessage);
      uploadFormData.append('defaultExtractorPrompt', formData.defaultExtractorPrompt);
      uploadFormData.append('defaultOutOfScopeContext', '');
      uploadFormData.append('defaultOutOfScopeMessage', '');
      uploadFormData.append('collectionName', collectionName);
      uploadFormData.append('collectionDescription', collectionDescription);
      uploadFormData.append('overrideSystemMessage', formData.overrideSystemMessage.toString());
      uploadFormData.append('overrideAssistantMessage', formData.overrideAssistantMessage.toString());
      uploadFormData.append('useUserPromptRewriting', formData.useUserPromptRewriting.toString());
      uploadFormData.append('userPromptRewritingPrompt', formData.userPromptRewritingPrompt);

      // Append files
      selectedFiles.forEach((file) => {
        // For web, use the File object directly if available
        if (file.file) {
          uploadFormData.append('file', file.file);
          console.log('Appending web file:', file.name, file.file.type);
        } else {
          // For React Native (mobile)
          uploadFormData.append('file', {
            uri: file.uri,
            name: file.name,
            type: file.mimeType || 'application/octet-stream',
          } as any);
          console.log('Appending native file:', file.name, file.mimeType);
        }
      });

      console.log('Sending request with', selectedFiles.length, 'files');
      const response = await fetch('http://localhost:8080/api/onboarding/createNewChat', {
        method: 'POST',
        body: uploadFormData,
        // DO NOT set Content-Type header - let the browser set it with boundary
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      const result = await response.json();
      setIsUploading(false);
      setCreateResult({
        success: true,
        message: `Chat "${result.chat.publicName}" created successfully!`,
      });
    } catch (error) {
      setIsUploading(false);
      setCreateResult({
        success: false,
        message: 'Failed to create chat: ' + (error as Error).message,
      });
      console.error('Upload error:', error);
    }
  };

  const handleCancelCreate = () => {
    setCreateConfirmVisible(false);
  };

  const handleCloseCreateModal = () => {
    setCreateResultVisible(false);
    const wasSuccessful = createResult?.success;
    if (wasSuccessful) {
      // Clear the form after successful creation
      handleClear();
    }
    setCreateResult(null);
  };

  // Load collections so the picker in ChatForm is populated during onboarding
  React.useEffect(() => {
    let mounted = true;
    getCollections()
      .then((res) => {
        const data = (res as any).data as Collection[];
        if (mounted) setCollections(data);
      })
      .catch((err) => {
        console.warn('Failed to load collections for onboarding:', err);
      });
    return () => { mounted = false; };
  }, []);

  return (
    <SafeAreaView style={styles.safeArea}>
      <ScrollView>
        <Window>
          <ChatForm
            data={formData}
            onFieldChange={handleFieldChange}
            collections={collections}
            disabled={isUploading}
            expandedAccordion={expandedAccordion}
            onAccordionChange={setExpandedAccordion}
            renderAfterBasic={() => (
              <>
                <Text style={styles.label}>{t('onboarding.collectionName')}</Text>
                <TextInput
                  style={styles.input}
                  placeholder={t('placeholders.collectionName')}
                  value={collectionName}
                  onChangeText={setCollectionName}
                  editable={!isUploading}
                />
                <Text style={styles.label}>{t('onboarding.collectionDescription')}</Text>
                <TextInput
                  style={styles.input}
                  placeholder={t('placeholders.collectionDescription')}
                  value={collectionDescription}
                  onChangeText={setCollectionDescription}
                  editable={!isUploading}
                />
                
                {/* File Upload Section */}
                <Text style={styles.label}>{t('onboarding.uploadTitle')}</Text>
                <TouchableOpacity style={styles.uploadButton} onPress={handlePickFiles} disabled={isUploading}>
                  <Text style={styles.uploadButtonText}>
                    {isUploading ? t('onboarding.uploading') : `+ ${t('onboarding.selectFiles')}`}
                  </Text>
                </TouchableOpacity>

                {selectedFiles.length > 0 && (
                  <View style={styles.filesContainer}>
                    <Text style={styles.filesTitle}>Selected Files ({selectedFiles.length}):</Text>
                    {selectedFiles.map((file, index) => (
                      <View key={index} style={styles.fileItem}>
                        <View style={styles.fileInfo}>
                          <Text style={styles.fileName} numberOfLines={1}>
                            {file.name}
                          </Text>
                          {file.size && (
                            <Text style={styles.fileSize}>
                              {(file.size / 1024).toFixed(1)} KB
                            </Text>
                          )}
                        </View>
                        <TouchableOpacity onPress={() => handleRemoveFile(index)} disabled={isUploading}>
                          <Text style={styles.removeButton}>✕</Text>
                        </TouchableOpacity>
                      </View>
                    ))}
                  </View>
                )}
              </>
            )}
          />

          <View style={styles.buttonRow}>
            <Button title={t('actions.cancel')} onPress={handleClear} color="#888" disabled={isUploading} />
            <Button 
              title={isUploading ? t('onboarding.creating') : t('actions.create')} 
              onPress={handleCreate} 
              color="#007bff" 
              disabled={isUploading}
            />
          </View>
        </Window>
      </ScrollView>
      
      <CreateModal
        confirmVisible={createConfirmVisible}
        itemName={formData.publicName}
        onConfirm={handleConfirmCreate}
        onCancel={handleCancelCreate}
        resultVisible={createResultVisible}
        creating={isUploading}
        createResult={createResult}
        onClose={handleCloseCreateModal}
        confirmMessage={t('onboarding.confirmCreate', { name: formData.publicName, count: selectedFiles.length })}
        creatingMessage={t('onboarding.creatingMessage')}
      />
    </SafeAreaView>
  );
}

// Onboarding styles moved to `src/styles/OnboardingStyles.ts`
