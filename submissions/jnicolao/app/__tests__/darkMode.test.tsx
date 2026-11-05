import { render, screen } from '@testing-library/react-native';
import { StyleSheet } from 'react-native';
import colours from '../assets/colours.json';
import OtpScreen from '../src/OtpScreen';

jest.mock('react-native/Libraries/Utilities/useColorScheme', () => ({
  __esModule: true,
  default: () => 'dark',
}));

const dark = colours.otp.dark;
const styleOf = (element: ReturnType<typeof screen.getByTestId>) => StyleSheet.flatten(element.props.style);

test('dark mode draws the screen with the dark palette', () => {
  render(<OtpScreen />);

  expect(styleOf(screen.getByText('Enter your code'))).toMatchObject({ color: dark.text_primary });
  expect(styleOf(screen.getByTestId('otp_digit_1'))).toMatchObject({
    backgroundColor: dark.digit_focused_bg,
    borderColor: dark.digit_focused_outline,
  });
  expect(styleOf(screen.getByTestId('otp_digit_2'))).toMatchObject({ backgroundColor: dark.digit_empty_bg });
  expect(styleOf(screen.getByTestId('otp_submit'))).toMatchObject({ backgroundColor: dark.button_disabled_bg });
});
