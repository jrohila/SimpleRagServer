import { StyleSheet } from 'react-native';
import { GlobalStyles, Colors, FontSizes, Spacing, BorderRadius } from './GlobalStyles';

const styles = StyleSheet.create({
  container: GlobalStyles.container,
  dropdownContainer: GlobalStyles.dropdownContainer,
  label: GlobalStyles.label,
  pickerWrapper: GlobalStyles.pickerWrapper,
  picker: GlobalStyles.picker,
  fieldContainer: {
    marginBottom: Spacing.lg,
  },
  input: GlobalStyles.input,
  checkboxContainer: {
    marginBottom: Spacing.lg,
  },
  checkbox: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  checkboxBox: {
    width: 24,
    height: 24,
    borderWidth: 2,
    borderColor: Colors.primary,
    borderRadius: BorderRadius.sm,
    marginRight: Spacing.sm,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: Colors.white,
  },
  checkboxBoxChecked: {
    backgroundColor: Colors.primary,
  },
  checkboxCheck: {
    color: Colors.white,
    fontSize: FontSizes.md,
    fontWeight: 'bold',
  },
  checkboxLabel: {
    fontSize: FontSizes.base,
    color: Colors.text,
  },
  boostTermsSection: {
    marginBottom: Spacing.lg,
  },
  sectionTitle: GlobalStyles.sectionTitle,
  boostTermRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: Spacing.sm,
    gap: Spacing.sm,
  },
  boostTermInput: {
    flex: 2,
  },
  boostWeightInput: {
    flex: 1,
  },
  removeButton: {
    width: 32,
    height: 32,
    borderRadius: BorderRadius.full,
    backgroundColor: Colors.danger,
    justifyContent: 'center',
    alignItems: 'center',
  },
  removeButtonText: {
    color: Colors.white,
    fontSize: FontSizes.lg,
    fontWeight: 'bold',
  },
  addButton: {
    ...GlobalStyles.button,
    ...GlobalStyles.buttonSuccess,
    marginTop: Spacing.sm,
  },
  addButtonText: GlobalStyles.buttonText,
  searchButtonContainer: {
    marginVertical: Spacing.lg,
  },
  loader: {
    marginVertical: Spacing.xl,
  },
  resultsContainer: {
    marginTop: Spacing.lg,
  },
  resultsTitle: {
    fontWeight: 'bold',
    fontSize: FontSizes.md,
    marginBottom: Spacing.md,
    color: Colors.text,
  },
  tableContainer: GlobalStyles.tableContainer,
  tableHeader: GlobalStyles.tableHeader,
  tableHeaderText: GlobalStyles.tableHeaderText,
  tableRow: GlobalStyles.tableRow,
  tableRowEven: GlobalStyles.tableRowEven,
  tableCell: GlobalStyles.tableCell,
  scoreColumn: {
    width: 60,
  },
  textColumn: {
    flex: 3,
    paddingRight: Spacing.sm,
  },
  documentColumn: {
    flex: 2,
    paddingRight: Spacing.sm,
  },
  sectionColumn: {
    flex: 1.5,
    paddingRight: Spacing.sm,
  },
  pageColumn: {
    width: 50,
    textAlign: 'center',
  },
  urlColumn: {
    flex: 1.5,
  },
  safeArea: GlobalStyles.safeArea,
});

export default styles;
