import 'dart:math' as math;

import 'package:flutter/material.dart';

import 'otp_input.dart';
import 'palette.dart';

/// Presentational bottom sheet for the OTP screen.
class OtpSheet extends StatelessWidget {
  const OtpSheet({
    super.key,
    required this.height,
    required this.colors,
    required this.logoAsset,
    required this.digits,
    required this.controller,
    required this.focusNode,
    required this.onChanged,
    required this.status,
    required this.isError,
    required this.secondsLeft,
    required this.onResend,
    required this.onSubmit,
  });

  final double height;
  final OtpColors colors;
  final String logoAsset;
  final String digits;
  final TextEditingController controller;
  final FocusNode focusNode;
  final ValueChanged<String> onChanged;
  final String? status;
  final bool isError;
  final int secondsLeft;
  final VoidCallback? onResend;
  final VoidCallback? onSubmit;

  @override
  Widget build(BuildContext context) {
    return Container(
      key: const Key('otp_sheet'),
      height: height,
      width: double.infinity,
      decoration: BoxDecoration(
        color: colors.sheet,
        borderRadius: const BorderRadius.vertical(
          top: Radius.circular(28),
        ),
      ),
      child: SafeArea(
        top: false,
        child: LayoutBuilder(
          builder: (context, constraints) {
            final boxSize = math.min(
              48.0,
              math.max(
                0.0,
                (constraints.maxWidth - 48 - 5 * OtpInput.gap) /
                    OtpInput.length,
              ),
            );
            return SingleChildScrollView(
              padding: const EdgeInsets.fromLTRB(24, 28, 24, 20),
              child: ConstrainedBox(
                constraints: BoxConstraints(
                  minHeight: constraints.maxHeight - 48,
                ),
                child: IntrinsicHeight(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      Center(
                        child: Image.asset(
                          logoAsset,
                          height: 58,
                          semanticLabel: 'Thamani logo',
                        ),
                      ),
                      const SizedBox(height: 18),
                      Text(
                        'Enter your code',
                        textAlign: TextAlign.center,
                        style: TextStyle(
                          color: colors.textPrimary,
                          fontSize: 24,
                          height: 1.2,
                          fontWeight: FontWeight.w700,
                          letterSpacing: -0.4,
                        ),
                      ),
                      const SizedBox(height: 8),
                      Text(
                        'A 6-digit code has been sent to your phone.',
                        textAlign: TextAlign.center,
                        style: TextStyle(
                          color: colors.textSecondary,
                          fontSize: 14,
                          height: 1.45,
                        ),
                      ),
                      const SizedBox(height: 26),
                      OtpInput(
                        digits: digits,
                        colors: colors,
                        controller: controller,
                        focusNode: focusNode,
                        onChanged: onChanged,
                        boxSize: boxSize,
                      ),
                      const SizedBox(height: 22),
                      _resendRow(context),
                      const Spacer(),
                      const SizedBox(height: 18),
                      Semantics(
                        liveRegion: true,
                        child: SizedBox(
                          height: 22,
                          child: Text(
                            status ?? '',
                            key: const Key('otp_status'),
                            textAlign: TextAlign.center,
                            style: TextStyle(
                              color: isError ? colors.error : colors.success,
                              fontSize: 13,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                        ),
                      ),
                      const SizedBox(height: 12),
                      SizedBox(
                        height: 54,
                        child: FilledButton(
                          key: const Key('otp_submit'),
                          onPressed: onSubmit,
                          style: FilledButton.styleFrom(
                            backgroundColor: colors.buttonBg,
                            foregroundColor: colors.buttonText,
                            disabledBackgroundColor: colors.buttonDisabledBg,
                            disabledForegroundColor: colors.buttonText
                                .withValues(alpha: 0.72),
                            shape: const StadiumBorder(),
                            textStyle: Theme.of(context)
                                .textTheme
                                .labelLarge
                                ?.copyWith(
                                  fontSize: 16,
                                  fontWeight: FontWeight.w600,
                                ),
                          ),
                          child: const Text('Submit'),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            );
          },
        ),
      ),
    );
  }

  Widget _resendRow(BuildContext context) {
    final counting = secondsLeft > 0;
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        Flexible(
          child: Text(
            "Didn't receive the code? ",
            textAlign: TextAlign.right,
            style: TextStyle(fontSize: 13, color: colors.textSecondary),
          ),
        ),
        TextButton(
          key: const Key('otp_resend'),
          onPressed: onResend,
          style: TextButton.styleFrom(
            foregroundColor: colors.link,
            disabledForegroundColor: colors.textSecondary,
            padding: const EdgeInsets.symmetric(horizontal: 2),
            minimumSize: Size.zero,
            tapTargetSize: MaterialTapTargetSize.shrinkWrap,
            textStyle: Theme.of(context).textTheme.labelLarge?.copyWith(
              fontSize: 13,
              fontWeight: FontWeight.w600,
            ),
          ),
          child: Text(
            counting ? 'Resend in ${_countdownLabel()}' : 'Resend',
          ),
        ),
      ],
    );
  }

  String _countdownLabel() {
    final seconds = secondsLeft.clamp(0, 59);
    return '0:${seconds.toString().padLeft(2, '0')}';
  }
}
