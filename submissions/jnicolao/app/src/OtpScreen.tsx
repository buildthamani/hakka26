import { useEffect, useReducer } from 'react';
import { Image, KeyboardAvoidingView, Platform, Pressable, StyleSheet, Text, View } from 'react-native';
import OtpInput from './OtpInput';
import { canResend, copy, initialState, isComplete, otpReducer, resendLabel, statusMessage } from './otp';
import { useOtpColours } from './theme';

export default function OtpScreen() {
  const colours = useOtpColours();
  const [state, dispatch] = useReducer(otpReducer, initialState);
  const complete = isComplete(state);
  const resendReady = canResend(state);

  useEffect(() => {
    if (state.secondsLeft === 0) {
      return;
    }
    const timer = setTimeout(() => dispatch({ type: 'tick' }), 1000);
    return () => clearTimeout(timer);
  }, [state.secondsLeft]);

  return (
    <View style={[styles.screen, { backgroundColor: colours.sheet }]}>
      <Text
        style={[styles.behindSheet, { color: colours.text_primary }]}
        accessibilityElementsHidden
        importantForAccessibility="no-hide-descendants">
        Welcome back
      </Text>
      <View style={[StyleSheet.absoluteFill, { backgroundColor: colours.scrim }]} />
      <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined} style={[StyleSheet.absoluteFill, styles.sheetArea]}>
        <View style={[styles.sheet, { backgroundColor: colours.sheet }]}>
          <Image
            source={require('../assets/thamani-logo.png')}
            style={styles.logo}
            resizeMode="contain"
            accessibilityLabel="Thamani logo"
          />
          <Text style={[styles.title, { color: colours.text_primary }]}>{copy.title}</Text>
          <Text style={[styles.subtitle, { color: colours.text_secondary }]}>{copy.subtitle}</Text>
          <View style={styles.digits}>
            <OtpInput
              digits={state.digits}
              focusedIndex={state.focusedIndex}
              colours={colours}
              onInput={(index, text) => dispatch({ type: 'input', index, text })}
              onBackspace={index => dispatch({ type: 'backspace', index })}
              onFocus={index => dispatch({ type: 'focus', index })}
            />
          </View>
          <View style={styles.resendRow}>
            <Text style={[styles.resendText, { color: colours.text_secondary }]}>{copy.resendQuestion}</Text>
            <Pressable testID="otp_resend" accessibilityRole="button" disabled={!resendReady} onPress={() => dispatch({ type: 'resend' })}>
              <Text style={[styles.resendText, styles.resendLink, { color: resendReady ? colours.link : colours.text_secondary }]}>
                {resendLabel(state.secondsLeft)}
              </Text>
            </Pressable>
          </View>
          <Text
            testID="otp_status"
            accessibilityLiveRegion="polite"
            style={[styles.status, { color: state.status === 'success' ? colours.success : colours.error }]}>
            {statusMessage[state.status]}
          </Text>
          <View style={styles.spacer} />
          <Pressable
            testID="otp_submit"
            accessibilityRole="button"
            disabled={!complete}
            onPress={() => dispatch({ type: 'submit' })}
            style={[styles.submit, { backgroundColor: complete ? colours.button_bg : colours.button_disabled_bg }]}>
            <Text style={[styles.submitText, { color: colours.button_text }]}>{copy.submit}</Text>
          </Pressable>
        </View>
      </KeyboardAvoidingView>
    </View>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
  },
  behindSheet: {
    marginTop: 72,
    textAlign: 'center',
    fontSize: 28,
    fontWeight: '600',
    opacity: 0.4,
  },
  sheetArea: {
    justifyContent: 'flex-end',
  },
  sheet: {
    minHeight: '86%',
    borderTopLeftRadius: 28,
    borderTopRightRadius: 28,
    paddingHorizontal: 24,
    paddingTop: 32,
    paddingBottom: 32,
    alignItems: 'center',
  },
  logo: {
    width: 64,
    height: 64,
  },
  title: {
    marginTop: 20,
    fontSize: 26,
    fontWeight: '600',
  },
  subtitle: {
    marginTop: 8,
    fontSize: 15,
    lineHeight: 22,
    textAlign: 'center',
  },
  digits: {
    marginTop: 36,
  },
  resendRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
    marginTop: 28,
  },
  resendText: {
    fontSize: 14,
  },
  resendLink: {
    fontWeight: '600',
  },
  status: {
    marginTop: 16,
    fontSize: 15,
    minHeight: 20,
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
  submitText: {
    fontSize: 16,
    fontWeight: '600',
  },
});
