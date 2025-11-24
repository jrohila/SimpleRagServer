import React from 'react';
import { View, Text, TextInput, Switch } from 'react-native';
import { List, Divider } from 'react-native-paper';
import { Picker } from '@react-native-picker/picker';
import styles from '../styles/ChatsStyles';
import { useTranslation } from 'react-i18next';

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
}

export interface ChatFormProps {
  // Field values
  data: ChatFormData;
  
  // Change handlers
  onFieldChange: (field: keyof ChatFormData, value: string | boolean) => void;
  
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
  return (
    <View style={styles.form}>
      <List.Section>
        <View style={styles.accordionContainer}>
          <List.Accordion
            title={t('sections.basic')}
            left={(props: any) => <List.Icon {...props} icon="information" />}
            expanded={expandedAccordion === 'basic'}
            onPress={() => onAccordionChange(expandedAccordion === 'basic' ? null : 'basic')}
            style={styles.accordionTitle}
          >
            <View style={styles.accordionContent}>
              <Text style={styles.label}>{t('basic.publicName')}</Text>
              <TextInput
                style={styles.input}
                value={data.publicName}
                onChangeText={(value) => onFieldChange('publicName', value)}
                placeholder={t('basic.publicName')}
                editable={!disabled}
              />
              <Text style={styles.label}>{t('basic.internalName')}</Text>
              <TextInput
                style={styles.input}
                value={data.internalName}
                onChangeText={(value) => onFieldChange('internalName', value)}
                placeholder={t('basic.internalName')}
                editable={!disabled}
              />
              <Text style={styles.label}>{t('basic.internalDescription')}</Text>
              <TextInput
                style={styles.input}
                value={data.internalDescription}
                onChangeText={(value) => onFieldChange('internalDescription', value)}
                placeholder={t('basic.internalDescription')}
                editable={!disabled}
              />
              <Text style={styles.label}>{t('basic.defaultLanguage')}</Text>
              <TextInput
                style={styles.input}
                value={data.defaultLanguage}
                onChangeText={(value) => onFieldChange('defaultLanguage', value)}
                placeholder={t('basic.defaultLanguage')}
                editable={!disabled}
              />
              <Text style={styles.label}>{t('basic.defaultCollection')}</Text>
              <View style={styles.pickerWrapper}>
                <Picker
                  selectedValue={data.defaultCollectionId}
                  onValueChange={(itemValue) => !disabled && onFieldChange('defaultCollectionId', itemValue)}
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
            left={(props: any) => <List.Icon {...props} icon="file-document" />}
            expanded={expandedAccordion === 'prompts'}
            onPress={() => onAccordionChange(expandedAccordion === 'prompts' ? null : 'prompts')}
            style={styles.accordionTitle}
          >
            <View style={styles.accordionContent}>
              <Text style={styles.label}>{t('labels.welcomeMessage', 'Welcome Message')}</Text>
              <TextInput
                style={[styles.input, styles.textarea]}
                value={data.welcomeMessage}
                onChangeText={(value) => onFieldChange('welcomeMessage', value)}
                placeholder={t('labels.welcomeMessage', 'Welcome Message')}
                editable={!disabled}
                multiline
                numberOfLines={5}
              />
              <Text style={styles.label}>{t('labels.systemPrompt', 'Default System Prompt')}</Text>
              <TextInput
                style={[styles.input, styles.textarea]}
                value={data.defaultSystemPrompt}
                onChangeText={(value) => onFieldChange('defaultSystemPrompt', value)}
                placeholder={t('labels.systemPrompt', 'Default System Prompt')}
                editable={!disabled}
                multiline
                numberOfLines={5}
              />
              <Text style={styles.label}>{t('labels.systemPromptAppend', 'Default System Prompt Append')}</Text>
              <TextInput
                style={[styles.input, styles.textarea]}
                value={data.defaultSystemPromptAppend}
                onChangeText={(value) => onFieldChange('defaultSystemPromptAppend', value)}
                placeholder={t('labels.systemPromptAppend', 'Default System Prompt Append')}
                editable={!disabled}
                multiline
                numberOfLines={5}
              />
              <Text style={styles.label}>{t('labels.outOfScopeMessage', 'Default Out of Scope Message')}</Text>
              <TextInput
                style={[styles.input, styles.textarea]}
                value={data.defaultOutOfScopeMessage}
                onChangeText={(value) => onFieldChange('defaultOutOfScopeMessage', value)}
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
            left={(props: any) => <List.Icon {...props} icon="cog" />}
            expanded={expandedAccordion === 'advanced'}
            onPress={() => onAccordionChange(expandedAccordion === 'advanced' ? null : 'advanced')}
            style={styles.accordionTitle}
          >
            <View style={styles.accordionContent}>
              <Text style={styles.label}>{t('advanced.contextPrompt')}</Text>
              <TextInput
                style={[styles.input, styles.textarea]}
                value={data.defaultContextPrompt}
                onChangeText={(value) => onFieldChange('defaultContextPrompt', value)}
                placeholder={t('advanced.contextPrompt')}
                editable={!disabled}
                multiline
                numberOfLines={5}
              />
              <Text style={styles.label}>{t('advanced.memoryPrompt')}</Text>
              <TextInput
                style={[styles.input, styles.textarea]}
                value={data.defaultMemoryPrompt}
                onChangeText={(value) => onFieldChange('defaultMemoryPrompt', value)}
                placeholder={t('advanced.memoryPrompt')}
                editable={!disabled}
                multiline
                numberOfLines={5}
              />
              <Text style={styles.label}>{t('advanced.extractorPrompt')}</Text>
              <TextInput
                style={[styles.input, styles.textarea]}
                value={data.defaultExtractorPrompt}
                onChangeText={(value) => onFieldChange('defaultExtractorPrompt', value)}
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
                style={[styles.input, styles.textarea]}
                value={data.userPromptRewritingPrompt}
                onChangeText={(value) => onFieldChange('userPromptRewritingPrompt', value)}
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
      </List.Section>
    </View>
  );
};
