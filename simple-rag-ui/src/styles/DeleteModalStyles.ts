import { StyleSheet } from 'react-native';
import { GlobalStyles, Colors, FontSizes, Spacing } from './GlobalStyles';

const styles = StyleSheet.create({
  modalOverlay: GlobalStyles.modalOverlay,
  modalContent: GlobalStyles.modalContent,
  modalTitle: GlobalStyles.modalTitle,
  modalBody: GlobalStyles.modalBody,
  modalMessage: GlobalStyles.modalMessage,
  modalActions: GlobalStyles.modalActions,
  modalButton: {
    ...GlobalStyles.button,
    ...GlobalStyles.buttonFlex,
  },
  modalButtonPrimary: GlobalStyles.buttonPrimary,
  modalButtonDanger: GlobalStyles.buttonDanger,
  modalButtonText: GlobalStyles.buttonText,
  warningText: {
    marginTop: Spacing.sm,
    fontSize: FontSizes.base,
    color: Colors.danger,
  },
});

export default styles;
