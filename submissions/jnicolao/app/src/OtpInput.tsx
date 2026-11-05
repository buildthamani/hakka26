import { ComponentRef, useEffect, useRef } from 'react';
import { StyleSheet, TextInput, View } from 'react-native';
import { OTP_LENGTH } from './otp';
import { OtpColours } from './theme';

type Props = {
  digits: string[];
  focusedIndex: number;
  colours: OtpColours;
  onInput: (index: number, text: string) => void;
  onBackspace: (index: number) => void;
  onFocus: (index: number) => void;
};

export default function OtpInput({ digits, focusedIndex, colours, onInput, onBackspace, onFocus }: Props) {
  const inputs = useRef<(ComponentRef<typeof TextInput> | null)[]>([]);

  useEffect(() => {
    inputs.current[focusedIndex]?.focus();
  }, [focusedIndex]);

  return (
    <View style={styles.row}>
      {digits.map((digit, index) => {
        const focused = index === focusedIndex;
        return (
          <TextInput
            key={index}
            ref={ref => {
              inputs.current[index] = ref;
            }}
            testID={`otp_digit_${index + 1}`}
            accessibilityLabel={`Digit ${index + 1} of ${OTP_LENGTH}`}
            value={digit}
            placeholder={focused ? undefined : '-'}
            placeholderTextColor={colours.digit_placeholder}
            keyboardType="number-pad"
            inputMode="numeric"
            autoFocus={index === 0}
            cursorColor={colours.digit_focused_outline}
            selectionColor={colours.digit_focused_outline}
            onFocus={() => onFocus(index)}
            onChangeText={text => onInput(index, text)}
            onKeyPress={({ nativeEvent }) => {
              if (nativeEvent.key === 'Backspace' && !digit) {
                onBackspace(index);
              }
            }}
            style={[styles.box, boxStyle(digit, focused, colours)]}
          />
        );
      })}
    </View>
  );
}

function boxStyle(digit: string, focused: boolean, colours: OtpColours) {
  if (digit) {
    return { backgroundColor: colours.digit_filled_bg, borderColor: colours.digit_filled_bg, color: colours.digit_filled_text };
  }
  if (focused) {
    return {
      backgroundColor: colours.digit_focused_bg,
      borderColor: colours.digit_focused_outline,
      borderWidth: 2,
      color: colours.digit_focused_outline,
    };
  }
  return { backgroundColor: colours.digit_empty_bg, borderColor: colours.digit_empty_outline, color: colours.digit_focused_outline };
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    gap: 10,
  },
  box: {
    width: 46,
    height: 46,
    borderRadius: 23,
    borderWidth: 1,
    padding: 0,
    textAlign: 'center',
    fontSize: 20,
    fontWeight: '600',
  },
});
