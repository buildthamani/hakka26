import 'dart:ui' show ImageFilter;

import 'package:flutter/material.dart';

import 'otp_colours.dart';
import 'otp_controller.dart';
import 'otp_copy.dart';
import 'otp_input.dart';

/// The OTP verification screen: a bottom sheet over a dimmed, blurred page.
class OtpScreen extends StatefulWidget {
  const OtpScreen({super.key, this.verifier});

  /// Overrides how codes are checked; defaults to [OtpController.acceptsDemoCode].
  final OtpVerifier? verifier;

  @override
  State<OtpScreen> createState() => _OtpScreenState();
}

class _OtpScreenState extends State<OtpScreen> {
  // A code has just been sent when this screen opens, so the cooldown starts.
  late final OtpController _controller = OtpController(
    verifier: widget.verifier,
  )..startResendCooldown();

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final colours = OtpColours.of(context);
    return Scaffold(
      backgroundColor: colours.sheet,
      body: Stack(
        fit: StackFit.expand,
        children: [
          const _BackgroundPage(),
          BackdropFilter(
            filter: ImageFilter.blur(sigmaX: 6, sigmaY: 6),
            child: ColoredBox(color: colours.scrim),
          ),
          Align(
            alignment: Alignment.bottomCenter,
            child: FractionallySizedBox(
              heightFactor: 0.88,
              widthFactor: 1,
              child: OtpSheet(controller: _controller),
            ),
          ),
        ],
      ),
    );
  }
}

/// Sheet content: logo, copy, digit boxes, resend line, status and Submit.
class OtpSheet extends StatelessWidget {
  const OtpSheet({super.key, required this.controller});

  final OtpController controller;

  @override
  Widget build(BuildContext context) {
    final colours = OtpColours.of(context);
    final isDark = Theme.of(context).brightness == Brightness.dark;
    return Material(
      key: const ValueKey('otp_sheet'),
      color: colours.sheet,
      borderRadius: const BorderRadius.vertical(top: Radius.circular(28)),
      clipBehavior: Clip.antiAlias,
      child: SafeArea(
        top: false,
        child: Padding(
          padding: const EdgeInsets.fromLTRB(24, 36, 24, 24),
          child: Column(
            children: [
              Expanded(
                child: SingleChildScrollView(
                  child: Column(
                    children: [
                      Image.asset(
                        isDark
                            ? 'assets/thamani-logo-white.png'
                            : 'assets/thamani-logo.png',
                        height: 64,
                        semanticLabel: 'Thamani logo',
                      ),
                      const SizedBox(height: 20),
                      Text(
                        OtpCopy.title,
                        textAlign: TextAlign.center,
                        style: TextStyle(
                          fontSize: 26,
                          fontWeight: FontWeight.w600,
                          color: colours.textPrimary,
                        ),
                      ),
                      const SizedBox(height: 8),
                      Text(
                        OtpCopy.subtitle,
                        textAlign: TextAlign.center,
                        style: TextStyle(
                          fontSize: 15,
                          height: 1.4,
                          color: colours.textSecondary,
                        ),
                      ),
                      const SizedBox(height: 32),
                      OtpInput(controller: controller),
                      const SizedBox(height: 20),
                      _ResendLine(controller: controller),
                      const SizedBox(height: 8),
                      _StatusMessage(controller: controller),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 16),
              _SubmitButton(controller: controller),
            ],
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
    final colours = OtpColours.of(context);
    return ListenableBuilder(
      listenable: controller,
      builder: (context, _) => Wrap(
        alignment: WrapAlignment.center,
        crossAxisAlignment: WrapCrossAlignment.center,
        children: [
          Text(
            OtpCopy.resendPrompt,
            style: TextStyle(fontSize: 14, color: colours.textSecondary),
          ),
          TextButton(
            key: const ValueKey('otp_resend'),
            onPressed: controller.canResend ? controller.resend : null,
            style: TextButton.styleFrom(
              foregroundColor: colours.link,
              disabledForegroundColor: colours.textSecondary,
              padding: const EdgeInsets.symmetric(horizontal: 4),
              minimumSize: const Size(48, 40),
              textStyle: const TextStyle(
                fontSize: 14,
                fontWeight: FontWeight.w600,
              ),
            ),
            child: Text(
              controller.canResend
                  ? OtpCopy.resend
                  : OtpCopy.resendIn(controller.secondsUntilResend),
            ),
          ),
        ],
      ),
    );
  }
}

class _StatusMessage extends StatelessWidget {
  const _StatusMessage({required this.controller});

  final OtpController controller;

  @override
  Widget build(BuildContext context) {
    final colours = OtpColours.of(context);
    return ListenableBuilder(
      listenable: controller,
      builder: (context, _) {
        final (message, colour) = switch (controller.status) {
          OtpStatus.success => (OtpCopy.success, colours.success),
          OtpStatus.error => (OtpCopy.error, colours.error),
          OtpStatus.idle => ('', colours.textSecondary),
        };
        return Semantics(
          liveRegion: true,
          child: Text(
            message,
            key: const ValueKey('otp_status'),
            textAlign: TextAlign.center,
            style: TextStyle(
              fontSize: 14,
              fontWeight: FontWeight.w500,
              color: colour,
            ),
          ),
        );
      },
    );
  }
}

class _SubmitButton extends StatelessWidget {
  const _SubmitButton({required this.controller});

  final OtpController controller;

  @override
  Widget build(BuildContext context) {
    final colours = OtpColours.of(context);
    return ListenableBuilder(
      listenable: controller,
      builder: (context, _) => SizedBox(
        width: double.infinity,
        height: 56,
        child: FilledButton(
          key: const ValueKey('otp_submit'),
          onPressed: controller.isComplete ? controller.submit : null,
          style: FilledButton.styleFrom(
            backgroundColor: colours.buttonBg,
            foregroundColor: colours.buttonText,
            disabledBackgroundColor: colours.buttonDisabledBg,
            disabledForegroundColor: colours.buttonText,
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

/// Stand-in for the page the sheet opens over; it is dimmed and blurred.
class _BackgroundPage extends StatelessWidget {
  const _BackgroundPage();

  @override
  Widget build(BuildContext context) {
    final colours = OtpColours.of(context);
    Widget block(double height, {double widthFactor = 1}) =>
        FractionallySizedBox(
          widthFactor: widthFactor,
          child: Container(
            height: height,
            margin: const EdgeInsets.only(bottom: 16),
            decoration: BoxDecoration(
              color: colours.digitEmptyBg,
              borderRadius: BorderRadius.circular(16),
            ),
          ),
        );

    return ExcludeSemantics(
      child: ColoredBox(
        color: colours.sheet,
        child: SafeArea(
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: Column(
              children: [
                block(28, widthFactor: 0.5),
                block(56),
                block(56),
                block(120),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
