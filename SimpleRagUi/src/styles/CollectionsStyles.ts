import { StyleSheet } from 'react-native';


const styles = StyleSheet.create({
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
    backgroundColor: '#f5f5f5',
    borderRightWidth: 1,
    borderRightColor: '#ddd',
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
});

export default styles;
