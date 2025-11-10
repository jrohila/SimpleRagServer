import { StyleSheet } from 'react-native';

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 16,
  },
  pageHeader: {
    fontWeight: 'bold',
    fontSize: 20,
    marginBottom: 16,
    color: '#333',
  },
  filtersContainer: {
    flexDirection: 'row',
    gap: 16,
    marginBottom: 16,
    flexWrap: 'wrap',
  },
  dropdownContainer: {
    flex: 1,
    minWidth: 250,
  },
  dropdownLabel: {
    fontWeight: 'bold',
    fontSize: 14,
    marginBottom: 4,
    color: '#666',
  },
  pickerWrapper: {
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 4,
    backgroundColor: '#fff',
  },
  picker: {
    height: 40,
  },
  chunksSection: {
    flex: 1,
  },
  row: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    width: '100%',
  },
  sidebar: {
    width: 200,
    minWidth: 120,
    maxWidth: 260,
    height: '100%',
    marginRight: 16,
  },
  sidebarHeader: {
    fontWeight: 'bold',
    fontSize: 14,
    paddingHorizontal: 10,
    paddingVertical: 8,
    backgroundColor: '#f5f5f5',
    borderBottomWidth: 1,
    borderBottomColor: '#ddd',
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
  mainContent: {
    flex: 1,
    padding: 0,
  },
  mainHeader: {
    fontWeight: 'bold',
    fontSize: 14,
    paddingHorizontal: 10,
    paddingVertical: 8,
    backgroundColor: '#f5f5f5',
    borderBottomWidth: 1,
    borderBottomColor: '#ddd',
  },
  mainContentBody: {
    padding: 16,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 8,
    width: '100%',
  },
  headerTitle: {
    fontWeight: 'bold',
    fontSize: 16,
  },
  paginationContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    width: '100%',
    justifyContent: 'space-between',
  },
  paginationButton: {
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 4,
  },
  paginationButtonActive: {
    backgroundColor: '#007bff',
    opacity: 1,
  },
  paginationButtonDisabled: {
    backgroundColor: '#ccc',
    opacity: 0.5,
  },
  paginationButtonText: {
    color: '#fff',
    fontSize: 12,
    fontWeight: 'bold',
  },
  paginationText: {
    fontSize: 12,
    color: '#666',
  },
  chunksContainer: {
    borderWidth: 1,
    borderColor: '#ccc',
    borderRadius: 6,
    overflow: 'hidden',
  },
  chunkRow: {
    borderBottomWidth: 1,
    borderBottomColor: '#ddd',
  },
  chunkRowLast: {
    borderBottomWidth: 0,
  },
  chunkTextRow: {
    padding: 8,
    backgroundColor: '#fafafa',
  },
  chunkTextLabel: {
    fontWeight: 'bold',
    marginBottom: 4,
    fontSize: 12,
    color: '#666',
  },
  chunkTextArea: {
    borderWidth: 1,
    borderColor: '#ddd',
    borderRadius: 4,
    padding: 8,
    minHeight: 80,
    backgroundColor: '#fff',
  },
  chunkTextAreaDisabled: {
    backgroundColor: '#f5f5f5',
  },
  chunkText: {
    fontFamily: 'monospace',
    fontSize: 12,
  },
  chunkTextDisabled: {
    color: '#999',
  },
  chunkMetadataRow: {
    flexDirection: 'row',
    padding: 8,
    backgroundColor: '#f9f9f9',
    flexWrap: 'wrap',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  chunkMetadataContainer: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    flex: 1,
  },
  chunkMetadataField: {
    flexDirection: 'row',
    marginRight: 16,
    marginBottom: 4,
  },
  chunkMetadataLabel: {
    fontWeight: 'bold',
    fontSize: 11,
    color: '#666',
  },
  chunkMetadataLabelDisabled: {
    color: '#999',
  },
  chunkMetadataValue: {
    fontSize: 11,
  },
  chunkMetadataValueDisabled: {
    color: '#999',
  },
  chunkActionsContainer: {
    flexDirection: 'row',
    gap: 8,
  },
  chunkUpdateButton: {
    backgroundColor: '#007bff',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 4,
  },
  chunkDeleteButton: {
    backgroundColor: '#dc3545',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 4,
  },
  chunkButtonDisabled: {
    backgroundColor: '#ccc',
  },
  chunkButtonText: {
    color: '#fff',
    fontSize: 11,
    fontWeight: 'bold',
  },
  disabledRow: {
    opacity: 0.5,
  },
  emptyMessageText: {
    color: '#888',
  },
  bottomPaginationContainer: {
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'center',
    gap: 8,
    marginTop: 16,
  },
});

export default styles;
