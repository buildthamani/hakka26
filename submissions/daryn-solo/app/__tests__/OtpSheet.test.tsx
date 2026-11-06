import React from 'react';
import ReactTestRenderer, { ReactTestInstance } from 'react-test-renderer';
import App from '../App';

jest.useFakeTimers();

let renderer: ReactTestRenderer.ReactTestRenderer;

const mount = async () => {
  await ReactTestRenderer.act(() => {
    renderer = ReactTestRenderer.create(<App />);
  });
};

afterEach(async () => {
  await ReactTestRenderer.act(() => renderer.unmount());
});

/** Host node carrying the testID (the one that receives native props). */
const byId = (id: string): ReactTestInstance =>
  renderer.root.findAll((node) => node.props.testID === id && typeof node.type === 'string')[0];

/** Nearest node with the testID that exposes the given handler prop. */
const withHandler = (id: string, handler: string): ReactTestInstance =>
  renderer.root.findAll(
    (node) => node.props.testID === id && typeof node.props[handler] === 'function',
  )[0];

const digitValues = () =>
  [1, 2, 3, 4, 5, 6].map((n) => byId(`otp_digit_${n}`).props.value as string);

const type = async (index: number, text: string) => {
  await ReactTestRenderer.act(() => {
    withHandler(`otp_digit_${index}`, 'onChangeText').props.onChangeText(text);
  });
};

const typeCode = async (code: string) => {
  for (let i = 0; i < code.length; i += 1) {
    await type(i + 1, code[i]);
  }
};

const press = async (id: string) => {
  await ReactTestRenderer.act(() => {
    withHandler(id, 'onPress').props.onPress();
  });
};

const textOf = (id: string) => {
  const node = byId(id);
  return JSON.stringify(node.children);
};

describe('OTP screen', () => {
  it('renders six accessible, digit-only boxes with the required test IDs', async () => {
    await mount();
    for (let n = 1; n <= 6; n += 1) {
      const box = byId(`otp_digit_${n}`);
      expect(box.props.accessibilityLabel).toBe(`Digit ${n} of 6`);
      expect(box.props.keyboardType).toBe('number-pad');
    }
    expect(byId('otp_digit_1').props.autoFocus).toBe(true);
  });

  it('shows the exact copy from the challenge', async () => {
    await mount();
    const dump = JSON.stringify(renderer.toJSON());
    expect(dump).toContain('Enter your code');
    expect(dump).toContain('A 6-digit code has been sent to your phone.');
    expect(dump).toContain("Didn't receive the code?");
    expect(dump).toContain('Submit');
  });

  it('shows a placeholder, never a real 0, in empty boxes', async () => {
    await mount();
    expect(digitValues()).toEqual(['', '', '', '', '', '']);
    expect(byId('otp_digit_1').props.placeholder).not.toBe('0');
  });

  it('disables Submit until all six digits are filled', async () => {
    await mount();
    expect(byId('otp_submit').props.accessibilityState.disabled).toBe(true);
    await typeCode('12345');
    expect(byId('otp_submit').props.accessibilityState.disabled).toBe(true);
    await type(6, '6');
    expect(byId('otp_submit').props.accessibilityState.disabled).toBe(false);
  });

  it('fills the boxes from pasted text', async () => {
    await mount();
    await type(1, 'Your code is 482913');
    expect(digitValues()).toEqual(['4', '8', '2', '9', '1', '3']);
  });

  it('rejects non-digit characters', async () => {
    await mount();
    await type(1, 'a');
    expect(digitValues()[0]).toBe('');
  });

  it('backspace on an empty box clears the previous digit', async () => {
    await mount();
    await typeCode('12');
    await ReactTestRenderer.act(() => {
      withHandler('otp_digit_3', 'onKeyPress').props.onKeyPress({
        nativeEvent: { key: 'Backspace' },
      });
    });
    expect(digitValues().slice(0, 3)).toEqual(['1', '', '']);
  });

  it('shows the success message for 123456', async () => {
    await mount();
    await typeCode('123456');
    await press('otp_submit');
    expect(textOf('otp_status')).toContain("You're in.");
  });

  it('shows the error, clears every box and refocuses the first for a wrong code', async () => {
    await mount();
    await typeCode('654321');
    await press('otp_submit');
    expect(textOf('otp_status')).toContain("That code didn't work. Try again.");
    expect(digitValues()).toEqual(['', '', '', '', '', '']);
    expect(byId('otp_submit').props.accessibilityState.disabled).toBe(true);
  });

  it('keeps Resend disabled for 30 s, counting down as 0:SS, then enables it', async () => {
    await mount();
    expect(byId('otp_resend').props.accessibilityState.disabled).toBe(true);
    expect(JSON.stringify(renderer.toJSON())).toContain('Resend in 0:30');

    await ReactTestRenderer.act(() => {
      jest.advanceTimersByTime(5000);
    });
    expect(JSON.stringify(renderer.toJSON())).toContain('Resend in 0:25');

    await ReactTestRenderer.act(() => {
      jest.advanceTimersByTime(25000);
    });
    expect(byId('otp_resend').props.accessibilityState.disabled).toBe(false);
  });

  it('restarts the countdown when Resend is tapped', async () => {
    await mount();
    await ReactTestRenderer.act(() => {
      jest.advanceTimersByTime(30000);
    });
    expect(byId('otp_resend').props.accessibilityState.disabled).toBe(false);

    await press('otp_resend');
    expect(byId('otp_resend').props.accessibilityState.disabled).toBe(true);
    expect(JSON.stringify(renderer.toJSON())).toContain('Resend in 0:30');
  });
});
