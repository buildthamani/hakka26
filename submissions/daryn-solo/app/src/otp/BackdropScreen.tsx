import React from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { OtpTheme } from './theme';

interface BackdropScreenProps {
  theme: OtpTheme;
  isDark: boolean;
}

/** Stand-in for the screen the sheet slides over; it only exists to be dimmed. */
export function BackdropScreen({ theme, isDark }: BackdropScreenProps) {
  const field = isDark ? '#1D221E' : '#E4E7E5';
  return (
    <View
      style={[styles.screen, { backgroundColor: isDark ? '#0D100E' : '#F4F5F4' }]}
      accessibilityElementsHidden
      importantForAccessibility="no-hide-descendants"
    >
      <Text style={[styles.title, { color: theme.text_primary }]}>Welcome back</Text>
      <View style={[styles.field, { backgroundColor: field }]} />
      <View style={[styles.field, { backgroundColor: field }]} />
      <View style={[styles.button, { backgroundColor: theme.button_bg }]} />
    </View>
  );
}

const styles = StyleSheet.create({
  screen: {
    ...StyleSheet.absoluteFill,
    paddingHorizontal: 24,
    paddingTop: 96,
  },
  title: {
    textAlign: 'center',
    fontSize: 22,
    fontWeight: '600',
    marginBottom: 32,
  },
  field: {
    height: 52,
    borderRadius: 14,
    marginBottom: 16,
  },
  button: {
    height: 52,
    borderRadius: 26,
    marginTop: 8,
  },
});
