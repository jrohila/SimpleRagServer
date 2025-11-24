import { StyleSheet } from 'react-native';

const styles = StyleSheet.create({
  safeArea: { flex: 1 },
  title: {
    fontSize: 24,
    fontWeight: 'bold',
  },
  label: {
    alignSelf: 'flex-start',
    fontWeight: 'bold',
    marginBottom: 2,
    marginTop: 6,
  },
  input: {
    width: '100%',
    borderWidth: 1,
    borderColor: '#ccc',
    borderRadius: 6,
    padding: 8,
    marginBottom: 12,
    backgroundColor: '#fff',
  },
  textarea: {
    minHeight: 100,
    textAlignVertical: 'top',
  },
  switchRow: {
    flexDirection: 'column',
    alignItems: 'flex-start',
    marginBottom: 16,
    width: '100%',
    gap: 4,
  },
  buttonRow: {
    flexDirection: 'column',
    alignItems: 'stretch',
    marginTop: 20,
    width: '100%',
    gap: 8,
  },
  uploadButton: {
    backgroundColor: '#007bff',
    padding: 12,
    borderRadius: 6,
    alignItems: 'center',
    marginBottom: 12,
  },
  uploadButtonText: {
    color: '#fff',
    fontWeight: 'bold',
    fontSize: 16,
  },
  filesContainer: {
    marginBottom: 16,
    padding: 12,
    backgroundColor: '#f8f9fa',
    borderRadius: 6,
    borderWidth: 1,
    borderColor: '#ddd',
  },
  filesTitle: {
    fontWeight: 'bold',
    marginBottom: 8,
    fontSize: 14,
  },
  fileItem: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: 8,
    backgroundColor: '#fff',
    borderRadius: 4,
    marginBottom: 6,
    borderWidth: 1,
    borderColor: '#e0e0e0',
  },
  fileInfo: {
    flex: 1,
    marginRight: 8,
  },
  fileName: {
    fontSize: 14,
    fontWeight: '500',
    marginBottom: 2,
  },
  fileSize: {
    fontSize: 12,
    color: '#666',
  },
  removeButton: {
    color: '#dc3545',
    fontSize: 20,
    fontWeight: 'bold',
    paddingHorizontal: 8,
  },
});

export default styles;
