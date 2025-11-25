import React from 'react';
import { Modal, View, Text, TouchableOpacity, ActivityIndicator } from 'react-native';
import styles from '../styles/UpdateModalStyles';

export type UpdateResult = {
  success: boolean;
  message: string;
};

export type UpdateModalProps = {
  // Confirmation modal
  confirmVisible: boolean;
  itemName: string;
  onConfirm: () => void;
  onCancel: () => void;
  
  // Result modal
  resultVisible: boolean;
  updating: boolean;
  updateResult: UpdateResult | null;
  onClose: () => void;
  
  // Optional customization
  confirmTitle?: string;
  confirmMessage?: string;
  confirmButtonText?: string;
  cancelButtonText?: string;
  resultTitle?: string;
  updatingMessage?: string;
};

export function UpdateModal({
  confirmVisible,
  itemName,
  onConfirm,
  onCancel,
  resultVisible,
  updating,
  updateResult,
  onClose,
  confirmTitle = 'Confirm Update',
  confirmMessage = `Are you sure you want to update "${itemName}"?`,
  confirmButtonText = 'Update',
  cancelButtonText = 'Cancel',
  resultTitle = 'Update Result',
  updatingMessage = 'Updating, please wait...',
}: UpdateModalProps) {
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

      {/* Update Result Modal */}
      <Modal
        visible={resultVisible}
        transparent={true}
        animationType="fade"
        onRequestClose={onClose}
      >
        <View style={styles.modalOverlay}>
          <View style={styles.modalContent}>
            <Text style={styles.modalTitle}>
              {updating ? `Updating ${itemName}...` : resultTitle}
            </Text>
            
            {updating ? (
              <View style={styles.modalBody}>
                <ActivityIndicator size="large" color="#007bff" />
                <Text style={styles.modalMessage}>{updatingMessage}</Text>
              </View>
            ) : updateResult ? (
              <View style={styles.modalBody}>
                <Text style={[
                  styles.modalMessage,
                  { color: updateResult.success ? '#28a745' : '#dc3545' }
                ]}>
                  {updateResult.success ? '✓' : '✗'} {updateResult.message}
                </Text>
              </View>
            ) : null}
            
            {!updating && updateResult && (
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

// styles moved to `src/styles/UpdateModalStyles.ts`
