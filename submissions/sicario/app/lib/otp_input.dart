import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'otp_colors.dart';

const otpLength = 6;

/// Six circular digit boxes drawn over one hidden, autofocused TextField.
///
/// The field holds the whole code, so typing advances to the next box and
/// backspace on an empty box clears the previous digit for free.
class OtpInput extends StatelessWidget {
  const OtpInput({super.key, required this.controller, required this.focusNode});

  final TextEditingController controller;
  final FocusNode focusNode;

  /// Keeps digits only; a paste of 6+ digits (e.g. "Your code is 482913")
  /// replaces the code with the last six digits pasted.
  static final _digitsFormatter = TextInputFormatter.withFunction((old, value) {
    var digits = value.text.replaceAll(RegExp(r'\D'), '');
    if (digits.length > otpLength) {
      final pasted = digits.length - old.text.length > 1;
      digits = pasted ? digits.substring(digits.length - otpLength) : old.text;
    }
    return TextEditingValue(
      text: digits,
      selection: TextSelection.collapsed(offset: digits.length),
    );
  });

  @override
  Widget build(BuildContext context) {
    return ListenableBuilder(
      listenable: Listenable.merge([controller, focusNode]),
      builder: (context, _) {
        final code = controller.text;
        return Stack(
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                for (var i = 0; i < otpLength; i++)
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 5),
                    child: _DigitBox(
                      index: i,
                      digit: i < code.length ? code[i] : null,
                      focused: focusNode.hasFocus && i == code.length,
                      onTap: focusNode.requestFocus,
                    ),
                  ),
              ],
            ),
            // Invisible but on top, so taps reach it and open the keyboard.
            Positioned.fill(
              child: Opacity(
                opacity: 0,
                child: TextField(
                  key: const Key('otp_field'),
                  controller: controller,
                  focusNode: focusNode,
                  autofocus: true,
                  keyboardType: TextInputType.number,
                  autofillHints: const [AutofillHints.oneTimeCode],
                  inputFormatters: [_digitsFormatter],
                  enableInteractiveSelection: false,
                  showCursor: false,
                  decoration: const InputDecoration(
                    border: InputBorder.none,
                    counterText: '',
                  ),
                ),
              ),
            ),
          ],
        );
      },
    );
  }
}

class _DigitBox extends StatelessWidget {
  const _DigitBox({
    required this.index,
    required this.digit,
    required this.focused,
    required this.onTap,
  });

  final int index;
  final String? digit;
  final bool focused;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final c = OtpColors.of(context);
    final filled = digit != null;
    final (bg, outline, fg) = filled
        ? (c.digitFilledBg, c.digitFilledBg, c.digitFilledText)
        : focused
        ? (c.digitFocusedBg, c.digitFocusedOutline, c.textPrimary)
        : (c.digitEmptyBg, c.digitEmptyOutline, c.digitPlaceholder);

    return Semantics(
      key: Key('otp_digit_${index + 1}'),
      identifier: 'otp_digit_${index + 1}',
      label: 'Digit ${index + 1} of $otpLength',
      value: digit ?? 'empty',
      focused: focused,
      onTap: onTap,
      excludeSemantics: true,
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 120),
        width: 46,
        height: 46,
        alignment: Alignment.center,
        decoration: BoxDecoration(
          color: bg,
          shape: BoxShape.circle,
          border: Border.all(color: outline, width: focused ? 1.5 : 1),
        ),
        child: Text(
          digit ?? '–',
          style: TextStyle(
            color: fg,
            fontSize: 18,
            fontWeight: FontWeight.w500,
          ),
        ),
      ),
    );
  }
}
