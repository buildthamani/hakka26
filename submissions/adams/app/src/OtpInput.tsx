import { Ref, useImperativeHandle, useRef, useState } from 'react';
import { StyleSheet, TextInput, TextInputInstance, TextInputKeyPressEvent, View } from 'react-native';

import { useOtpColours } from './theme';

export const OTP_LENGTH = 6;

export const emptyDigits = (): string[] => Array<string>(OTP_LENGTH).fill('');

export type OtpInputHandle = {
  focus: (index?: number) => void;
};

type Props = {
  digits: string[];
  onChange: (digits: string[]) => void;
  autoFocus?: boolean;
  editable?: boolean;
  ref?: Ref<OtpInputHandle>;
};

export default function OtpInput({ digits, onChange, autoFocus = true, editable = true, ref }: Props) {
  const colours = useOtpColours();
  const inputs = useRef<(TextInputInstance | null)[]>([]);
  const [focusedIndex, setFocusedIndex] = useState<number | null>(null);

  const focusAt = (index: number) => {
    const clamped = Math.max(0, Math.min(OTP_LENGTH - 1, index));
    inputs.current[clamped]?.focus();
  };

  useImperativeHandle(ref, () => ({ focus: (index = 0) => focusAt(index) }), []);

  const handleChangeText = (index: number, text: string) => {
    const typed = text.replace(/\D/g, '');
    const current = digits[index];

    // Non-digit input only (e.g. a letter): ignore it entirely.
    if (text.length > 0 && typed.length === 0) return;

    const next = [...digits];

    if (typed.length === 0) {
      next[index] = '';
      onChange(next);
      return;
    }

    if (typed.length === 1 || (typed.length === 2 && current !== '')) {
      // A single keystroke. If the cell already held a digit, the new text is
      // that digit plus the new one (either side of the caret): keep the new one.
      const digit = typed.length === 1 ? typed : typed[0] === current ? typed[1] : typed[0];
      next[index] = digit;
      onChange(next);
      if (index < OTP_LENGTH - 1) focusAt(index + 1);
      return;
    }

    // Paste: a full code fills from the first circle, a partial one from here.
    const start = typed.length >= OTP_LENGTH ? 0 : index;
    const chars = typed.slice(0, OTP_LENGTH - start).split('');
    chars.forEach((c, i) => {
      next[start + i] = c;
    });
    onChange(next);
    focusAt(start + chars.length);
  };

  const handleKeyPress = (index: number, e: TextInputKeyPressEvent) => {
    if (e.nativeEvent.key !== 'Backspace') return;
    if (digits[index] !== '' || index === 0) return;
    const next = [...digits];
    next[index - 1] = '';
    onChange(next);
    focusAt(index - 1);
  };

  return (
    <View style={styles.row}>
      {digits.map((digit, index) => {
        const filled = digit !== '';
        const focused = focusedIndex === index;
        return (
          <TextInput
            key={index}
            ref={(el) => {
              inputs.current[index] = el;
            }}
            testID={`otp_digit_${index + 1}`}
            accessibilityLabel={`Digit ${index + 1} of ${OTP_LENGTH}`}
            value={digit}
            placeholder="-"
            placeholderTextColor={colours.digit_placeholder}
            onChangeText={(text) => handleChangeText(index, text)}
            onKeyPress={(e) => handleKeyPress(index, e)}
            onFocus={() => setFocusedIndex(index)}
            onBlur={() => setFocusedIndex((f) => (f === index ? null : f))}
            autoFocus={autoFocus && index === 0}
            editable={editable}
            keyboardType="number-pad"
            inputMode="numeric"
            textContentType={index === 0 ? 'oneTimeCode' : 'none'}
            autoComplete={index === 0 ? 'sms-otp' : 'off'}
            selectTextOnFocus
            caretHidden
            style={[
              styles.circle,
              filled
                ? { backgroundColor: colours.digit_filled_bg, borderColor: colours.digit_filled_bg, color: colours.digit_filled_text }
                : focused
                  ? { backgroundColor: colours.digit_focused_bg, borderColor: colours.digit_focused_outline, color: colours.text_primary }
                  : { backgroundColor: colours.digit_empty_bg, borderColor: colours.digit_empty_outline, color: colours.text_primary },
              focused && { borderColor: colours.digit_focused_outline, borderWidth: 2 },
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
    gap: 8,
  },
  circle: {
    width: 44,
    height: 44,
    borderRadius: 22,
    borderWidth: 1,
    padding: 0,
    textAlign: 'center',
    fontSize: 20,
    fontWeight: '600',
  },
});
