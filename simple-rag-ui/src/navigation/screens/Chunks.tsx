import React, { useEffect, useState } from 'react';
import { View, SafeAreaView, ScrollView, ActivityIndicator, Text, TouchableOpacity, Alert } from 'react-native';
import { Picker } from '@react-native-picker/picker';
import { Window } from '../../components/Window';
import styles from '../../styles/ChunksStyles';
import { getCollections } from '../../api/collections';
import { getDocuments } from '../../api/documents';
import { getChunks, updateChunk, deleteChunk } from '../../api/chunks';
import { DeleteModal, DeleteResult } from '../../components/DeleteModal';

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

  const handleUpdateChunk = (chunk: Chunk) => {
    // For now, just log - you can add an edit modal later
    console.log('Update chunk:', chunk);
    Alert.alert('Update Chunk', `Update functionality for chunk ${chunk.id} - implement edit form here`);
  };

  const handleDeleteChunk = (chunk: Chunk) => {
    setChunkToDelete(chunk);
    setConfirmModalVisible(true);
  };

  const handleConfirmDelete = () => {
    if (!chunkToDelete) return;
    setConfirmModalVisible(false);
    setDeleting(true);
    setDeleteModalVisible(true);
    setDeleteResult(null);
    // Use the correct endpoint: /chunks/delete/{id}
    deleteChunk(selectedCollectionId, chunkToDelete.id)
      .then(() => {
        setDeleteResult({
          success: true,
          message: `Chunk deleted successfully.`
        });
        setDeleting(false);
      })
      .catch((error) => {
        const errorMessage = error?.response?.data?.message || error?.message || 'Unknown error occurred';
        setDeleteResult({
          success: false,
          message: `Failed to delete chunk: ${errorMessage}`
        });
        setDeleting(false);
      });
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

  useEffect(() => {
    setLoadingCollections(true);
    getCollections().then((res) => {
      console.log('Collections response:', res.data);
      setCollections(res.data || []);
      setLoadingCollections(false);
    });
  }, []);

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
    <SafeAreaView style={{ flex: 1 }}>
      <ScrollView>
        <Window>
          <View style={styles.container}>
            {/* Dropdowns for Collections and Documents */}
            <View style={styles.filtersContainer}>
              <View style={styles.dropdownContainer}>
                <Text style={styles.dropdownLabel}>Collection:</Text>
                {loadingCollections ? (
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
                        label={!selectedCollectionId ? "Select a collection first..." : documents.length === 0 ? "No documents found" : "All documents"} 
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
                    <Text style={styles.paginationButtonText}>← Back</Text>
                  </TouchableOpacity>
                  <Text style={styles.paginationText}>Page {currentPage + 1}</Text>
                  <TouchableOpacity
                    onPress={() => setCurrentPage(currentPage + 1)}
                    disabled={!hasMorePages}
                    style={[
                      styles.paginationButton,
                      !hasMorePages ? styles.paginationButtonDisabled : styles.paginationButtonActive,
                    ]}
                  >
                    <Text style={styles.paginationButtonText}>Next →</Text>
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
                        <Text style={styles.chunkTextLabel}>Text:</Text>
                        <View style={[styles.chunkTextArea, styles.chunkTextAreaDisabled]}>
                          <Text style={[styles.chunkText, styles.chunkTextDisabled]}>
                            {!selectedCollectionId ? 'Select a collection and document to view chunks' : 
                             !selectedDocumentId ? 'Select a document to view chunks' : 
                             'No chunks found'}
                          </Text>
                        </View>
                      </View>
                      {/* Second row: All other fields */}
                      <View style={styles.chunkMetadataRow}>
                        <View style={styles.chunkMetadataContainer}>
                          <View style={styles.chunkMetadataField}>
                            <Text style={[styles.chunkMetadataLabel, styles.chunkMetadataLabelDisabled]}>Type: </Text>
                            <Text style={[styles.chunkMetadataValue, styles.chunkMetadataValueDisabled]}>-</Text>
                          </View>
                          <View style={styles.chunkMetadataField}>
                            <Text style={[styles.chunkMetadataLabel, styles.chunkMetadataLabelDisabled]}>Section: </Text>
                            <Text style={[styles.chunkMetadataValue, styles.chunkMetadataValueDisabled]}>-</Text>
                          </View>
                          <View style={styles.chunkMetadataField}>
                            <Text style={[styles.chunkMetadataLabel, styles.chunkMetadataLabelDisabled]}>Page: </Text>
                            <Text style={[styles.chunkMetadataValue, styles.chunkMetadataValueDisabled]}>-</Text>
                          </View>
                          <View style={styles.chunkMetadataField}>
                            <Text style={[styles.chunkMetadataLabel, styles.chunkMetadataLabelDisabled]}>Language: </Text>
                            <Text style={[styles.chunkMetadataValue, styles.chunkMetadataValueDisabled]}>-</Text>
                          </View>
                          <View style={styles.chunkMetadataField}>
                            <Text style={[styles.chunkMetadataLabel, styles.chunkMetadataLabelDisabled]}>Document: </Text>
                            <Text style={[styles.chunkMetadataValue, styles.chunkMetadataValueDisabled]}>-</Text>
                          </View>
                          <View style={styles.chunkMetadataField}>
                            <Text style={[styles.chunkMetadataLabel, styles.chunkMetadataLabelDisabled]}>Created: </Text>
                            <Text style={[styles.chunkMetadataValue, styles.chunkMetadataValueDisabled]}>-</Text>
                          </View>
                          <View style={styles.chunkMetadataField}>
                            <Text style={[styles.chunkMetadataLabel, styles.chunkMetadataLabelDisabled]}>Modified: </Text>
                            <Text style={[styles.chunkMetadataValue, styles.chunkMetadataValueDisabled]}>-</Text>
                          </View>
                        </View>
                        {/* Action buttons - disabled */}
                        <View style={styles.chunkActionsContainer}>
                          <TouchableOpacity
                            disabled
                            style={[styles.chunkUpdateButton, styles.chunkButtonDisabled]}
                          >
                            <Text style={styles.chunkButtonText}>Update</Text>
                          </TouchableOpacity>
                          <TouchableOpacity
                            disabled
                            style={[styles.chunkDeleteButton, styles.chunkButtonDisabled]}
                          >
                            <Text style={styles.chunkButtonText}>Delete</Text>
                          </TouchableOpacity>
                        </View>
                      </View>
                    </View>
                  ) : (
                    chunks.map((chunk, index) => (
                    <View key={chunk.id} style={[styles.chunkRow, index === chunks.length - 1 && styles.chunkRowLast]}>
                      {/* First row: Text in textarea */}
                      <View style={styles.chunkTextRow}>
                        <Text style={styles.chunkTextLabel}>Text:</Text>
                        <View style={styles.chunkTextArea}>
                          <Text style={styles.chunkText}>{chunk.text}</Text>
                        </View>
                      </View>
                      {/* Second row: All other fields */}
                      <View style={styles.chunkMetadataRow}>
                        <View style={styles.chunkMetadataContainer}>
                          <View style={styles.chunkMetadataField}>
                            <Text style={styles.chunkMetadataLabel}>Type: </Text>
                            <Text style={styles.chunkMetadataValue}>{chunk.type}</Text>
                          </View>
                          <View style={styles.chunkMetadataField}>
                            <Text style={styles.chunkMetadataLabel}>Section: </Text>
                            <Text style={styles.chunkMetadataValue}>{chunk.sectionTitle}</Text>
                          </View>
                          <View style={styles.chunkMetadataField}>
                            <Text style={styles.chunkMetadataLabel}>Page: </Text>
                            <Text style={styles.chunkMetadataValue}>{chunk.pageNumber}</Text>
                          </View>
                          <View style={styles.chunkMetadataField}>
                            <Text style={styles.chunkMetadataLabel}>Language: </Text>
                            <Text style={styles.chunkMetadataValue}>{chunk.language}</Text>
                          </View>
                          <View style={styles.chunkMetadataField}>
                            <Text style={styles.chunkMetadataLabel}>Document: </Text>
                            <Text style={styles.chunkMetadataValue}>{chunk.documentName}</Text>
                          </View>
                          <View style={styles.chunkMetadataField}>
                            <Text style={styles.chunkMetadataLabel}>Created: </Text>
                            <Text style={styles.chunkMetadataValue}>{chunk.created}</Text>
                          </View>
                          <View style={styles.chunkMetadataField}>
                            <Text style={styles.chunkMetadataLabel}>Modified: </Text>
                            <Text style={styles.chunkMetadataValue}>{chunk.modified}</Text>
                          </View>
                        </View>
                        {/* Action buttons */}
                        <View style={styles.chunkActionsContainer}>
                          <TouchableOpacity
                            onPress={() => handleUpdateChunk(chunk)}
                            style={styles.chunkUpdateButton}
                          >
                            <Text style={styles.chunkButtonText}>Update</Text>
                          </TouchableOpacity>
                          <TouchableOpacity
                            onPress={() => handleDeleteChunk(chunk)}
                            style={styles.chunkDeleteButton}
                          >
                            <Text style={styles.chunkButtonText}>Delete</Text>
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
                    <Text style={styles.paginationButtonText}>← Back</Text>
                  </TouchableOpacity>
                  <Text style={styles.paginationText}>Page {currentPage + 1}</Text>
                  <TouchableOpacity
                    onPress={() => setCurrentPage(currentPage + 1)}
                    disabled={!hasMorePages}
                    style={[
                      styles.paginationButton,
                      !hasMorePages ? styles.paginationButtonDisabled : styles.paginationButtonActive
                    ]}
                  >
                    <Text style={styles.paginationButtonText}>Next →</Text>
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
        confirmMessage={`Are you sure you want to delete this chunk?`}
        deletingMessage="Deleting chunk..."
      />
    </SafeAreaView>
  );
}