export const OTP_LENGTH = 6;
export const RESEND_SECONDS = 30;
export const VALID_CODE = '123456';

export const copy = {
  title: 'Enter your code',
  subtitle: 'A 6-digit code has been sent to your phone.',
  resendQuestion: "Didn't receive the code?",
  resend: 'Resend',
  submit: 'Submit',
  success: "You're in.",
  error: "That code didn't work. Try again.",
};

export type Status = 'idle' | 'success' | 'error';

export type OtpState = {
  digits: string[];
  focusedIndex: number;
  status: Status;
  secondsLeft: number;
};

export type OtpAction =
  | { type: 'input'; index: number; text: string }
  | { type: 'backspace'; index: number }
  | { type: 'focus'; index: number }
  | { type: 'submit' }
  | { type: 'tick' }
  | { type: 'resend' };

export const initialState: OtpState = {
  digits: Array(OTP_LENGTH).fill(''),
  focusedIndex: 0,
  status: 'idle',
  secondsLeft: RESEND_SECONDS,
};

export function isComplete(state: OtpState): boolean {
  return state.digits.every(Boolean);
}

export function canResend(state: OtpState): boolean {
  return state.secondsLeft === 0;
}

export const statusMessage: Record<Status, string> = {
  idle: '',
  success: copy.success,
  error: copy.error,
};

export function resendLabel(secondsLeft: number): string {
  return secondsLeft > 0 ? `Resend in 0:${String(secondsLeft).padStart(2, '0')}` : copy.resend;
}

export function otpReducer(state: OtpState, action: OtpAction): OtpState {
  switch (action.type) {
    case 'input':
      return input(state, action.index, action.text);
    case 'backspace':
      return backspace(state, action.index);
    case 'focus':
      return state.focusedIndex === action.index ? state : { ...state, focusedIndex: action.index };
    case 'submit':
      return submit(state);
    case 'tick':
      return { ...state, secondsLeft: Math.max(0, state.secondsLeft - 1) };
    case 'resend':
      return canResend(state) ? { ...state, secondsLeft: RESEND_SECONDS } : state;
  }
}

function input(state: OtpState, index: number, text: string): OtpState {
  if (!text) {
    return afterEdit(state, replaceAt(state.digits, index, ''), index);
  }
  const numbers = text.replace(/\D/g, '');
  if (!numbers || numbers === state.digits[index]) {
    return state;
  }
  const fresh = newDigits(state.digits[index], numbers);
  if (fresh.length > 1) {
    return paste(state, fresh);
  }
  return afterEdit(state, replaceAt(state.digits, index, fresh), Math.min(index + 1, OTP_LENGTH - 1));
}

// Typing or pasting over a filled box reports the digit already there together with the new text.
function newDigits(current: string, numbers: string): string {
  if (current && numbers.length > 1) {
    if (numbers.startsWith(current)) {
      return numbers.slice(1);
    }
    if (numbers.endsWith(current)) {
      return numbers.slice(0, -1);
    }
  }
  return numbers;
}

function paste(state: OtpState, numbers: string): OtpState {
  const pasted = numbers.slice(0, OTP_LENGTH).split('');
  const digits = Array.from({ length: OTP_LENGTH }, (_, i) => pasted[i] ?? '');
  return afterEdit(state, digits, Math.min(pasted.length, OTP_LENGTH - 1));
}

function backspace(state: OtpState, index: number): OtpState {
  if (index === 0) {
    return state;
  }
  return afterEdit(state, replaceAt(state.digits, index - 1, ''), index - 1);
}

function submit(state: OtpState): OtpState {
  if (!isComplete(state)) {
    return state;
  }
  if (state.digits.join('') === VALID_CODE) {
    return { ...state, status: 'success' };
  }
  return { ...state, digits: initialState.digits, focusedIndex: 0, status: 'error' };
}

function afterEdit(state: OtpState, digits: string[], focusedIndex: number): OtpState {
  return { ...state, digits, focusedIndex, status: 'idle' };
}

function replaceAt(digits: string[], index: number, digit: string): string[] {
  return digits.map((existing, i) => (i === index ? digit : existing));
}
