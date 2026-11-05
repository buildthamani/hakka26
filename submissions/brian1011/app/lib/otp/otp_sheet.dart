import 'dart:ui' show ImageFilter;

import 'package:flutter/material.dart';

import 'otp_colors.dart';
import 'otp_controller.dart';
import 'otp_input.dart';

class OtpCopy {
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

/// OTP verification shown as a bottom sheet over a dimmed, blurred page.
class OtpScreen extends StatefulWidget {
  const OtpScreen({super.key, this.controller});

  /// Optional, for tests; otherwise the screen creates its own.
  final OtpController? controller;

  @override
  State<OtpScreen> createState() => _OtpScreenState();
}

class _OtpScreenState extends State<OtpScreen> {
  late final OtpController _otp = widget.controller ?? OtpController();

  @override
  void dispose() {
    if (widget.controller == null) _otp.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final colors = OtpColors.of(context);
    return Scaffold(
      body: Stack(
        fit: StackFit.expand,
        children: [
          const _BackgroundPage(),
          BackdropFilter(
            filter: ImageFilter.blur(sigmaX: 6, sigmaY: 6),
            child: ColoredBox(color: colors.scrim),
          ),
          LayoutBuilder(
            builder: (context, constraints) => Align(
              alignment: Alignment.bottomCenter,
              child: SizedBox(
                height: constraints.maxHeight * 0.86,
                child: _OtpSheet(controller: _otp),
              ),
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
    return DecoratedBox(
      decoration: BoxDecoration(
        color: colors.sheet,
        borderRadius: const BorderRadius.vertical(top: Radius.circular(28)),
      ),
      child: SafeArea(
        top: false,
        child: CustomScrollView(
          slivers: [
            SliverFillRemaining(
              hasScrollBody: false,
              child: Padding(
                padding: const EdgeInsets.fromLTRB(24, 32, 24, 24),
                child: ListenableBuilder(
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
                        style: TextStyle(
                          fontSize: 26,
                          fontWeight: FontWeight.w600,
                          color: colors.textPrimary,
                        ),
                      ),
                      const SizedBox(height: 8),
                      Text(
                        OtpCopy.subtitle,
                        textAlign: TextAlign.center,
                        style: TextStyle(fontSize: 15, height: 1.4, color: colors.textSecondary),
                      ),
                      const SizedBox(height: 28),
                      OtpInput(controller: controller),
                      const SizedBox(height: 16),
                      _StatusMessage(status: controller.status),
                      const SizedBox(height: 8),
                      _ResendLine(controller: controller),
                      const Spacer(),
                      const SizedBox(height: 24),
                      _SubmitButton(controller: controller),
                    ],
                  ),
                ),
              ),
            ),
          ],
        ),
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
      OtpStatus.idle => ('', colors.textSecondary),
    };
    return Semantics(
      liveRegion: true,
      child: Text(
        text,
        key: const Key('otp_status'),
        textAlign: TextAlign.center,
        style: TextStyle(fontSize: 14, fontWeight: FontWeight.w500, color: color),
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
    final ready = controller.canResend;
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        if (ready)
          Text(OtpCopy.resendPrompt, style: TextStyle(fontSize: 14, color: colors.textSecondary)),
        TextButton(
          key: const Key('otp_resend'),
          onPressed: ready ? controller.resend : null,
          style: TextButton.styleFrom(
            padding: const EdgeInsets.symmetric(horizontal: 4),
            minimumSize: const Size(0, 36),
            foregroundColor: colors.link,
            disabledForegroundColor: colors.textSecondary,
          ),
          child: Text(
            ready ? OtpCopy.resend : OtpCopy.resendIn(controller.secondsLeft),
            style: TextStyle(fontSize: 14, fontWeight: ready ? FontWeight.w600 : FontWeight.w400),
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
    return SizedBox(
      width: double.infinity,
      height: 56,
      child: FilledButton(
        key: const Key('otp_submit'),
        onPressed: controller.isComplete ? controller.submit : null,
        style: FilledButton.styleFrom(
          shape: const StadiumBorder(),
          backgroundColor: colors.buttonBg,
          foregroundColor: colors.buttonText,
          disabledBackgroundColor: colors.buttonDisabledBg,
          disabledForegroundColor: colors.buttonText,
        ),
        child:
            const Text(OtpCopy.submit, style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600)),
      ),
    );
  }
}

/// Placeholder page behind the sheet, so the dimmed background reads as an app.
class _BackgroundPage extends StatelessWidget {
  const _BackgroundPage();

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return ExcludeSemantics(
      child: ColoredBox(
        color: scheme.surface,
        child: SafeArea(
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: Column(
              children: [
                const SizedBox(height: 24),
                Text('Welcome back', style: Theme.of(context).textTheme.headlineSmall),
                const SizedBox(height: 24),
                for (var i = 0; i < 3; i++)
                  Container(
                    height: 56,
                    margin: const EdgeInsets.only(bottom: 12),
                    decoration: BoxDecoration(
                      color: scheme.surfaceContainerHighest,
                      borderRadius: BorderRadius.circular(16),
                    ),
                  ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
