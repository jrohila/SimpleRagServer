import { StyleSheet } from 'react-native';
import { GlobalStyles, Colors, FontSizes, Spacing, BorderRadius } from './GlobalStyles';

const styles = StyleSheet.create({
  container: GlobalStyles.container,
  dropdownContainer: GlobalStyles.dropdownContainer,
  dropdownLabel: GlobalStyles.label,
  pickerWrapper: {
    ...GlobalStyles.pickerWrapper,
    marginVertical: Spacing.sm,
  },
  picker: GlobalStyles.picker,
  title: {
    fontSize: FontSizes.xxl,
    fontWeight: 'bold',
    marginBottom: Spacing.lg,
    textAlign: 'center',
    color: Colors.text,
  },
  row: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    width: '100%',
  },
  sidebar: {
    ...GlobalStyles.sidebar,
    width: '20%',
  },
  sidebarContent: {
    paddingVertical: Spacing.sm,
  },
  chatItem: GlobalStyles.sidebarItem,
  chatItemSelected: GlobalStyles.sidebarItemSelected,
  chatItemText: GlobalStyles.sidebarItemText,
  form: {
    flex: 1,
    width: '100%',
  },
  input: {
    ...GlobalStyles.input,
    marginVertical: Spacing.sm,
  },
  label: GlobalStyles.label,
  checkboxRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginVertical: Spacing.sm,
    gap: Spacing.sm,
  },
  checkbox: {
    marginLeft: Spacing.sm,
    width: 20,
    height: 20,
  },
  textarea: {
    ...GlobalStyles.input,
    ...GlobalStyles.textarea,
  },
  accordionContainer: {
    backgroundColor: Colors.white,
    borderRadius: BorderRadius.lg,
    marginVertical: Spacing.xs,
    overflow: 'hidden',
  },
  accordionTitle: {
    backgroundColor: Colors.background,
  },
  accordionContent: {
    backgroundColor: Colors.white,
    paddingHorizontal: Spacing.lg,
    paddingVertical: Spacing.sm,
  },
  spacer: GlobalStyles.spacer,
  safeArea: GlobalStyles.safeArea,
});

export default styles;
