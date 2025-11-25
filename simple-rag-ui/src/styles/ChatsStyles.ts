import { StyleSheet } from 'react-native';

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 16,
  },
  dropdownContainer: {
    marginBottom: 16,
  },
  dropdownLabel: {
    fontWeight: 'bold',
    fontSize: 14,
    marginBottom: 4,
    color: '#666',
  },
  pickerWrapper: {
    borderWidth: 1,
    borderColor: '#ccc',
    borderRadius: 6,
    marginVertical: 8,
    overflow: 'hidden',
    backgroundColor: '#fff',
  },
  picker: {
    width: '100%',
    height: 40,
    borderRadius: 6,
    backgroundColor: 'transparent',
  },
  title: {
    fontSize: 22,
    fontWeight: 'bold',
    marginBottom: 16,
    textAlign: 'center',
  },
  row: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    width: '100%',
  },
  sidebar: {
    width: '20%',
    minWidth: 120,
    maxWidth: 260,
    height: '100%',
    marginRight: 16,
  },
  sidebarContent: {
    paddingVertical: 8,
  },
  chatItem: {
    paddingVertical: 12,
    paddingHorizontal: 10,
    borderBottomWidth: 1,
    borderBottomColor: '#eee',
    backgroundColor: 'transparent',
  },
  chatItemSelected: {
    backgroundColor: '#e0eaff',
  },
  chatItemText: {
    fontSize: 16,
  },
  form: {
    flex: 1,
    width: '100%',
  },
  input: {
    borderWidth: 1,
    borderColor: '#ccc',
    borderRadius: 6,
    padding: 8,
    marginVertical: 8,
  },
  label: {
    alignSelf: 'flex-start',
    fontWeight: 'bold',
    marginBottom: 2,
    marginTop: 6,
  },
  checkboxRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginVertical: 8,
    gap: 8,
  },
  checkbox: {
    marginLeft: 8,
    width: 20,
    height: 20,
  },
  textarea: {
    minHeight: 100,
    textAlignVertical: 'top',
  },
  accordionContainer: {
    backgroundColor: '#fff',
    borderRadius: 8,
    marginVertical: 4,
    overflow: 'hidden',
  },
  accordionTitle: {
    backgroundColor: '#f5f5f5',
  },
  accordionContent: {
    backgroundColor: '#fff',
    paddingHorizontal: 16,
    paddingVertical: 8,
  },
  spacer: {
    height: 10,
  },
  safeArea: {
    flex: 1,
  },
});

export default styles;
