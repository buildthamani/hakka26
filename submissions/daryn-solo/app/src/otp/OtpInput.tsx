import React, { useEffect, useRef } from 'react';
import { StyleSheet, TextInput, View } from 'react-native';
import { copy } from './copy';
import { OtpDigits } from './otp';
import { OtpTheme } from './theme';

interface OtpInputProps {
  digits: OtpDigits;
  focusedIndex: number;
  focusRequestId: number;
  editable: boolean;
  theme: OtpTheme;
  onChangeDigit: (index: number, text: string) => void;
  onBackspace: (index: number) => void;
  onBoxFocus: (index: number) => void;
}

const BOX_SIZE = 44;

/** Six circular, single-digit boxes. Owns focus handling; holds no code state. */
export function OtpInput({
  digits,
  focusedIndex,
  focusRequestId,
  editable,
  theme,
  onChangeDigit,
  onBackspace,
  onBoxFocus,
}: OtpInputProps) {
  const inputs = useRef<(React.ComponentRef<typeof TextInput> | null)[]>([]);

  useEffect(() => {
    inputs.current[focusedIndex]?.focus();
    // Re-run on every focus request, even when the target box is unchanged.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [focusRequestId]);

  return (
    <View style={styles.row}>
      {digits.map((digit, index) => {
        const isFilled = digit !== '';
        const isFocused = index === focusedIndex && !isFilled;
        return (
          <TextInput
            key={index}
            ref={(node) => {
              inputs.current[index] = node;
            }}
            testID={`otp_digit_${index + 1}`}
            accessibilityLabel={copy.digitLabel(index + 1)}
            value={digit}
            placeholder={copy.placeholder}
            placeholderTextColor={theme.digit_placeholder}
            keyboardType="number-pad"
            inputMode="numeric"
            textContentType="oneTimeCode"
            autoComplete={index === 0 ? 'sms-otp' : 'off'}
            autoFocus={index === 0}
            editable={editable}
            selectTextOnFocus
            caretHidden
            onChangeText={(text) => onChangeDigit(index, text)}
            onKeyPress={({ nativeEvent }) => {
              if (nativeEvent.key === 'Backspace') onBackspace(index);
            }}
            onFocus={() => onBoxFocus(index)}
            style={[
              styles.box,
              {
                backgroundColor: isFilled
                  ? theme.digit_filled_bg
                  : isFocused
                    ? theme.digit_focused_bg
                    : theme.digit_empty_bg,
                borderColor: isFilled
                  ? theme.digit_filled_bg
                  : isFocused
                    ? theme.digit_focused_outline
                    : theme.digit_empty_outline,
                color: isFilled ? theme.digit_filled_text : theme.text_primary,
              },
              isFocused && styles.boxFocused,
            ]}
          />
        );
      })}
    </View>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    justifyContent: 'center',
    gap: 10,
  },
  box: {
    width: BOX_SIZE,
    height: BOX_SIZE,
    borderRadius: BOX_SIZE / 2,
    borderWidth: 1,
    textAlign: 'center',
    fontSize: 18,
    fontWeight: '500',
    padding: 0,
  },
  boxFocused: {
    borderWidth: 2,
  },
});
