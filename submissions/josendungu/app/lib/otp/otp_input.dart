import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../theme/otp_colors.dart';
import 'otp_controller.dart';

/// A row of circular, single-digit boxes driven by an [OtpController].
class OtpInput extends StatelessWidget {
  const OtpInput({super.key, required this.controller});

  final OtpController controller;

  @override
  Widget build(BuildContext context) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        for (var i = 0; i < controller.length; i++)
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 4),
            child: OtpDigitBox(controller: controller, index: i),
          ),
      ],
    );
  }
}

enum OtpDigitState { filled, focused, empty }

class OtpDigitBox extends StatelessWidget {
  const OtpDigitBox({super.key, required this.controller, required this.index});

  static const double size = 48;
  static const String placeholder = '–';

  final OtpController controller;
  final int index;

  @override
  Widget build(BuildContext context) {
    final focusNode = controller.focusNodes[index];
    return ListenableBuilder(
      listenable: Listenable.merge([controller, focusNode]),
      builder: (context, _) {
        final digit = controller.digits[index];
        final state = focusNode.hasFocus
            ? OtpDigitState.focused
            : digit.isNotEmpty
            ? OtpDigitState.filled
            : OtpDigitState.empty;
        return _buildBox(context, state);
      },
    );
  }

  Widget _buildBox(BuildContext context, OtpDigitState state) {
    final colors = OtpColors.of(context);
    final (background, outline, textColor) = switch (state) {
      OtpDigitState.filled => (
        colors.digitFilledBg,
        colors.digitFilledBg,
        colors.digitFilledText,
      ),
      OtpDigitState.focused => (
        colors.digitFocusedBg,
        colors.digitFocusedOutline,
        colors.textPrimary,
      ),
      OtpDigitState.empty => (
        colors.digitEmptyBg,
        colors.digitEmptyOutline,
        colors.textPrimary,
      ),
    };
    final number = index + 1;

    return Semantics(
      identifier: 'otp_digit_$number',
      label: 'Digit $number of ${controller.length}',
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 150),
        width: size,
        height: size,
        alignment: Alignment.center,
        decoration: BoxDecoration(
          color: background,
          shape: BoxShape.circle,
          border: Border.all(
            color: outline,
            width: state == OtpDigitState.focused ? 1.5 : 1,
          ),
        ),
        // Hide the drag handle on tap; long-press still offers Paste.
        child: TextSelectionTheme(
          data: const TextSelectionThemeData(
            selectionHandleColor: Colors.transparent,
          ),
          child: TextField(
            key: ValueKey('otp_digit_$number'),
            controller: controller.textControllers[index],
            focusNode: controller.focusNodes[index],
            autofocus: index == 0,
            keyboardType: TextInputType.number,
            inputFormatters: [FilteringTextInputFormatter.digitsOnly],
            autofillHints: index == 0
                ? const [AutofillHints.oneTimeCode]
                : null,
            textAlign: TextAlign.center,
            showCursor: false,
            style: TextStyle(
              fontSize: 18,
              fontWeight: FontWeight.w500,
              color: textColor,
            ),
            decoration: InputDecoration.collapsed(
              hintText: state == OtpDigitState.empty ? placeholder : null,
              hintStyle: TextStyle(
                fontSize: 18,
                color: colors.digitPlaceholder,
              ),
            ),
            onChanged: (value) => controller.onBoxChanged(index, value),
          ),
        ),
      ),
    );
  }
}
