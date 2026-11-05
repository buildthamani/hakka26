import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'otp_colours.dart';
import 'otp_controller.dart';
import 'otp_copy.dart';

/// A row of circular, single-digit boxes driven by an [OtpController].
class OtpInput extends StatelessWidget {
  const OtpInput({super.key, required this.controller});

  final OtpController controller;

  static const _gap = 10.0;
  static const _maxBoxSize = 48.0;

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        final gaps = _gap * (controller.length - 1);
        final size = math.min(
          _maxBoxSize,
          (constraints.maxWidth - gaps) / controller.length,
        );
        return Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            for (var i = 0; i < controller.length; i++) ...[
              if (i > 0) const SizedBox(width: _gap),
              OtpDigitBox(controller: controller, index: i, size: size),
            ],
          ],
        );
      },
    );
  }
}

enum OtpBoxState { empty, focused, filled }

/// One circular box. Its look follows [OtpBoxState]; the focused state wins
/// over filled so the active box is always obvious (as in the reference).
class OtpDigitBox extends StatelessWidget {
  const OtpDigitBox({
    super.key,
    required this.controller,
    required this.index,
    required this.size,
  });

  final OtpController controller;
  final int index;
  final double size;

  @override
  Widget build(BuildContext context) {
    final focusNode = controller.focusNodes[index];
    return ListenableBuilder(
      listenable: Listenable.merge([controller, focusNode]),
      builder: (context, _) {
        final colours = OtpColours.of(context);
        final digit = controller.digits[index];
        final state = focusNode.hasFocus
            ? OtpBoxState.focused
            : digit.isNotEmpty
            ? OtpBoxState.filled
            : OtpBoxState.empty;

        final (background, outline, textColour) = switch (state) {
          OtpBoxState.filled => (
            colours.digitFilledBg,
            colours.digitFilledBg,
            colours.digitFilledText,
          ),
          OtpBoxState.focused => (
            colours.digitFocusedBg,
            colours.digitFocusedOutline,
            colours.textPrimary,
          ),
          OtpBoxState.empty => (
            colours.digitEmptyBg,
            colours.digitEmptyOutline,
            colours.textPrimary,
          ),
        };

        return MergeSemantics(
          child: Semantics(
            label: OtpCopy.digitLabel(index + 1, controller.length),
            child: AnimatedContainer(
              duration: const Duration(milliseconds: 120),
              width: size,
              height: size,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                color: background,
                border: Border.all(
                  color: outline,
                  width: state == OtpBoxState.focused ? 2 : 1,
                ),
              ),
              child: Stack(
                alignment: Alignment.center,
                children: [
                  if (state == OtpBoxState.empty)
                    ExcludeSemantics(
                      child: Text(
                        OtpCopy.placeholder,
                        style: TextStyle(
                          fontSize: 20,
                          color: colours.digitPlaceholder,
                        ),
                      ),
                    ),
                  TextField(
                    key: ValueKey('otp_digit_${index + 1}'),
                    controller: controller.fieldControllers[index],
                    focusNode: focusNode,
                    autofocus: index == 0,
                    autofillHints: index == 0
                        ? const [AutofillHints.oneTimeCode]
                        : null,
                    keyboardType: TextInputType.number,
                    inputFormatters: [
                      FilteringTextInputFormatter.allow(
                        RegExp('[0-9${OtpController.fieldSentinel}]'),
                      ),
                    ],
                    textAlign: TextAlign.center,
                    textAlignVertical: TextAlignVertical.center,
                    showCursor: false,
                    style: TextStyle(
                      fontSize: 20,
                      fontWeight: FontWeight.w600,
                      color: textColour,
                    ),
                    decoration: const InputDecoration.collapsed(hintText: null),
                    onTap: () => controller.onFieldTapped(index),
                    onChanged: (value) =>
                        controller.onFieldChanged(index, value),
                  ),
                ],
              ),
            ),
          ),
        );
      },
    );
  }
}
