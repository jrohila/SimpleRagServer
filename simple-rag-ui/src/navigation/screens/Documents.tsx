import React, { useEffect, useState, useCallback } from 'react';
import { View, Text, Button, ActivityIndicator, ScrollView, TextInput, TouchableOpacity, Platform } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { Picker } from '@react-native-picker/picker';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Window } from '../../components/Window';
import styles from '../../styles/CollectionsStyles';
import { useTranslation } from 'react-i18next';
import { useNavigation } from '@react-navigation/native';
import { getCollections } from '../../api/collections';
import { getDocuments, getDocument, updateDocument, deleteDocument } from '../../api/documents';
import { DeleteModal, DeleteResult } from '../../components/DeleteModal';
import { UpdateModal, UpdateResult } from '../../components/UpdateModal';
// Use a small web file picker instead of expo-document-picker

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
  const { t } = useTranslation();
  const navigation = useNavigation();

  React.useEffect(() => {
    try {
      navigation.setOptions({ title: t('navigation.documents') as any });
    } catch (e) {
      // ignore when navigation not available
    }
  }, [t, navigation]);
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
          message: t('messages.documentUpdateSuccess'),
        });
      })
      .catch((error) => {
        const errorMessage = error?.response?.data?.message || error?.message || 'Unknown error occurred';
        setUpdating(false);
        setUpdateResult({
          success: false,
          message: t('messages.documentUpdateFailed', { error: errorMessage }),
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
          message: t('messages.documentDeleted'),
        });
        setDeleting(false);
        setSelectedDocumentId('');
        setDocument(null);
      })
      .catch((error) => {
        const errorMessage = error?.response?.data?.message || error?.message || 'Unknown error occurred';
        setDeleteResult({
          success: false,
          message: t('messages.documentDeleteFailed', { error: errorMessage }),
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
      if (Platform.OS === 'web') {
        // create an invisible file input and click it
        const input = document.createElement('input');
        input.type = 'file';
        input.accept = '*/*';
        input.onchange = (e: any) => {
          const f = e.target.files && e.target.files[0];
          if (f) {
            setFile(f as File);
            setFileName(f.name);
          }
        };
        input.click();
      } else {
        // Native platforms: document picker not available in this build.
        alert(t('messages.filePickNotSupported'));
      }
    } catch (error) {
      console.error('Error picking document:', error);
      alert(t('messages.filePickFailed'));
    }
  };

  return (
    <SafeAreaView style={styles.safeArea}>
      <ScrollView>
        <Window>
          <View style={styles.container}>
            {/* Dropdowns for Collections and Documents */}
            <View style={styles.filtersContainer}>
              <View style={styles.dropdownContainer}>
                <Text style={styles.dropdownLabel}>{t('documents.collection')}</Text>
                {loading ? (
                  <ActivityIndicator />
                ) : (
                  <View style={styles.pickerWrapper}>
                    <Picker
                      selectedValue={selectedCollectionId}
                      onValueChange={(value) => setSelectedCollectionId(value)}
                      style={styles.picker}
                    >
                      <Picker.Item label={t('basic.selectCollection')} value="" />
                      {collections.map((col) => (
                        <Picker.Item key={col.id} label={col.name} value={col.id} />
                      ))}
                    </Picker>
                  </View>
                )}
              </View>

              <View style={styles.dropdownContainer}>
                <Text style={styles.dropdownLabel}>{t('documents.label')}</Text>
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
                        label={!selectedCollectionId ? t('documents.selectCollectionFirst') : documents.length === 0 ? t('documents.noDocuments') : t('documents.selectDocument')} 
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
            <View style={[styles.form, styles.formFlex]}> 
              <View style={styles.columnFullWidth}>
                {[ 
                  { label: t('documents.table.filename'), value: document?.originalFilename || '' },
                  { label: t('documents.table.type'), value: document?.mimeType || '' },
                  { label: t('documents.table.size'), value: document?.contentLen?.toString() || '' },
                  { label: t('documents.table.created'), value: document?.createdTime || '' },
                  { label: t('documents.table.updated'), value: document?.updatedTime || '' },
                  { label: t('documents.table.state'), value: document?.state || '' },
                ].map((field) => (
                  <View key={String(field.label)} style={styles.documentFieldRow}>
                    <Text style={[styles.label, styles.fieldLabel]}>{field.label}</Text>
                    <TextInput style={[styles.input, styles.documentInputFlex]} value={field.value} editable={false} />
                  </View>
                ))}
                <View style={styles.documentFieldRow}>
                  <Text style={[styles.label, styles.fieldLabel]}>{t('documents.selectFile')}</Text>
                  <View style={styles.fileRow}>
                    <TouchableOpacity 
                      onPress={handleFileSelect} 
                      disabled={!selectedDocumentId}
                      style={[styles.fileButton, !selectedDocumentId ? styles.fileButtonDisabled : styles.fileButtonPrimary]}
                    >
                      <Text style={styles.fileButtonText}>{t('documents.chooseFile')}</Text>
                    </TouchableOpacity>
                    {fileName && (
                      <Text style={styles.fileNameText} numberOfLines={1}>
                        {fileName}
                      </Text>
                    )}
                  </View>
                </View>
              </View>
              <View style={[styles.buttonCol, { flexDirection: 'column', alignItems: 'flex-end', marginTop: 16 }]}> 
                <View style={styles.buttonWrapper}>
                  <Button title={t('actions.update')} onPress={handleUpdate} disabled={updating || !file} />
                </View>
                <View style={styles.buttonWrapper}>
                  <Button title={t('actions.delete')} onPress={handleDelete} color="red" disabled={updating || !selectedDocumentId} />
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
              confirmMessage={t('messages.documentConfirmDelete', { name: document?.originalFilename || '' })}
              deletingMessage={t('messages.deletingDocument')}
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
              confirmMessage={t('messages.documentConfirmUpdate', { name: document?.originalFilename || '', file: fileName })}
              updatingMessage={t('messages.updatingDocument')}
            />
          </View>
        </Window>
      </ScrollView>
    </SafeAreaView>
  );
}
