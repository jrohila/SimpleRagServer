import { StyleSheet } from 'react-native';

const styles = StyleSheet.create({
  window: {
    backgroundColor: '#fff',
    borderRadius: 12,
    padding: 20,
    marginVertical: 24,
    width: '98%',
    alignSelf: 'center',
    // Shadow for iOS
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.1,
    shadowRadius: 8,
    // Elevation for Android
    elevation: 4,
  },
});

export default styles;
