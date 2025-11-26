import { StyleSheet } from 'react-native';
import { GlobalStyles } from './GlobalStyles';

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
  modalButtonSecondary: GlobalStyles.buttonSecondary,
  modalButtonText: GlobalStyles.buttonText,
});

export default styles;
