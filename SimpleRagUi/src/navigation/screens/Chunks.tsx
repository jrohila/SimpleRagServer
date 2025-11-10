


import React, { useEffect, useState } from 'react';
import { View, SafeAreaView, ScrollView, ActivityIndicator, Text, TouchableOpacity, Alert } from 'react-native';
import { Window } from '../../components/Window';
import SidebarPicker from '../../components/SidebarPicker';
import styles from '../../styles/CollectionsStyles';
import { getCollections } from '../../api/collections';
import { getDocuments } from '../../api/documents';
import { getChunks, updateChunk, deleteChunk } from '../../api/chunks';

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

  const handleUpdateChunk = (chunk: Chunk) => {
    // For now, just log - you can add an edit modal later
    console.log('Update chunk:', chunk);
    Alert.alert('Update Chunk', `Update functionality for chunk ${chunk.id} - implement edit form here`);
  };

  const handleDeleteChunk = (chunk: Chunk) => {
    Alert.alert(
      'Delete Chunk',
      'Are you sure you want to delete this chunk?',
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Delete',
          style: 'destructive',
          onPress: () => {
            deleteChunk(selectedCollectionId, chunk.id)
              .then(() => {
                // Refresh chunks list
                setChunks(chunks.filter((c) => c.id !== chunk.id));
                Alert.alert('Success', 'Chunk deleted successfully');
              })
              .catch((err) => {
                console.error('Error deleting chunk:', err);
                Alert.alert('Error', 'Failed to delete chunk');
              });
          },
        },
      ]
    );
  };

  useEffect(() => {
    if (selectedCollectionId && selectedDocumentId) {
      console.log('Fetching chunks for:', { collectionId: selectedCollectionId, documentId: selectedDocumentId, page: currentPage, size: pageSize });
      setLoadingChunks(true);
      getChunks({ collectionId: selectedCollectionId, documentId: selectedDocumentId, page: currentPage, size: pageSize })
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
  }, [selectedCollectionId]);  return (
    <SafeAreaView style={{ flex: 1 }}>
      <ScrollView>
        <Window>
          <View style={styles.row}>
            {/* Collections Sidebar */}
            <View style={styles.sidebar}>
              {loadingCollections ? (
                <ActivityIndicator />
              ) : collections.length === 0 ? (
                <Text style={{ padding: 8, color: '#888' }}>No collections found.</Text>
              ) : (
                <SidebarPicker
                  items={collections}
                  getItemLabel={(col) => col.name}
                  getItemKey={(col) => col.id}
                  selectedItem={collections.find((c) => c.id === selectedCollectionId) || null}
                  onSelect={(col) => setSelectedCollectionId(col.id)}
                  containerStyle={styles.sidebarContent}
                  itemStyle={styles.chatItem}
                  selectedItemStyle={styles.chatItemSelected}
                  textStyle={styles.chatItemText}
                  selectedTextStyle={styles.chatItemText}
                  emptyMessage="No collections found."
                />
              )}
            </View>
            {/* Documents Sidebar */}
            <View style={styles.sidebar}>
              {loadingDocuments ? (
                <View style={styles.sidebarContent}>
                  <ActivityIndicator />
                </View>
              ) : (
                <SidebarPicker
                  items={documents}
                  getItemLabel={(doc) => doc.originalFilename}
                  getItemKey={(doc) => doc.id}
                  selectedItem={documents.find((d) => d.id === selectedDocumentId) || null}
                  onSelect={(doc) => setSelectedDocumentId(doc.id)}
                  containerStyle={styles.sidebarContent}
                  itemStyle={styles.chatItem}
                  selectedItemStyle={styles.chatItemSelected}
                  textStyle={styles.chatItemText}
                  selectedTextStyle={styles.chatItemText}
                  emptyMessage={!selectedCollectionId ? 'Select a collection first.' : 'No documents found.'}
                />
              )}
            </View>
            {/* Chunks Table */}
            <View style={{ flex: 1, padding: 16 }}>
              <View style={{ flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', marginBottom: 8 }}>
                <Text style={{ fontWeight: 'bold', fontSize: 16 }}>Chunks</Text>
                {/* Pagination controls */}
                <View style={{ flexDirection: 'row', alignItems: 'center', gap: 8 }}>
                  <TouchableOpacity
                    onPress={() => setCurrentPage(currentPage - 1)}
                    disabled={currentPage === 0}
                    style={{
                      backgroundColor: currentPage === 0 ? '#ccc' : '#007bff',
                      paddingHorizontal: 12,
                      paddingVertical: 6,
                      borderRadius: 4,
                      opacity: currentPage === 0 ? 0.5 : 1,
                    }}
                  >
                    <Text style={{ color: '#fff', fontSize: 12, fontWeight: 'bold' }}>← Back</Text>
                  </TouchableOpacity>
                  <Text style={{ fontSize: 12, color: '#666' }}>Page {currentPage + 1}</Text>
                  <TouchableOpacity
                    onPress={() => setCurrentPage(currentPage + 1)}
                    disabled={!hasMorePages}
                    style={{
                      backgroundColor: !hasMorePages ? '#ccc' : '#007bff',
                      paddingHorizontal: 12,
                      paddingVertical: 6,
                      borderRadius: 4,
                      opacity: !hasMorePages ? 0.5 : 1,
                    }}
                  >
                    <Text style={{ color: '#fff', fontSize: 12, fontWeight: 'bold' }}>Next →</Text>
                  </TouchableOpacity>
                </View>
              </View>
              {loadingChunks ? (
                <ActivityIndicator />
              ) : (
                <View style={{ borderWidth: 1, borderColor: '#ccc', borderRadius: 6, overflow: 'hidden' }}>
                  {chunks.length === 0 ? (
                    // Empty disabled row when no chunks
                    <View style={{ opacity: 0.5 }}>
                      {/* First row: Text in textarea */}
                      <View style={{ padding: 8, backgroundColor: '#fafafa' }}>
                        <Text style={{ fontWeight: 'bold', marginBottom: 4, fontSize: 12, color: '#666' }}>Text:</Text>
                        <View
                          style={{
                            borderWidth: 1,
                            borderColor: '#ddd',
                            borderRadius: 4,
                            padding: 8,
                            minHeight: 80,
                            backgroundColor: '#f5f5f5',
                          }}
                        >
                          <Text style={{ fontFamily: 'monospace', fontSize: 12, color: '#999' }}>
                            {!selectedCollectionId ? 'Select a collection and document to view chunks' : 
                             !selectedDocumentId ? 'Select a document to view chunks' : 
                             'No chunks found'}
                          </Text>
                        </View>
                      </View>
                      {/* Second row: All other fields */}
                      <View style={{ flexDirection: 'row', padding: 8, backgroundColor: '#f9f9f9', flexWrap: 'wrap', alignItems: 'center', justifyContent: 'space-between' }}>
                        <View style={{ flexDirection: 'row', flexWrap: 'wrap', flex: 1 }}>
                          <View style={{ flexDirection: 'row', marginRight: 16, marginBottom: 4 }}>
                            <Text style={{ fontWeight: 'bold', fontSize: 11, color: '#999' }}>Type: </Text>
                            <Text style={{ fontSize: 11, color: '#999' }}>-</Text>
                          </View>
                          <View style={{ flexDirection: 'row', marginRight: 16, marginBottom: 4 }}>
                            <Text style={{ fontWeight: 'bold', fontSize: 11, color: '#999' }}>Section: </Text>
                            <Text style={{ fontSize: 11, color: '#999' }}>-</Text>
                          </View>
                          <View style={{ flexDirection: 'row', marginRight: 16, marginBottom: 4 }}>
                            <Text style={{ fontWeight: 'bold', fontSize: 11, color: '#999' }}>Page: </Text>
                            <Text style={{ fontSize: 11, color: '#999' }}>-</Text>
                          </View>
                          <View style={{ flexDirection: 'row', marginRight: 16, marginBottom: 4 }}>
                            <Text style={{ fontWeight: 'bold', fontSize: 11, color: '#999' }}>Language: </Text>
                            <Text style={{ fontSize: 11, color: '#999' }}>-</Text>
                          </View>
                          <View style={{ flexDirection: 'row', marginRight: 16, marginBottom: 4 }}>
                            <Text style={{ fontWeight: 'bold', fontSize: 11, color: '#999' }}>Document: </Text>
                            <Text style={{ fontSize: 11, color: '#999' }}>-</Text>
                          </View>
                          <View style={{ flexDirection: 'row', marginRight: 16, marginBottom: 4 }}>
                            <Text style={{ fontWeight: 'bold', fontSize: 11, color: '#999' }}>Created: </Text>
                            <Text style={{ fontSize: 11, color: '#999' }}>-</Text>
                          </View>
                          <View style={{ flexDirection: 'row', marginRight: 16, marginBottom: 4 }}>
                            <Text style={{ fontWeight: 'bold', fontSize: 11, color: '#999' }}>Modified: </Text>
                            <Text style={{ fontSize: 11, color: '#999' }}>-</Text>
                          </View>
                        </View>
                        {/* Action buttons - disabled */}
                        <View style={{ flexDirection: 'row', gap: 8 }}>
                          <TouchableOpacity
                            disabled
                            style={{
                              backgroundColor: '#ccc',
                              paddingHorizontal: 12,
                              paddingVertical: 6,
                              borderRadius: 4,
                            }}
                          >
                            <Text style={{ color: '#fff', fontSize: 11, fontWeight: 'bold' }}>Update</Text>
                          </TouchableOpacity>
                          <TouchableOpacity
                            disabled
                            style={{
                              backgroundColor: '#ccc',
                              paddingHorizontal: 12,
                              paddingVertical: 6,
                              borderRadius: 4,
                            }}
                          >
                            <Text style={{ color: '#fff', fontSize: 11, fontWeight: 'bold' }}>Delete</Text>
                          </TouchableOpacity>
                        </View>
                      </View>
                    </View>
                  ) : (
                    chunks.map((chunk, index) => (
                    <View key={chunk.id} style={{ borderBottomWidth: index < chunks.length - 1 ? 1 : 0, borderBottomColor: '#ddd' }}>
                      {/* First row: Text in textarea */}
                      <View style={{ padding: 8, backgroundColor: '#fafafa' }}>
                        <Text style={{ fontWeight: 'bold', marginBottom: 4, fontSize: 12, color: '#666' }}>Text:</Text>
                        <View
                          style={{
                            borderWidth: 1,
                            borderColor: '#ddd',
                            borderRadius: 4,
                            padding: 8,
                            minHeight: 80,
                            backgroundColor: '#fff',
                          }}
                        >
                          <Text style={{ fontFamily: 'monospace', fontSize: 12 }}>{chunk.text}</Text>
                        </View>
                      </View>
                      {/* Second row: All other fields */}
                      <View style={{ flexDirection: 'row', padding: 8, backgroundColor: '#f9f9f9', flexWrap: 'wrap', alignItems: 'center', justifyContent: 'space-between' }}>
                        <View style={{ flexDirection: 'row', flexWrap: 'wrap', flex: 1 }}>
                          <View style={{ flexDirection: 'row', marginRight: 16, marginBottom: 4 }}>
                            <Text style={{ fontWeight: 'bold', fontSize: 11, color: '#666' }}>Type: </Text>
                            <Text style={{ fontSize: 11 }}>{chunk.type}</Text>
                          </View>
                          <View style={{ flexDirection: 'row', marginRight: 16, marginBottom: 4 }}>
                            <Text style={{ fontWeight: 'bold', fontSize: 11, color: '#666' }}>Section: </Text>
                            <Text style={{ fontSize: 11 }}>{chunk.sectionTitle}</Text>
                          </View>
                          <View style={{ flexDirection: 'row', marginRight: 16, marginBottom: 4 }}>
                            <Text style={{ fontWeight: 'bold', fontSize: 11, color: '#666' }}>Page: </Text>
                            <Text style={{ fontSize: 11 }}>{chunk.pageNumber}</Text>
                          </View>
                          <View style={{ flexDirection: 'row', marginRight: 16, marginBottom: 4 }}>
                            <Text style={{ fontWeight: 'bold', fontSize: 11, color: '#666' }}>Language: </Text>
                            <Text style={{ fontSize: 11 }}>{chunk.language}</Text>
                          </View>
                          <View style={{ flexDirection: 'row', marginRight: 16, marginBottom: 4 }}>
                            <Text style={{ fontWeight: 'bold', fontSize: 11, color: '#666' }}>Document: </Text>
                            <Text style={{ fontSize: 11 }}>{chunk.documentName}</Text>
                          </View>
                          <View style={{ flexDirection: 'row', marginRight: 16, marginBottom: 4 }}>
                            <Text style={{ fontWeight: 'bold', fontSize: 11, color: '#666' }}>Created: </Text>
                            <Text style={{ fontSize: 11 }}>{chunk.created}</Text>
                          </View>
                          <View style={{ flexDirection: 'row', marginRight: 16, marginBottom: 4 }}>
                            <Text style={{ fontWeight: 'bold', fontSize: 11, color: '#666' }}>Modified: </Text>
                            <Text style={{ fontSize: 11 }}>{chunk.modified}</Text>
                          </View>
                        </View>
                        {/* Action buttons */}
                        <View style={{ flexDirection: 'row', gap: 8 }}>
                          <TouchableOpacity
                            onPress={() => handleUpdateChunk(chunk)}
                            style={{
                              backgroundColor: '#007bff',
                              paddingHorizontal: 12,
                              paddingVertical: 6,
                              borderRadius: 4,
                            }}
                          >
                            <Text style={{ color: '#fff', fontSize: 11, fontWeight: 'bold' }}>Update</Text>
                          </TouchableOpacity>
                          <TouchableOpacity
                            onPress={() => handleDeleteChunk(chunk)}
                            style={{
                              backgroundColor: '#dc3545',
                              paddingHorizontal: 12,
                              paddingVertical: 6,
                              borderRadius: 4,
                            }}
                          >
                            <Text style={{ color: '#fff', fontSize: 11, fontWeight: 'bold' }}>Delete</Text>
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
                <View style={{ flexDirection: 'row', justifyContent: 'center', alignItems: 'center', gap: 8, marginTop: 16 }}>
                  <TouchableOpacity
                    onPress={() => setCurrentPage(currentPage - 1)}
                    disabled={currentPage === 0}
                    style={{
                      backgroundColor: currentPage === 0 ? '#ccc' : '#007bff',
                      paddingHorizontal: 12,
                      paddingVertical: 6,
                      borderRadius: 4,
                      opacity: currentPage === 0 ? 0.5 : 1,
                    }}
                  >
                    <Text style={{ color: '#fff', fontSize: 12, fontWeight: 'bold' }}>← Back</Text>
                  </TouchableOpacity>
                  <Text style={{ fontSize: 12, color: '#666' }}>Page {currentPage + 1}</Text>
                  <TouchableOpacity
                    onPress={() => setCurrentPage(currentPage + 1)}
                    disabled={!hasMorePages}
                    style={{
                      backgroundColor: !hasMorePages ? '#ccc' : '#007bff',
                      paddingHorizontal: 12,
                      paddingVertical: 6,
                      borderRadius: 4,
                      opacity: !hasMorePages ? 0.5 : 1,
                    }}
                  >
                    <Text style={{ color: '#fff', fontSize: 12, fontWeight: 'bold' }}>Next →</Text>
                  </TouchableOpacity>
                </View>
              )}
            </View>
          </View>
        </Window>
      </ScrollView>
    </SafeAreaView>
  );
}