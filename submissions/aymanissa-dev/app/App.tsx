import { useEffect, useState } from 'react';
import { Image, Platform, Pressable, StyleSheet, Text, useColorScheme, useWindowDimensions, View } from 'react-native';
import { StatusBar } from 'expo-status-bar';

import OtpInput from './src/OtpInput';
import {
  COPY,
  createOtpState,
  isComplete,
  resendCountdown,
  restartResend,
  applyBackspace,
  applyInput,
  statusMessage,
  submitCode,
  tickResend,
  digitLayout,
} from './src/otp';
import { paletteFor } from './src/palette';

const FONT = Platform.select({ web: 'Inter, "Noto Sans", sans-serif', default: undefined });

const webBlur =
  Platform.OS === 'web'
    ? ({ filter: 'blur(8px)', WebkitBackdropFilter: 'blur(8px)' } as const)
    : null;

export default function App() {
  const scheme = useColorScheme();
  const palette = paletteFor(scheme);
  const isDark = scheme === 'dark';
  const { width } = useWindowDimensions();
  const layout = digitLayout(width);
  const [state, setState] = useState(createOtpState);
  const [resendRound, setResendRound] = useState(0);

  useEffect(() => {
    const timer = setInterval(() => {
      setState((current) => tickResend(current));
    }, 1000);
    return () => clearInterval(timer);
  }, [resendRound]);

  const canResend = state.resendSecondsLeft <= 0;
  const complete = isComplete(state);
  const message = statusMessage(state.status);
  const countdownLabel = resendCountdown(state.resendSecondsLeft);
  const logo = isDark ? require('./assets/thamani-logo-white.png') : require('./assets/thamani-logo.png');

  const resend = () => {
    setState((current) => restartResend(current));
    setResendRound((round) => round + 1);
  };

  return (
    <View style={[styles.root, { backgroundColor: palette.sheet }]}>
      <StatusBar style={isDark ? 'light' : 'dark'} />
      <View pointerEvents="none" style={styles.behind}>
        <Text style={[styles.behindTitle, webBlur, { color: palette.text_primary }]}>Welcome back</Text>
      </View>
      <View pointerEvents="none" style={[styles.scrim, { backgroundColor: palette.scrim }]} />
      <View style={[styles.sheet, { backgroundColor: palette.sheet, paddingHorizontal: layout.padding }]}>
        <Image source={logo} style={styles.logo} resizeMode="contain" accessibilityLabel="Thamani" />
        <Text style={[styles.title, { color: palette.text_primary }]}>{COPY.title}</Text>
        <Text style={[styles.subtitle, { color: palette.text_secondary }]}>{COPY.subtitle}</Text>
        <OtpInput
          digits={state.digits}
          focusedIndex={state.focusedIndex}
          palette={palette}
          boxSize={layout.size}
          gap={layout.gap}
          onInput={(index, text) => setState((current) => applyInput(current, index, text))}
          onBackspace={(index) => setState((current) => applyBackspace(current, index))}
          onFocus={(index) =>
            setState((current) => (current.focusedIndex === index ? current : { ...current, focusedIndex: index }))
          }
        />
        <View style={styles.resendRow}>
          {canResend ? (
            <Text style={[styles.resendPrompt, { color: palette.text_secondary }]}>{"Didn't receive the code? "}</Text>
          ) : null}
          <Pressable
            testID="otp_resend"
            accessibilityRole="button"
            accessibilityLabel={canResend ? COPY.resendIdle : countdownLabel}
            accessibilityState={{ disabled: !canResend }}
            disabled={!canResend}
            onPress={resend}
            hitSlop={8}
          >
            <Text style={[styles.resendAction, { color: canResend ? palette.link : palette.text_secondary }]}>
              {canResend ? COPY.resendAction : countdownLabel}
            </Text>
          </Pressable>
        </View>
        <Text
          testID="otp_status"
          accessibilityLiveRegion="polite"
          style={[
            styles.status,
            {
              color:
                state.status === 'success' ? palette.success : state.status === 'error' ? palette.error : palette.text_secondary,
            },
          ]}
        >
          {message}
        </Text>
        <View style={styles.spacer} />
        <Pressable
          testID="otp_submit"
          accessibilityRole="button"
          accessibilityState={{ disabled: !complete }}
          disabled={!complete}
          onPress={() => setState((current) => submitCode(current))}
          style={[
            styles.submit,
            { backgroundColor: complete ? palette.button_bg : palette.button_disabled_bg },
          ]}
        >
          <Text style={[styles.submitText, { color: palette.button_text }]}>{COPY.submit}</Text>
        </Pressable>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
  },
  behind: {
    ...StyleSheet.absoluteFill,
    alignItems: 'center',
    paddingTop: 56,
  },
  behindTitle: {
    fontFamily: FONT,
    fontSize: 28,
    fontWeight: '600',
    opacity: 0.45,
  },
  scrim: {
    ...StyleSheet.absoluteFill,
  },
  sheet: {
    position: 'absolute',
    left: 0,
    right: 0,
    bottom: 0,
    height: '86%',
    borderTopLeftRadius: 28,
    borderTopRightRadius: 28,
    paddingTop: 32,
    paddingBottom: 28,
    alignItems: 'center',
    shadowColor: '#000000',
    shadowOffset: { width: 0, height: -8 },
    shadowOpacity: 0.16,
    shadowRadius: 20,
    elevation: 16,
  },
  logo: {
    width: 78,
    height: 62,
    marginBottom: 22,
  },
  title: {
    fontFamily: FONT,
    fontSize: 26,
    lineHeight: 32,
    fontWeight: '700',
    textAlign: 'center',
  },
  subtitle: {
    fontFamily: FONT,
    fontSize: 15,
    lineHeight: 22,
    textAlign: 'center',
    marginTop: 8,
    marginBottom: 28,
  },
  resendRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'center',
    alignItems: 'center',
    marginTop: 26,
  },
  resendPrompt: {
    fontFamily: FONT,
    fontSize: 14,
    lineHeight: 20,
  },
  resendAction: {
    fontFamily: FONT,
    fontSize: 14,
    lineHeight: 20,
    fontWeight: '600',
  },
  status: {
    fontFamily: FONT,
    fontSize: 15,
    lineHeight: 22,
    textAlign: 'center',
    minHeight: 22,
    marginTop: 12,
  },
  spacer: {
    flex: 1,
    minHeight: 24,
  },
  submit: {
    alignSelf: 'stretch',
    height: 56,
    borderRadius: 28,
    alignItems: 'center',
    justifyContent: 'center',
  },
  submitText: {
    fontFamily: FONT,
    fontSize: 16,
    lineHeight: 22,
    fontWeight: '600',
  },
});
