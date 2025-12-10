import React, { useState } from 'react';
import { View, Text, TextInput, TouchableOpacity, Modal, StyleSheet, ScrollView, Switch } from 'react-native';
import { MaterialCommunityIcons } from './Icons';
import { useTranslation } from 'react-i18next';

export interface WebGpuConfig {
  modelId?: string;
  systemPrompt?: string;
  overrideParentSystemPrompt?: boolean;
  systemPromptAppend?: string;
  overrideParentSystemPromptAppend?: boolean;
  contextPrompt?: string;
  overrideParentContextPrompt?: boolean;
  memoryPrompt?: string;
  overrideParentMemoryPrompt?: boolean;
  extractorPrompt?: string;
  overrideParentExtractorPrompt?: boolean;
  usePromptRewriting?: boolean;
  userPromptRewritingPrompt?: string;
  overrideParentUserPromptRewriting?: boolean;
}

export interface WebGpuConfigDrawerProps {
  visible: boolean;
  onClose: () => void;
  config: WebGpuConfig | null | undefined;
  onChange: (config: WebGpuConfig) => void;
  disabled?: boolean;
}

export const WebGpuConfigDrawer: React.FC<WebGpuConfigDrawerProps> = ({
  visible,
  onClose,
  config,
  onChange,
  disabled = false,
}) => {
  const { t } = useTranslation();
  
  // Local state for form
  const [localConfig, setLocalConfig] = useState<WebGpuConfig>(config || {});

  // Update local state when config changes
  React.useEffect(() => {
    setLocalConfig(config || {});
  }, [config]);

  const handleFieldChange = (field: keyof WebGpuConfig, value: string | boolean) => {
    const updated = { ...localConfig, [field]: value };
    setLocalConfig(updated);
  };

  const handleSave = () => {
    onChange(localConfig);
    onClose();
  };

  const handleCancel = () => {
    setLocalConfig(config || {});
    onClose();
  };

  return (
    <Modal
      visible={visible}
      animationType="slide"
      transparent={false}
      onRequestClose={handleCancel}
    >
      <View style={styles.container}>
        <View style={styles.header}>
          <Text style={styles.headerTitle}>{t('webgpu.title', 'WebGPU Configuration')}</Text>
          <TouchableOpacity onPress={handleCancel} style={styles.closeButton}>
            <MaterialCommunityIcons name="close" size={24} color="#333" />
          </TouchableOpacity>
        </View>

        <ScrollView style={styles.content}>
          {/* Model Selection */}
          <View style={styles.section}>
            <Text style={styles.label}>{t('webgpu.modelId', 'Model ID')}</Text>
            <TextInput
              style={styles.input}
              value={localConfig.modelId || ''}
              onChangeText={(value) => handleFieldChange('modelId', value)}
              placeholder={t('webgpu.modelIdPlaceholder', 'e.g., onnx-community/Olmo-3-7B-Instruct-ONNX')}
              editable={!disabled}
            />
          </View>

          {/* System Prompt */}
          <View style={styles.section}>
            <View style={styles.labelRow}>
              <Text style={styles.label}>{t('webgpu.systemPrompt', 'System Prompt')}</Text>
              <View style={styles.switchContainer}>
                <Text style={styles.switchLabel}>{t('webgpu.override', 'Override')}</Text>
                <Switch
                  value={localConfig.overrideParentSystemPrompt || false}
                  onValueChange={(value) => handleFieldChange('overrideParentSystemPrompt', value)}
                  disabled={disabled}
                />
              </View>
            </View>
            <TextInput
              style={[styles.input, styles.textarea]}
              value={localConfig.systemPrompt || ''}
              onChangeText={(value) => handleFieldChange('systemPrompt', value)}
              placeholder={t('webgpu.systemPromptPlaceholder', 'Enter system prompt...')}
              editable={!disabled && (localConfig.overrideParentSystemPrompt || false)}
              multiline
              numberOfLines={5}
            />
          </View>

          {/* System Prompt Append */}
          <View style={styles.section}>
            <View style={styles.labelRow}>
              <Text style={styles.label}>{t('webgpu.systemPromptAppend', 'System Prompt Append')}</Text>
              <View style={styles.switchContainer}>
                <Text style={styles.switchLabel}>{t('webgpu.override', 'Override')}</Text>
                <Switch
                  value={localConfig.overrideParentSystemPromptAppend || false}
                  onValueChange={(value) => handleFieldChange('overrideParentSystemPromptAppend', value)}
                  disabled={disabled}
                />
              </View>
            </View>
            <TextInput
              style={[styles.input, styles.textarea]}
              value={localConfig.systemPromptAppend || ''}
              onChangeText={(value) => handleFieldChange('systemPromptAppend', value)}
              placeholder={t('webgpu.systemPromptAppendPlaceholder', 'Enter system prompt append...')}
              editable={!disabled && (localConfig.overrideParentSystemPromptAppend || false)}
              multiline
              numberOfLines={5}
            />
          </View>

          {/* Context Prompt */}
          <View style={styles.section}>
            <View style={styles.labelRow}>
              <Text style={styles.label}>{t('webgpu.contextPrompt', 'Context Prompt')}</Text>
              <View style={styles.switchContainer}>
                <Text style={styles.switchLabel}>{t('webgpu.override', 'Override')}</Text>
                <Switch
                  value={localConfig.overrideParentContextPrompt || false}
                  onValueChange={(value) => handleFieldChange('overrideParentContextPrompt', value)}
                  disabled={disabled}
                />
              </View>
            </View>
            <TextInput
              style={[styles.input, styles.textarea]}
              value={localConfig.contextPrompt || ''}
              onChangeText={(value) => handleFieldChange('contextPrompt', value)}
              placeholder={t('webgpu.contextPromptPlaceholder', 'Enter context prompt...')}
              editable={!disabled && (localConfig.overrideParentContextPrompt || false)}
              multiline
              numberOfLines={5}
            />
          </View>

          {/* Memory Prompt */}
          <View style={styles.section}>
            <View style={styles.labelRow}>
              <Text style={styles.label}>{t('webgpu.memoryPrompt', 'Memory Prompt')}</Text>
              <View style={styles.switchContainer}>
                <Text style={styles.switchLabel}>{t('webgpu.override', 'Override')}</Text>
                <Switch
                  value={localConfig.overrideParentMemoryPrompt || false}
                  onValueChange={(value) => handleFieldChange('overrideParentMemoryPrompt', value)}
                  disabled={disabled}
                />
              </View>
            </View>
            <TextInput
              style={[styles.input, styles.textarea]}
              value={localConfig.memoryPrompt || ''}
              onChangeText={(value) => handleFieldChange('memoryPrompt', value)}
              placeholder={t('webgpu.memoryPromptPlaceholder', 'Enter memory prompt...')}
              editable={!disabled && (localConfig.overrideParentMemoryPrompt || false)}
              multiline
              numberOfLines={5}
            />
          </View>

          {/* Extractor Prompt */}
          <View style={styles.section}>
            <View style={styles.labelRow}>
              <Text style={styles.label}>{t('webgpu.extractorPrompt', 'Extractor Prompt')}</Text>
              <View style={styles.switchContainer}>
                <Text style={styles.switchLabel}>{t('webgpu.override', 'Override')}</Text>
                <Switch
                  value={localConfig.overrideParentExtractorPrompt || false}
                  onValueChange={(value) => handleFieldChange('overrideParentExtractorPrompt', value)}
                  disabled={disabled}
                />
              </View>
            </View>
            <TextInput
              style={[styles.input, styles.textarea]}
              value={localConfig.extractorPrompt || ''}
              onChangeText={(value) => handleFieldChange('extractorPrompt', value)}
              placeholder={t('webgpu.extractorPromptPlaceholder', 'Enter extractor prompt...')}
              editable={!disabled && (localConfig.overrideParentExtractorPrompt || false)}
              multiline
              numberOfLines={5}
            />
          </View>

          {/* Prompt Rewriting */}
          <View style={styles.section}>
            <View style={styles.labelRow}>
              <Text style={styles.label}>{t('webgpu.usePromptRewriting', 'Use Prompt Rewriting')}</Text>
              <Switch
                value={localConfig.usePromptRewriting || false}
                onValueChange={(value) => handleFieldChange('usePromptRewriting', value)}
                disabled={disabled}
              />
            </View>
          </View>

          <View style={styles.section}>
            <View style={styles.labelRow}>
              <Text style={styles.label}>{t('webgpu.userPromptRewritingPrompt', 'User Prompt Rewriting Prompt')}</Text>
              <View style={styles.switchContainer}>
                <Text style={styles.switchLabel}>{t('webgpu.override', 'Override')}</Text>
                <Switch
                  value={localConfig.overrideParentUserPromptRewriting || false}
                  onValueChange={(value) => handleFieldChange('overrideParentUserPromptRewriting', value)}
                  disabled={disabled}
                />
              </View>
            </View>
            <TextInput
              style={[styles.input, styles.textarea]}
              value={localConfig.userPromptRewritingPrompt || ''}
              onChangeText={(value) => handleFieldChange('userPromptRewritingPrompt', value)}
              placeholder={t('webgpu.userPromptRewritingPlaceholder', 'Enter user prompt rewriting prompt...')}
              editable={!disabled && (localConfig.overrideParentUserPromptRewriting || false)}
              multiline
              numberOfLines={5}
            />
          </View>
        </ScrollView>

        <View style={styles.footer}>
          <TouchableOpacity
            style={[styles.button, styles.cancelButton]}
            onPress={handleCancel}
          >
            <Text style={styles.buttonText}>{t('actions.cancel', 'Cancel')}</Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={[styles.button, styles.saveButton, disabled && styles.disabledButton]}
            onPress={handleSave}
            disabled={disabled}
          >
            <Text style={[styles.buttonText, styles.saveButtonText]}>{t('actions.save', 'Save')}</Text>
          </TouchableOpacity>
        </View>
      </View>
    </Modal>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#fff',
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#e0e0e0',
    backgroundColor: '#f5f5f5',
  },
  headerTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#333',
  },
  closeButton: {
    padding: 4,
  },
  content: {
    flex: 1,
    padding: 16,
  },
  section: {
    marginBottom: 20,
  },
  label: {
    fontSize: 14,
    fontWeight: '600',
    marginBottom: 8,
    color: '#333',
  },
  labelRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 8,
  },
  switchContainer: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  switchLabel: {
    fontSize: 12,
    color: '#666',
    marginRight: 8,
  },
  input: {
    borderWidth: 1,
    borderColor: '#ccc',
    borderRadius: 4,
    padding: 10,
    fontSize: 14,
    backgroundColor: '#fff',
  },
  textarea: {
    height: 100,
    textAlignVertical: 'top',
  },
  footer: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    padding: 16,
    borderTopWidth: 1,
    borderTopColor: '#e0e0e0',
    backgroundColor: '#f5f5f5',
  },
  button: {
    flex: 1,
    padding: 12,
    borderRadius: 4,
    alignItems: 'center',
    marginHorizontal: 4,
  },
  cancelButton: {
    backgroundColor: '#f0f0f0',
    borderWidth: 1,
    borderColor: '#ccc',
  },
  saveButton: {
    backgroundColor: '#007AFF',
  },
  disabledButton: {
    backgroundColor: '#ccc',
  },
  buttonText: {
    fontSize: 16,
    fontWeight: '600',
    color: '#333',
  },
  saveButtonText: {
    color: '#fff',
  },
});
