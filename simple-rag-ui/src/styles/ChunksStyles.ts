import { StyleSheet } from 'react-native';
import { GlobalStyles, Colors, FontSizes, Spacing, BorderRadius } from './GlobalStyles';

const styles = StyleSheet.create({
  container: GlobalStyles.container,
  pageHeader: {
    ...GlobalStyles.title,
    marginBottom: Spacing.lg,
  },
  filtersContainer: {
    flexDirection: 'row',
    gap: Spacing.lg,
    marginBottom: Spacing.lg,
    flexWrap: 'wrap',
  },
  dropdownContainer: {
    flex: 1,
    minWidth: 250,
  },
  dropdownLabel: GlobalStyles.label,
  pickerWrapper: GlobalStyles.pickerWrapper,
  picker: GlobalStyles.picker,
  chunksSection: {
    flex: 1,
  },
  row: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    width: '100%',
  },
  sidebar: GlobalStyles.sidebar,
  sidebarHeader: {
    ...GlobalStyles.label,
    paddingHorizontal: 10,
    paddingVertical: Spacing.sm,
    backgroundColor: Colors.background,
    borderBottomWidth: 1,
    borderBottomColor: Colors.border,
  },
  sidebarContent: {
    paddingVertical: Spacing.sm,
  },
  chatItem: GlobalStyles.sidebarItem,
  chatItemSelected: GlobalStyles.sidebarItemSelected,
  chatItemText: GlobalStyles.sidebarItemText,
  mainContent: {
    flex: 1,
    padding: 0,
  },
  mainHeader: {
    ...GlobalStyles.label,
    paddingHorizontal: 10,
    paddingVertical: Spacing.sm,
    backgroundColor: Colors.background,
    borderBottomWidth: 1,
    borderBottomColor: Colors.border,
  },
  mainContentBody: {
    padding: Spacing.lg,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: Spacing.sm,
    width: '100%',
  },
  headerTitle: GlobalStyles.subtitle,
  paginationContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: Spacing.sm,
    width: '100%',
    justifyContent: 'space-between',
  },
  paginationButton: {
    paddingHorizontal: Spacing.md,
    paddingVertical: Spacing.xs + 2,
    borderRadius: BorderRadius.sm,
  },
  paginationButtonActive: {
    backgroundColor: Colors.primary,
    opacity: 1,
  },
  paginationButtonDisabled: GlobalStyles.buttonDisabled,
  paginationButtonText: {
    color: Colors.white,
    fontSize: FontSizes.sm,
    fontWeight: 'bold',
  },
  paginationText: {
    fontSize: FontSizes.sm,
    color: Colors.textSecondary,
  },
  chunksContainer: {
    borderWidth: 1,
    borderColor: Colors.border,
    borderRadius: BorderRadius.md,
    overflow: 'hidden',
  },
  chunkRow: {
    borderBottomWidth: 1,
    borderBottomColor: Colors.border,
  },
  chunkRowLast: {
    borderBottomWidth: 0,
  },
  chunkTextRow: {
    padding: Spacing.sm,
    backgroundColor: Colors.gray50,
  },
  chunkTextLabel: {
    ...GlobalStyles.label,
    marginBottom: Spacing.xs,
    fontSize: FontSizes.sm,
  },
  chunkTextArea: {
    ...GlobalStyles.input,
    minHeight: 80,
  },
  chunkTextAreaDisabled: {
    backgroundColor: Colors.background,
  },
  chunkText: {
    fontFamily: 'monospace',
    fontSize: FontSizes.sm,
  },
  chunkTextDisabled: GlobalStyles.textDisabled,
  chunkMetadataRow: {
    flexDirection: 'row',
    padding: Spacing.sm,
    backgroundColor: Colors.gray50,
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
    marginRight: Spacing.lg,
    marginBottom: Spacing.xs,
  },
  chunkMetadataLabel: {
    ...GlobalStyles.label,
    fontSize: FontSizes.xs,
  },
  chunkMetadataLabelDisabled: GlobalStyles.textDisabled,
  chunkMetadataValue: {
    fontSize: FontSizes.xs,
  },
  chunkMetadataValueDisabled: GlobalStyles.textDisabled,
  chunkActionsContainer: {
    flexDirection: 'row',
    gap: Spacing.sm,
  },
  chunkUpdateButton: {
    ...GlobalStyles.button,
    ...GlobalStyles.buttonPrimary,
    paddingHorizontal: Spacing.md,
    paddingVertical: Spacing.xs + 2,
  },
  chunkDeleteButton: {
    ...GlobalStyles.button,
    ...GlobalStyles.buttonDanger,
    paddingHorizontal: Spacing.md,
    paddingVertical: Spacing.xs + 2,
  },
  chunkButtonDisabled: GlobalStyles.buttonDisabled,
  chunkButtonText: {
    color: Colors.white,
    fontSize: FontSizes.xs,
    fontWeight: 'bold',
  },
  disabledRow: GlobalStyles.disabledRow,
  emptyMessageText: {
    color: Colors.textTertiary,
  },
  bottomPaginationContainer: {
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'center',
    gap: Spacing.sm,
    marginTop: Spacing.lg,
  },
  safeArea: GlobalStyles.safeArea,
});

export default styles;
