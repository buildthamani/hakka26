import { ColorSchemeName, useColorScheme } from 'react-native';
import colours from '../assets/colours.json';

export type OtpColours = typeof colours.otp.light;

export function paletteFor(scheme: ColorSchemeName | null): OtpColours {
  const palette = scheme === 'dark' ? colours.otp.dark : colours.otp.light;
  return { ...palette, scrim: argbToRgba(palette.scrim) };
}

export function useOtpColours(): OtpColours {
  return paletteFor(useColorScheme());
}

// colours.json stores the scrim as #AARRGGBB, but React Native reads 8-digit hex as #RRGGBBAA.
function argbToRgba(hex: string): string {
  const alpha = parseInt(hex.slice(1, 3), 16) / 255;
  const [r, g, b] = [3, 5, 7].map(start => parseInt(hex.slice(start, start + 2), 16));
  return `rgba(${r}, ${g}, ${b}, ${alpha.toFixed(2)})`;
}
