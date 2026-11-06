import { CODE_LENGTH } from './otp';

export const copy = {
  title: 'Enter your code',
  subtitle: `A ${CODE_LENGTH}-digit code has been sent to your phone.`,
  resendPrompt: "Didn't receive the code?",
  resend: 'Resend',
  resendIn: (time: string) => `Resend in ${time}`,
  submit: 'Submit',
  success: "You're in.",
  error: "That code didn't work. Try again.",
  digitLabel: (position: number) => `Digit ${position} of ${CODE_LENGTH}`,
  placeholder: '•',
} as const;
