import React, { useEffect, useState } from 'react';
import { View, Text, TextInput, Button, ActivityIndicator, ScrollView, SafeAreaView, TouchableOpacity, Alert } from 'react-native';
import { Picker } from '@react-native-picker/picker';
import { Window } from '../../components/Window';
import styles from '../../styles/SearchStyles';
import { getCollections } from '../../api/collections';
import { hybridSearch } from '../../api/search';

type SearchResult = {
  score: number;
  text: string;
  documentName: string;
  sectionTitle: string;
  pageNumber: number;
  url?: string;
};

type BoostTerm = {
  term: string;
  weight: number;
};

import { useTranslation } from 'react-i18next';

export function Search() {
  const { t } = useTranslation();
  const [collections, setCollections] = useState<Array<{ id: string; name: string }>>([]);
  const [selectedCollectionId, setSelectedCollectionId] = useState('');
  const [loadingCollections, setLoadingCollections] = useState(false);
  
  // Search parameters
  const [query, setQuery] = useState('');
  const [language, setLanguage] = useState<string | null>(null);
  const [enableFuzziness, setEnableFuzziness] = useState(false);
  const [boostTerms, setBoostTerms] = useState<BoostTerm[]>([{ term: '', weight: 1.0 }]);
  
  // Search results
  const [results, setResults] = useState<SearchResult[]>([]);
  const [searching, setSearching] = useState(false);

  useEffect(() => {
    setLoadingCollections(true);
    getCollections()
      .then((res) => {
        setCollections(res.data || []);
        setLoadingCollections(false);
      })
      .catch(() => setLoadingCollections(false));
  }, []);

  const handleAddBoostTerm = () => {
    setBoostTerms([...boostTerms, { term: '', weight: 1.0 }]);
  };

  const handleRemoveBoostTerm = (index: number) => {
    if (boostTerms.length > 1) {
      setBoostTerms(boostTerms.filter((_, i) => i !== index));
    }
  };

  const handleBoostTermChange = (index: number, field: 'term' | 'weight', value: string) => {
    const updated = [...boostTerms];
    if (field === 'term') {
      updated[index].term = value;
    } else {
      updated[index].weight = parseFloat(value) || 1.0;
    }
    setBoostTerms(updated);
  };

  const handleSearch = () => {
    if (!selectedCollectionId || !query) {
      Alert.alert(t('messages.errorTitle'), t('search.errors.selectCollectionAndQuery'));
      return;
    }

    setSearching(true);
    const searchParams: any = {
      collectionId: selectedCollectionId,
      query,
      size: 20,
    };

    // Add optional parameters
    if (language) searchParams.language = language;
    if (enableFuzziness) searchParams.enableFuzziness = true;
    
    // Add boost terms if any are filled - format according to API schema
    const validBoostTerms = boostTerms.filter(bt => bt.term.trim() !== '');
    if (validBoostTerms.length > 0) {
      searchParams.terms = validBoostTerms.map(bt => ({
        term: bt.term,
        boostWeight: bt.weight,
        mandatory: false
      }));
    }

    hybridSearch(searchParams)
      .then((res) => {
        console.log('Search results:', res.data);
        const data = Array.isArray(res.data) ? res.data : [];
        setResults(data);
        setSearching(false);
      })
      .catch((err) => {
        console.error('Search error:', err);
        Alert.alert(t('messages.errorTitle'), t('search.errors.failed'));
        setResults([]);
        setSearching(false);
      });
  };

  return (
    <SafeAreaView style={styles.safeArea}>
      <ScrollView>
        <Window>
          <View style={styles.container}>
            {/* Collection Dropdown */}
            <View style={styles.dropdownContainer}>
              <Text style={styles.label}>{t('search.collection')}</Text>
              {loadingCollections ? (
                <ActivityIndicator />
              ) : (
                <View style={styles.pickerWrapper}>
                  <Picker
                    selectedValue={selectedCollectionId}
                    onValueChange={(value) => setSelectedCollectionId(value)}
                    style={styles.picker}
                  >
                    <Picker.Item label={t('basic.selectCollection')} value="" />
                    {collections.map((col) => (
                      <Picker.Item key={col.id} label={col.name} value={col.id} />
                    ))}
                  </Picker>
                </View>
              )}
            </View>

            {/* Search Query */}
            <View style={styles.fieldContainer}>
              <Text style={styles.label}>{t('search.queryLabel')}</Text>
              <TextInput
                style={styles.input}
                value={query}
                onChangeText={setQuery}
                placeholder={t('placeholders.searchQuery')}
                multiline
              />
            </View>

            {/* Language */}
            <View style={styles.fieldContainer}>
              <Text style={styles.label}>{t('search.languageLabel')}</Text>
              <TextInput
                style={styles.input}
                value={language || ''}
                onChangeText={(text) => setLanguage(text || null)}
                placeholder={t('placeholders.languageExample')}
              />
            </View>

            {/* Enable Fuzziness */}
            <View style={styles.checkboxContainer}>
              <TouchableOpacity
                style={styles.checkbox}
                onPress={() => setEnableFuzziness(!enableFuzziness)}
              >
                <View style={[styles.checkboxBox, enableFuzziness && styles.checkboxBoxChecked]}>
                  {enableFuzziness && <Text style={styles.checkboxCheck}>✓</Text>}
                </View>
                <Text style={styles.checkboxLabel}>{t('search.enableFuzziness')}</Text>
              </TouchableOpacity>
            </View>

            {/* Boost Terms */}
            <View style={styles.boostTermsSection}>
              <Text style={styles.sectionTitle}>{t('search.boostTermsTitle')}</Text>
              {boostTerms.map((bt, index) => (
                <View key={index} style={styles.boostTermRow}>
                  <TextInput
                    style={[styles.input, styles.boostTermInput]}
                    value={bt.term}
                    onChangeText={(text) => handleBoostTermChange(index, 'term', text)}
                    placeholder={t('placeholders.term')}
                  />
                  <TextInput
                    style={[styles.input, styles.boostWeightInput]}
                    value={bt.weight.toString()}
                    onChangeText={(text) => handleBoostTermChange(index, 'weight', text)}
                    placeholder={t('placeholders.weight')}
                    keyboardType="numeric"
                  />
                  {boostTerms.length > 1 && (
                    <TouchableOpacity
                      style={styles.removeButton}
                      onPress={() => handleRemoveBoostTerm(index)}
                    >
                      <Text style={styles.removeButtonText}>✕</Text>
                    </TouchableOpacity>
                  )}
                </View>
              ))}
              <TouchableOpacity style={styles.addButton} onPress={handleAddBoostTerm}>
                <Text style={styles.addButtonText}>+ {t('actions.addBoostTerm')}</Text>
              </TouchableOpacity>
            </View>

            {/* Search Button */}
            <View style={styles.searchButtonContainer}>
              <Button title={t('actions.search')} onPress={handleSearch} disabled={searching || !selectedCollectionId || !query} />
            </View>

            {/* Results */}
            {searching ? (
              <ActivityIndicator style={styles.loader} />
            ) : results.length > 0 ? (
              <View style={styles.resultsContainer}>
                <Text style={styles.resultsTitle}>{t('search.resultsTitle', { count: results.length })}</Text>
                <View style={styles.tableContainer}>
                  {/* Table Header */}
                  <View style={styles.tableHeader}>
                    <Text style={[styles.tableHeaderText, styles.scoreColumn]}>{t('search.table.score')}</Text>
                    <Text style={[styles.tableHeaderText, styles.textColumn]}>{t('search.table.text')}</Text>
                    <Text style={[styles.tableHeaderText, styles.documentColumn]}>{t('search.table.document')}</Text>
                    <Text style={[styles.tableHeaderText, styles.sectionColumn]}>{t('search.table.section')}</Text>
                    <Text style={[styles.tableHeaderText, styles.pageColumn]}>{t('search.table.page')}</Text>
                    <Text style={[styles.tableHeaderText, styles.urlColumn]}>{t('search.table.url')}</Text>
                  </View>
                  {/* Table Rows */}
                  {results.map((result, index) => (
                    <View key={index} style={[styles.tableRow, index % 2 === 0 && styles.tableRowEven]}>
                      <Text style={[styles.tableCell, styles.scoreColumn]}>{result.score.toFixed(3)}</Text>
                      <Text style={[styles.tableCell, styles.textColumn]} numberOfLines={3}>{result.text}</Text>
                      <Text style={[styles.tableCell, styles.documentColumn]}>{result.documentName}</Text>
                      <Text style={[styles.tableCell, styles.sectionColumn]}>{result.sectionTitle}</Text>
                      <Text style={[styles.tableCell, styles.pageColumn]}>{result.pageNumber}</Text>
                      <Text style={[styles.tableCell, styles.urlColumn]} numberOfLines={1}>{result.url || '-'}</Text>
                    </View>
                  ))}
                </View>
              </View>
            ) : null}
          </View>
        </Window>
      </ScrollView>
    </SafeAreaView>
  );
}
