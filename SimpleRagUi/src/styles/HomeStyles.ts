import { StyleSheet } from 'react-native';

export const HomeStyles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#000',
  },
  messagesContainer: {
    paddingHorizontal: 12,
    paddingBottom: 12,
    backgroundColor: '#000',
  },
  input: {
    color: '#fff',
    backgroundColor: '#222',
    borderRadius: 20,
    paddingHorizontal: 16,
    paddingVertical: 10,
    margin: 8,
    fontSize: 16,
    width: '100%',
    boxSizing: 'border-box',
  },
  sendContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    marginRight: 8,
    height: undefined,
  },
  dropdownContainer: {
    backgroundColor: '#111',
    paddingHorizontal: 16,
    paddingVertical: 12,
    borderBottomWidth: 1,
    borderBottomColor: '#333',
  },
  pickerWrapper: {
    backgroundColor: '#222',
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#444',
    overflow: 'hidden',
  },
  picker: {
    color: '#fff',
    backgroundColor: '#222',
  },
});
