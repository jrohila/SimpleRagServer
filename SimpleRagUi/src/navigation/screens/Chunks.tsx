import React, { useEffect, useState } from 'react';
import { View, SafeAreaView, ScrollView, ActivityIndicator, Text } from 'react-native';
import { Window } from '../../components/Window';
import SidebarPicker from '../../components/SidebarPicker';
import styles from '../../styles/CollectionsStyles';
import { getCollections } from '../../api/collections';
import { getDocuments } from '../../api/documents';

export function Chunks() {
  const [collections, setCollections] = useState<Array<{ id: string; name: string }>>([]);
  const [selectedCollectionId, setSelectedCollectionId] = useState('');
  const [documents, setDocuments] = useState<Array<{ id: string; originalFilename: string }>>([]);
  const [selectedDocumentId, setSelectedDocumentId] = useState('');
  const [loadingCollections, setLoadingCollections] = useState(false);
  const [loadingDocuments, setLoadingDocuments] = useState(false);

  useEffect(() => {
    setLoadingCollections(true);
    getCollections().then((res) => {
      setCollections(res.data || []);
      setLoadingCollections(false);
    });
  }, []);

  useEffect(() => {
    if (selectedCollectionId) {
      setLoadingDocuments(true);
      getDocuments(selectedCollectionId).then((res) => {
        setDocuments(res.data || []);
        setLoadingDocuments(false);
      });
    } else {
      setDocuments([]);
      setSelectedDocumentId('');
    }
  }, [selectedCollectionId]);

  return (
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
            {/* ...rest of the Chunks page UI... */}
          </View>
        </Window>
      </ScrollView>
    </SafeAreaView>
  );
}
