import React from 'react';
import { Modal, View, Text, TouchableOpacity, ActivityIndicator } from 'react-native';
import { StyleSheet } from 'react-native';

export type CreateResult = {
  success: boolean;
  message: string;
};

export type CreateModalProps = {
  // Confirmation modal
  confirmVisible: boolean;
  itemName: string;
  onConfirm: () => void;
  onCancel: () => void;
  
  // Result modal
  resultVisible: boolean;
  creating: boolean;
  createResult: CreateResult | null;
  onClose: () => void;
  
  // Optional customization
  confirmTitle?: string;
  confirmMessage?: string;
  confirmButtonText?: string;
  cancelButtonText?: string;
  resultTitle?: string;
  creatingMessage?: string;
};

export function CreateModal({
  confirmVisible,
  itemName,
  onConfirm,
  onCancel,
  resultVisible,
  creating,
  createResult,
  onClose,
  confirmTitle = 'Confirm Create',
  confirmMessage = `Are you sure you want to create "${itemName}"?`,
  confirmButtonText = 'Create',
  cancelButtonText = 'Cancel',
  resultTitle = 'Create Result',
  creatingMessage = 'Creating, please wait...',
}: CreateModalProps) {
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
            </View>
            
            <View style={styles.modalActions}>
              <TouchableOpacity 
                style={[styles.modalButton, styles.modalButtonSecondary]}
                onPress={onCancel}
              >
                <Text style={styles.modalButtonText}>{cancelButtonText}</Text>
              </TouchableOpacity>
              <TouchableOpacity 
                style={[styles.modalButton, styles.modalButtonPrimary]}
                onPress={onConfirm}
              >
                <Text style={styles.modalButtonText}>{confirmButtonText}</Text>
              </TouchableOpacity>
            </View>
          </View>
        </View>
      </Modal>

      {/* Create Result Modal */}
      <Modal
        visible={resultVisible}
        transparent={true}
        animationType="fade"
        onRequestClose={onClose}
      >
        <View style={styles.modalOverlay}>
          <View style={styles.modalContent}>
            <Text style={styles.modalTitle}>
              {creating ? `Creating ${itemName}...` : resultTitle}
            </Text>
            
            {creating ? (
              <View style={styles.modalBody}>
                <ActivityIndicator size="large" color="#007bff" />
                <Text style={styles.modalMessage}>{creatingMessage}</Text>
              </View>
            ) : createResult ? (
              <View style={styles.modalBody}>
                <Text style={[
                  styles.modalMessage,
                  { color: createResult.success ? '#28a745' : '#dc3545' }
                ]}>
                  {createResult.success ? '✓' : '✗'} {createResult.message}
                </Text>
              </View>
            ) : null}
            
            {!creating && createResult && (
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
  modalButtonSecondary: {
    backgroundColor: '#6c757d',
  },
  modalButtonText: {
    color: 'white',
    fontSize: 16,
    fontWeight: '600',
  },
});
