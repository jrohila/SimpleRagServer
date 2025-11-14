import React, { useEffect, useState, useCallback } from 'react';
import { View, Text, Button, ActivityIndicator, ScrollView, TextInput, TouchableOpacity, Platform } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { Picker } from '@react-native-picker/picker';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Window } from '../../components/Window';
import styles from '../../styles/CollectionsStyles';
import { getCollections } from '../../api/collections';
import { getDocuments, getDocument, updateDocument, deleteDocument } from '../../api/documents';
import { DeleteModal, DeleteResult } from '../../components/DeleteModal';
import { UpdateModal, UpdateResult } from '../../components/UpdateModal';
import * as DocumentPicker from 'expo-document-picker';

type Collection = {
  id: string;
  name: string;
};
type DocumentEntity = {
  id: string;
  originalFilename: string;
  mimeType: string;
  contentLen: number;
  createdTime: string;
  updatedTime: string;
  state: string;
};

export function Documents() {
  const [collections, setCollections] = useState<Collection[]>([]);
  const [selectedCollectionId, setSelectedCollectionId] = useState('');
  const [documents, setDocuments] = useState<DocumentEntity[]>([]);
  const [selectedDocumentId, setSelectedDocumentId] = useState('');
  const [document, setDocument] = useState<DocumentEntity | null>(null);
  const [loading, setLoading] = useState(false);
  const [docLoading, setDocLoading] = useState(false);
  const [file, setFile] = useState<File | null>(null);
  const [fileName, setFileName] = useState<string>('');
  const [updating, setUpdating] = useState(false);
  const [deleteModalVisible, setDeleteModalVisible] = useState(false);
  const [deleteResult, setDeleteResult] = useState<DeleteResult | null>(null);
  const [deleting, setDeleting] = useState(false);
  const [confirmModalVisible, setConfirmModalVisible] = useState(false);
  
  // Update modal state
  const [updateConfirmVisible, setUpdateConfirmVisible] = useState(false);
  const [updateResultVisible, setUpdateResultVisible] = useState(false);
  const [updateResult, setUpdateResult] = useState<UpdateResult | null>(null);

  useFocusEffect(
    useCallback(() => {
      setLoading(true);
      getCollections().then((res) => {
        setCollections(res.data || []);
        setLoading(false);
      });
    }, [])
  );

  useEffect(() => {
    if (selectedCollectionId) {
      setDocLoading(true);
      getDocuments(selectedCollectionId).then((res) => {
        setDocuments(res.data || []);
        setDocLoading(false);
      });
    } else {
      setDocuments([]);
      setSelectedDocumentId('');
      setDocument(null);
    }
  }, [selectedCollectionId]);

  useEffect(() => {
    if (selectedCollectionId && selectedDocumentId) {
      setDocLoading(true);
      getDocument(selectedCollectionId, selectedDocumentId).then((res) => {
        setDocument(res.data);
        setDocLoading(false);
      });
    } else {
      setDocument(null);
    }
  }, [selectedCollectionId, selectedDocumentId]);

  const handleUpdate = () => {
    if (!selectedCollectionId || !selectedDocumentId || !file) return;
    setUpdateConfirmVisible(true);
  };

  const handleConfirmUpdate = () => {
    if (!selectedCollectionId || !selectedDocumentId || !file) return;
    setUpdateConfirmVisible(false);
    setUpdateResultVisible(true);
    setUpdating(true);
    setUpdateResult(null);
    
    updateDocument(selectedCollectionId, selectedDocumentId, file)
      .then(() => {
        setUpdating(false);
        setUpdateResult({
          success: true,
          message: 'Document updated successfully',
        });
      })
      .catch((error) => {
        const errorMessage = error?.response?.data?.message || error?.message || 'Unknown error occurred';
        setUpdating(false);
        setUpdateResult({
          success: false,
          message: `Failed to update document: ${errorMessage}`,
        });
      });
  };

  const handleCancelUpdate = () => {
    setUpdateConfirmVisible(false);
  };

  const handleCloseUpdateModal = () => {
    setUpdateResultVisible(false);
    const wasSuccessful = updateResult?.success;
    if (wasSuccessful && selectedCollectionId && selectedDocumentId) {
      // Clear file selection
      setFile(null);
      setFileName('');
      // Reload document after successful update
      getDocument(selectedCollectionId, selectedDocumentId).then((res) => setDocument(res.data));
    }
    setUpdateResult(null);
  };

  const handleDelete = () => {
    if (!selectedCollectionId || !selectedDocumentId) return;
    setConfirmModalVisible(true);
  };

  const handleConfirmDelete = () => {
    setConfirmModalVisible(false);
    setDeleting(true);
    setDeleteModalVisible(true);
    setDeleteResult(null);
    deleteDocument(selectedCollectionId, selectedDocumentId)
      .then(() => {
        setDeleteResult({
          success: true,
          message: `Document deleted successfully.`
        });
        setDeleting(false);
        setSelectedDocumentId('');
        setDocument(null);
      })
      .catch((error) => {
        const errorMessage = error?.response?.data?.message || error?.message || 'Unknown error occurred';
        setDeleteResult({
          success: false,
          message: `Failed to delete document: ${errorMessage}`
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
    if (wasSuccessful && selectedCollectionId) {
      // Reload documents after successful delete
      getDocuments(selectedCollectionId).then((res) => setDocuments(res.data || []));
    }
  };

  // File picker implementation
  const handleFileSelect = async () => {
    try {
      const result = await DocumentPicker.getDocumentAsync({
        type: '*/*',
        copyToCacheDirectory: true,
      });

      if (!result.canceled && result.assets && result.assets.length > 0) {
        const selectedFile = result.assets[0];
        
        // For web, we can use the file directly
        if (Platform.OS === 'web') {
          // @ts-ignore - file property exists on web
          const webFile = selectedFile.file;
          if (webFile) {
            setFile(webFile as File);
            setFileName(selectedFile.name);
          }
        } else {
          // For native platforms, create a file-like object
          const fileObject = {
            uri: selectedFile.uri,
            name: selectedFile.name,
            type: selectedFile.mimeType || 'application/octet-stream',
          } as any;
          setFile(fileObject);
          setFileName(selectedFile.name);
        }
      }
    } catch (error) {
      console.error('Error picking document:', error);
      alert('Failed to select file');
    }
  };

  return (
    <SafeAreaView style={{ flex: 1 }}>
      <ScrollView>
        <Window>
          <View style={styles.container}>
            {/* Dropdowns for Collections and Documents */}
            <View style={styles.filtersContainer}>
              <View style={styles.dropdownContainer}>
                <Text style={styles.dropdownLabel}>Collection:</Text>
                {loading ? (
                  <ActivityIndicator />
                ) : (
                  <View style={styles.pickerWrapper}>
                    <Picker
                      selectedValue={selectedCollectionId}
                      onValueChange={(value) => setSelectedCollectionId(value)}
                      style={styles.picker}
                    >
                      <Picker.Item label="Select a collection..." value="" />
                      {collections.map((col) => (
                        <Picker.Item key={col.id} label={col.name} value={col.id} />
                      ))}
                    </Picker>
                  </View>
                )}
              </View>

              <View style={styles.dropdownContainer}>
                <Text style={styles.dropdownLabel}>Document:</Text>
                {docLoading ? (
                  <ActivityIndicator />
                ) : (
                  <View style={styles.pickerWrapper}>
                    <Picker
                      selectedValue={selectedDocumentId}
                      onValueChange={(value) => setSelectedDocumentId(value)}
                      style={styles.picker}
                      enabled={!!selectedCollectionId && documents.length > 0}
                    >
                      <Picker.Item 
                        label={!selectedCollectionId ? "Select a collection first..." : documents.length === 0 ? "No documents found" : "Select a document..."} 
                        value="" 
                      />
                      {documents.map((doc) => (
                        <Picker.Item key={doc.id} label={doc.originalFilename} value={doc.id} />
                      ))}
                    </Picker>
                  </View>
                )}
              </View>
            </View>

            {/* Document Form */}
            <View style={[styles.form, { flex: 1, justifyContent: 'space-between' }]}> 
              <View style={{ flexDirection: 'column', width: '100%' }}>
                {[ 
                  { label: 'Filename', value: document?.originalFilename || '' },
                  { label: 'Type', value: document?.mimeType || '' },
                  { label: 'Size', value: document?.contentLen?.toString() || '' },
                  { label: 'Created', value: document?.createdTime || '' },
                  { label: 'Updated', value: document?.updatedTime || '' },
                  { label: 'State', value: document?.state || '' },
                ].map((field) => (
                  <View key={field.label} style={{ flexDirection: 'row', alignItems: 'center', marginBottom: 8 }}>
                    <Text style={[styles.label, { minWidth: 90, marginRight: 12, marginBottom: 0, flexShrink: 1, flexGrow: 0 }]}>{field.label}</Text>
                    <TextInput style={[styles.input, { flex: 1, marginVertical: 0 }]} value={field.value} editable={false} />
                  </View>
                ))}
                <View style={{ flexDirection: 'row', alignItems: 'center', marginBottom: 8 }}>
                  <Text style={[styles.label, { minWidth: 90, marginRight: 12, marginBottom: 0, flexShrink: 1, flexGrow: 0 }]}>Select File</Text>
                  <View style={{ flex: 1, flexDirection: 'row', alignItems: 'center', gap: 8 }}>
                    <TouchableOpacity 
                      onPress={handleFileSelect} 
                      disabled={!selectedDocumentId}
                      style={{
                        backgroundColor: !selectedDocumentId ? '#ccc' : '#007bff',
                        paddingVertical: 8,
                        paddingHorizontal: 16,
                        borderRadius: 4,
                      }}
                    >
                      <Text style={{ color: 'white', fontWeight: '600' }}>Choose File</Text>
                    </TouchableOpacity>
                    {fileName && (
                      <Text style={{ flex: 1, fontSize: 14, color: '#666' }} numberOfLines={1}>
                        {fileName}
                      </Text>
                    )}
                  </View>
                </View>
              </View>
              <View style={[styles.buttonCol, { flexDirection: 'column', alignItems: 'flex-end', marginTop: 16 }]}> 
                <View style={styles.buttonWrapper}>
                  <Button title="Update" onPress={handleUpdate} disabled={updating || !file} />
                </View>
                <View style={styles.buttonWrapper}>
                  <Button title="Delete" onPress={handleDelete} color="red" disabled={updating || !selectedDocumentId} />
                </View>
              </View>
            </View>
            <DeleteModal
              confirmVisible={confirmModalVisible}
              itemName={document?.originalFilename || ''}
              onConfirm={handleConfirmDelete}
              onCancel={handleCancelDelete}
              resultVisible={deleteModalVisible}
              deleting={deleting}
              deleteResult={deleteResult}
              onClose={handleCloseDeleteModal}
              confirmMessage={`Are you sure you want to delete the document \"${document?.originalFilename || ''}\"?`}
              deletingMessage="Deleting document..."
            />
            <UpdateModal
              confirmVisible={updateConfirmVisible}
              itemName={document?.originalFilename || ''}
              onConfirm={handleConfirmUpdate}
              onCancel={handleCancelUpdate}
              resultVisible={updateResultVisible}
              updating={updating}
              updateResult={updateResult}
              onClose={handleCloseUpdateModal}
              confirmMessage={`Are you sure you want to update the document \"${document?.originalFilename || ''}\" with \"${fileName}\"?`}
              updatingMessage="Updating document..."
            />
          </View>
        </Window>
      </ScrollView>
    </SafeAreaView>
  );
}
