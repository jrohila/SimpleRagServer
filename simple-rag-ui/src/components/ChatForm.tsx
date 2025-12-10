import React from 'react';
import { View, Text, TextInput, Switch } from 'react-native';
import { List, Divider } from 'react-native-paper';
import { MaterialCommunityIcons } from './Icons';
import { Picker } from '@react-native-picker/picker';
import styles from '../styles/ChatsStyles';
import { useTranslation } from 'react-i18next';
import { searchHfModels } from '../api/hfModels';
import { ModelComboBox } from './ModelComboBox';

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

export interface Collection {
  id: string;
  name: string;
  [key: string]: any;
}

export interface ChatFormData {
  publicName: string;
  internalName: string;
  internalDescription: string;
  defaultLanguage: string;
  defaultCollectionId: string;
  welcomeMessage: string;
  defaultSystemPrompt: string;
  defaultSystemPromptAppend: string;
  defaultOutOfScopeMessage: string;
  defaultContextPrompt: string;
  defaultMemoryPrompt: string;
  defaultExtractorPrompt: string;
  overrideSystemMessage: boolean;
  overrideAssistantMessage: boolean;
  useUserPromptRewriting: boolean;
  userPromptRewritingPrompt: string;
  webGpuConfig?: WebGpuConfig | null;
}

export interface ChatFormProps {
  // Field values
  data: ChatFormData;
  
  // Change handlers
  onFieldChange: (field: keyof ChatFormData, value: string | boolean | WebGpuConfig | null) => void;
  
  // Collections data
  collections: Collection[];
  
  // UI state
  disabled?: boolean;
  expandedAccordion: string | null;
  onAccordionChange: (accordion: string | null) => void;
  
  // Slot-based composition - allow injecting custom content per section
  renderAfterBasic?: () => React.ReactNode;
  renderAfterPrompts?: () => React.ReactNode;
  renderAfterAdvanced?: () => React.ReactNode;
}

export const ChatForm: React.FC<ChatFormProps> = ({
  data,
  onFieldChange,
  collections,
  disabled = false,
  expandedAccordion,
  onAccordionChange,
  renderAfterBasic,
  renderAfterPrompts,
  renderAfterAdvanced,
}) => {
  const { t } = useTranslation();
  
  // HF Model search state
  const [hfModels, setHfModels] = React.useState<any[]>([]);
  const [hfSearchSize, setHfSearchSize] = React.useState('');
  const [hfLoading, setHfLoading] = React.useState(false);
  const searchTimerRef = React.useRef<any>(null);

  // Load initial models when accordion opens
  React.useEffect(() => {
    if (expandedAccordion === 'webgpu' && hfModels.length === 0) {
      handleHfSearch('');
    }
  }, [expandedAccordion]);

  // Debounced search by size
  React.useEffect(() => {
    if (expandedAccordion !== 'webgpu') return;
    
    if (searchTimerRef.current) {
      clearTimeout(searchTimerRef.current);
    }

    searchTimerRef.current = setTimeout(() => {
      handleHfSearch('');
    }, 500);

    return () => {
      if (searchTimerRef.current) {
        clearTimeout(searchTimerRef.current);
      }
    };
  }, [hfSearchSize]);

  const handleHfSearch = async (nameQuery: string) => {
    setHfLoading(true);
    try {
      const sizeMb = hfSearchSize ? parseFloat(hfSearchSize) : null;
      const response = await searchHfModels(nameQuery, sizeMb, 50);
      setHfModels(response.data || []);
    } catch (error) {
      console.error('Failed to search HF models:', error);
      setHfModels([]);
    } finally {
      setHfLoading(false);
    }
  };

  return (
    <View style={styles.form}>
      <List.Section>
        <View style={styles.accordionContainer}>
          <List.Accordion
            title={t('sections.basic')}
            left={(props: any) => (
              <List.Icon {...props} icon={() => <MaterialCommunityIcons name="information" size={props.size} color={props.color} />} />
            )}
            right={(props: any) => (
              <MaterialCommunityIcons name={expandedAccordion === 'basic' ? 'chevron-up' : 'chevron-down'} size={props.size} color={props.color} />
            )}
            expanded={expandedAccordion === 'basic'}
            onPress={() => onAccordionChange(expandedAccordion === 'basic' ? null : 'basic')}
            style={styles.accordionTitle}
          >
            <View style={styles.accordionContent}>
              <Text style={styles.label}>{t('basic.publicName')}</Text>
              <TextInput
                style={[styles.input, disabled && styles.inputDisabled]}
                value={data.publicName}
                onChangeText={(value: string) => onFieldChange('publicName', value)}
                placeholder={t('basic.publicName')}
                editable={!disabled}
              />
              <Text style={styles.label}>{t('basic.internalName')}</Text>
              <TextInput
                style={[styles.input, disabled && styles.inputDisabled]}
                value={data.internalName}
                onChangeText={(value: string) => onFieldChange('internalName', value)}
                placeholder={t('basic.internalName')}
                editable={!disabled}
              />
              <Text style={styles.label}>{t('basic.internalDescription')}</Text>
              <TextInput
                style={[styles.input, disabled && styles.inputDisabled]}
                value={data.internalDescription}
                onChangeText={(value: string) => onFieldChange('internalDescription', value)}
                placeholder={t('basic.internalDescription')}
                editable={!disabled}
              />
              <Text style={styles.label}>{t('basic.defaultLanguage')}</Text>
              <TextInput
                style={[styles.input, disabled && styles.inputDisabled]}
                value={data.defaultLanguage}
                onChangeText={(value: string) => onFieldChange('defaultLanguage', value)}
                placeholder={t('basic.defaultLanguage')}
                editable={!disabled}
              />
              <Text style={styles.label}>{t('basic.defaultCollection')}</Text>
              <View style={[styles.pickerWrapper, disabled && styles.pickerWrapperDisabled]}>
                <Picker
                  selectedValue={data.defaultCollectionId}
                  onValueChange={(itemValue: string) => !disabled && onFieldChange('defaultCollectionId', itemValue)}
                  enabled={!disabled}
                  style={styles.picker}
                >
                  <Picker.Item label={t('basic.selectCollection')} value="" />
                  {collections.map((col) => (
                    <Picker.Item key={col.id} label={col.name} value={col.id} />
                  ))}
                </Picker>
              </View>
              {renderAfterBasic && renderAfterBasic()}
            </View>
          </List.Accordion>
        </View>

        <Divider />

        <View style={styles.accordionContainer}>
          <List.Accordion
            title={t('sections.prompts')}
            left={(props: any) => (
              <List.Icon {...props} icon={() => <MaterialCommunityIcons name="file-document" size={props.size} color={props.color} />} />
            )}
            right={(props: any) => (
              <MaterialCommunityIcons name={expandedAccordion === 'prompts' ? 'chevron-up' : 'chevron-down'} size={props.size} color={props.color} />
            )}
            expanded={expandedAccordion === 'prompts'}
            onPress={() => onAccordionChange(expandedAccordion === 'prompts' ? null : 'prompts')}
            style={styles.accordionTitle}
          >
            <View style={styles.accordionContent}>
              <Text style={styles.label}>{t('labels.welcomeMessage', 'Welcome Message')}</Text>
              <TextInput
                style={[styles.input, styles.textarea, disabled && styles.textareaDisabled]}
                value={data.welcomeMessage}
                onChangeText={(value: string) => onFieldChange('welcomeMessage', value)}
                placeholder={t('labels.welcomeMessage', 'Welcome Message')}
                editable={!disabled}
                multiline
                numberOfLines={5}
              />
              <Text style={styles.label}>{t('labels.systemPrompt', 'Default System Prompt')}</Text>
              <TextInput
                style={[styles.input, styles.textarea, disabled && styles.textareaDisabled]}
                value={data.defaultSystemPrompt}
                onChangeText={(value: string) => onFieldChange('defaultSystemPrompt', value)}
                placeholder={t('labels.systemPrompt', 'Default System Prompt')}
                editable={!disabled}
                multiline
                numberOfLines={5}
              />
              <Text style={styles.label}>{t('labels.systemPromptAppend', 'Default System Prompt Append')}</Text>
              <TextInput
                style={[styles.input, styles.textarea, disabled && styles.textareaDisabled]}
                value={data.defaultSystemPromptAppend}
                onChangeText={(value: string) => onFieldChange('defaultSystemPromptAppend', value)}
                placeholder={t('labels.systemPromptAppend', 'Default System Prompt Append')}
                editable={!disabled}
                multiline
                numberOfLines={5}
              />
              <Text style={styles.label}>{t('labels.outOfScopeMessage', 'Default Out of Scope Message')}</Text>
              <TextInput
                style={[styles.input, styles.textarea, disabled && styles.textareaDisabled]}
                value={data.defaultOutOfScopeMessage}
                onChangeText={(value: string) => onFieldChange('defaultOutOfScopeMessage', value)}
                placeholder={t('labels.outOfScopeMessage', 'Default Out of Scope Message')}
                editable={!disabled}
                multiline
                numberOfLines={5}
              />
              {renderAfterPrompts && renderAfterPrompts()}
            </View>
          </List.Accordion>
        </View>

        <Divider />

        <View style={styles.accordionContainer}>
          <List.Accordion
            title={t('sections.advanced')}
            left={(props: any) => (
              <List.Icon {...props} icon={() => <MaterialCommunityIcons name="cog" size={props.size} color={props.color} />} />
            )}
            right={(props: any) => (
              <MaterialCommunityIcons name={expandedAccordion === 'advanced' ? 'chevron-up' : 'chevron-down'} size={props.size} color={props.color} />
            )}
            expanded={expandedAccordion === 'advanced'}
            onPress={() => onAccordionChange(expandedAccordion === 'advanced' ? null : 'advanced')}
            style={styles.accordionTitle}
          >
            <View style={styles.accordionContent}>
              <Text style={styles.label}>{t('advanced.contextPrompt')}</Text>
              <TextInput
                style={[styles.input, styles.textarea, disabled && styles.textareaDisabled]}
                value={data.defaultContextPrompt}
                onChangeText={(value: string) => onFieldChange('defaultContextPrompt', value)}
                placeholder={t('advanced.contextPrompt')}
                editable={!disabled}
                multiline
                numberOfLines={5}
              />
              <Text style={styles.label}>{t('advanced.memoryPrompt')}</Text>
              <TextInput
                style={[styles.input, styles.textarea, disabled && styles.textareaDisabled]}
                value={data.defaultMemoryPrompt}
                onChangeText={(value: string) => onFieldChange('defaultMemoryPrompt', value)}
                placeholder={t('advanced.memoryPrompt')}
                editable={!disabled}
                multiline
                numberOfLines={5}
              />
              <Text style={styles.label}>{t('advanced.extractorPrompt')}</Text>
              <TextInput
                style={[styles.input, styles.textarea, disabled && styles.textareaDisabled]}
                value={data.defaultExtractorPrompt}
                onChangeText={(value: string) => onFieldChange('defaultExtractorPrompt', value)}
                placeholder={t('advanced.extractorPrompt')}
                editable={!disabled}
                multiline
                numberOfLines={5}
              />
              <View style={styles.spacer} />
              <Text style={styles.label}>{t('advanced.useRewriting')}</Text>
              <View style={styles.checkboxRow}>
                <Switch
                  value={data.useUserPromptRewriting}
                  onValueChange={() => {
                    if (!disabled) onFieldChange('useUserPromptRewriting', !data.useUserPromptRewriting);
                  }}
                  disabled={disabled}
                />
              </View>
              <Text style={styles.label}>{t('advanced.userRewrite')}</Text>
              <TextInput
                style={[styles.input, styles.textarea, disabled && styles.textareaDisabled]}
                value={data.userPromptRewritingPrompt}
                onChangeText={(value: string) => onFieldChange('userPromptRewritingPrompt', value)}
                placeholder={t('advanced.userRewrite')}
                editable={!disabled}
                multiline
                numberOfLines={5}
              />
              <View style={styles.spacer} />
              <Text style={styles.label}>{t('advanced.overrideSystem')}</Text>
              <View style={styles.checkboxRow}>
                <Switch
                  value={data.overrideSystemMessage}
                  onValueChange={() => {
                    if (!disabled) onFieldChange('overrideSystemMessage', !data.overrideSystemMessage);
                  }}
                  disabled={disabled}
                />
              </View>
              <Text style={styles.label}>{t('advanced.overrideAssistant')}</Text>
              <View style={styles.checkboxRow}>
                <Switch
                  value={data.overrideAssistantMessage}
                  onValueChange={() => {
                    if (!disabled) onFieldChange('overrideAssistantMessage', !data.overrideAssistantMessage);
                  }}
                  disabled={disabled}
                />
              </View>
              {renderAfterAdvanced && renderAfterAdvanced()}
            </View>
          </List.Accordion>
        </View>

        <Divider />

        <View style={styles.accordionContainer}>
          <List.Accordion
            title={t('sections.webgpu', 'WebGPU Configuration')}
            left={(props: any) => (
              <List.Icon {...props} icon={() => <MaterialCommunityIcons name="gpu" size={props.size} color={props.color} />} />
            )}
            right={(props: any) => (
              <MaterialCommunityIcons name={expandedAccordion === 'webgpu' ? 'chevron-up' : 'chevron-down'} size={props.size} color={props.color} />
            )}
            expanded={expandedAccordion === 'webgpu'}
            onPress={() => onAccordionChange(expandedAccordion === 'webgpu' ? null : 'webgpu')}
            style={styles.accordionTitle}
          >
            <View style={styles.accordionContent}>
              {/* Max Size Filter */}
              <Text style={styles.label}>{t('webgpu.maxSizeMb', 'Max Size (MB)')}</Text>
              <TextInput
                style={[styles.input, disabled && styles.inputDisabled]}
                value={hfSearchSize}
                onChangeText={setHfSearchSize}
                placeholder={t('webgpu.maxSizePlaceholder', 'e.g., 1000')}
                keyboardType="numeric"
                editable={!disabled}
              />

              {/* Model ComboBox - Search and Select */}
              <ModelComboBox
                value={data.webGpuConfig?.modelId || ''}
                onChange={(modelId) => onFieldChange('webGpuConfig', { ...data.webGpuConfig, modelId })}
                options={hfModels}
                placeholder={t('webgpu.searchModelPlaceholder', 'Search and select model...')}
                disabled={disabled}
                loading={hfLoading}
                onSearchChange={(searchText) => handleHfSearch(searchText)}
                label={t('webgpu.modelId', 'Model')}
              />

              <View style={styles.spacer} />

              {/* System Prompt */}
              <View style={styles.checkboxRow}>
                <Text style={styles.label}>{t('webgpu.overrideSystemPrompt', 'Override System Prompt')}</Text>
                <Switch
                  value={data.webGpuConfig?.overrideParentSystemPrompt || false}
                  onValueChange={() => {
                    if (!disabled) {
                      onFieldChange('webGpuConfig', {
                        ...data.webGpuConfig,
                        overrideParentSystemPrompt: !data.webGpuConfig?.overrideParentSystemPrompt
                      });
                    }
                  }}
                  disabled={disabled}
                />
              </View>
              <TextInput
                style={[styles.input, styles.textarea, (disabled || !data.webGpuConfig?.overrideParentSystemPrompt) && styles.textareaDisabled]}
                value={data.webGpuConfig?.systemPrompt || ''}
                onChangeText={(value: string) => onFieldChange('webGpuConfig', { ...data.webGpuConfig, systemPrompt: value })}
                placeholder={t('webgpu.systemPromptPlaceholder', 'Enter system prompt...')}
                editable={!disabled && (data.webGpuConfig?.overrideParentSystemPrompt || false)}
                multiline
                numberOfLines={5}
              />

              <View style={styles.spacer} />

              {/* System Prompt Append */}
              <View style={styles.checkboxRow}>
                <Text style={styles.label}>{t('webgpu.overrideSystemPromptAppend', 'Override System Prompt Append')}</Text>
                <Switch
                  value={data.webGpuConfig?.overrideParentSystemPromptAppend || false}
                  onValueChange={() => {
                    if (!disabled) {
                      onFieldChange('webGpuConfig', {
                        ...data.webGpuConfig,
                        overrideParentSystemPromptAppend: !data.webGpuConfig?.overrideParentSystemPromptAppend
                      });
                    }
                  }}
                  disabled={disabled}
                />
              </View>
              <TextInput
                style={[styles.input, styles.textarea, (disabled || !data.webGpuConfig?.overrideParentSystemPromptAppend) && styles.textareaDisabled]}
                value={data.webGpuConfig?.systemPromptAppend || ''}
                onChangeText={(value: string) => onFieldChange('webGpuConfig', { ...data.webGpuConfig, systemPromptAppend: value })}
                placeholder={t('webgpu.systemPromptAppendPlaceholder', 'Enter system prompt append...')}
                editable={!disabled && (data.webGpuConfig?.overrideParentSystemPromptAppend || false)}
                multiline
                numberOfLines={5}
              />

              <View style={styles.spacer} />

              {/* Context Prompt */}
              <View style={styles.checkboxRow}>
                <Text style={styles.label}>{t('webgpu.overrideContextPrompt', 'Override Context Prompt')}</Text>
                <Switch
                  value={data.webGpuConfig?.overrideParentContextPrompt || false}
                  onValueChange={() => {
                    if (!disabled) {
                      onFieldChange('webGpuConfig', {
                        ...data.webGpuConfig,
                        overrideParentContextPrompt: !data.webGpuConfig?.overrideParentContextPrompt
                      });
                    }
                  }}
                  disabled={disabled}
                />
              </View>
              <TextInput
                style={[styles.input, styles.textarea, (disabled || !data.webGpuConfig?.overrideParentContextPrompt) && styles.textareaDisabled]}
                value={data.webGpuConfig?.contextPrompt || ''}
                onChangeText={(value: string) => onFieldChange('webGpuConfig', { ...data.webGpuConfig, contextPrompt: value })}
                placeholder={t('webgpu.contextPromptPlaceholder', 'Enter context prompt...')}
                editable={!disabled && (data.webGpuConfig?.overrideParentContextPrompt || false)}
                multiline
                numberOfLines={5}
              />

              <View style={styles.spacer} />

              {/* Memory Prompt */}
              <View style={styles.checkboxRow}>
                <Text style={styles.label}>{t('webgpu.overrideMemoryPrompt', 'Override Memory Prompt')}</Text>
                <Switch
                  value={data.webGpuConfig?.overrideParentMemoryPrompt || false}
                  onValueChange={() => {
                    if (!disabled) {
                      onFieldChange('webGpuConfig', {
                        ...data.webGpuConfig,
                        overrideParentMemoryPrompt: !data.webGpuConfig?.overrideParentMemoryPrompt
                      });
                    }
                  }}
                  disabled={disabled}
                />
              </View>
              <TextInput
                style={[styles.input, styles.textarea, (disabled || !data.webGpuConfig?.overrideParentMemoryPrompt) && styles.textareaDisabled]}
                value={data.webGpuConfig?.memoryPrompt || ''}
                onChangeText={(value: string) => onFieldChange('webGpuConfig', { ...data.webGpuConfig, memoryPrompt: value })}
                placeholder={t('webgpu.memoryPromptPlaceholder', 'Enter memory prompt...')}
                editable={!disabled && (data.webGpuConfig?.overrideParentMemoryPrompt || false)}
                multiline
                numberOfLines={5}
              />

              <View style={styles.spacer} />

              {/* Extractor Prompt */}
              <View style={styles.checkboxRow}>
                <Text style={styles.label}>{t('webgpu.overrideExtractorPrompt', 'Override Extractor Prompt')}</Text>
                <Switch
                  value={data.webGpuConfig?.overrideParentExtractorPrompt || false}
                  onValueChange={() => {
                    if (!disabled) {
                      onFieldChange('webGpuConfig', {
                        ...data.webGpuConfig,
                        overrideParentExtractorPrompt: !data.webGpuConfig?.overrideParentExtractorPrompt
                      });
                    }
                  }}
                  disabled={disabled}
                />
              </View>
              <TextInput
                style={[styles.input, styles.textarea, (disabled || !data.webGpuConfig?.overrideParentExtractorPrompt) && styles.textareaDisabled]}
                value={data.webGpuConfig?.extractorPrompt || ''}
                onChangeText={(value: string) => onFieldChange('webGpuConfig', { ...data.webGpuConfig, extractorPrompt: value })}
                placeholder={t('webgpu.extractorPromptPlaceholder', 'Enter extractor prompt...')}
                editable={!disabled && (data.webGpuConfig?.overrideParentExtractorPrompt || false)}
                multiline
                numberOfLines={5}
              />

              <View style={styles.spacer} />

              {/* Prompt Rewriting */}
              <View style={styles.checkboxRow}>
                <Text style={styles.label}>{t('webgpu.usePromptRewriting', 'Use Prompt Rewriting')}</Text>
                <Switch
                  value={data.webGpuConfig?.usePromptRewriting || false}
                  onValueChange={() => {
                    if (!disabled) {
                      onFieldChange('webGpuConfig', {
                        ...data.webGpuConfig,
                        usePromptRewriting: !data.webGpuConfig?.usePromptRewriting
                      });
                    }
                  }}
                  disabled={disabled}
                />
              </View>

              <View style={styles.checkboxRow}>
                <Text style={styles.label}>{t('webgpu.overrideUserPromptRewriting', 'Override User Prompt Rewriting')}</Text>
                <Switch
                  value={data.webGpuConfig?.overrideParentUserPromptRewriting || false}
                  onValueChange={() => {
                    if (!disabled) {
                      onFieldChange('webGpuConfig', {
                        ...data.webGpuConfig,
                        overrideParentUserPromptRewriting: !data.webGpuConfig?.overrideParentUserPromptRewriting
                      });
                    }
                  }}
                  disabled={disabled}
                />
              </View>
              <TextInput
                style={[styles.input, styles.textarea, (disabled || !data.webGpuConfig?.overrideParentUserPromptRewriting) && styles.textareaDisabled]}
                value={data.webGpuConfig?.userPromptRewritingPrompt || ''}
                onChangeText={(value: string) => onFieldChange('webGpuConfig', { ...data.webGpuConfig, userPromptRewritingPrompt: value })}
                placeholder={t('webgpu.userPromptRewritingPlaceholder', 'Enter user prompt rewriting prompt...')}
                editable={!disabled && (data.webGpuConfig?.overrideParentUserPromptRewriting || false)}
                multiline
                numberOfLines={5}
              />
            </View>
          </List.Accordion>
        </View>
      </List.Section>
    </View>
  );
};
