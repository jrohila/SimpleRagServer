import { StyleSheet } from 'react-native';
import { GlobalStyles, Colors, FontSizes, Spacing, BorderRadius, Shadows } from './GlobalStyles';

const styles = StyleSheet.create({
  container: GlobalStyles.container,
  dropdownContainer: {
    ...GlobalStyles.dropdownContainer,
    flex: 1,
    minWidth: 250,
  },
  dropdownLabel: GlobalStyles.label,
  pickerWrapper: GlobalStyles.pickerWrapper,
  picker: GlobalStyles.picker,
  filtersContainer: {
    flexDirection: 'row',
    gap: Spacing.lg,
    marginBottom: Spacing.lg,
    flexWrap: 'wrap',
  },
  row: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    width: '100%',
  },
  sidebar: GlobalStyles.sidebar,
  sidebarContent: {
    paddingVertical: Spacing.sm,
  },
  chatItem: GlobalStyles.sidebarItem,
  chatItemSelected: GlobalStyles.sidebarItemSelected,
  chatItemText: GlobalStyles.sidebarItemText,
  form: {
    width: '100%',
  },
  input: {
    ...GlobalStyles.input,
    marginVertical: Spacing.sm,
    width: '100%',
  },
  label: GlobalStyles.label,
  dateField: {
    marginVertical: Spacing.sm,
    color: Colors.textTertiary,
  },
  buttonCol: {
    flexDirection: 'column',
    gap: Spacing.md,
    marginTop: Spacing.lg,
  },
  buttonWrapper: {
    width: '100%',
    marginBottom: Spacing.sm,
  },
  // Modal styles
  modalOverlay: GlobalStyles.modalOverlay,
  modalContent: {
    ...GlobalStyles.modalContent,
    padding: Spacing.xxl,
  },
  modalTitle: GlobalStyles.modalTitle,
  modalBody: GlobalStyles.modalBody,
  modalMessage: {
    ...GlobalStyles.modalMessage,
    lineHeight: 24,
    marginTop: Spacing.md,
  },
  modalActions: {
    ...GlobalStyles.modalActions,
    justifyContent: 'center',
  },
  modalButton: {
    ...GlobalStyles.button,
    paddingHorizontal: Spacing.xxl,
    minWidth: 100,
  },
  modalButtonPrimary: GlobalStyles.buttonPrimary,
  modalButtonSecondary: GlobalStyles.buttonSecondary,
  modalButtonDanger: GlobalStyles.buttonDanger,
  modalButtonText: GlobalStyles.buttonText,
  safeArea: GlobalStyles.safeArea,
  documentsSection: {
    marginTop: Spacing.xxl,
    paddingVertical: Spacing.sm,
  },
  documentsTitle: {
    ...GlobalStyles.subtitle,
    marginBottom: Spacing.md,
  },
  documentsContainer: {
    ...GlobalStyles.tableContainer,
    marginTop: Spacing.sm,
  },
  documentsHeaderRow: GlobalStyles.tableHeader,
  documentsHeaderText: GlobalStyles.tableHeaderText,
  documentRow: GlobalStyles.tableRow,
  columnFullWidth: { flexDirection: 'column', width: '100%' },
  docCellFilename: { flex: 2 },
  docCellType: { flex: 1 },
  docCellSize: { flex: 1 },
  docCellCreated: { flex: 2 },
  docCellUpdated: { flex: 2 },
  docCellState: { flex: 1 },
  docEmptyText: { padding: 8, color: '#888' },
  formFlex: { flex: 1, justifyContent: 'space-between' },
  documentFieldRow: { flexDirection: 'row', alignItems: 'center', marginBottom: 8 },
  fieldLabel: { minWidth: 90, marginRight: 12, marginBottom: 0, flexShrink: 1, flexGrow: 0 },
  documentInputFlex: { flex: 1, marginVertical: 0 },
  fileRow: { flex: 1, flexDirection: 'row', alignItems: 'center', gap: Spacing.sm },
  fileButton: {
    ...GlobalStyles.button,
    paddingVertical: Spacing.sm,
  },
  fileButtonPrimary: GlobalStyles.buttonPrimary,
  fileButtonDisabled: GlobalStyles.buttonDisabled,
  fileButtonText: GlobalStyles.buttonText,
  fileNameText: { flex: 1, fontSize: FontSizes.base, color: Colors.textSecondary },
});

export default styles;
