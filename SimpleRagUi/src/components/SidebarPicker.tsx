import React from 'react';
import { ScrollView, TouchableOpacity, Text, View, StyleProp, ViewStyle, TextStyle } from 'react-native';

interface SidebarPickerProps<T> {
  items: T[];
  getItemLabel: (item: T) => string;
  getItemKey: (item: T) => string | number;
  selectedItem: T | null;
  onSelect: (item: T) => void;
  containerStyle?: StyleProp<ViewStyle>;
  itemStyle?: StyleProp<ViewStyle>;
  selectedItemStyle?: StyleProp<ViewStyle>;
  textStyle?: StyleProp<TextStyle>;
  selectedTextStyle?: StyleProp<TextStyle>;
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
}: SidebarPickerProps<T>) {
  return (
    <View style={{ borderWidth: 1, borderColor: '#ccc', borderRadius: 6, overflow: 'hidden', flex: 1, height: '100%' }}>
      <ScrollView style={[{ flex: 1 }, containerStyle]} contentContainerStyle={{ flexGrow: 1 }}>
        {items.map((item) => {
          const isSelected = selectedItem && getItemKey(item) === getItemKey(selectedItem);
          return (
            <TouchableOpacity
              key={getItemKey(item)}
              style={[itemStyle, isSelected && selectedItemStyle]}
              onPress={() => onSelect(item)}
            >
              <Text style={[textStyle, isSelected && selectedTextStyle]}>
                {getItemLabel(item)}
              </Text>
            </TouchableOpacity>
          );
        })}
      </ScrollView>
    </View>
  );
}

export default SidebarPicker;
