import React from 'react';
import { Image, KeyboardAvoidingView, Platform, Pressable, StyleSheet, Text, View } from 'react-native';
import { BackdropScreen } from './BackdropScreen';
import { copy } from './copy';
import { OtpInput } from './OtpInput';
import { ResendLine } from './ResendLine';
import { argbToRgba, useOtpTheme } from './theme';
import { useOtp } from './useOtp';

const LOGO_FOR_LIGHT_THEME = require('../../assets/thamani-logo.png');
const LOGO_FOR_DARK_THEME = require('../../assets/thamani-logo-white.png');

/** The OTP verification screen: a bottom sheet over a dimmed backdrop. */
export function OtpSheet() {
  const { theme, isDark } = useOtpTheme();
  const otp = useOtp();

  const statusColor = otp.status === 'success' ? theme.success : theme.error;

  return (
    <View style={styles.backdrop}>
      <BackdropScreen theme={theme} isDark={isDark} />
      <View style={[styles.scrim, { backgroundColor: argbToRgba(theme.scrim) }]} />

      <KeyboardAvoidingView
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
        style={styles.sheetHost}
      >
        <View style={[styles.sheet, { backgroundColor: theme.sheet }]}>
          <Image
            source={isDark ? LOGO_FOR_DARK_THEME : LOGO_FOR_LIGHT_THEME}
            style={styles.logo}
            resizeMode="contain"
            accessibilityLabel="Thamani logo"
          />

          <Text accessibilityRole="header" style={[styles.title, { color: theme.text_primary }]}>
            {copy.title}
          </Text>
          <Text style={[styles.subtitle, { color: theme.text_secondary }]}>{copy.subtitle}</Text>

          <View style={styles.inputBlock}>
            <OtpInput
              digits={otp.digits}
              focusedIndex={otp.focusedIndex}
              focusRequestId={otp.focusRequestId}
              editable={!otp.isLocked}
              theme={theme}
              onChangeDigit={otp.changeDigit}
              onBackspace={otp.backspace}
              onBoxFocus={otp.boxFocused}
            />
          </View>

          <ResendLine theme={theme} />

          <Text
            testID="otp_status"
            accessibilityLiveRegion="polite"
            style={[styles.status, { color: statusColor }]}
          >
            {otp.status === 'success' ? copy.success : otp.status === 'error' ? copy.error : ''}
          </Text>

          <View style={styles.spacer} />

          <Pressable
            testID="otp_submit"
            accessibilityRole="button"
            accessibilityState={{ disabled: !otp.canSubmit }}
            disabled={!otp.canSubmit}
            onPress={otp.submit}
            style={({ pressed }) => [
              styles.submit,
              {
                backgroundColor: otp.canSubmit ? theme.button_bg : theme.button_disabled_bg,
                opacity: pressed ? 0.85 : 1,
              },
            ]}
          >
            <Text style={[styles.submitLabel, { color: theme.button_text }]}>{copy.submit}</Text>
          </Pressable>
        </View>
      </KeyboardAvoidingView>
    </View>
  );
}

const styles = StyleSheet.create({
  backdrop: {
    flex: 1,
  },
  scrim: {
    ...StyleSheet.absoluteFill,
  },
  sheetHost: {
    ...StyleSheet.absoluteFill,
    justifyContent: 'flex-end',
    padding: 12,
  },
  sheet: {
    minHeight: '78%',
    borderRadius: 32,
    paddingHorizontal: 24,
    paddingTop: 36,
    paddingBottom: 32,
    alignItems: 'center',
  },
  logo: {
    width: 72,
    height: 72,
    marginBottom: 20,
  },
  title: {
    fontSize: 28,
    fontWeight: '600',
    textAlign: 'center',
  },
  subtitle: {
    marginTop: 12,
    maxWidth: 260,
    fontSize: 15,
    lineHeight: 22,
    textAlign: 'center',
  },
  inputBlock: {
    marginTop: 32,
    marginBottom: 24,
  },
  status: {
    marginTop: 16,
    minHeight: 22,
    fontSize: 15,
    fontWeight: '500',
    textAlign: 'center',
  },
  spacer: {
    flex: 1,
  },
  submit: {
    alignSelf: 'stretch',
    height: 56,
    borderRadius: 28,
    alignItems: 'center',
    justifyContent: 'center',
  },
  submitLabel: {
    fontSize: 16,
    fontWeight: '600',
  },
});
