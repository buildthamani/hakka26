import 'dart:ui';

import 'package:flutter/material.dart';

import 'otp_code.dart';
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

  static String resendIn(int seconds) => 'Resend in ${formatCountdown(seconds)}';
}

/// The verification screen: a bottom sheet over a dimmed, blurred page.
class OtpScreen extends StatefulWidget {
  const OtpScreen({super.key, this.verifier = demoVerifier});

  final OtpVerifier verifier;

  @override
  State<OtpScreen> createState() => _OtpScreenState();
}

class _OtpScreenState extends State<OtpScreen> {
  late final OtpController _controller = OtpController(verifier: widget.verifier);
  final FocusNode _focusNode = FocusNode(debugLabel: 'otp');
  OtpStatus _lastStatus = OtpStatus.idle;

  @override
  void initState() {
    super.initState();
    _controller.addListener(_onControllerChange);
  }

  void _onControllerChange() {
    // After a wrong code the boxes are cleared; put focus back on the first box.
    if (_controller.status == OtpStatus.error && _lastStatus != OtpStatus.error) {
      _focusNode.requestFocus();
    }
    _lastStatus = _controller.status;
  }

  @override
  void dispose() {
    _controller.removeListener(_onControllerChange);
    _controller.dispose();
    _focusNode.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final colors = OtpColors.of(context);
    return Scaffold(
      body: Stack(
        children: [
          const _BackgroundPage(),
          Positioned.fill(
            child: BackdropFilter(
              filter: ImageFilter.blur(sigmaX: 6, sigmaY: 6),
              child: ColoredBox(color: colors.scrim),
            ),
          ),
          SafeArea(
            child: Padding(
              padding: const EdgeInsets.fromLTRB(8, 56, 8, 8),
              child: ListenableBuilder(
                listenable: _controller,
                builder: (context, _) => _OtpSheet(controller: _controller, focusNode: _focusNode),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _OtpSheet extends StatelessWidget {
  const _OtpSheet({required this.controller, required this.focusNode});

  final OtpController controller;
  final FocusNode focusNode;

  @override
  Widget build(BuildContext context) {
    final colors = OtpColors.of(context);
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return DecoratedBox(
      decoration: BoxDecoration(color: colors.sheet, borderRadius: BorderRadius.circular(28)),
      child: CustomScrollView(
        slivers: [
          SliverFillRemaining(
            hasScrollBody: false,
            child: Padding(
              padding: const EdgeInsets.fromLTRB(24, 32, 24, 24),
              child: Column(
                children: [
                  Image.asset(
                    isDark ? 'assets/thamani-logo-white.png' : 'assets/thamani-logo.png',
                    height: 56,
                    semanticLabel: 'Thamani logo',
                  ),
                  const SizedBox(height: 20),
                  Text(
                    OtpCopy.title,
                    textAlign: TextAlign.center,
                    style: TextStyle(fontSize: 26, fontWeight: FontWeight.w600, color: colors.textPrimary),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    OtpCopy.subtitle,
                    textAlign: TextAlign.center,
                    style: TextStyle(fontSize: 15, height: 1.4, color: colors.textSecondary),
                  ),
                  const SizedBox(height: 32),
                  OtpInput(
                    code: controller.code,
                    focusNode: focusNode,
                    onChanged: controller.updateCode,
                    enabled: controller.status != OtpStatus.success,
                  ),
                  const SizedBox(height: 20),
                  _ResendLine(controller: controller),
                  const SizedBox(height: 12),
                  _StatusMessage(status: controller.status),
                  const Spacer(),
                  const SizedBox(height: 24),
                  _SubmitButton(onPressed: controller.canSubmit ? controller.submit : null),
                ],
              ),
            ),
          ),
        ],
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
    final canResend = controller.canResend;
    return Wrap(
      alignment: WrapAlignment.center,
      crossAxisAlignment: WrapCrossAlignment.center,
      children: [
        Text(OtpCopy.resendPrompt, style: TextStyle(fontSize: 14, color: colors.textSecondary)),
        TextButton(
          key: const Key('otp_resend'),
          onPressed: canResend ? controller.resend : null,
          style: TextButton.styleFrom(
            padding: const EdgeInsets.symmetric(horizontal: 4),
            minimumSize: const Size(44, 40),
            foregroundColor: colors.link,
            disabledForegroundColor: colors.textSecondary,
            textStyle: const TextStyle(fontSize: 14, fontWeight: FontWeight.w600),
          ),
          child: Text(canResend ? OtpCopy.resend : OtpCopy.resendIn(controller.secondsLeft)),
        ),
      ],
    );
  }
}

class _StatusMessage extends StatelessWidget {
  const _StatusMessage({required this.status});

  final OtpStatus status;

  @override
  Widget build(BuildContext context) {
    final colors = OtpColors.of(context);
    final (String text, Color color) = switch (status) {
      OtpStatus.success => (OtpCopy.success, colors.success),
      OtpStatus.error => (OtpCopy.error, colors.error),
      OtpStatus.idle => ('', colors.textSecondary),
    };
    return Semantics(
      liveRegion: true,
      child: SizedBox(
        height: 22,
        child: Text(
          text,
          key: const Key('otp_status'),
          textAlign: TextAlign.center,
          style: TextStyle(fontSize: 14, fontWeight: FontWeight.w500, color: color),
        ),
      ),
    );
  }
}

class _SubmitButton extends StatelessWidget {
  const _SubmitButton({required this.onPressed});

  final VoidCallback? onPressed;

  @override
  Widget build(BuildContext context) {
    final colors = OtpColors.of(context);
    return SizedBox(
      width: double.infinity,
      height: 56,
      child: FilledButton(
        key: const Key('otp_submit'),
        onPressed: onPressed,
        style: FilledButton.styleFrom(
          shape: const StadiumBorder(),
          backgroundColor: colors.buttonBg,
          foregroundColor: colors.buttonText,
          disabledBackgroundColor: colors.buttonDisabledBg,
          disabledForegroundColor: colors.buttonText,
          textStyle: const TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
        ),
        child: const Text(OtpCopy.submit),
      ),
    );
  }
}

/// The page behind the sheet, shown dimmed and blurred.
class _BackgroundPage extends StatelessWidget {
  const _BackgroundPage();

  @override
  Widget build(BuildContext context) {
    final colors = OtpColors.of(context);
    final block = colors.textSecondary.withValues(alpha: 0.15);
    return ExcludeSemantics(
      child: ColoredBox(
        color: colors.sheet,
        child: SafeArea(
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                const SizedBox(height: 24),
                Text(
                  'Welcome back',
                  textAlign: TextAlign.center,
                  style: TextStyle(fontSize: 22, fontWeight: FontWeight.w600, color: colors.textPrimary),
                ),
                const SizedBox(height: 32),
                for (var i = 0; i < 4; i++) ...[
                  Container(
                    height: 56,
                    decoration: BoxDecoration(color: block, borderRadius: BorderRadius.circular(16)),
                  ),
                  const SizedBox(height: 16),
                ],
              ],
            ),
          ),
        ),
      ),
    );
  }
}
