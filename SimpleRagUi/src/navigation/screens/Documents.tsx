import React, { useEffect, useState } from 'react';
import { View, Text, Button, ActivityIndicator, ScrollView, TextInput } from 'react-native';
import SidebarPicker from '../../components/SidebarPicker';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Window } from '../../components/Window';
import styles from '../../styles/CollectionsStyles';
import { getCollections } from '../../api/collections';
import { getDocuments, getDocument, updateDocument, deleteDocument } from '../../api/documents';

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
  const [updating, setUpdating] = useState(false);

  useEffect(() => {
    setLoading(true);
    getCollections().then((res) => {
      setCollections(res.data || []);
      setLoading(false);
    });
  }, []);

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
    setUpdating(true);
    updateDocument(selectedCollectionId, selectedDocumentId, file)
      .then(() => {
        setFile(null);
        setUpdating(false);
        // Optionally reload document
        getDocument(selectedCollectionId, selectedDocumentId).then((res) => setDocument(res.data));
      })
      .catch(() => setUpdating(false));
  };

  const handleDelete = () => {
    if (!selectedCollectionId || !selectedDocumentId) return;
    setUpdating(true);
    deleteDocument(selectedCollectionId, selectedDocumentId)
      .then(() => {
        setSelectedDocumentId('');
        setDocument(null);
        setUpdating(false);
        // Optionally reload documents
        getDocuments(selectedCollectionId).then((res) => setDocuments(res.data || []));
      })
      .catch(() => setUpdating(false));
  };

  // File picker placeholder (replace with real file picker in production)
  const handleFileSelect = () => {
    alert('File picker not implemented.');
  };

  return (
    <SafeAreaView style={{ flex: 1 }}>
      <ScrollView>
        <Window>
          <View style={{ flexDirection: 'row', alignItems: 'flex-start', width: '100%', minHeight: 400 }}>
            {/* Collections Sidebar */}
              <View style={styles.sidebar}>
                {loading ? (
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
                {docLoading ? (
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
            {/* Document Form */}
            <View style={styles.form}> 
              <Text style={styles.label}>Filename</Text>
              <TextInput style={styles.input} value={document?.originalFilename || ''} editable={false} />
              <Text style={styles.label}>Type</Text>
              <TextInput style={styles.input} value={document?.mimeType || ''} editable={false} />
              <Text style={styles.label}>Size</Text>
              <TextInput style={styles.input} value={document?.contentLen?.toString() || ''} editable={false} />
              <Text style={styles.label}>Created</Text>
              <TextInput style={styles.input} value={document?.createdTime || ''} editable={false} />
              <Text style={styles.label}>Updated</Text>
              <TextInput style={styles.input} value={document?.updatedTime || ''} editable={false} />
              <Text style={styles.label}>State</Text>
              <TextInput style={styles.input} value={document?.state || ''} editable={false} />
              <Text style={styles.label}>Select File</Text>
              <Button title={file ? 'File Selected' : 'Choose File'} onPress={handleFileSelect} />
              <View style={styles.buttonCol}>
                <View style={styles.buttonWrapper}>
                  <Button title="Update" onPress={handleUpdate} disabled={updating || !file} />
                </View>
                <View style={styles.buttonWrapper}>
                  <Button title="Delete" onPress={handleDelete} color="red" disabled={updating || !selectedDocumentId} />
                </View>
              </View>
            </View>
          </View>
        </Window>
      </ScrollView>
    </SafeAreaView>
  );
}
