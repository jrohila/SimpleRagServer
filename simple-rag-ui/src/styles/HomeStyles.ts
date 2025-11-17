import { StyleSheet, Platform } from 'react-native';

export const HomeStyles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  chatContainer: {
    flex: 1,
  },
  messagesContainer: {
    paddingHorizontal: 12,
    paddingBottom: 12,
    backgroundColor: '#f5f5f5',
  },
  input: {
    color: '#000',
    backgroundColor: '#fff',
    borderRadius: 20,
    paddingHorizontal: 16,
    paddingVertical: 10,
    margin: 8,
    fontSize: 16,
    width: '100%',
    boxSizing: 'border-box',
    borderWidth: 1,
    borderColor: '#ddd',
  },
  sendContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    marginRight: 8,
    height: undefined,
  },
  dropdownContainer: {
    backgroundColor: '#fff',
    paddingHorizontal: 16,
    paddingVertical: 14,
    borderBottomWidth: 1,
    borderBottomColor: '#e0e0e0',
    flexDirection: 'row',
    alignItems: 'center',
    gap: 12,
  },
  pickerWrapper: {
    flex: 1,
    backgroundColor: '#fff',
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#ddd',
    overflow: 'hidden',
    minHeight: 48,
  },
  picker: {
    color: '#000',
    backgroundColor: 'transparent',
    borderRadius: 8,
    fontSize: 16,
    height: 48,
  },
  clearButton: {
    backgroundColor: '#ff6b6b',
    paddingHorizontal: 16,
    paddingVertical: 12,
    borderRadius: 8,
    justifyContent: 'center',
    alignItems: 'center',
    minWidth: 70,
  },
  clearButtonDisabled: {
    backgroundColor: '#ccc',
    opacity: 0.5,
  },
  clearButtonText: {
    color: '#fff',
    fontSize: 14,
    fontWeight: '600',
  },
  usernameContainer: {
    marginBottom: 4,
    marginLeft: 10,
  },
  usernameText: {
    color: '#666',
    fontSize: 12,
    fontWeight: '600',
  },
  loadingContainer: {
    padding: 8,
    flexDirection: 'row',
    alignItems: 'center',
  },
  loadingBarsContainer: {
    marginLeft: 8,
  },
  loadingBar1: {
    width: 40,
    height: 8,
    backgroundColor: '#e0e0e0',
    borderRadius: 4,
    marginBottom: 4,
  },
  loadingBar2: {
    width: 60,
    height: 8,
    backgroundColor: '#e0e0e0',
    borderRadius: 4,
  },
  messageTextContainer: {
    padding: 8,
  },
  messageContainer: {
    marginLeft: 0,
    marginRight: 0,
    paddingLeft: 0,
    paddingRight: 0,
    alignItems: 'flex-start',
  },
});

export const bubbleStyles = {
  wrapperStyle: {
    left: {
      backgroundColor: '#e3f2fd',
      borderWidth: 1,
      borderColor: '#2196f3',
      marginLeft: 0,
      marginRight: 0,
    },
    right: {
      backgroundColor: '#e3f2fd',
      borderWidth: 1,
      borderColor: '#2196f3',
      marginLeft: 0,
      marginRight: 0,
    },
  },
  textStyle: {
    left: { color: '#222' },
    right: { color: '#222' },
  },
  containerToNextStyle: {
    left: { marginBottom: 20, alignSelf: 'flex-start' as const },
    right: { marginBottom: 20, alignSelf: 'flex-start' as const },
  },
  containerToPreviousStyle: {
    left: { marginBottom: 20, alignSelf: 'flex-start' as const },
    right: { marginBottom: 20, alignSelf: 'flex-start' as const },
  },
  containerStyle: {
    left: { marginLeft: 0, marginRight: 0, alignItems: 'flex-start' as const },
    right: { marginLeft: 0, marginRight: 0, alignItems: 'flex-start' as const },
  },
};

export const userBubbleStyles = {
  wrapperStyle: {
    left: {
      backgroundColor: '#fff',
      borderWidth: 1,
      borderColor: '#e0e0e0',
      marginLeft: 0,
      marginRight: 0,
    },
    right: {
      backgroundColor: '#fff',
      borderWidth: 1,
      borderColor: '#e0e0e0',
      marginLeft: 0,
      marginRight: 0,
    },
  },
  textStyle: {
    left: { color: '#222' },
    right: { color: '#222' },
  },
  containerToNextStyle: {
    left: { marginBottom: 20, alignSelf: 'flex-start' as const },
    right: { marginBottom: 20, alignSelf: 'flex-start' as const },
  },
  containerToPreviousStyle: {
    left: { marginBottom: 20, alignSelf: 'flex-start' as const },
    right: { marginBottom: 20, alignSelf: 'flex-start' as const },
  },
  containerStyle: {
    left: { marginLeft: 0, marginRight: 0, alignItems: 'flex-start' as const },
    right: { marginLeft: 0, marginRight: 0, alignItems: 'flex-start' as const },
  },
};

export const markdownStyles = {
  assistant: {
    body: { color: '#222', fontSize: 16 },
    code_inline: {
      backgroundColor: '#f0f0f0',
      color: '#d63384',
      paddingHorizontal: 4,
      paddingVertical: 2,
      borderRadius: 3,
      fontFamily: Platform.OS === 'ios' ? 'Menlo' : 'monospace',
    },
    code_block: {
      backgroundColor: '#f6f8fa',
      color: '#24292e',
      padding: 10,
      borderRadius: 6,
      fontFamily: Platform.OS === 'ios' ? 'Menlo' : 'monospace',
    },
    fence: {
      backgroundColor: '#f6f8fa',
      color: '#24292e',
      padding: 10,
      borderRadius: 6,
      fontFamily: Platform.OS === 'ios' ? 'Menlo' : 'monospace',
    },
    strong: { fontWeight: 'bold' as const },
    em: { fontStyle: 'italic' as const },
    link: { color: '#0366d6', textDecorationLine: 'underline' as const },
    blockquote: {
      backgroundColor: '#f6f8fa',
      borderLeftColor: '#dfe2e5',
      borderLeftWidth: 4,
      paddingLeft: 10,
      marginLeft: 0,
    },
    list_item: { flexDirection: 'row' as const, marginBottom: 4 },
    bullet_list: { marginBottom: 8 },
    ordered_list: { marginBottom: 8 },
    heading1: { fontSize: 24, fontWeight: 'bold' as const, marginBottom: 8 },
    heading2: { fontSize: 20, fontWeight: 'bold' as const, marginBottom: 6 },
    heading3: { fontSize: 18, fontWeight: 'bold' as const, marginBottom: 4 },
  },
  user: {
    body: { color: '#222', fontSize: 16 },
    code_inline: {
      backgroundColor: '#f0f0f0',
      color: '#d63384',
      paddingHorizontal: 4,
      paddingVertical: 2,
      borderRadius: 3,
    },
  },
};
