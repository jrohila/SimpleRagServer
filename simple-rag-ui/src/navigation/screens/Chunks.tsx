import React, { useEffect, useState, useCallback } from 'react';
import { View, SafeAreaView, ScrollView, ActivityIndicator, Text, TouchableOpacity, Alert, TextInput } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { Picker } from '@react-native-picker/picker';
import { Window } from '../../components/Window';
import styles from '../../styles/ChunksStyles';
import { useTranslation } from 'react-i18next';
import { useNavigation } from '@react-navigation/native';
import { getCollections } from '../../api/collections';
import { getDocuments } from '../../api/documents';
import { getChunks, updateChunk, deleteChunk } from '../../api/chunks';
import { DeleteModal, DeleteResult } from '../../components/DeleteModal';
import { UpdateModal, UpdateResult } from '../../components/UpdateModal';

type Chunk = {
  id: string;
  text: string;
  type: string;
  sectionTitle: string;
  pageNumber: number;
  language: string;
  documentName: string;
  created: string;
  modified: string;
};

export function Chunks() {
  const { t } = useTranslation();
  const navigation = useNavigation();

  React.useEffect(() => {
    try {
      navigation.setOptions({ title: t('navigation.chunks') as any });
    } catch (e) {
      // ignore when navigation not available
    }
  }, [t, navigation]);
  const [collections, setCollections] = useState<Array<{ id: string; name: string }>>([]);
  const [selectedCollectionId, setSelectedCollectionId] = useState('');
  const [documents, setDocuments] = useState<Array<{ id: string; originalFilename: string }>>([]);
  const [selectedDocumentId, setSelectedDocumentId] = useState('');
  const [loadingCollections, setLoadingCollections] = useState(false);
  const [loadingDocuments, setLoadingDocuments] = useState(false);
  const [chunks, setChunks] = useState<Chunk[]>([]);
  const [loadingChunks, setLoadingChunks] = useState(false);
  const [currentPage, setCurrentPage] = useState(0);
  const [pageSize] = useState(20);
  const [hasMorePages, setHasMorePages] = useState(false);
  const [deleteModalVisible, setDeleteModalVisible] = useState(false);
  const [deleteResult, setDeleteResult] = useState<DeleteResult | null>(null);
  const [deleting, setDeleting] = useState(false);
  const [confirmModalVisible, setConfirmModalVisible] = useState(false);
  const [chunkToDelete, setChunkToDelete] = useState<Chunk | null>(null);
  const [editedChunkTexts, setEditedChunkTexts] = useState<{ [id: string]: string }>({});
  
  // Update modal state
  const [updateConfirmVisible, setUpdateConfirmVisible] = useState(false);
  const [updateResultVisible, setUpdateResultVisible] = useState(false);
  const [updating, setUpdating] = useState(false);
  const [updateResult, setUpdateResult] = useState<UpdateResult | null>(null);
  const [chunkToUpdate, setChunkToUpdate] = useState<Chunk | null>(null);

  const handleChunkTextChange = (chunkId: string, newText: string) => {
    setEditedChunkTexts((prev) => ({ ...prev, [chunkId]: newText }));
  };

  const isChunkTextChanged = (chunk: Chunk) => {
    return (
      editedChunkTexts[chunk.id] !== undefined &&
      editedChunkTexts[chunk.id] !== chunk.text
    );
  };

  const handleUpdateChunk = (chunk: Chunk) => {
    if (!isChunkTextChanged(chunk)) return;
    setChunkToUpdate(chunk);
    setUpdateConfirmVisible(true);
  };

  const handleConfirmUpdate = () => {
    if (!chunkToUpdate) return;
    const newText = editedChunkTexts[chunkToUpdate.id];
    setUpdateConfirmVisible(false);
    setUpdateResultVisible(true);
    setUpdating(true);
    
    // Call updateChunk API
    updateChunk(selectedCollectionId, chunkToUpdate.id, { ...chunkToUpdate, text: newText })
      .then(() => {
        setUpdating(false);
        setUpdateResult({
          success: true,
          message: 'Chunk updated successfully',
        });
      })
      .catch((err) => {
        setUpdating(false);
        setUpdateResult({
          success: false,
          message: err.message || 'Failed to update chunk',
        });
      });
  };

  const handleCancelUpdate = () => {
    setUpdateConfirmVisible(false);
    setChunkToUpdate(null);
  };

  const handleCloseUpdateModal = () => {
    setUpdateResultVisible(false);
    const wasSuccessful = updateResult?.success;
    if (wasSuccessful) {
      // Reload the chunks list for the current page and filters
      const params: any = {
        collectionId: selectedCollectionId,
        page: currentPage,
        size: pageSize,
      };
      if (selectedDocumentId) {
        params.documentId = selectedDocumentId;
      }
      setLoadingChunks(true);
      getChunks(params)
        .then((res) => {
          const data = res.data;
          const chunksArray = Array.isArray(data) ? data : [];
          setChunks(chunksArray);
          setHasMorePages(chunksArray.length === pageSize);
          setLoadingChunks(false);
        })
        .catch((err) => {
          setChunks([]);
          setHasMorePages(false);
          setLoadingChunks(false);
        });
      setEditedChunkTexts((prev) => {
        const updated = { ...prev };
        delete updated[chunkToUpdate!.id];
        return updated;
      });
    }
    setUpdateResult(null);
    setChunkToUpdate(null);
  };

  const handleDeleteChunk = (chunk: Chunk) => {
    setChunkToDelete(chunk);
    setConfirmModalVisible(true);
  };

  const handleConfirmDelete = () => {
    if (!chunkToDelete) return;
    setConfirmModalVisible(false);
  };

  const handleCancelDelete = () => {
    setConfirmModalVisible(false);
    setChunkToDelete(null);
  };

  const handleCloseDeleteModal = () => {
    setDeleteModalVisible(false);
    const wasSuccessful = deleteResult?.success;
    if (wasSuccessful) {
      // Reload chunks for the current page and filters
      const params: any = {
        collectionId: selectedCollectionId,
        page: currentPage,
        size: pageSize,
      };
      if (selectedDocumentId) {
        params.documentId = selectedDocumentId;
      }
      setLoadingChunks(true);
      getChunks(params)
        .then((res) => {
          const data = res.data;
          const chunksArray = Array.isArray(data) ? data : [];
          setChunks(chunksArray);
          setHasMorePages(chunksArray.length === pageSize);
          setLoadingChunks(false);
        })
        .catch((err) => {
          setChunks([]);
          setHasMorePages(false);
          setLoadingChunks(false);
        });
    }
    setDeleteResult(null);
    setChunkToDelete(null);
  };

  useEffect(() => {
    if (selectedCollectionId) {
      const params: any = { 
        collectionId: selectedCollectionId, 
        page: currentPage, 
        size: pageSize 
      };
      
      // Add documentId to params only if a document is selected
      if (selectedDocumentId) {
        params.documentId = selectedDocumentId;
      }
      
      console.log('Fetching chunks for:', params);
      setLoadingChunks(true);
      getChunks(params)
        .then((res) => {
          console.log('Chunks response:', res.data);
          const data = res.data;
          const chunksArray = Array.isArray(data) ? data : [];
          setChunks(chunksArray);
          // If we got exactly pageSize items, there might be more pages
          setHasMorePages(chunksArray.length === pageSize);
          setLoadingChunks(false);
        })
        .catch((err) => {
          console.error('Error fetching chunks:', err);
          setChunks([]);
          setHasMorePages(false);
          setLoadingChunks(false);
        });
    } else {
      setChunks([]);
      setCurrentPage(0);
      setHasMorePages(false);
    }
  }, [selectedCollectionId, selectedDocumentId, currentPage, pageSize]);

  useFocusEffect(
    useCallback(() => {
      setLoadingCollections(true);
      getCollections().then((res) => {
        console.log('Collections response:', res.data);
        setCollections(res.data || []);
        setLoadingCollections(false);
      });
    }, [])
  );

  useEffect(() => {
    if (selectedCollectionId) {
      console.log('Fetching documents for collection:', selectedCollectionId);
      setLoadingDocuments(true);
      getDocuments(selectedCollectionId).then((res) => {
        console.log('Documents response:', res.data);
        setDocuments(res.data || []);
        setLoadingDocuments(false);
      });
    } else {
      setDocuments([]);
      setSelectedDocumentId('');
    }
    // Reset page when collection changes
    setCurrentPage(0);
  }, [selectedCollectionId]);

  return (
    <SafeAreaView style={styles.safeArea}>
      <ScrollView>
        <Window>
          <View style={styles.container}>
            {/* Dropdowns for Collections and Documents */}
            <View style={styles.filtersContainer}>
              <View style={styles.dropdownContainer}>
                <Text style={styles.dropdownLabel}>{t('chunks.collection')}</Text>
                {loadingCollections ? (
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
                <Text style={styles.dropdownLabel}>{t('chunks.document')}</Text>
                {loadingDocuments ? (
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
                        label={!selectedCollectionId ? t('chunks.selectCollectionFirst') : documents.length === 0 ? t('chunks.noDocuments') : t('chunks.allDocuments')} 
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

            {/* Chunks Table */}
            <View style={styles.chunksSection}>
              <View style={styles.header}>
                {/* Pagination controls */}
                  <View style={styles.paginationContainer}>
                  <TouchableOpacity
                    onPress={() => setCurrentPage(currentPage - 1)}
                    disabled={currentPage === 0}
                    style={[
                      styles.paginationButton,
                      currentPage === 0 ? styles.paginationButtonDisabled : styles.paginationButtonActive,
                    ]}
                  >
                    <Text style={styles.paginationButtonText}>{t('chunks.pagination.back')}</Text>
                  </TouchableOpacity>
                  <Text style={styles.paginationText}>{t('chunks.pagination.page', { page: currentPage + 1 })}</Text>
                  <TouchableOpacity
                    onPress={() => setCurrentPage(currentPage + 1)}
                    disabled={!hasMorePages}
                    style={[
                      styles.paginationButton,
                      !hasMorePages ? styles.paginationButtonDisabled : styles.paginationButtonActive,
                    ]}
                  >
                    <Text style={styles.paginationButtonText}>{t('chunks.pagination.next')}</Text>
                  </TouchableOpacity>
                </View>
              </View>
              {loadingChunks ? (
                <ActivityIndicator />
              ) : (
                <View style={styles.chunksContainer}>
                      {chunks.length === 0 ? (
                    // Empty disabled row when no chunks
                    <View style={styles.disabledRow}>
                      {/* First row: Text in textarea */}
                      <View style={styles.chunkTextRow}>
                            <Text style={styles.chunkTextLabel}>{t('chunks.textLabel')}</Text>
                        <View style={[styles.chunkTextArea, styles.chunkTextAreaDisabled]}>
                           <Text style={[styles.chunkText, styles.chunkTextDisabled]}>
                           {!selectedCollectionId ? t('chunks.messages.selectCollectionAndDocument') : 
                            !selectedDocumentId ? t('chunks.messages.selectDocument') : 
                            t('chunks.messages.noChunks')}
                          </Text>
                        </View>
                      </View>
                      {/* Second row: All other fields */}
                      <View style={styles.chunkMetadataRow}>
                        <View style={styles.chunkMetadataContainer}>
                          <View style={styles.chunkMetadataField}>
                            <Text style={[styles.chunkMetadataLabel, styles.chunkMetadataLabelDisabled]}>{t('chunks.table.type')}: </Text>
                            <Text style={[styles.chunkMetadataValue, styles.chunkMetadataValueDisabled]}>-</Text>
                          </View>
                          <View style={styles.chunkMetadataField}>
                            <Text style={[styles.chunkMetadataLabel, styles.chunkMetadataLabelDisabled]}>{t('chunks.table.section')}: </Text>
                            <Text style={[styles.chunkMetadataValue, styles.chunkMetadataValueDisabled]}>-</Text>
                          </View>
                          <View style={styles.chunkMetadataField}>
                            <Text style={[styles.chunkMetadataLabel, styles.chunkMetadataLabelDisabled]}>{t('chunks.table.page')}: </Text>
                            <Text style={[styles.chunkMetadataValue, styles.chunkMetadataValueDisabled]}>-</Text>
                          </View>
                          <View style={styles.chunkMetadataField}>
                            <Text style={[styles.chunkMetadataLabel, styles.chunkMetadataLabelDisabled]}>{t('chunks.table.language')}: </Text>
                            <Text style={[styles.chunkMetadataValue, styles.chunkMetadataValueDisabled]}>-</Text>
                          </View>
                          <View style={styles.chunkMetadataField}>
                            <Text style={[styles.chunkMetadataLabel, styles.chunkMetadataLabelDisabled]}>{t('chunks.table.document')}: </Text>
                            <Text style={[styles.chunkMetadataValue, styles.chunkMetadataValueDisabled]}>-</Text>
                          </View>
                          <View style={styles.chunkMetadataField}>
                            <Text style={[styles.chunkMetadataLabel, styles.chunkMetadataLabelDisabled]}>{t('chunks.table.created')}: </Text>
                            <Text style={[styles.chunkMetadataValue, styles.chunkMetadataValueDisabled]}>-</Text>
                          </View>
                          <View style={styles.chunkMetadataField}>
                            <Text style={[styles.chunkMetadataLabel, styles.chunkMetadataLabelDisabled]}>{t('chunks.table.modified')}: </Text>
                            <Text style={[styles.chunkMetadataValue, styles.chunkMetadataValueDisabled]}>-</Text>
                          </View>
                        </View>
                        {/* Action buttons - disabled */}
                        <View style={styles.chunkActionsContainer}>
                          <TouchableOpacity
                            disabled
                            style={[styles.chunkUpdateButton, styles.chunkButtonDisabled]}
                          >
                            <Text style={styles.chunkButtonText}>{t('actions.update')}</Text>
                          </TouchableOpacity>
                          <TouchableOpacity
                            disabled
                            style={[styles.chunkDeleteButton, styles.chunkButtonDisabled]}
                          >
                            <Text style={styles.chunkButtonText}>{t('actions.delete')}</Text>
                          </TouchableOpacity>
                        </View>
                      </View>
                    </View>
                  ) : (
                    chunks.map((chunk, index) => (
                    <View key={chunk.id} style={[styles.chunkRow, index === chunks.length - 1 && styles.chunkRowLast]}>
                      {/* First row: Text in textarea */}
                      <View style={styles.chunkTextRow}>
                        <Text style={styles.chunkTextLabel}>{t('chunks.textLabel')}</Text>
                        <View style={styles.chunkTextArea}>
                          <TextInput
                            style={styles.chunkText}
                            multiline
                            numberOfLines={5}
                            value={
                              editedChunkTexts[chunk.id] !== undefined
                                ? editedChunkTexts[chunk.id]
                                : chunk.text
                            }
                            onChangeText={(text) => handleChunkTextChange(chunk.id, text)}
                          />
                        </View>
                      </View>
                      {/* Second row: All other fields */}
                      <View style={styles.chunkMetadataRow}>
                        <View style={styles.chunkMetadataContainer}>
                          <View style={styles.chunkMetadataField}>
                            <Text style={styles.chunkMetadataLabel}>{t('chunks.table.type')}: </Text>
                            <Text style={styles.chunkMetadataValue}>{chunk.type}</Text>
                          </View>
                          <View style={styles.chunkMetadataField}>
                            <Text style={styles.chunkMetadataLabel}>{t('chunks.table.section')}: </Text>
                            <Text style={styles.chunkMetadataValue}>{chunk.sectionTitle}</Text>
                          </View>
                          <View style={styles.chunkMetadataField}>
                            <Text style={styles.chunkMetadataLabel}>{t('chunks.table.page')}: </Text>
                            <Text style={styles.chunkMetadataValue}>{chunk.pageNumber}</Text>
                          </View>
                          <View style={styles.chunkMetadataField}>
                            <Text style={styles.chunkMetadataLabel}>{t('chunks.table.language')}: </Text>
                            <Text style={styles.chunkMetadataValue}>{chunk.language}</Text>
                          </View>
                          <View style={styles.chunkMetadataField}>
                            <Text style={styles.chunkMetadataLabel}>{t('chunks.table.document')}: </Text>
                            <Text style={styles.chunkMetadataValue}>{chunk.documentName}</Text>
                          </View>
                          <View style={styles.chunkMetadataField}>
                            <Text style={styles.chunkMetadataLabel}>{t('chunks.table.created')}: </Text>
                            <Text style={styles.chunkMetadataValue}>{chunk.created}</Text>
                          </View>
                          <View style={styles.chunkMetadataField}>
                            <Text style={styles.chunkMetadataLabel}>{t('chunks.table.modified')}: </Text>
                            <Text style={styles.chunkMetadataValue}>{chunk.modified}</Text>
                          </View>
                        </View>
                        {/* Action buttons */}
                        <View style={styles.chunkActionsContainer}>
                          <TouchableOpacity
                            onPress={() => handleUpdateChunk(chunk)}
                            style={[
                              styles.chunkUpdateButton,
                              !isChunkTextChanged(chunk) && styles.chunkButtonDisabled,
                            ]}
                            disabled={!isChunkTextChanged(chunk)}
                          >
                            <Text style={styles.chunkButtonText}>{t('actions.update')}</Text>
                          </TouchableOpacity>
                          <TouchableOpacity
                            onPress={() => handleDeleteChunk(chunk)}
                            style={styles.chunkDeleteButton}
                          >
                            <Text style={styles.chunkButtonText}>{t('actions.delete')}</Text>
                          </TouchableOpacity>
                        </View>
                      </View>
                    </View>
                  ))
                  )}
                </View>
              )}
              {/* Pagination controls at bottom */}
              {!loadingChunks && chunks.length > 0 && (
                <View style={styles.bottomPaginationContainer}>
                  <TouchableOpacity
                    onPress={() => setCurrentPage(currentPage - 1)}
                    disabled={currentPage === 0}
                    style={[
                      styles.paginationButton,
                      currentPage === 0 ? styles.paginationButtonDisabled : styles.paginationButtonActive
                    ]}
                  >
                    <Text style={styles.paginationButtonText}>{t('chunks.pagination.back')}</Text>
                  </TouchableOpacity>
                  <Text style={styles.paginationText}>{t('chunks.pagination.page', { page: currentPage + 1 })}</Text>
                  <TouchableOpacity
                    onPress={() => setCurrentPage(currentPage + 1)}
                    disabled={!hasMorePages}
                    style={[
                      styles.paginationButton,
                      !hasMorePages ? styles.paginationButtonDisabled : styles.paginationButtonActive
                    ]}
                  >
                    <Text style={styles.paginationButtonText}>{t('chunks.pagination.next')}</Text>
                  </TouchableOpacity>
                </View>
              )}
            </View>
          </View>
        </Window>
      </ScrollView>
      <DeleteModal
        confirmVisible={confirmModalVisible}
        itemName={chunkToDelete?.id || ''}
        onConfirm={handleConfirmDelete}
        onCancel={handleCancelDelete}
        resultVisible={deleteModalVisible}
        deleting={deleting}
        deleteResult={deleteResult}
        onClose={handleCloseDeleteModal}
        confirmMessage={t('messages.chunkConfirmDelete')}
        deletingMessage={t('messages.deletingChunk')}
      />
      <UpdateModal
        confirmVisible={updateConfirmVisible}
        itemName={chunkToUpdate?.id || ''}
        onConfirm={handleConfirmUpdate}
        onCancel={handleCancelUpdate}
        resultVisible={updateResultVisible}
        updating={updating}
        updateResult={updateResult}
        onClose={handleCloseUpdateModal}
        confirmMessage={t('messages.chunkConfirmUpdate')}
        updatingMessage={t('messages.updatingChunk')}
      />
    </SafeAreaView>
  );
}