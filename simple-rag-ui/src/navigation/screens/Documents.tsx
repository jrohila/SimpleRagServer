import React, { useEffect, useState } from 'react';
import { View, Text, Button, ActivityIndicator, ScrollView, TextInput } from 'react-native';
import { Picker } from '@react-native-picker/picker';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Window } from '../../components/Window';
import styles from '../../styles/CollectionsStyles';
import { getCollections } from '../../api/collections';
import { getDocuments, getDocument, updateDocument, deleteDocument } from '../../api/documents';
import { DeleteModal, DeleteResult } from '../../components/DeleteModal';

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
  const [deleteModalVisible, setDeleteModalVisible] = useState(false);
  const [deleteResult, setDeleteResult] = useState<DeleteResult | null>(null);
  const [deleting, setDeleting] = useState(false);
  const [confirmModalVisible, setConfirmModalVisible] = useState(false);

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

  // File picker placeholder (replace with real file picker in production)
  const handleFileSelect = () => {
    alert('File picker not implemented.');
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
          </View>
        </Window>
      </ScrollView>
    </SafeAreaView>
  );
}
