import { useColorScheme } from 'react-native';
import colours from '../../assets/colours.json';

export type OtpTheme = typeof colours.otp.light;

/** Palette for the current system appearance (light or dark). */
export function useOtpTheme(): { theme: OtpTheme; isDark: boolean } {
  const isDark = useColorScheme() === 'dark';
  return { theme: isDark ? colours.otp.dark : colours.otp.light, isDark };
}

/** The scrim colours are `#AARRGGBB`; React Native expects `#RRGGBBAA`. */
export const argbToRgba = (argb: string): string =>
  argb.length === 9 ? `#${argb.slice(3)}${argb.slice(1, 3)}` : argb;
