import { ColorSchemeName } from 'react-native';

import colours from '../assets/colours.json';

export type OtpPalette = {
  sheet: string;
  scrim: string;
  text_primary: string;
  text_secondary: string;
  digit_filled_bg: string;
  digit_filled_text: string;
  digit_focused_bg: string;
  digit_focused_outline: string;
  digit_empty_bg: string;
  digit_empty_outline: string;
  digit_placeholder: string;
  button_bg: string;
  button_text: string;
  button_disabled_bg: string;
  link: string;
  success: string;
  error: string;
};

/** colours.json stores the scrim as Android #AARRGGBB. React Native wants rgba(). */
export function argbToRgba(hex: string): string {
  const raw = hex.replace('#', '');
  if (raw.length !== 8) return hex;
  const alpha = parseInt(raw.slice(0, 2), 16) / 255;
  const red = parseInt(raw.slice(2, 4), 16);
  const green = parseInt(raw.slice(4, 6), 16);
  const blue = parseInt(raw.slice(6, 8), 16);
  return `rgba(${red}, ${green}, ${blue}, ${alpha.toFixed(3)})`;
}

export function paletteFor(scheme: ColorSchemeName | null | undefined): OtpPalette {
  const source = scheme === 'dark' ? colours.otp.dark : colours.otp.light;
  return { ...source, scrim: argbToRgba(source.scrim) };
}
