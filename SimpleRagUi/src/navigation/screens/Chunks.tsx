


import React, { useEffect, useState } from 'react';
import { View, SafeAreaView, ScrollView, ActivityIndicator, Text } from 'react-native';
import { Window } from '../../components/Window';
import SidebarPicker from '../../components/SidebarPicker';
import styles from '../../styles/CollectionsStyles';
import { getCollections } from '../../api/collections';
import { getDocuments } from '../../api/documents';
import { getChunks } from '../../api/chunks';

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

  useEffect(() => {
    if (selectedCollectionId && selectedDocumentId) {
      console.log('Fetching chunks for:', { collectionId: selectedCollectionId, documentId: selectedDocumentId });
      setLoadingChunks(true);
      getChunks({ collectionId: selectedCollectionId, documentId: selectedDocumentId })
        .then((res) => {
          console.log('Chunks response:', res.data);
          const data = res.data;
          setChunks(Array.isArray(data) ? data : []);
          setLoadingChunks(false);
        })
        .catch((err) => {
          console.error('Error fetching chunks:', err);
          setChunks([]);
          setLoadingChunks(false);
        });
    } else {
      setChunks([]);
    }
  }, [selectedCollectionId, selectedDocumentId]);

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
                />
              )}
            </View>
            {/* Documents Sidebar */}
            <View style={styles.sidebar}>
              {loadingDocuments ? (
                <ActivityIndicator />
              ) : documents.length === 0 ? (
                <Text style={{ padding: 8, color: '#888' }}>No documents found.</Text>
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
                />
              )}
            </View>
            {/* Chunks Table */}
            <View style={{ flex: 1, padding: 16 }}>
              <Text style={{ fontWeight: 'bold', fontSize: 16, marginBottom: 8 }}>Chunks</Text>
              {loadingChunks ? (
                <ActivityIndicator />
              ) : chunks.length === 0 ? (
                <Text style={{ color: '#888' }}>No chunks found.</Text>
              ) : (
                <View style={{ borderWidth: 1, borderColor: '#ccc', borderRadius: 6, overflow: 'hidden' }}>
                  <View style={{ flexDirection: 'row', backgroundColor: '#f0f0f0', padding: 8 }}>
                    <Text style={{ flex: 2, fontWeight: 'bold' }}>Text</Text>
                    <Text style={{ flex: 1, fontWeight: 'bold' }}>Type</Text>
                    <Text style={{ flex: 2, fontWeight: 'bold' }}>Section Title</Text>
                    <Text style={{ flex: 1, fontWeight: 'bold' }}>Page</Text>
                    <Text style={{ flex: 1, fontWeight: 'bold' }}>Language</Text>
                    <Text style={{ flex: 2, fontWeight: 'bold' }}>Document</Text>
                    <Text style={{ flex: 2, fontWeight: 'bold' }}>Created</Text>
                    <Text style={{ flex: 2, fontWeight: 'bold' }}>Modified</Text>
                  </View>
                  {chunks.map((chunk) => (
                    <View key={chunk.id} style={{ flexDirection: 'row', padding: 8, borderBottomWidth: 1, borderBottomColor: '#eee', alignItems: 'flex-start' }}>
                      <View style={{ flex: 2, marginRight: 8 }}>
                        <Text
                          style={{
                            borderWidth: 1,
                            borderColor: '#ddd',
                            borderRadius: 4,
                            padding: 4,
                            minHeight: 48,
                            fontFamily: 'monospace',
                            backgroundColor: '#fafafa',
                          }}
                          numberOfLines={4}
                        >
                          {chunk.text}
                        </Text>
                      </View>
                      <Text style={{ flex: 1 }}>{chunk.type}</Text>
                      <Text style={{ flex: 2 }}>{chunk.sectionTitle}</Text>
                      <Text style={{ flex: 1 }}>{chunk.pageNumber}</Text>
                      <Text style={{ flex: 1 }}>{chunk.language}</Text>
                      <Text style={{ flex: 2 }}>{chunk.documentName}</Text>
                      <Text style={{ flex: 2 }}>{chunk.created}</Text>
                      <Text style={{ flex: 2 }}>{chunk.modified}</Text>
                    </View>
                  ))}
                </View>
              )}
            </View>
          </View>
        </Window>
      </ScrollView>
    </SafeAreaView>
  );
}