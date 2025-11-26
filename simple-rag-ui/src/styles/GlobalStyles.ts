import { StyleSheet } from 'react-native';

// Unified color palette
export const Colors = {
  // Primary colors
  primary: '#007bff',
  primaryDark: '#0056b3',
  primaryLight: '#e0eaff',
  primaryBackground: '#e3f2fd',
  
  // Semantic colors
  success: '#28a745',
  danger: '#dc3545',
  warning: '#ffc107',
  warningBackground: '#fff3cd',
  warningText: '#856404',
  info: '#17a2b8',
  
  // Neutral colors
  white: '#fff',
  black: '#000',
  gray50: '#f9f9f9',
  gray100: '#f5f5f5',
  gray200: '#f0f0f0',
  gray300: '#e0e0e0',
  gray400: '#ddd',
  gray500: '#ccc',
  gray600: '#999',
  gray700: '#666',
  gray800: '#333',
  gray900: '#222',
  
  // Borders
  border: '#ddd',
  borderLight: '#eee',
  borderDark: '#ccc',
  
  // Backgrounds
  background: '#f5f5f5',
  backgroundAlt: '#f8f9fa',
  backgroundDark: '#f0f0f0',
  
  // Text
  text: '#333',
  textSecondary: '#666',
  textTertiary: '#888',
  textDisabled: '#999',
  
  // Code/monospace
  codeBg: '#f6f8fa',
  codeText: '#24292e',
  codeInline: '#d63384',
  codeInlineBg: '#f0f0f0',
};

// Unified spacing system
export const Spacing = {
  xs: 4,
  sm: 8,
  md: 12,
  lg: 16,
  xl: 20,
  xxl: 24,
};

// Unified border radius
export const BorderRadius = {
  sm: 4,
  md: 6,
  lg: 8,
  xl: 12,
  full: 9999,
};

// Unified font sizes
export const FontSizes = {
  xs: 11,
  sm: 12,
  base: 14,
  md: 16,
  lg: 18,
  xl: 20,
  xxl: 22,
  xxxl: 24,
};

// Unified font weights
export const FontWeights = {
  normal: '400' as const,
  medium: '500' as const,
  semibold: '600' as const,
  bold: '700' as const,
};

// Unified shadows
export const Shadows = {
  modal: {
    shadowColor: Colors.black,
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.25,
    shadowRadius: 4,
    elevation: 5,
  },
};

export const GlobalStyles = StyleSheet.create({
  // Layout
  centerContent: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  container: {
    flex: 1,
    padding: Spacing.lg,
  },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  
  // Modal styles (unified)
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    justifyContent: 'center',
    alignItems: 'center',
    padding: Spacing.xl,
  },
  modalContent: {
    backgroundColor: Colors.white,
    borderRadius: BorderRadius.lg,
    padding: Spacing.xl,
    minWidth: 300,
    maxWidth: 500,
    ...Shadows.modal,
  },
  modalTitle: {
    fontSize: FontSizes.xl,
    fontWeight: FontWeights.bold,
    marginBottom: Spacing.lg,
    textAlign: 'center',
    color: Colors.text,
  },
  modalBody: {
    marginBottom: Spacing.xl,
    alignItems: 'center',
  },
  modalMessage: {
    fontSize: FontSizes.md,
    textAlign: 'center',
    marginBottom: Spacing.sm,
    color: Colors.text,
  },
  modalActions: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    gap: Spacing.md,
  },
  
  // Button styles (unified)
  button: {
    paddingVertical: Spacing.md,
    paddingHorizontal: Spacing.xl,
    borderRadius: BorderRadius.sm,
    alignItems: 'center',
    justifyContent: 'center',
  },
  buttonPrimary: {
    backgroundColor: Colors.primary,
  },
  buttonSecondary: {
    backgroundColor: '#6c757d',
  },
  buttonDanger: {
    backgroundColor: Colors.danger,
  },
  buttonSuccess: {
    backgroundColor: Colors.success,
  },
  buttonDisabled: {
    backgroundColor: Colors.gray500,
    opacity: 0.5,
  },
  buttonText: {
    color: Colors.white,
    fontSize: FontSizes.md,
    fontWeight: FontWeights.semibold,
  },
  buttonFlex: {
    flex: 1,
  },
  
  // Input styles (unified)
  input: {
    borderWidth: 1,
    borderColor: Colors.border,
    borderRadius: BorderRadius.md,
    padding: Spacing.sm,
    backgroundColor: Colors.white,
    fontSize: FontSizes.base,
    color: Colors.text,
  },
  textarea: {
    minHeight: 100,
    textAlignVertical: 'top',
  },
  
  // Label styles (unified)
  label: {
    fontWeight: FontWeights.bold,
    fontSize: FontSizes.base,
    marginBottom: Spacing.xs,
    color: Colors.textSecondary,
  },
  
  // Picker/Dropdown styles (unified)
  pickerWrapper: {
    borderWidth: 1,
    borderColor: Colors.border,
    borderRadius: BorderRadius.md,
    backgroundColor: Colors.white,
    overflow: 'hidden',
  },
  picker: {
    height: 40,
    backgroundColor: 'transparent',
    borderRadius: BorderRadius.md,
  },
  
  // Dropdown container
  dropdownContainer: {
    marginBottom: Spacing.lg,
  },
  
  // Table styles (unified)
  tableContainer: {
    borderWidth: 1,
    borderColor: Colors.border,
    borderRadius: BorderRadius.sm,
    overflow: 'hidden',
  },
  tableHeader: {
    flexDirection: 'row',
    backgroundColor: Colors.backgroundDark,
    padding: Spacing.sm,
    borderBottomWidth: 2,
    borderBottomColor: Colors.border,
  },
  tableHeaderText: {
    fontWeight: FontWeights.bold,
    fontSize: FontSizes.sm,
    color: Colors.text,
  },
  tableRow: {
    flexDirection: 'row',
    padding: Spacing.sm,
    borderBottomWidth: 1,
    borderBottomColor: Colors.borderLight,
  },
  tableRowEven: {
    backgroundColor: Colors.gray50,
  },
  tableCell: {
    fontSize: FontSizes.xs,
    color: Colors.text,
  },
  
  // Sidebar styles (unified)
  sidebar: {
    width: 200,
    minWidth: 120,
    maxWidth: 260,
    height: '100%',
    marginRight: Spacing.lg,
  },
  sidebarItem: {
    paddingVertical: Spacing.md,
    paddingHorizontal: 10,
    borderBottomWidth: 1,
    borderBottomColor: Colors.borderLight,
    backgroundColor: 'transparent',
  },
  sidebarItemSelected: {
    backgroundColor: Colors.primaryLight,
  },
  sidebarItemText: {
    fontSize: FontSizes.md,
    color: Colors.text,
  },
  
  // Title styles (unified)
  title: {
    fontSize: FontSizes.xxxl,
    fontWeight: FontWeights.bold,
    color: Colors.text,
  },
  subtitle: {
    fontSize: FontSizes.lg,
    fontWeight: FontWeights.semibold,
    color: Colors.text,
  },
  sectionTitle: {
    fontSize: FontSizes.md,
    fontWeight: FontWeights.bold,
    color: Colors.text,
  },
  
  // Utility styles
  safeArea: {
    flex: 1,
  },
  spacer: {
    height: 10,
  },
  textCenter: {
    textAlign: 'center',
  },
  textDisabled: {
    color: Colors.textDisabled,
  },
  disabledRow: {
    opacity: 0.5,
  },
});
