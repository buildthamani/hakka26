import { StatusBar } from 'expo-status-bar';
import { StyleSheet, Text, View } from 'react-native';

import OtpSheet from './src/OtpSheet';
import { useOtpColours } from './src/theme';

export const APP_NAME = 'hakkahakka26';

export default function App() {
  const colours = useOtpColours();

  return (
    <View style={[styles.container, { backgroundColor: colours.sheet }]}>
      {/* Host screen, dimmed behind the sheet's scrim. */}
      <Text style={[styles.title, { color: colours.text_primary }]}>{APP_NAME}</Text>
      <OtpSheet />
      <StatusBar style="light" />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  title: {
    fontSize: 24,
  },
});
