import { render, screen } from '@testing-library/react-native';
import App from '../App';

test('renders the OTP screen', () => {
  render(<App />);

  expect(screen.getByText('Enter your code')).toBeOnTheScreen();
  expect(screen.getByTestId('otp_submit')).toBeOnTheScreen();
});
