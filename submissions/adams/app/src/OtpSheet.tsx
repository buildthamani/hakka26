import { useEffect, useRef, useState } from 'react';
import { Image, KeyboardAvoidingView, Platform, Pressable, StyleSheet, Text, View } from 'react-native';

import OtpInput, { emptyDigits, OTP_LENGTH, OtpInputHandle } from './OtpInput';
import { useOtpColours, useScheme } from './theme';

export const VALID_CODE = '123456';
export const RESEND_SECONDS = 30;

export const COPY = {
  title: 'Enter your code',
  subtitle: 'A 6-digit code has been sent to your phone.',
  resendPrompt: "Didn't receive the code? ",
  resend: 'Resend',
  submit: 'Submit',
  success: "You're in.",
  error: "That code didn't work. Try again.",
} as const;

export const formatCountdown = (seconds: number) => `Resend in 0:${String(seconds).padStart(2, '0')}`;

type Status = { kind: 'success' | 'error'; message: string } | null;

const logos = {
  light: require('../assets/thamani-logo.png'),
  dark: require('../assets/thamani-logo-white.png'),
};

export default function OtpSheet() {
  const colours = useOtpColours();
  const scheme = useScheme();
  const otpRef = useRef<OtpInputHandle>(null);

  const [digits, setDigits] = useState<string[]>(emptyDigits);
  const [status, setStatus] = useState<Status>(null);
  // The code was just sent when the sheet opens, so the cooldown starts now.
  const [secondsLeft, setSecondsLeft] = useState(RESEND_SECONDS);

  const counting = secondsLeft > 0;
  useEffect(() => {
    if (!counting) return;
    const id = setInterval(() => setSecondsLeft((s) => Math.max(0, s - 1)), 1000);
    return () => clearInterval(id);
  }, [counting]);

  const code = digits.join('');
  const complete = code.length === OTP_LENGTH;
  const succeeded = status?.kind === 'success';
  const canResend = secondsLeft === 0;

  const handleChange = (next: string[]) => {
    setDigits(next);
    if (status?.kind === 'error') setStatus(null);
  };

  const handleSubmit = () => {
    if (!complete) return;
    if (code === VALID_CODE) {
      setStatus({ kind: 'success', message: COPY.success });
      return;
    }
    setStatus({ kind: 'error', message: COPY.error });
    setDigits(emptyDigits());
    otpRef.current?.focus(0);
  };

  const handleResend = () => {
    if (!canResend) return;
    setSecondsLeft(RESEND_SECONDS);
  };

  return (
    <View style={[styles.scrim, { backgroundColor: colours.scrim }]} testID="otp_scrim">
      <KeyboardAvoidingView
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
        style={styles.avoider}
      >
        <View
          style={[styles.sheet, { backgroundColor: colours.sheet }]}
          accessibilityViewIsModal
          testID="otp_sheet"
        >
          <View style={[styles.handle, { backgroundColor: colours.digit_empty_outline }]} />

          <Image
            source={logos[scheme]}
            style={styles.logo}
            resizeMode="contain"
            accessibilityLabel="Thamani logo"
          />

          <Text style={[styles.title, { color: colours.text_primary }]} accessibilityRole="header">
            {COPY.title}
          </Text>
          <Text style={[styles.subtitle, { color: colours.text_secondary }]}>{COPY.subtitle}</Text>

          <View style={styles.otp}>
            <OtpInput ref={otpRef} digits={digits} onChange={handleChange} editable={!succeeded} />
          </View>

          <Text
            testID="otp_status"
            accessibilityLiveRegion="polite"
            style={[
              styles.status,
              { color: status?.kind === 'success' ? colours.success : colours.error },
            ]}
          >
            {status?.message ?? ''}
          </Text>

          <View style={styles.resendRow}>
            <Text style={[styles.resendPrompt, { color: colours.text_secondary }]}>{COPY.resendPrompt}</Text>
            <Pressable
              testID="otp_resend"
              onPress={handleResend}
              disabled={!canResend}
              accessibilityRole="button"
              accessibilityState={{ disabled: !canResend }}
              hitSlop={8}
            >
              <Text
                style={[
                  styles.resend,
                  { color: canResend ? colours.link : colours.text_secondary },
                ]}
              >
                {canResend ? COPY.resend : formatCountdown(secondsLeft)}
              </Text>
            </Pressable>
          </View>

          <Pressable
            testID="otp_submit"
            onPress={handleSubmit}
            disabled={!complete || succeeded}
            accessibilityRole="button"
            accessibilityState={{ disabled: !complete || succeeded }}
            style={({ pressed }) => [
              styles.button,
              { backgroundColor: complete && !succeeded ? colours.button_bg : colours.button_disabled_bg },
              pressed && styles.buttonPressed,
            ]}
          >
            <Text style={[styles.buttonText, { color: colours.button_text }]}>{COPY.submit}</Text>
          </Pressable>
        </View>
      </KeyboardAvoidingView>
    </View>
  );
}

const styles = StyleSheet.create({
  scrim: {
    ...StyleSheet.absoluteFill,
    justifyContent: 'flex-end',
  },
  avoider: {
    flex: 1,
    justifyContent: 'flex-end',
  },
  sheet: {
    borderTopLeftRadius: 24,
    borderTopRightRadius: 24,
    paddingHorizontal: 16,
    paddingTop: 12,
    paddingBottom: 32,
    alignItems: 'center',
  },
  handle: {
    width: 40,
    height: 4,
    borderRadius: 2,
    marginBottom: 16,
  },
  logo: {
    width: 56,
    height: 45,
    marginBottom: 16,
  },
  title: {
    fontSize: 22,
    fontWeight: '700',
    textAlign: 'center',
  },
  subtitle: {
    fontSize: 15,
    marginTop: 8,
    textAlign: 'center',
  },
  otp: {
    marginTop: 24,
  },
  status: {
    minHeight: 20,
    marginTop: 12,
    fontSize: 14,
    textAlign: 'center',
  },
  resendRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
    flexWrap: 'wrap',
    marginTop: 8,
  },
  resendPrompt: {
    fontSize: 14,
  },
  resend: {
    fontSize: 14,
    fontWeight: '600',
    fontVariant: ['tabular-nums'],
  },
  button: {
    alignSelf: 'stretch',
    height: 52,
    borderRadius: 26,
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: 24,
  },
  buttonPressed: {
    opacity: 0.85,
  },
  buttonText: {
    fontSize: 16,
    fontWeight: '600',
  },
});
