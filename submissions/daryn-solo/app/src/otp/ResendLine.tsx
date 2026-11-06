import React from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { copy } from './copy';
import { formatCountdown, RESEND_SECONDS } from './otp';
import { OtpTheme } from './theme';
import { useCountdown } from './useCountdown';

interface ResendLineProps {
  theme: OtpTheme;
  onResend?: () => void;
}

/** "Didn't receive the code? Resend", disabled during a 30 s countdown. */
export function ResendLine({ theme, onResend }: ResendLineProps) {
  const { remaining, isRunning, restart } = useCountdown(RESEND_SECONDS);

  const handleResend = () => {
    if (isRunning) return;
    onResend?.();
    restart();
  };

  return (
    <View style={styles.row}>
      <Text style={[styles.text, { color: theme.text_secondary }]}>{copy.resendPrompt} </Text>
      <Pressable
        testID="otp_resend"
        accessibilityRole="button"
        accessibilityState={{ disabled: isRunning }}
        disabled={isRunning}
        onPress={handleResend}
        hitSlop={8}
      >
        <Text
          style={[
            styles.text,
            styles.action,
            { color: isRunning ? theme.text_secondary : theme.link },
          ]}
        >
          {isRunning ? copy.resendIn(formatCountdown(remaining)) : copy.resend}
        </Text>
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'center',
    alignItems: 'center',
  },
  text: {
    fontSize: 14,
  },
  action: {
    fontWeight: '600',
  },
});
