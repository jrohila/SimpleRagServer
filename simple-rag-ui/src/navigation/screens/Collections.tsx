import React, { useEffect, useState } from 'react';
import { View, ScrollView, Text, TextInput, Button, Alert, ActivityIndicator, FlatList } from 'react-native';
import { Picker } from '@react-native-picker/picker';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Window } from '../../components/Window';
import { DeleteModal, DeleteResult } from '../../components/DeleteModal';
import { UpdateModal, UpdateResult } from '../../components/UpdateModal';
import styles from '../../styles/CollectionsStyles';
import { getCollections, getCollectionById, updateCollection, deleteCollection } from '../../api/collections';
import { getDocuments } from '../../api/documents';

type Collection = {
  id: string;
  name: string;
  description?: string;
  created?: string;
  modified?: string;
  [key: string]: any;
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

export function Collections() {
  const [collections, setCollections] = useState<Collection[]>([]);
  const [documents, setDocuments] = useState<DocumentEntity[]>([]);
  const [documentsLoading, setDocumentsLoading] = useState(false);
  const [selectedId, setSelectedId] = useState('');
  const [collection, setCollection] = useState<Collection | null>(null);
  const [loading, setLoading] = useState(false);
  const [updating, setUpdating] = useState(false);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [deleteModalVisible, setDeleteModalVisible] = useState(false);
  const [deleteResult, setDeleteResult] = useState<DeleteResult | null>(null);
  const [deleting, setDeleting] = useState(false);
  const [confirmModalVisible, setConfirmModalVisible] = useState(false);
  
  // Update modal state
  const [updateConfirmVisible, setUpdateConfirmVisible] = useState(false);
  const [updateResultVisible, setUpdateResultVisible] = useState(false);
  const [updateResult, setUpdateResult] = useState<UpdateResult | null>(null);

  useEffect(() => {
    setLoading(true);
    getCollections()
      .then((res) => {
        const data = (res as any).data as Collection[];
        setCollections(data);
        setLoading(false);
      })
      .catch(() => setLoading(false));
  }, []);

  useEffect(() => {
    if (selectedId) {
      setLoading(true);
      getCollectionById(selectedId)
        .then((res) => {
          const data = (res as any).data as Collection;
          setCollection(data);
          setName(data.name || '');
          setDescription(data.description || '');
          setLoading(false);
        })
        .catch(() => setLoading(false));
      // Load documents for this collection
      setDocumentsLoading(true);
      getDocuments(selectedId)
        .then((res) => {
          setDocuments((res as any).data || []);
          setDocumentsLoading(false);
        })
        .catch(() => {
          setDocuments([]);
          setDocumentsLoading(false);
        });
    } else {
      setCollection(null);
      setName('');
      setDescription('');
      setDocuments([]);
    }
  }, [selectedId]);

  // Check if name or description has changed
  const hasChanges = () => {
    if (!collection) return false;
    return (
      name !== (collection.name || '') ||
      description !== (collection.description || '')
    );
  };

  const handleUpdate = () => {
    if (!selectedId || !collection || !hasChanges()) return;
    setUpdateConfirmVisible(true);
  };

  const handleConfirmUpdate = () => {
    if (!selectedId || !collection) return;
    setUpdateConfirmVisible(false);
    setUpdateResultVisible(true);
    setUpdating(true);
    setUpdateResult(null);
    
    const updated = { ...collection, name, description };
    updateCollection(selectedId, updated)
      .then(() => {
        setUpdating(false);
        setUpdateResult({
          success: true,
          message: 'Collection updated successfully',
        });
      })
      .catch((error) => {
        const errorMessage = error?.response?.data?.message || error?.message || 'Unknown error occurred';
        setUpdating(false);
        setUpdateResult({
          success: false,
          message: `Failed to update collection: ${errorMessage}`,
        });
      });
  };

  const handleCancelUpdate = () => {
    setUpdateConfirmVisible(false);
  };

  const handleCloseUpdateModal = () => {
    setUpdateResultVisible(false);
    const wasSuccessful = updateResult?.success;
    if (wasSuccessful && selectedId) {
      // Reload the collections dropdown list
      getCollections()
        .then((res) => {
          const data = (res as any).data as Collection[];
          setCollections(data);
        })
        .catch(() => {
          console.error('Failed to reload collections list');
        });
      
      // Reload the collection after successful update
      getCollectionById(selectedId)
        .then((res) => {
          const data = res.data as Collection;
          setCollection(data);
          setName(data.name || '');
          setDescription(data.description || '');
        })
        .catch(() => {
          console.error('Failed to reload collection');
        });
    }
    setUpdateResult(null);
  };

  const handleDelete = () => {
    console.log('handleDelete called, selectedId:', selectedId, 'collection:', collection);
    if (!selectedId) {
      console.log('No collection selected');
      return;
    }
    
    // Show confirmation modal
    setConfirmModalVisible(true);
  };

  const handleConfirmDelete = () => {
    console.log('Delete confirmed, starting operation...');
    setConfirmModalVisible(false);
    setDeleting(true);
    setDeleteModalVisible(true);
    setDeleteResult(null);
    
    deleteCollection(selectedId)
      .then(() => {
        console.log('Delete successful');
        setDeleteResult({
          success: true,
          message: `Collection "${name}" has been successfully deleted.`
        });
        setDeleting(false);
        
        // Refresh collections list
        console.log('Refreshing collections list...');
        getCollections().then((res) => {
          const data = (res as any).data as Collection[];
          setCollections(data);
          console.log('Collections list refreshed, count:', data.length);
        });
      })
      .catch((error) => {
        console.error('Delete failed:', error);
        const errorMessage = error?.response?.data?.message || error?.message || 'Unknown error occurred';
        setDeleteResult({
          success: false,
          message: `Failed to delete collection: ${errorMessage}`
        });
        setDeleting(false);
      });
  };

  const handleCancelDelete = () => {
    console.log('Delete cancelled by user');
    setConfirmModalVisible(false);
  };

  const handleCloseDeleteModal = () => {
    setDeleteModalVisible(false);
    const wasSuccessful = deleteResult?.success;
    setDeleteResult(null);
    
    if (wasSuccessful) {
      console.log('Delete was successful, reloading collections and clearing selection...');
      // Clear selection after successful delete
      setSelectedId('');
      setCollection(null);
      setName('');
      setDescription('');
      setDocuments([]);
      
      // Reload collections list
      getCollections().then((res) => {
        const data = (res as any).data as Collection[];
        setCollections(data);
        console.log('Collections reloaded after modal close, count:', data.length);
      }).catch((error) => {
        console.error('Failed to reload collections:', error);
      });
    }
  };

  return (
    <SafeAreaView style={{ flex: 1 }}>
      <ScrollView>
        <Window>
          {loading && <ActivityIndicator />}
          <View style={styles.container}>
            {/* Collection Dropdown */}
            <View style={styles.dropdownContainer}>
              <Text style={styles.dropdownLabel}>Collection:</Text>
              <View style={styles.pickerWrapper}>
                <Picker
                  selectedValue={selectedId}
                  onValueChange={(value) => setSelectedId(value)}
                  style={styles.picker}
                >
                  <Picker.Item label="Select a collection..." value="" />
                  {collections.map((col) => (
                    <Picker.Item key={col.id} label={col.name} value={col.id} />
                  ))}
                </Picker>
              </View>
            </View>
            
            <View style={styles.form}>
              <Text style={styles.label}>Name</Text>
              <TextInput
                style={styles.input}
                value={name}
                onChangeText={setName}
                placeholder="Name"
                editable={!!collection}
              />
              <Text style={styles.label}>Description</Text>
              <TextInput
                style={styles.input}
                value={description}
                onChangeText={setDescription}
                placeholder="Description"
                editable={!!collection}
              />
              <Text style={styles.label}>Created</Text>
              <Text style={styles.dateField}>{collection?.created || ''}</Text>
              <Text style={styles.label}>Modified</Text>
              <Text style={styles.dateField}>{collection?.modified || ''}</Text>
              <View style={styles.buttonCol}>
                <View style={styles.buttonWrapper}>
                  <Button title="Update" onPress={handleUpdate} disabled={updating || !collection || !hasChanges()} />
                </View>
                <View style={styles.buttonWrapper}>
                  <Button 
                    title="Delete" 
                    onPress={() => {
                      console.log('Delete button pressed');
                      handleDelete();
                    }} 
                    color="red" 
                    disabled={updating || deleting || !collection} 
                  />
                </View>
              </View>
              {/* Documents Table - Always Visible */}
              <View style={{ marginTop: 24, paddingVertical: 8 }}>
                <Text style={[styles.label, { fontSize: 16, marginBottom: 12 }]}>Documents</Text>
                {documentsLoading ? (
                  <ActivityIndicator />
                ) : (
                  <View style={{ borderWidth: 1, borderColor: '#ccc', borderRadius: 4, marginTop: 8 }}>
                    <View style={{ flexDirection: 'row', backgroundColor: '#f0f0f0', padding: 8 }}>
                      <Text style={{ flex: 2, fontWeight: 'bold' }}>Filename</Text>
                      <Text style={{ flex: 1, fontWeight: 'bold' }}>Type</Text>
                      <Text style={{ flex: 1, fontWeight: 'bold' }}>Size</Text>
                      <Text style={{ flex: 2, fontWeight: 'bold' }}>Created</Text>
                      <Text style={{ flex: 2, fontWeight: 'bold' }}>Updated</Text>
                      <Text style={{ flex: 1, fontWeight: 'bold' }}>State</Text>
                    </View>
                    <FlatList
                      data={documents}
                      keyExtractor={(item) => item.id}
                      scrollEnabled={false}
                      initialNumToRender={Math.min(documents.length || 0, 20)}
                      renderItem={({ item }) => (
                        <View style={{ flexDirection: 'row', padding: 8, borderBottomWidth: 1, borderBottomColor: '#eee' }}>
                          <Text style={{ flex: 2 }}>{item.originalFilename}</Text>
                          <Text style={{ flex: 1 }}>{item.mimeType}</Text>
                          <Text style={{ flex: 1 }}>{item.contentLen}</Text>
                          <Text style={{ flex: 2 }}>{item.createdTime}</Text>
                          <Text style={{ flex: 2 }}>{item.updatedTime}</Text>
                          <Text style={{ flex: 1 }}>{item.state}</Text>
                        </View>
                      )}
                      ListEmptyComponent={<Text style={{ padding: 8, color: '#888' }}>No documents found.</Text>}
                    />
                  </View>
                )}
              </View>
            </View>
          </View>
        </Window>
      </ScrollView>

      {/* Use the reusable DeleteModal component */}
      <DeleteModal
        confirmVisible={confirmModalVisible}
        itemName={name}
        onConfirm={handleConfirmDelete}
        onCancel={handleCancelDelete}
        resultVisible={deleteModalVisible}
        deleting={deleting}
        deleteResult={deleteResult}
        onClose={handleCloseDeleteModal}
        confirmMessage={`Are you sure you want to delete the collection "${name}"?`}
        deletingMessage="Deleting collection..."
      />
      
      {/* Use the reusable UpdateModal component */}
      <UpdateModal
        confirmVisible={updateConfirmVisible}
        itemName={name}
        onConfirm={handleConfirmUpdate}
        onCancel={handleCancelUpdate}
        resultVisible={updateResultVisible}
        updating={updating}
        updateResult={updateResult}
        onClose={handleCloseUpdateModal}
        confirmMessage={`Are you sure you want to update the collection "${name}"?`}
        updatingMessage="Updating collection..."
      />
    </SafeAreaView>
  );
}
