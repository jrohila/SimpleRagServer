import { StyleSheet } from 'react-native';

const styles = StyleSheet.create({
  container: { width: '100%' },
  row: { flexDirection: 'row', alignItems: 'center', width: '100%' },
  globeOutside: { marginRight: 12, color: '#333', marginLeft: 2 },
  selectorWrapper: { flex: 1, position: 'relative' },
  selector: {
    paddingVertical: 8,
    paddingHorizontal: 12,
    borderRadius: 8,
    backgroundColor: 'transparent',
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    borderWidth: 1,
    borderColor: '#eee',
  },
  selectorText: { fontSize: 14, color: '#222' },
  caretIcon: { color: '#666' },
  dropdown: {
    position: 'absolute',
    top: '100%',
    left: 0,
    right: 0,
    marginTop: 6,
    backgroundColor: '#fff',
    borderRadius: 6,
    overflow: 'hidden',
    borderWidth: 1,
    borderColor: '#eee',
    zIndex: 999,
  },
  elevated: { boxShadow: '0 2px 6px rgba(0,0,0,0.15)' as any },
  item: { paddingVertical: 8, paddingHorizontal: 12 },
  itemText: { fontSize: 14 },
  itemCurrent: { fontWeight: '700' },
});

export default styles;
