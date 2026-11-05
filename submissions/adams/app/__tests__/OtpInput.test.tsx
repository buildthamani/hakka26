import React, { useState } from 'react';
import { StyleSheet } from 'react-native';
import ReactTestRenderer, { act, ReactTestInstance, ReactTestRenderer as Renderer } from 'react-test-renderer';

import OtpInput, { emptyDigits } from '../src/OtpInput';
import { otpColours } from '../src/theme';

const light = otpColours.light;

function Harness({ initial = emptyDigits() }: { initial?: string[] }) {
  const [digits, setDigits] = useState(initial);
  return <OtpInput digits={digits} onChange={setDigits} />;
}

let renderer: Renderer;
let focused: number | null;

const digit = (n: number): ReactTestInstance =>
  renderer.root.findAll((node) => node.props.testID === `otp_digit_${n}`)[0];

const values = () => [1, 2, 3, 4, 5, 6].map((n) => digit(n).props.value);

const styleOf = (n: number) => StyleSheet.flatten(digit(n).props.style);

// The jest TextInput mock does not fire focus events, so emulate them.
function installFocusSpies() {
  focused = null;
  [1, 2, 3, 4, 5, 6].forEach((n) => {
    const node = digit(n);
    // The mock's focus() is one jest.fn shared by every TextInput, so give
    // each instance its own.
    node.instance.focus = jest.fn(() => {
      if (focused !== null && focused !== n) digit(focused).props.onBlur?.({});
      focused = n;
      node.props.onFocus?.({});
    });
  });
}

async function mount(initial?: string[]) {
  await act(async () => {
    renderer = ReactTestRenderer.create(<Harness initial={initial} />);
  });
  installFocusSpies();
}

async function type(n: number, text: string) {
  await act(async () => digit(n).props.onChangeText(text));
}

async function backspace(n: number) {
  await act(async () => digit(n).props.onKeyPress({ nativeEvent: { key: 'Backspace' } }));
  // A real input emits onChangeText('') only when it had something to delete.
  if (digit(n).props.value !== '') await type(n, '');
}

async function focus(n: number) {
  await act(async () => digit(n).instance.focus());
}

afterEach(async () => {
  await act(async () => renderer.unmount());
});

describe('OtpInput', () => {
  it('renders six empty circles with labels, placeholders and testIDs', async () => {
    await mount();
    [1, 2, 3, 4, 5, 6].forEach((n) => {
      const node = digit(n);
      expect(node.props.accessibilityLabel).toBe(`Digit ${n} of 6`);
      expect(node.props.placeholder).toBe('-');
      expect(node.props.placeholderTextColor).toBe(light.digit_placeholder);
      expect(node.props.value).toBe('');
      expect(node.props.keyboardType).toBe('number-pad');
    });
    expect(renderer.root.findAll((node) => /^otp_digit_\d$/.test(node.props.testID ?? '') && node.instance)).toHaveLength(6);
  });

  it('autofocuses only the first circle', async () => {
    await mount();
    expect(digit(1).props.autoFocus).toBe(true);
    [2, 3, 4, 5, 6].forEach((n) => expect(digit(n).props.autoFocus).toBe(false));
  });

  it('typing a digit fills the circle and advances focus', async () => {
    await mount();
    await focus(1);
    await type(1, '4');
    expect(values()).toEqual(['4', '', '', '', '', '']);
    expect(focused).toBe(2);
    await type(2, '2');
    expect(focused).toBe(3);
    expect(values()).toEqual(['4', '2', '', '', '', '']);
  });

  it('ignores non-digits', async () => {
    await mount();
    await type(1, 'a');
    await type(1, ' ');
    expect(values()).toEqual(['', '', '', '', '', '']);
    await type(1, 'x7');
    expect(values()[0]).toBe('7');
  });

  it('typing over a filled circle replaces its digit', async () => {
    await mount(['1', '', '', '', '', '']);
    await type(1, '19'); // caret after the old digit
    expect(values()[0]).toBe('9');
    await type(1, '39'); // caret before the old digit
    expect(values()[0]).toBe('3');
  });

  it('paste extracts digits and fills every circle', async () => {
    await mount();
    await focus(1);
    await type(1, 'Your code: 123-456');
    expect(values()).toEqual(['1', '2', '3', '4', '5', '6']);
    expect(focused).toBe(6);
  });

  it('a full-length paste into a later circle still fills from the first', async () => {
    await mount();
    await type(4, '987654');
    expect(values()).toEqual(['9', '8', '7', '6', '5', '4']);
  });

  it('a partial paste fills forward from the current circle', async () => {
    await mount();
    await type(2, '5 6 7');
    expect(values()).toEqual(['', '5', '6', '7', '', '']);
    expect(focused).toBe(5);
  });

  it('backspace on a filled circle clears it and stays put', async () => {
    await mount(['1', '2', '3', '', '', '']);
    await focus(3);
    await backspace(3);
    expect(values()).toEqual(['1', '2', '', '', '', '']);
    expect(focused).toBe(3);
  });

  it('backspace on an empty circle clears the previous one and moves back', async () => {
    await mount(['1', '2', '3', '', '', '']);
    await focus(4);
    await backspace(4);
    expect(values()).toEqual(['1', '2', '', '', '', '']);
    expect(focused).toBe(3);
  });

  it('backspace on the empty first circle does nothing', async () => {
    await mount();
    await focus(1);
    await backspace(1);
    expect(values()).toEqual(['', '', '', '', '', '']);
    expect(focused).toBe(1);
  });

  it('styles circles as filled (green), focused (outlined) and empty (grey)', async () => {
    await mount(['5', '', '', '', '', '']);
    await focus(2);

    const filled = styleOf(1);
    expect(filled.backgroundColor).toBe(light.digit_filled_bg);
    expect(filled.color).toBe(light.digit_filled_text);

    const focusedStyle = styleOf(2);
    expect(focusedStyle.backgroundColor).toBe(light.digit_focused_bg);
    expect(focusedStyle.borderColor).toBe(light.digit_focused_outline);
    expect(focusedStyle.borderWidth).toBe(2);

    const empty = styleOf(3);
    expect(empty.backgroundColor).toBe(light.digit_empty_bg);
    expect(empty.borderColor).toBe(light.digit_empty_outline);
    expect(empty.borderWidth).toBe(1);

    expect(light.digit_filled_bg).toBe('#12361B');
    expect(light.digit_empty_bg).toBe('#F1F1F1');
  });
});
