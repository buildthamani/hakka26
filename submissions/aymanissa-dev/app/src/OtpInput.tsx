import { useEffect, useRef } from 'react';
import { Platform, StyleSheet, Text, TextInput, View } from 'react-native';

import { OtpPalette } from './palette';

const PLACEHOLDER = '–';
const FONT = Platform.select({ web: 'Inter, "Noto Sans", sans-serif', default: undefined });

type OtpInputProps = {
  digits: readonly string[];
  focusedIndex: number;
  onInput: (index: number, text: string) => void;
  onBackspace: (index: number) => void;
  onFocus: (index: number) => void;
  palette: OtpPalette;
  boxSize: number;
  gap: number;
};

export default function OtpInput({
  digits,
  focusedIndex,
  onInput,
  onBackspace,
  onFocus,
  palette,
  boxSize,
  gap,
}: OtpInputProps) {
  const inputs = useRef<Array<{ focus: () => void } | null>>([]);

  useEffect(() => {
    inputs.current[focusedIndex]?.focus();
  }, [focusedIndex]);

  return (
    <View style={[styles.row, { gap }]}>
      {digits.map((digit, index) => {
        const focused = focusedIndex === index;
        const filled = digit.length > 0;
        const backgroundColor = filled
          ? palette.digit_filled_bg
          : focused
            ? palette.digit_focused_bg
            : palette.digit_empty_bg;
        const borderColor = filled
          ? palette.digit_filled_bg
          : focused
            ? palette.digit_focused_outline
            : palette.digit_empty_outline;
        const borderWidth = focused && !filled ? 2 : 1;

        const onKeyPress = (event: { nativeEvent: { key: string } }) => {
          if (event.nativeEvent.key === 'Backspace' && digit.length === 0) {
            onBackspace(index);
          }
        };

        const onChangeText = (text: string) => {
          if (text === '') {
            onBackspace(index);
            return;
          }
          onInput(index, text);
        };

        return (
          <View
            key={index}
            style={[
              styles.box,
              {
                width: boxSize,
                height: boxSize,
                borderRadius: boxSize / 2,
                backgroundColor,
                borderColor,
                borderWidth,
              },
            ]}
          >
            <TextInput
              ref={(node) => {
                inputs.current[index] = node as { focus: () => void } | null;
              }}
              testID={`otp_digit_${index + 1}`}
              accessibilityLabel={`Digit ${index + 1} of 6`}
              value={digit}
              onChangeText={onChangeText}
              onKeyPress={onKeyPress}
              onFocus={() => onFocus(index)}
              keyboardType="number-pad"
              inputMode="numeric"
              autoFocus={index === 0}
              autoComplete="sms-otp"
              textContentType="oneTimeCode"
              caretHidden
              selectTextOnFocus
              underlineColorAndroid="transparent"
              importantForAutofill="yes"
              style={[
                styles.input,
                { fontSize: Math.round(boxSize * 0.4) },
                Platform.OS === 'web' ? ({ outlineStyle: 'none', outlineWidth: 0 } as object) : null,
              ]}
              maxLength={80}
            />
            {filled ? (
              <Text
                pointerEvents="none"
                importantForAccessibility="no"
                style={[styles.glyph, { color: palette.digit_filled_text, fontSize: Math.round(boxSize * 0.4) }]}
              >
                {digit}
              </Text>
            ) : focused ? (
              <View
                pointerEvents="none"
                style={[styles.caret, { backgroundColor: palette.digit_focused_outline, height: Math.round(boxSize * 0.38) }]}
              />
            ) : (
              <Text
                pointerEvents="none"
                importantForAccessibility="no"
                style={[styles.glyph, { color: palette.digit_placeholder, fontSize: Math.round(boxSize * 0.42) }]}
              >
                {PLACEHOLDER}
              </Text>
            )}
          </View>
        );
      })}
    </View>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'center',
  },
  box: {
    alignItems: 'center',
    justifyContent: 'center',
    overflow: 'hidden',
  },
  glyph: {
    fontFamily: FONT,
    fontWeight: '600',
    textAlign: 'center',
  },
  caret: {
    width: 2,
    borderRadius: 1,
  },
  input: {
    ...StyleSheet.absoluteFill,
    color: 'transparent',
    backgroundColor: 'transparent',
    textAlign: 'center',
    fontFamily: FONT,
    padding: 0,
    margin: 0,
    borderWidth: 0,
  },
});
