import React from 'react';
import { StyleSheet, useColorScheme } from 'react-native';
import ReactTestRenderer, { act, ReactTestInstance, ReactTestRenderer as Renderer } from 'react-test-renderer';

import App from '../App';
import OtpSheet, { COPY, formatCountdown } from '../src/OtpSheet';
import { otpColours, toRnColour } from '../src/theme';

const mockedScheme = useColorScheme as unknown as jest.Mock;

let renderer: Renderer;
let focused: number | null;

const byId = (id: string): ReactTestInstance => renderer.root.findAll((node) => node.props.testID === id)[0];
const digit = (n: number) => byId(`otp_digit_${n}`);
const values = () => [1, 2, 3, 4, 5, 6].map((n) => digit(n).props.value);
const textIn = (node: ReactTestInstance) =>
  node
    .findAll((n) => (n.type as unknown) === 'Text')
    .map((n) => [n.props.children].flat().filter((c) => typeof c === 'string').join(''))
    .join('');
const status = () => textIn(byId('otp_status'));
const resendLabel = () => textIn(byId('otp_resend'));
const allText = () => textIn(renderer.root);

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

async function mount(element: React.ReactElement = <OtpSheet />) {
  await act(async () => {
    renderer = ReactTestRenderer.create(element);
  });
  installFocusSpies();
}

async function enter(code: string) {
  for (let i = 0; i < code.length; i++) {
    await act(async () => digit(i + 1).props.onChangeText(code[i]));
  }
}

async function press(id: string) {
  await act(async () => byId(id).props.onPress());
}

async function tick(ms: number) {
  await act(async () => {
    jest.advanceTimersByTime(ms);
  });
}

beforeEach(() => {
  jest.useFakeTimers();
  mockedScheme.mockReturnValue('light');
});

afterEach(async () => {
  await act(async () => renderer.unmount());
  jest.useRealTimers();
});

describe('OtpSheet copy and layout', () => {
  it('shows the exact copy', async () => {
    await mount();
    const text = allText();
    expect(text).toContain('Enter your code');
    expect(text).toContain('A 6-digit code has been sent to your phone.');
    expect(text).toContain("Didn't receive the code? ");
    expect(textIn(byId('otp_submit'))).toBe('Submit');
    expect(status()).toBe('');
  });

  it('renders a logo, the scrim and the sheet with the light palette', async () => {
    await mount();
    expect(renderer.root.findAll((n) => n.props.accessibilityLabel === 'Thamani logo').length).toBeGreaterThan(0);
    expect(StyleSheet.flatten(byId('otp_scrim').props.style).backgroundColor).toBe('#00000099');
    expect(StyleSheet.flatten(byId('otp_sheet').props.style).backgroundColor).toBe('#FFFFFF');
  });

  it('uses the dark palette in dark mode', async () => {
    mockedScheme.mockReturnValue('dark');
    await mount();
    expect(StyleSheet.flatten(byId('otp_scrim').props.style).backgroundColor).toBe('#000000CC');
    expect(StyleSheet.flatten(byId('otp_sheet').props.style).backgroundColor).toBe('#161A17');
    expect(StyleSheet.flatten(digit(1).props.style).backgroundColor).toBe(otpColours.dark.digit_empty_bg);
  });

  it('converts Android ARGB colours to React Native RGBA', () => {
    expect(toRnColour('#99000000')).toBe('#00000099');
    expect(toRnColour('#12361B')).toBe('#12361B');
  });

  it('App hosts the sheet', async () => {
    await mount(<App />);
    expect(byId('otp_sheet')).toBeDefined();
    expect(byId('otp_submit')).toBeDefined();
  });
});

describe('Submit', () => {
  it('is disabled until all six digits are entered', async () => {
    await mount();
    expect(byId('otp_submit').props.disabled).toBe(true);
    await enter('12345');
    expect(byId('otp_submit').props.disabled).toBe(true);
    await enter('123456');
    expect(byId('otp_submit').props.disabled).toBe(false);
    expect(byId('otp_submit').props.accessibilityState).toEqual({ disabled: false });
  });

  it('123456 succeeds', async () => {
    await mount();
    await enter('123456');
    await press('otp_submit');
    expect(status()).toBe("You're in.");
    expect(StyleSheet.flatten(byId('otp_status').props.style).color).toBe(otpColours.light.success);
  });

  it('a wrong code shows the error, clears every circle and refocuses the first', async () => {
    await mount();
    await enter('654321');
    await act(async () => digit(6).instance.focus());
    await press('otp_submit');
    expect(status()).toBe("That code didn't work. Try again.");
    expect(StyleSheet.flatten(byId('otp_status').props.style).color).toBe(otpColours.light.error);
    expect(values()).toEqual(['', '', '', '', '', '']);
    expect(focused).toBe(1);
    expect(byId('otp_submit').props.disabled).toBe(true);
  });

  it('can succeed after a failed attempt', async () => {
    await mount();
    await enter('000000');
    await press('otp_submit');
    await enter('123456');
    await press('otp_submit');
    expect(status()).toBe("You're in.");
  });

  it('a pasted code can be submitted', async () => {
    await mount();
    await act(async () => digit(1).props.onChangeText('123 456'));
    await press('otp_submit');
    expect(status()).toBe(COPY.success);
  });
});

describe('Resend', () => {
  it('counts down from 0:30 when the sheet opens', async () => {
    await mount();
    expect(resendLabel()).toBe('Resend in 0:30');
    expect(byId('otp_resend').props.disabled).toBe(true);
    await tick(1000);
    expect(resendLabel()).toBe('Resend in 0:29');
    await tick(20000);
    expect(resendLabel()).toBe('Resend in 0:09');
  });

  it('shows "Didn\'t receive the code? Resend" once the countdown ends', async () => {
    await mount();
    await tick(30000);
    expect(resendLabel()).toBe('Resend');
    expect(allText()).toContain("Didn't receive the code? Resend");
    expect(byId('otp_resend').props.disabled).toBe(false);
  });

  it('tapping Resend restarts the 30 second countdown', async () => {
    await mount();
    await tick(30000);
    await press('otp_resend');
    expect(resendLabel()).toBe('Resend in 0:30');
    await tick(5000);
    expect(resendLabel()).toBe('Resend in 0:25');
    await tick(25000);
    expect(resendLabel()).toBe('Resend');
    await tick(5000);
    expect(resendLabel()).toBe('Resend');
  });

  it('formats seconds as 0:SS', () => {
    expect(formatCountdown(30)).toBe('Resend in 0:30');
    expect(formatCountdown(5)).toBe('Resend in 0:05');
  });
});
