import React, { useState, useRef, useEffect } from 'react';
import { View, TextInput, Text, TouchableOpacity, ScrollView, StyleSheet, ActivityIndicator } from 'react-native';
import { MaterialCommunityIcons } from './Icons';

interface ModelOption {
  id: string;
  totalWeightMB?: number;
  [key: string]: any;
}

interface ModelComboBoxProps {
  value: string;
  onChange: (modelId: string) => void;
  options: ModelOption[];
  placeholder?: string;
  disabled?: boolean;
  loading?: boolean;
  onSearchChange?: (searchText: string) => void;
  label?: string;
}

export const ModelComboBox: React.FC<ModelComboBoxProps> = ({
  value,
  onChange,
  options,
  placeholder = 'Search and select model...',
  disabled = false,
  loading = false,
  onSearchChange,
  label,
}) => {
  const [isOpen, setIsOpen] = useState(false);
  const [searchText, setSearchText] = useState('');
  const [filteredOptions, setFilteredOptions] = useState<ModelOption[]>(options);
  const inputRef = useRef<any>(null);

  // Update filtered options when options change
  useEffect(() => {
    if (searchText.trim()) {
      const filtered = options.filter((option) =>
        option.id.toLowerCase().includes(searchText.toLowerCase())
      );
      setFilteredOptions(filtered);
    } else {
      setFilteredOptions(options);
    }
  }, [options, searchText]);

  // Find selected model name
  const selectedModel = options.find((opt) => opt.id === value);
  const displayValue = selectedModel ? selectedModel.id : value || '';

  const handleInputChange = (text: string) => {
    setSearchText(text);
    setIsOpen(true);
    if (onSearchChange) {
      onSearchChange(text);
    }
  };

  const handleSelect = (modelId: string) => {
    onChange(modelId);
    setSearchText('');
    setIsOpen(false);
  };

  const handleFocus = () => {
    setIsOpen(true);
    setSearchText('');
  };

  const handleBlur = () => {
    // Delay to allow click on dropdown item
    setTimeout(() => {
      setIsOpen(false);
      setSearchText('');
    }, 200);
  };

  return (
    <View style={styles.container}>
      {label && <Text style={styles.label}>{label}</Text>}
      
      <View style={styles.inputContainer}>
        <TextInput
          ref={inputRef}
          style={[styles.input, disabled && styles.inputDisabled]}
          value={isOpen ? searchText : displayValue}
          onChangeText={handleInputChange}
          onFocus={handleFocus}
          onBlur={handleBlur}
          placeholder={placeholder}
          editable={!disabled}
        />
        
        <TouchableOpacity
          style={styles.iconButton}
          onPress={() => {
            if (!disabled) {
              if (isOpen) {
                setIsOpen(false);
                setSearchText('');
              } else {
                setIsOpen(true);
                inputRef.current?.focus();
              }
            }
          }}
          disabled={disabled}
        >
          <MaterialCommunityIcons
            name={isOpen ? 'chevron-up' : 'chevron-down'}
            size={24}
            color={disabled ? '#ccc' : '#666'}
          />
        </TouchableOpacity>
      </View>

      {isOpen && !disabled && (
        <View style={styles.dropdown}>
          {loading ? (
            <View style={styles.loadingContainer}>
              <ActivityIndicator size="small" />
              <Text style={styles.loadingText}>Loading models...</Text>
            </View>
          ) : filteredOptions.length === 0 ? (
            <View style={styles.emptyContainer}>
              <Text style={styles.emptyText}>No models found</Text>
            </View>
          ) : (
            <ScrollView style={styles.scrollView} nestedScrollEnabled>
              {filteredOptions.map((option) => (
                <TouchableOpacity
                  key={option.id}
                  style={[
                    styles.option,
                    option.id === value && styles.optionSelected,
                  ]}
                  onPress={() => handleSelect(option.id)}
                >
                  <Text
                    style={[
                      styles.optionText,
                      option.id === value && styles.optionTextSelected,
                    ]}
                    numberOfLines={1}
                  >
                    {option.id}
                  </Text>
                  {option.totalWeightMB && (
                    <Text style={styles.optionSize}>
                      {option.totalWeightMB.toFixed(2)} MB
                    </Text>
                  )}
                </TouchableOpacity>
              ))}
            </ScrollView>
          )}
        </View>
      )}
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    marginBottom: 16,
    zIndex: 1000,
  },
  label: {
    fontSize: 14,
    fontWeight: '600',
    marginBottom: 8,
    color: '#333',
  },
  inputContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    position: 'relative',
  },
  input: {
    flex: 1,
    borderWidth: 1,
    borderColor: '#ccc',
    borderRadius: 4,
    padding: 10,
    fontSize: 14,
    backgroundColor: '#fff',
    paddingRight: 40,
  },
  inputDisabled: {
    backgroundColor: '#f5f5f5',
    color: '#999',
  },
  iconButton: {
    position: 'absolute',
    right: 8,
    padding: 4,
  },
  dropdown: {
    position: 'absolute',
    top: '100%',
    left: 0,
    right: 0,
    maxHeight: 250,
    backgroundColor: '#fff',
    borderWidth: 1,
    borderColor: '#ccc',
    borderTopWidth: 0,
    borderBottomLeftRadius: 4,
    borderBottomRightRadius: 4,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.25,
    shadowRadius: 3.84,
    elevation: 5,
    zIndex: 1000,
  },
  scrollView: {
    maxHeight: 250,
  },
  option: {
    padding: 12,
    borderBottomWidth: 1,
    borderBottomColor: '#f0f0f0',
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  optionSelected: {
    backgroundColor: '#e3f2fd',
  },
  optionText: {
    fontSize: 14,
    color: '#333',
    flex: 1,
  },
  optionTextSelected: {
    fontWeight: '600',
    color: '#1976d2',
  },
  optionSize: {
    fontSize: 12,
    color: '#666',
    marginLeft: 8,
  },
  loadingContainer: {
    padding: 20,
    alignItems: 'center',
  },
  loadingText: {
    marginTop: 8,
    fontSize: 14,
    color: '#666',
  },
  emptyContainer: {
    padding: 20,
    alignItems: 'center',
  },
  emptyText: {
    fontSize: 14,
    color: '#999',
  },
});
