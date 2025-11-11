import React from 'react';
import { Modal, View, Text, TouchableOpacity, ActivityIndicator } from 'react-native';
import { StyleSheet } from 'react-native';

export type DeleteResult = {
  success: boolean;
  message: string;
};

export type DeleteModalProps = {
  // Confirmation modal
  confirmVisible: boolean;
  itemName: string;
  onConfirm: () => void;
  onCancel: () => void;
  
  // Result modal
  resultVisible: boolean;
  deleting: boolean;
  deleteResult: DeleteResult | null;
  onClose: () => void;
  
  // Optional customization
  confirmTitle?: string;
  confirmMessage?: string;
  confirmButtonText?: string;
  cancelButtonText?: string;
  resultTitle?: string;
  deletingMessage?: string;
};

export function DeleteModal({
  confirmVisible,
  itemName,
  onConfirm,
  onCancel,
  resultVisible,
  deleting,
  deleteResult,
  onClose,
  confirmTitle = 'Confirm Delete',
  confirmMessage = `Are you sure you want to delete "${itemName}"?`,
  confirmButtonText = 'Delete',
  cancelButtonText = 'Cancel',
  resultTitle = 'Delete Result',
  deletingMessage = 'Please wait...',
}: DeleteModalProps) {
  return (
    <>
      {/* Confirmation Modal */}
      <Modal
        visible={confirmVisible}
        transparent={true}
        animationType="fade"
        onRequestClose={onCancel}
      >
        <View style={styles.modalOverlay}>
          <View style={styles.modalContent}>
            <Text style={styles.modalTitle}>{confirmTitle}</Text>
            
            <View style={styles.modalBody}>
              <Text style={styles.modalMessage}>{confirmMessage}</Text>
              <Text style={[styles.modalMessage, styles.warningText]}>
                This action cannot be undone.
              </Text>
            </View>
            
            <View style={styles.modalActions}>
              <TouchableOpacity 
                style={[styles.modalButton, styles.modalButtonPrimary]}
                onPress={onCancel}
              >
                <Text style={styles.modalButtonText}>{cancelButtonText}</Text>
              </TouchableOpacity>
              <TouchableOpacity 
                style={[styles.modalButton, styles.modalButtonDanger]}
                onPress={onConfirm}
              >
                <Text style={styles.modalButtonText}>{confirmButtonText}</Text>
              </TouchableOpacity>
            </View>
          </View>
        </View>
      </Modal>

      {/* Delete Result Modal */}
      <Modal
        visible={resultVisible}
        transparent={true}
        animationType="fade"
        onRequestClose={onClose}
      >
        <View style={styles.modalOverlay}>
          <View style={styles.modalContent}>
            <Text style={styles.modalTitle}>
              {deleting ? `Deleting ${itemName}...` : resultTitle}
            </Text>
            
            {deleting ? (
              <View style={styles.modalBody}>
                <ActivityIndicator size="large" color="#007bff" />
                <Text style={styles.modalMessage}>{deletingMessage}</Text>
              </View>
            ) : deleteResult ? (
              <View style={styles.modalBody}>
                <Text style={[
                  styles.modalMessage,
                  { color: deleteResult.success ? '#28a745' : '#dc3545' }
                ]}>
                  {deleteResult.success ? '✓' : '✗'} {deleteResult.message}
                </Text>
              </View>
            ) : null}
            
            {!deleting && deleteResult && (
              <View style={styles.modalActions}>
                <TouchableOpacity 
                  style={[styles.modalButton, styles.modalButtonPrimary]}
                  onPress={onClose}
                >
                  <Text style={styles.modalButtonText}>OK</Text>
                </TouchableOpacity>
              </View>
            )}
          </View>
        </View>
      </Modal>
    </>
  );
}

const styles = StyleSheet.create({
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.5)',
    justifyContent: 'center',
    alignItems: 'center',
  },
  modalContent: {
    backgroundColor: 'white',
    borderRadius: 8,
    padding: 20,
    minWidth: 300,
    maxWidth: 500,
    shadowColor: '#000',
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.25,
    shadowRadius: 4,
    elevation: 5,
  },
  modalTitle: {
    fontSize: 20,
    fontWeight: 'bold',
    marginBottom: 16,
    textAlign: 'center',
  },
  modalBody: {
    marginBottom: 20,
    alignItems: 'center',
  },
  modalMessage: {
    fontSize: 16,
    textAlign: 'center',
    marginBottom: 8,
  },
  warningText: {
    marginTop: 8,
    fontSize: 14,
    color: '#dc3545',
  },
  modalActions: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    gap: 12,
  },
  modalButton: {
    flex: 1,
    paddingVertical: 12,
    paddingHorizontal: 20,
    borderRadius: 4,
    alignItems: 'center',
  },
  modalButtonPrimary: {
    backgroundColor: '#007bff',
  },
  modalButtonDanger: {
    backgroundColor: '#dc3545',
  },
  modalButtonText: {
    color: 'white',
    fontSize: 16,
    fontWeight: '600',
  },
});
