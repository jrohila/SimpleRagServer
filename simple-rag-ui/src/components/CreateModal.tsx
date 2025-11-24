import React from 'react';
import { Modal, View, Text, TouchableOpacity, ActivityIndicator } from 'react-native';
import styles from '../styles/CreateModalStyles';

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

// styles moved to `src/styles/CreateModalStyles.ts`
