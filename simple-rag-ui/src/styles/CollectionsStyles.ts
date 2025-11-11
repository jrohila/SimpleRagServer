import { StyleSheet } from 'react-native';


const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 16,
  },
  dropdownContainer: {
    marginBottom: 16,
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
  filtersContainer: {
    flexDirection: 'row',
    gap: 16,
    marginBottom: 16,
    flexWrap: 'wrap',
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
    width: '100%',
  },
  input: {
    borderWidth: 1,
    borderColor: '#ccc',
    borderRadius: 6,
    padding: 8,
    marginVertical: 8,
    width: '100%',
  },
  label: {
    alignSelf: 'flex-start',
    fontWeight: 'bold',
    marginBottom: 2,
    marginTop: 6,
  },
  dateField: {
    marginVertical: 8,
    color: '#888',
  },
  buttonCol: {
    flexDirection: 'column',
    gap: 12,
    marginTop: 16,
  },
  buttonWrapper: {
    width: '100%',
    marginBottom: 8,
  },
  // Modal styles
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    justifyContent: 'center',
    alignItems: 'center',
  },
  modalContent: {
    backgroundColor: '#fff',
    borderRadius: 8,
    padding: 24,
    minWidth: 300,
    maxWidth: 500,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.25,
    shadowRadius: 4,
    elevation: 5,
  },
  modalTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    marginBottom: 16,
    textAlign: 'center',
    color: '#333',
  },
  modalBody: {
    marginBottom: 20,
    alignItems: 'center',
  },
  modalMessage: {
    fontSize: 16,
    textAlign: 'center',
    lineHeight: 24,
    marginTop: 12,
  },
  modalActions: {
    flexDirection: 'row',
    justifyContent: 'center',
    gap: 12,
  },
  modalButton: {
    paddingVertical: 12,
    paddingHorizontal: 24,
    borderRadius: 6,
    minWidth: 100,
    alignItems: 'center',
  },
  modalButtonPrimary: {
    backgroundColor: '#007bff',
  },
  modalButtonSecondary: {
    backgroundColor: '#6c757d',
  },
  modalButtonDanger: {
    backgroundColor: '#dc3545',
  },
  modalButtonText: {
    color: '#fff',
    fontSize: 16,
    fontWeight: '600',
  },
});

export default styles;
