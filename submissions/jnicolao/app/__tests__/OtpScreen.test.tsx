import { act, fireEvent, render, screen } from '@testing-library/react-native';
import { StyleSheet } from 'react-native';
import colours from '../assets/colours.json';
import OtpScreen from '../src/OtpScreen';

const light = colours.otp.light;

const box = (n: number) => screen.getByTestId(`otp_digit_${n}`);
const boxStyle = (n: number) => StyleSheet.flatten(box(n).props.style);
const typeCode = (code: string) => code.split('').forEach((digit, i) => fireEvent.changeText(box(i + 1), digit));
const backspaceOn = (n: number) => fireEvent(box(n), 'keyPress', { nativeEvent: { key: 'Backspace' } });

beforeEach(() => {
  jest.useFakeTimers();
  render(<OtpScreen />);
});

afterEach(() => {
  jest.useRealTimers();
});

test('shows the title, subtitle, resend line and button', () => {
  expect(screen.getByText('Enter your code')).toBeOnTheScreen();
  expect(screen.getByText('A 6-digit code has been sent to your phone.')).toBeOnTheScreen();
  expect(screen.getByText("Didn't receive the code?")).toBeOnTheScreen();
  expect(screen.getByText('Resend in 0:30')).toBeOnTheScreen();
  expect(screen.getByText('Submit')).toBeOnTheScreen();
});

test('renders six digit boxes that open the numeric keyboard and are labelled for screen readers', () => {
  for (let n = 1; n <= 6; n++) {
    expect(box(n).props.keyboardType).toBe('number-pad');
    expect(box(n)).toHaveAccessibleName(`Digit ${n} of 6`);
  }
  expect(box(1).props.autoFocus).toBe(true);
  expect(box(2).props.autoFocus).toBe(false);
});

test('styles filled, focused and empty boxes differently', () => {
  typeCode('21');

  expect(boxStyle(1)).toMatchObject({ backgroundColor: light.digit_filled_bg, color: light.digit_filled_text });
  expect(boxStyle(3)).toMatchObject({ backgroundColor: light.digit_focused_bg, borderColor: light.digit_focused_outline });
  expect(boxStyle(4)).toMatchObject({ backgroundColor: light.digit_empty_bg, borderColor: light.digit_empty_outline });
  expect(box(3).props.placeholder).toBeUndefined();
  expect(box(4).props.placeholder).toBeTruthy();
  expect(box(4).props.placeholder).not.toBe('0');
  expect(box(4).props.value).toBe('');
});

test('typing a digit fills the box and moves focus to the next one', () => {
  fireEvent.changeText(box(1), '4');

  expect(box(1).props.value).toBe('4');
  expect(boxStyle(2).borderColor).toBe(light.digit_focused_outline);
});

test('rejects anything that is not a digit', () => {
  fireEvent.changeText(box(1), 'x');

  expect(box(1).props.value).toBe('');
});

test('backspace on an empty box moves back and clears the previous digit', () => {
  typeCode('21');
  backspaceOn(3);

  expect(box(2).props.value).toBe('');
  expect(boxStyle(2).borderColor).toBe(light.digit_focused_outline);
});

test('pasting "Your code is 482913" fills the six boxes', () => {
  fireEvent.changeText(box(1), 'Your code is 482913');

  expect([1, 2, 3, 4, 5, 6].map(n => box(n).props.value).join('')).toBe('482913');
});

test('submit stays disabled until all six digits are filled', () => {
  const submit = screen.getByTestId('otp_submit');
  typeCode('12345');
  expect(submit).toBeDisabled();

  fireEvent.changeText(box(6), '6');
  expect(submit).toBeEnabled();
});

test('123456 shows the success message', () => {
  typeCode('123456');
  fireEvent.press(screen.getByTestId('otp_submit'));

  expect(screen.getByTestId('otp_status')).toHaveTextContent("You're in.");
});

test('a wrong code shows the error, clears every box and focuses the first one', () => {
  typeCode('654321');
  fireEvent.press(screen.getByTestId('otp_submit'));

  expect(screen.getByTestId('otp_status')).toHaveTextContent("That code didn't work. Try again.");
  [1, 2, 3, 4, 5, 6].forEach(n => expect(box(n).props.value).toBe(''));
  expect(boxStyle(1).borderColor).toBe(light.digit_focused_outline);
  expect(screen.getByTestId('otp_submit')).toBeDisabled();
});

test('resend waits for the 30 second countdown, then tapping it restarts the countdown', () => {
  const resend = screen.getByTestId('otp_resend');
  expect(resend).toBeDisabled();
  expect(screen.getByText('Resend in 0:30')).toBeOnTheScreen();

  for (let second = 0; second < 30; second++) {
    act(() => jest.advanceTimersByTime(1000));
  }
  expect(screen.getByText('Resend')).toBeOnTheScreen();
  expect(resend).toBeEnabled();

  fireEvent.press(resend);
  expect(screen.getByText('Resend in 0:30')).toBeOnTheScreen();
  expect(resend).toBeDisabled();
});
