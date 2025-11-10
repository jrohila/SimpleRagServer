import React, { useEffect, useState } from 'react';
import { View, ScrollView, Text, TextInput, Button, Alert, ActivityIndicator, FlatList } from 'react-native';
import { Picker } from '@react-native-picker/picker';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Window } from '../../components/Window';
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

  const handleUpdate = () => {
    if (!selectedId || !collection) return;
    setUpdating(true);
    const updated = { ...collection, name, description };
    updateCollection(selectedId, updated)
      .then(() => {
        Alert.alert('Success', 'Collection updated');
        setUpdating(false);
      })
      .catch(() => {
        Alert.alert('Error', 'Failed to update collection');
        setUpdating(false);
      });
  };

  const handleDelete = () => {
    if (!selectedId) return;
    Alert.alert('Confirm', 'Delete this collection?', [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Delete', style: 'destructive', onPress: () => {
          setUpdating(true);
          deleteCollection(selectedId)
            .then(() => {
              Alert.alert('Deleted', 'Collection deleted');
              setSelectedId('');
              setUpdating(false);
              getCollections().then((res) => {
                const data = (res as any).data as Collection[];
                setCollections(data);
              });
            })
            .catch(() => {
              Alert.alert('Error', 'Failed to delete collection');
              setUpdating(false);
            });
        }
      }
    ]);
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
                  <Button title="Update" onPress={handleUpdate} disabled={updating || !collection} />
                </View>
                <View style={styles.buttonWrapper}>
                  <Button title="Delete" onPress={handleDelete} color="red" disabled={updating || !collection} />
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
    </SafeAreaView>
  );
}
