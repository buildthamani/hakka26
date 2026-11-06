import { useCallback, useReducer } from 'react';
import {
  applyBackspace,
  applyInput,
  emptyDigits,
  isComplete,
  joinDigits,
  OtpDigits,
  verifyCode,
} from './otp';

export type OtpStatus = 'idle' | 'success' | 'error';

export interface OtpState {
  digits: OtpDigits;
  /** Box that currently has focus (drives the "focused" visual state). */
  focusedIndex: number;
  /** Bumped whenever focus must be moved imperatively, even to the same box. */
  focusRequestId: number;
  status: OtpStatus;
}

type OtpAction =
  | { type: 'change'; index: number; text: string }
  | { type: 'backspace'; index: number }
  | { type: 'focused'; index: number }
  | { type: 'submit' };

export const initialOtpState: OtpState = {
  digits: emptyDigits(),
  focusedIndex: 0,
  focusRequestId: 0,
  status: 'idle',
};

const moveFocus = (state: OtpState, index: number): OtpState => ({
  ...state,
  focusedIndex: index,
  focusRequestId: state.focusRequestId + 1,
});

export function otpReducer(state: OtpState, action: OtpAction): OtpState {
  switch (action.type) {
    case 'change': {
      if (state.status === 'success') return state;
      const { digits, focusIndex } = applyInput(state.digits, action.index, action.text);
      if (digits === state.digits) return state;
      return moveFocus({ ...state, digits, status: 'idle' }, focusIndex);
    }
    case 'backspace': {
      if (state.status === 'success') return state;
      const { digits, focusIndex } = applyBackspace(state.digits, action.index);
      return moveFocus({ ...state, digits, status: 'idle' }, focusIndex);
    }
    case 'focused':
      return { ...state, focusedIndex: action.index };
    case 'submit': {
      if (!isComplete(state.digits) || state.status === 'success') return state;
      if (verifyCode(joinDigits(state.digits))) {
        return { ...state, status: 'success' };
      }
      return moveFocus({ ...state, digits: emptyDigits(), status: 'error' }, 0);
    }
  }
}

export function useOtp() {
  const [state, dispatch] = useReducer(otpReducer, initialOtpState);

  const changeDigit = useCallback(
    (index: number, text: string) => dispatch({ type: 'change', index, text }),
    [],
  );
  const backspace = useCallback((index: number) => dispatch({ type: 'backspace', index }), []);
  const boxFocused = useCallback((index: number) => dispatch({ type: 'focused', index }), []);
  const submit = useCallback(() => dispatch({ type: 'submit' }), []);

  return {
    ...state,
    canSubmit: isComplete(state.digits) && state.status !== 'success',
    isLocked: state.status === 'success',
    changeDigit,
    backspace,
    boxFocused,
    submit,
  };
}
