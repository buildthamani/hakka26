export const CODE_LENGTH = 6;
export const RESEND_SECONDS = 30;

/** Demo-only stand-in for a server check. */
const ACCEPTED_CODE = '123456';

export type OtpDigits = readonly string[];

export interface InputResult {
  digits: OtpDigits;
  /** Index that should receive focus after the change. */
  focusIndex: number;
}

export const emptyDigits = (): OtpDigits => Array.from({ length: CODE_LENGTH }, () => '');

/** Keeps only 0-9, so pasted text like "Your code is 482913" becomes "482913". */
export const extractDigits = (text: string): string => text.replace(/\D/g, '');

export const isComplete = (digits: OtpDigits): boolean =>
  digits.length === CODE_LENGTH && digits.every((digit) => digit !== '');

export const joinDigits = (digits: OtpDigits): string => digits.join('');

export const verifyCode = (code: string): boolean => code === ACCEPTED_CODE;

const clampIndex = (index: number): number => Math.min(Math.max(index, 0), CODE_LENGTH - 1);

const withDigit = (digits: OtpDigits, index: number, value: string): OtpDigits =>
  digits.map((current, i) => (i === index ? value : current));

/**
 * Typing or pasting into a box that already holds a digit makes the platform
 * report "<old><new>" (or "<new><old>"). Drop the old digit in the two cases
 * that arise: one typed character, or a full code pasted over a filled box.
 */
function withoutExistingDigit(current: string, incoming: string): string {
  const looksLikeAppended =
    incoming.length === 2 || incoming.length === CODE_LENGTH + current.length;
  if (current === '' || !looksLikeAppended) return incoming;
  if (incoming.startsWith(current)) return incoming.slice(current.length);
  if (incoming.endsWith(current)) return incoming.slice(0, -current.length);
  return incoming;
}

/**
 * Applies text typed or pasted into box `index`.
 *
 * - Non-digit input is rejected (the box keeps its value).
 * - Empty text clears the box.
 * - A single digit fills the box and advances.
 * - Several digits (a paste) fill the boxes from the start for a full code,
 *   otherwise from the box that received the paste.
 */
export function applyInput(digits: OtpDigits, index: number, text: string): InputResult {
  if (text === '') {
    return { digits: withDigit(digits, index, ''), focusIndex: index };
  }

  const incoming = extractDigits(text);
  if (incoming === '') {
    return { digits, focusIndex: index };
  }

  const typed = withoutExistingDigit(digits[index], incoming);

  if (typed.length === 1) {
    return {
      digits: withDigit(digits, index, typed),
      focusIndex: clampIndex(index + 1),
    };
  }

  const start = typed.length >= CODE_LENGTH ? 0 : index;
  const next = digits.map((existing, i) => {
    const offset = i - start;
    return offset >= 0 && offset < typed.length ? typed[offset] : existing;
  });
  return {
    digits: next,
    focusIndex: clampIndex(start + typed.length),
  };
}

/** Backspace on an empty box moves back and clears the previous digit. */
export function applyBackspace(digits: OtpDigits, index: number): InputResult {
  if (digits[index] !== '' || index === 0) {
    return { digits: withDigit(digits, index, ''), focusIndex: index };
  }
  const previous = index - 1;
  return { digits: withDigit(digits, previous, ''), focusIndex: previous };
}

/** Formats remaining seconds as `0:SS`. */
export const formatCountdown = (seconds: number): string =>
  `0:${String(Math.max(seconds, 0)).padStart(2, '0')}`;
