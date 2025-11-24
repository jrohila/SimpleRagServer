import React from 'react';
import { View, Text, TextInput, Switch } from 'react-native';
import { List, Divider } from 'react-native-paper';
import { Picker } from '@react-native-picker/picker';
import styles from '../styles/ChatsStyles';

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
  return (
    <View style={styles.form}>
      <List.Section>
        <View style={styles.accordionContainer}>
          <List.Accordion
            title="Basic"
            left={(props: any) => <List.Icon {...props} icon="information" />}
            expanded={expandedAccordion === 'basic'}
            onPress={() => onAccordionChange(expandedAccordion === 'basic' ? null : 'basic')}
            style={styles.accordionTitle}
          >
            <View style={styles.accordionContent}>
              <Text style={styles.label}>Public Name</Text>
              <TextInput
                style={styles.input}
                value={data.publicName}
                onChangeText={(value) => onFieldChange('publicName', value)}
                placeholder="Public Name"
                editable={!disabled}
              />
              <Text style={styles.label}>Internal Name</Text>
              <TextInput
                style={styles.input}
                value={data.internalName}
                onChangeText={(value) => onFieldChange('internalName', value)}
                placeholder="Internal Name"
                editable={!disabled}
              />
              <Text style={styles.label}>Internal Description</Text>
              <TextInput
                style={styles.input}
                value={data.internalDescription}
                onChangeText={(value) => onFieldChange('internalDescription', value)}
                placeholder="Internal Description"
                editable={!disabled}
              />
              <Text style={styles.label}>Default Language</Text>
              <TextInput
                style={styles.input}
                value={data.defaultLanguage}
                onChangeText={(value) => onFieldChange('defaultLanguage', value)}
                placeholder="Default Language"
                editable={!disabled}
              />
              <Text style={styles.label}>Default Collection</Text>
              <View style={styles.pickerWrapper}>
                <Picker
                  selectedValue={data.defaultCollectionId}
                  onValueChange={(itemValue) => !disabled && onFieldChange('defaultCollectionId', itemValue)}
                  enabled={!disabled}
                  style={styles.picker}
                >
                  <Picker.Item label="Select a collection..." value="" />
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
            title="Prompts"
            left={(props: any) => <List.Icon {...props} icon="file-document" />}
            expanded={expandedAccordion === 'prompts'}
            onPress={() => onAccordionChange(expandedAccordion === 'prompts' ? null : 'prompts')}
            style={styles.accordionTitle}
          >
            <View style={styles.accordionContent}>
              <Text style={styles.label}>Welcome Message</Text>
              <TextInput
                style={[styles.input, styles.textarea]}
                value={data.welcomeMessage}
                onChangeText={(value) => onFieldChange('welcomeMessage', value)}
                placeholder="Welcome Message"
                editable={!disabled}
                multiline
                numberOfLines={5}
              />
              <Text style={styles.label}>Default System Prompt</Text>
              <TextInput
                style={[styles.input, styles.textarea]}
                value={data.defaultSystemPrompt}
                onChangeText={(value) => onFieldChange('defaultSystemPrompt', value)}
                placeholder="Default System Prompt"
                editable={!disabled}
                multiline
                numberOfLines={5}
              />
              <Text style={styles.label}>Default System Prompt Append</Text>
              <TextInput
                style={[styles.input, styles.textarea]}
                value={data.defaultSystemPromptAppend}
                onChangeText={(value) => onFieldChange('defaultSystemPromptAppend', value)}
                placeholder="Default System Prompt Append"
                editable={!disabled}
                multiline
                numberOfLines={5}
              />
              <Text style={styles.label}>Default Out of Scope Message</Text>
              <TextInput
                style={[styles.input, styles.textarea]}
                value={data.defaultOutOfScopeMessage}
                onChangeText={(value) => onFieldChange('defaultOutOfScopeMessage', value)}
                placeholder="Default Out of Scope Message"
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
            title="Advanced Prompts"
            left={(props: any) => <List.Icon {...props} icon="cog" />}
            expanded={expandedAccordion === 'advanced'}
            onPress={() => onAccordionChange(expandedAccordion === 'advanced' ? null : 'advanced')}
            style={styles.accordionTitle}
          >
            <View style={styles.accordionContent}>
              <Text style={styles.label}>Default Context Prompt</Text>
              <TextInput
                style={[styles.input, styles.textarea]}
                value={data.defaultContextPrompt}
                onChangeText={(value) => onFieldChange('defaultContextPrompt', value)}
                placeholder="Default Context Prompt"
                editable={!disabled}
                multiline
                numberOfLines={5}
              />
              <Text style={styles.label}>Default Memory Prompt</Text>
              <TextInput
                style={[styles.input, styles.textarea]}
                value={data.defaultMemoryPrompt}
                onChangeText={(value) => onFieldChange('defaultMemoryPrompt', value)}
                placeholder="Default Memory Prompt"
                editable={!disabled}
                multiline
                numberOfLines={5}
              />
              <Text style={styles.label}>Default Extractor Prompt</Text>
              <TextInput
                style={[styles.input, styles.textarea]}
                value={data.defaultExtractorPrompt}
                onChangeText={(value) => onFieldChange('defaultExtractorPrompt', value)}
                placeholder="Default Extractor Prompt"
                editable={!disabled}
                multiline
                numberOfLines={5}
              />
              <View style={{ height: 10 }} />
              <Text style={styles.label}>Use User Prompt Rewriting</Text>
              <View style={styles.checkboxRow}>
                <Switch
                  value={data.useUserPromptRewriting}
                  onValueChange={() => {
                    if (!disabled) onFieldChange('useUserPromptRewriting', !data.useUserPromptRewriting);
                  }}
                  disabled={disabled}
                />
              </View>
              <Text style={styles.label}>User Prompt Rewriting Prompt</Text>
              <TextInput
                style={[styles.input, styles.textarea]}
                value={data.userPromptRewritingPrompt}
                onChangeText={(value) => onFieldChange('userPromptRewritingPrompt', value)}
                placeholder="User Prompt Rewriting Prompt"
                editable={!disabled}
                multiline
                numberOfLines={5}
              />
              <View style={{ height: 10 }} />
              <Text style={styles.label}>Override System Message</Text>
              <View style={styles.checkboxRow}>
                <Switch
                  value={data.overrideSystemMessage}
                  onValueChange={() => {
                    if (!disabled) onFieldChange('overrideSystemMessage', !data.overrideSystemMessage);
                  }}
                  disabled={disabled}
                />
              </View>
              <Text style={styles.label}>Override Assistant Message</Text>
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
