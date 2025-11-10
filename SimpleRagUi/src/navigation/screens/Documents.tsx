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
                ) : (
                  <SidebarPicker
                    items={collections}
                    getItemLabel={(col) => col.name}
                    getItemKey={(col) => col.id}
                    selectedItem={collections.find((c) => c.id === selectedCollectionId) || null}
                    onSelect={(col) => setSelectedCollectionId(col?.id || '')}
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
                {docLoading ? (
                  <ActivityIndicator />
                ) : (
                  <SidebarPicker
                    items={documents}
                    getItemLabel={(doc) => doc.originalFilename}
                    getItemKey={(doc) => doc.id}
                    selectedItem={documents.find((d) => d.id === selectedDocumentId) || null}
                    onSelect={(doc) => setSelectedDocumentId(doc?.id || '')}
                    containerStyle={styles.sidebarContent}
                    itemStyle={styles.chatItem}
                    selectedItemStyle={styles.chatItemSelected}
                    textStyle={styles.chatItemText}
                    selectedTextStyle={styles.chatItemText}
                    emptyMessage="No documents found."
                  />
                )}
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
                  <View style={{ flex: 1 }}>
                    <Button title={file ? 'File Selected' : 'Choose File'} onPress={handleFileSelect} />
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
          </View>
        </Window>
      </ScrollView>
    </SafeAreaView>
  );
}
