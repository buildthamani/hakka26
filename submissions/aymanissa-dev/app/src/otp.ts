export const OTP_LENGTH = 6;
export const RESEND_SECONDS = 30;
export const VALID_CODE = '123456';

export const COPY = {
  title: 'Enter your code',
  subtitle: 'A 6-digit code has been sent to your phone.',
  resendIdle: "Didn't receive the code? Resend",
  resendAction: 'Resend',
  submit: 'Submit',
  success: "You're in.",
  error: "That code didn't work. Try again.",
} as const;

export type OtpStatus = 'idle' | 'success' | 'error';

export type OtpState = {
  digits: string[];
  focusedIndex: number;
  status: OtpStatus;
  resendSecondsLeft: number;
};

export function createOtpState(): OtpState {
  return {
    digits: Array.from({ length: OTP_LENGTH }, () => ''),
    focusedIndex: 0,
    status: 'idle',
    resendSecondsLeft: RESEND_SECONDS,
  };
}

export function resendCountdown(secondsLeft: number): string {
  const seconds = Math.max(0, Math.min(RESEND_SECONDS, secondsLeft));
  return `Resend in 0:${String(seconds).padStart(2, '0')}`;
}

export function statusMessage(status: OtpStatus): string {
  if (status === 'success') return COPY.success;
  if (status === 'error') return COPY.error;
  return '';
}

export function codeOf(state: OtpState): string {
  return state.digits.join('');
}

export function isComplete(state: OtpState): boolean {
  return state.digits.every((digit) => digit.length === 1);
}

function withEdit(state: OtpState, digits: string[], focusedIndex: number): OtpState {
  return {
    ...state,
    digits,
    focusedIndex,
    status: 'idle',
  };
}

function putDigit(state: OtpState, index: number, digit: string): OtpState {
  const digits = [...state.digits];
  digits[index] = digit;
  return withEdit(state, digits, Math.min(index + 1, OTP_LENGTH - 1));
}

function putRun(state: OtpState, index: number, extracted: string): OtpState {
  const digits = [...state.digits];
  const taken = extracted.slice(0, OTP_LENGTH - index);
  for (let offset = 0; offset < taken.length; offset += 1) {
    digits[index + offset] = taken[offset];
  }
  return withEdit(state, digits, Math.min(index + taken.length, OTP_LENGTH - 1));
}

function pasteDigits(state: OtpState, extracted: string): OtpState {
  const taken = extracted.slice(0, OTP_LENGTH);
  const digits = Array.from({ length: OTP_LENGTH }, (_, index) => taken[index] ?? '');
  return withEdit(state, digits, Math.min(Math.max(taken.length, 1) - 1, OTP_LENGTH - 1));
}

/** Accept a keystroke or a pasted string. Non-digits are ignored; a longer paste fills boxes from the digits it contains. */
export function applyInput(state: OtpState, index: number, text: string): OtpState {
  const extracted = text.replace(/\D/g, '');
  if (extracted.length === 0) return state;

  const hasNonDigit = /\D/.test(text);
  if (extracted.length >= OTP_LENGTH || (hasNonDigit && extracted.length > 1)) {
    return pasteDigits(state, extracted);
  }
  if (extracted.length === 1) {
    return putDigit(state, index, extracted);
  }

  const current = state.digits[index];
  if (current && extracted.startsWith(current) && extracted.length === current.length + 1) {
    return putDigit(state, index, extracted[extracted.length - 1]);
  }
  return putRun(state, index, extracted);
}

/** Backspace clears this box, or the previous box when this one is already empty. */
export function applyBackspace(state: OtpState, index: number): OtpState {
  const digits = [...state.digits];
  if (digits[index]) {
    digits[index] = '';
    return withEdit(state, digits, index);
  }
  if (index === 0) return state;
  digits[index - 1] = '';
  return withEdit(state, digits, index - 1);
}

export function submitCode(state: OtpState): OtpState {
  if (!isComplete(state)) return state;
  if (codeOf(state) === VALID_CODE) {
    return { ...state, status: 'success' };
  }
  return {
    ...state,
    digits: Array.from({ length: OTP_LENGTH }, () => ''),
    focusedIndex: 0,
    status: 'error',
  };
}

export function tickResend(state: OtpState): OtpState {
  if (state.resendSecondsLeft <= 0) return state;
  return { ...state, resendSecondsLeft: state.resendSecondsLeft - 1 };
}

export function restartResend(state: OtpState): OtpState {
  if (state.resendSecondsLeft > 0) return state;
  return { ...state, resendSecondsLeft: RESEND_SECONDS };
}

export function digitLayout(screenWidth: number): { size: number; gap: number; padding: number } {
  const padding = screenWidth < 360 ? 16 : 24;
  const gap = screenWidth < 360 ? 8 : 10;
  const available = screenWidth - padding * 2 - gap * 5;
  const size = Math.max(36, Math.min(52, Math.floor(available / OTP_LENGTH)));
  return { size, gap, padding };
}
