import { StatusBar } from 'expo-status-bar';
import { OtpSheet } from './src/otp/OtpSheet';

export default function App() {
  return (
    <>
      <OtpSheet />
      <StatusBar style="auto" />
    </>
  );
}
