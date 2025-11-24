
import React, { useState } from 'react';
import { View, Text, TextInput, Button, ScrollView, StyleSheet, Alert, TouchableOpacity } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import * as DocumentPicker from 'expo-document-picker';
import { Window } from '../../components/Window';
import { GlobalStyles } from '../../styles/GlobalStyles';
import { CreateModal, CreateResult } from '../../components/CreateModal';
import { ChatForm, ChatFormData } from '../../components/ChatForm';
import { DEFAULT_PROMPTS, DEFAULT_CHAT_CONFIG, DEFAULT_ONBOARDING_COLLECTION } from '../../constants/defaultPrompts';

interface SelectedFile {
  uri: string;
  name: string;
  size?: number;
  mimeType?: string;
  file?: File; // For web compatibility
}

export function Onboarding() {
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
      const result = await DocumentPicker.getDocumentAsync({
        type: '*/*',
        multiple: true,
        copyToCacheDirectory: true,
      });

      if (!result.canceled && result.assets) {
        const newFiles: SelectedFile[] = result.assets.map((asset) => ({
          uri: asset.uri,
          name: asset.name,
          size: asset.size,
          mimeType: asset.mimeType || undefined,
          file: (asset as any).file, // Keep raw File object for web
        }));
        setSelectedFiles((prev) => [...prev, ...newFiles]);
        console.log('Files selected:', newFiles.map(f => f.name).join(', '));
      }
    } catch (error) {
      Alert.alert('Error', 'Failed to pick files');
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

  return (
    <SafeAreaView style={{ flex: 1 }}>
      <ScrollView>
        <Window>
          <ChatForm
            data={formData}
            onFieldChange={handleFieldChange}
            collections={[]} // No collections needed in onboarding
            disabled={isUploading}
            expandedAccordion={expandedAccordion}
            onAccordionChange={setExpandedAccordion}
            renderAfterBasic={() => (
              <>
                <Text style={styles.label}>Collection Name</Text>
                <TextInput
                  style={styles.input}
                  placeholder="Collection Name"
                  value={collectionName}
                  onChangeText={setCollectionName}
                  editable={!isUploading}
                />
                <Text style={styles.label}>Collection Description</Text>
                <TextInput
                  style={styles.input}
                  placeholder="Collection Description"
                  value={collectionDescription}
                  onChangeText={setCollectionDescription}
                  editable={!isUploading}
                />
                
                {/* File Upload Section */}
                <Text style={styles.label}>Upload Documents (Optional)</Text>
                <TouchableOpacity style={styles.uploadButton} onPress={handlePickFiles} disabled={isUploading}>
                  <Text style={styles.uploadButtonText}>
                    {isUploading ? 'Uploading...' : '+ Select Files'}
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
            <Button title="Cancel" onPress={handleClear} color="#888" disabled={isUploading} />
            <Button 
              title={isUploading ? 'Creating...' : 'Create'} 
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
        confirmMessage={`Are you sure you want to create the chat "${formData.publicName}" with ${selectedFiles.length} file(s)?`}
        creatingMessage="Creating chat and uploading files..."
      />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  title: {
    fontSize: 24,
    fontWeight: 'bold',
  },
  label: {
    alignSelf: 'flex-start',
    fontWeight: 'bold',
    marginBottom: 2,
    marginTop: 6,
  },
  input: {
    width: '100%',
    borderWidth: 1,
    borderColor: '#ccc',
    borderRadius: 6,
    padding: 8,
    marginBottom: 12,
    backgroundColor: '#fff',
  },
  textarea: {
    minHeight: 100,
    textAlignVertical: 'top',
  },
  switchRow: {
    flexDirection: 'column',
    alignItems: 'flex-start',
    marginBottom: 16,
    width: '100%',
    gap: 4,
  },
  buttonRow: {
    flexDirection: 'column',
    alignItems: 'stretch',
    marginTop: 20,
    width: '100%',
    gap: 8,
  },
  uploadButton: {
    backgroundColor: '#007bff',
    padding: 12,
    borderRadius: 6,
    alignItems: 'center',
    marginBottom: 12,
  },
  uploadButtonText: {
    color: '#fff',
    fontWeight: 'bold',
    fontSize: 16,
  },
  filesContainer: {
    marginBottom: 16,
    padding: 12,
    backgroundColor: '#f8f9fa',
    borderRadius: 6,
    borderWidth: 1,
    borderColor: '#ddd',
  },
  filesTitle: {
    fontWeight: 'bold',
    marginBottom: 8,
    fontSize: 14,
  },
  fileItem: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: 8,
    backgroundColor: '#fff',
    borderRadius: 4,
    marginBottom: 6,
    borderWidth: 1,
    borderColor: '#e0e0e0',
  },
  fileInfo: {
    flex: 1,
    marginRight: 8,
  },
  fileName: {
    fontSize: 14,
    fontWeight: '500',
    marginBottom: 2,
  },
  fileSize: {
    fontSize: 12,
    color: '#666',
  },
  removeButton: {
    color: '#dc3545',
    fontSize: 20,
    fontWeight: 'bold',
    paddingHorizontal: 8,
  },
});
