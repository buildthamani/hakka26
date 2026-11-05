import 'dart:ui' show ImageFilter;

import 'package:flutter/material.dart';

import '../theme/otp_colors.dart';
import 'otp_controller.dart';
import 'otp_input.dart';
import 'otp_verifier.dart';

abstract final class OtpCopy {
  static const title = 'Enter your code';
  static const subtitle = 'A 6-digit code has been sent to your phone.';
  static const resendPrompt = "Didn't receive the code? ";
  static const resend = 'Resend';
  static const submit = 'Submit';
  static const success = "You're in.";
  static const error = "That code didn't work. Try again.";

  static String resendIn(int seconds) =>
      'Resend in ${seconds ~/ 60}:${(seconds % 60).toString().padLeft(2, '0')}';
}

/// The OTP verification sheet, shown over a dimmed, blurred sign-in screen.
class OtpScreen extends StatefulWidget {
  const OtpScreen({super.key, required this.verifier});

  final OtpVerifier verifier;

  @override
  State<OtpScreen> createState() => _OtpScreenState();
}

class _OtpScreenState extends State<OtpScreen> {
  late final OtpController _controller = OtpController(
    verifier: widget.verifier,
  );

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final colors = OtpColors.of(context);
    return Scaffold(
      body: Stack(
        fit: StackFit.expand,
        children: [
          const _BlurredBackdrop(),
          ColoredBox(color: colors.scrim),
          Align(
            alignment: Alignment.bottomCenter,
            child: FractionallySizedBox(
              heightFactor: 0.86,
              widthFactor: 1,
              child: _OtpSheet(controller: _controller),
            ),
          ),
        ],
      ),
    );
  }
}

class _OtpSheet extends StatelessWidget {
  const _OtpSheet({required this.controller});

  final OtpController controller;

  @override
  Widget build(BuildContext context) {
    final colors = OtpColors.of(context);
    return Material(
      color: colors.sheet,
      borderRadius: const BorderRadius.vertical(top: Radius.circular(28)),
      child: SafeArea(
        top: false,
        // Scrolls only when the keyboard leaves too little room for the sheet.
        child: LayoutBuilder(
          builder: (context, constraints) => SingleChildScrollView(
            padding: const EdgeInsets.fromLTRB(24, 32, 24, 24),
            child: ConstrainedBox(
              constraints: BoxConstraints(
                minHeight: (constraints.maxHeight - 56).clamp(
                  0,
                  double.infinity,
                ),
              ),
              child: IntrinsicHeight(child: _buildContent(context)),
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildContent(BuildContext context) {
    final colors = OtpColors.of(context);
    final textTheme = Theme.of(context).textTheme;
    return ListenableBuilder(
      listenable: controller,
      builder: (context, _) => Column(
        children: [
          Image.asset(
            'assets/thamani-logo.png',
            height: 56,
            semanticLabel: 'Thamani logo',
          ),
          const SizedBox(height: 20),
          Text(
            OtpCopy.title,
            textAlign: TextAlign.center,
            style: textTheme.headlineSmall?.copyWith(
              color: colors.textPrimary,
              fontWeight: FontWeight.w500,
            ),
          ),
          const SizedBox(height: 12),
          Text(
            OtpCopy.subtitle,
            textAlign: TextAlign.center,
            style: textTheme.bodyMedium?.copyWith(
              color: colors.textSecondary,
              height: 1.4,
            ),
          ),
          const SizedBox(height: 32),
          OtpInput(controller: controller),
          const SizedBox(height: 20),
          _StatusMessage(status: controller.status),
          const SizedBox(height: 8),
          _ResendLine(controller: controller),
          const Spacer(),
          _SubmitButton(controller: controller),
        ],
      ),
    );
  }
}

class _StatusMessage extends StatelessWidget {
  const _StatusMessage({required this.status});

  final OtpStatus status;

  @override
  Widget build(BuildContext context) {
    final colors = OtpColors.of(context);
    final (text, color) = switch (status) {
      OtpStatus.success => (OtpCopy.success, colors.success),
      OtpStatus.error => (OtpCopy.error, colors.error),
      OtpStatus.idle || OtpStatus.verifying => ('', colors.textSecondary),
    };
    return Semantics(
      identifier: 'otp_status',
      liveRegion: true,
      child: SizedBox(
        height: 20,
        child: Text(
          text,
          key: const ValueKey('otp_status'),
          textAlign: TextAlign.center,
          style: Theme.of(context).textTheme.bodyMedium?.copyWith(
            color: color,
            fontWeight: FontWeight.w500,
          ),
        ),
      ),
    );
  }
}

class _ResendLine extends StatelessWidget {
  const _ResendLine({required this.controller});

  final OtpController controller;

  @override
  Widget build(BuildContext context) {
    final colors = OtpColors.of(context);
    final style = Theme.of(
      context,
    ).textTheme.bodyMedium?.copyWith(color: colors.textSecondary);
    final canResend = controller.canResend;
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        Text(OtpCopy.resendPrompt, style: style),
        Semantics(
          identifier: 'otp_resend',
          button: true,
          enabled: canResend,
          child: InkWell(
            key: const ValueKey('otp_resend'),
            onTap: canResend ? controller.resend : null,
            borderRadius: BorderRadius.circular(4),
            child: Padding(
              padding: const EdgeInsets.symmetric(vertical: 8),
              child: Text(
                canResend
                    ? OtpCopy.resend
                    : OtpCopy.resendIn(controller.resendSecondsLeft),
                style: style?.copyWith(
                  color: canResend ? colors.link : colors.textSecondary,
                  fontWeight: canResend ? FontWeight.w600 : FontWeight.w400,
                ),
              ),
            ),
          ),
        ),
      ],
    );
  }
}

class _SubmitButton extends StatelessWidget {
  const _SubmitButton({required this.controller});

  final OtpController controller;

  @override
  Widget build(BuildContext context) {
    final colors = OtpColors.of(context);
    return Semantics(
      identifier: 'otp_submit',
      child: SizedBox(
        width: double.infinity,
        height: 56,
        child: FilledButton(
          key: const ValueKey('otp_submit'),
          onPressed: controller.canSubmit ? controller.submit : null,
          style: FilledButton.styleFrom(
            backgroundColor: colors.buttonBg,
            foregroundColor: colors.buttonText,
            disabledBackgroundColor: colors.buttonDisabledBg,
            disabledForegroundColor: colors.buttonText,
            shape: const StadiumBorder(),
            textStyle: const TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.w600,
            ),
          ),
          child: const Text(OtpCopy.submit),
        ),
      ),
    );
  }
}

/// A stand-in sign-in screen behind the sheet, blurred so the sheet stands out.
class _BlurredBackdrop extends StatelessWidget {
  const _BlurredBackdrop();

  @override
  Widget build(BuildContext context) {
    final colors = OtpColors.of(context);
    Widget bar({double width = double.infinity}) => Container(
      width: width,
      height: 52,
      margin: const EdgeInsets.only(bottom: 16),
      decoration: BoxDecoration(
        color: colors.digitEmptyBg,
        borderRadius: BorderRadius.circular(26),
      ),
    );
    return ExcludeSemantics(
      child: ImageFiltered(
        imageFilter: ImageFilter.blur(sigmaX: 6, sigmaY: 6),
        child: ColoredBox(
          color: colors.sheet,
          child: Padding(
            padding: const EdgeInsets.fromLTRB(24, 72, 24, 24),
            child: Column(
              children: [
                Text(
                  'Welcome back',
                  style: Theme.of(context).textTheme.headlineSmall?.copyWith(
                    color: colors.textPrimary,
                  ),
                ),
                const SizedBox(height: 40),
                bar(),
                bar(),
                const Spacer(),
                bar(),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
