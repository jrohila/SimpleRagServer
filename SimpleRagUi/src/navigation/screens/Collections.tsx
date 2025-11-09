import React, { useEffect, useState } from 'react';
import { View, ScrollView, Text, TextInput, Button, Alert, ActivityIndicator, TouchableOpacity } from 'react-native';
import { Picker } from '@react-native-picker/picker';
import { SafeAreaView } from 'react-native-safe-area-context';
import { Window } from '../../components/Window';
import styles from '../../styles/CollectionsStyles';
import { getCollections, getCollectionById, updateCollection, deleteCollection } from '../../api/collections';

type Collection = {
  id: string;
  name: string;
  description?: string;
  created?: string;
  modified?: string;
  [key: string]: any;
};

export function Collections() {
  const [collections, setCollections] = useState<Collection[]>([]);
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
    } else {
      setCollection(null);
      setName('');
      setDescription('');
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
          <View style={styles.row}>
            <View style={styles.sidebar}>
              <ScrollView contentContainerStyle={styles.sidebarContent}>
                {collections.map((col) => (
                  <TouchableOpacity
                    key={col.id}
                    style={[
                      styles.chatItem,
                      selectedId === col.id && styles.chatItemSelected
                    ]}
                    onPress={() => setSelectedId(col.id)}
                  >
                    <Text style={styles.chatItemText}>{col.name}</Text>
                  </TouchableOpacity>
                ))}
              </ScrollView>
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
            </View>
          </View>
        </Window>
      </ScrollView>
    </SafeAreaView>
  );
}
