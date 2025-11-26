import { StyleSheet } from 'react-native';
import { GlobalStyles, Colors, FontSizes, Spacing, BorderRadius } from './GlobalStyles';

const styles = StyleSheet.create({
  safeArea: GlobalStyles.safeArea,
  title: GlobalStyles.title,
  label: GlobalStyles.label,
  input: GlobalStyles.input,
  textarea: {
    ...GlobalStyles.input,
    ...GlobalStyles.textarea,
  },
  switchRow: {
    flexDirection: 'column',
    alignItems: 'flex-start',
    marginBottom: Spacing.lg,
    width: '100%',
    gap: Spacing.xs,
  },
  buttonRow: {
    flexDirection: 'column',
    alignItems: 'stretch',
    marginTop: Spacing.xl,
    width: '100%',
    gap: Spacing.sm,
  },
  uploadButton: {
    ...GlobalStyles.button,
    ...GlobalStyles.buttonPrimary,
    marginBottom: Spacing.md,
  },
  uploadButtonText: GlobalStyles.buttonText,
  filesContainer: {
    marginBottom: Spacing.lg,
    padding: Spacing.md,
    backgroundColor: Colors.backgroundAlt,
    borderRadius: BorderRadius.md,
    borderWidth: 1,
    borderColor: Colors.border,
  },
  filesTitle: {
    fontWeight: 'bold',
    marginBottom: Spacing.sm,
    fontSize: FontSizes.base,
  },
  fileItem: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: Spacing.sm,
    backgroundColor: Colors.white,
    borderRadius: BorderRadius.sm,
    marginBottom: Spacing.xs + 2,
    borderWidth: 1,
    borderColor: Colors.gray300,
  },
  fileInfo: {
    flex: 1,
    marginRight: Spacing.sm,
  },
  fileName: {
    fontSize: FontSizes.base,
    fontWeight: '500',
    marginBottom: 2,
  },
  fileSize: {
    fontSize: FontSizes.sm,
    color: Colors.textSecondary,
  },
  removeButton: {
    color: Colors.danger,
    fontSize: FontSizes.xl,
    fontWeight: 'bold',
    paddingHorizontal: Spacing.sm,
  },
});

export default styles;
