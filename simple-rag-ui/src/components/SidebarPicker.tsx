import React from 'react';
import { ScrollView, TouchableOpacity, Text, View, StyleProp, ViewStyle, TextStyle } from 'react-native';
import styles from '../styles/SidebarPickerStyles';

interface SidebarPickerProps<T> {
  items: T[];
  getItemLabel: (item: T) => string;
  getItemKey: (item: T) => string | number;
  selectedItem: T | null;
  onSelect: (item: T | null) => void;
  containerStyle?: StyleProp<ViewStyle>;
  itemStyle?: StyleProp<ViewStyle>;
  selectedItemStyle?: StyleProp<ViewStyle>;
  textStyle?: StyleProp<TextStyle>;
  selectedTextStyle?: StyleProp<TextStyle>;
  emptyMessage?: string;
}

function SidebarPicker<T>({
  items,
  getItemLabel,
  getItemKey,
  selectedItem,
  onSelect,
  containerStyle,
  itemStyle,
  selectedItemStyle,
  textStyle,
  selectedTextStyle,
  emptyMessage,
}: SidebarPickerProps<T>) {
  return (
    <View style={styles.container}>
      <ScrollView style={[styles.scroll, containerStyle]} contentContainerStyle={styles.scrollContent}>
        {items.length === 0 ? (
          <View style={styles.emptyContainer}>
            <Text style={styles.emptyText}>
              {emptyMessage || 'No items found.'}
            </Text>
          </View>
        ) : (
          items.map((item) => {
            const isSelected = selectedItem && getItemKey(item) === getItemKey(selectedItem);
            return (
              <TouchableOpacity
                key={getItemKey(item)}
                style={[itemStyle, isSelected && selectedItemStyle]}
                onPress={() => {
                  // If already selected, deselect by passing null; otherwise select the item
                  onSelect(isSelected ? null : item);
                }}
              >
                <Text style={[textStyle, isSelected && selectedTextStyle]}>
                  {getItemLabel(item)}
                </Text>
              </TouchableOpacity>
            );
          })
        )}
      </ScrollView>
    </View>
  );
}

export default SidebarPicker;
