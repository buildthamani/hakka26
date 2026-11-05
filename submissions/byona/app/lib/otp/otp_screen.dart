import 'dart:math' as math;
import 'dart:ui' show ImageFilter;

import 'package:flutter/material.dart';

import 'otp_colors.dart';
import 'otp_controller.dart';
import 'otp_input.dart';

const _title = 'Enter your code';
const _subtitle = 'A 6-digit code has been sent to your phone.';
const _resendPrompt = "Didn't receive the code? ";
const _resendAction = 'Resend';
const _submitLabel = 'Submit';
const _successMessage = "You're in.";
const _errorMessage = "That code didn't work. Try again.";

const _sheetMargin = 16.0;
const _sheetMaxWidth = 480.0;
const _sheetScreenHeightFactor = 0.83;

/// The OTP sheet floating over a blurred, dimmed page.
class OtpScreen extends StatefulWidget {
  const OtpScreen({super.key});

  @override
  State<OtpScreen> createState() => _OtpScreenState();
}

class _OtpScreenState extends State<OtpScreen> {
  final _otp = OtpController();

  @override
  void dispose() {
    _otp.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Stack(
      fit: StackFit.expand,
      children: [
        const _BlurredBackdrop(),
        // The Scaffold shrinks its body to sit above the keyboard. The backdrop
        // stays outside it so that only the sheet moves.
        Scaffold(
          backgroundColor: Colors.transparent,
          body: SafeArea(
            child: Padding(
              padding: const EdgeInsets.all(_sheetMargin),
              child: LayoutBuilder(
                builder: (context, constraints) {
                  final screenHeight = MediaQuery.sizeOf(context).height;
                  final sheetHeight = math.min(
                    constraints.maxHeight,
                    screenHeight * _sheetScreenHeightFactor,
                  );
                  return Align(
                    alignment: Alignment.bottomCenter,
                    child: ConstrainedBox(
                      constraints: const BoxConstraints(
                        maxWidth: _sheetMaxWidth,
                      ),
                      child: SizedBox(
                        width: double.infinity,
                        height: sheetHeight,
                        child: _OtpSheet(otp: _otp),
                      ),
                    ),
                  );
                },
              ),
            ),
          ),
        ),
      ],
    );
  }
}

/// A placeholder page, unreadable behind the blur, for the sheet to sit over.
class _BlurredBackdrop extends StatelessWidget {
  const _BlurredBackdrop();

  @override
  Widget build(BuildContext context) {
    final colors = OtpColors.of(context);
    Widget block(double height) => Container(
      height: height,
      margin: const EdgeInsets.only(bottom: 20),
      decoration: BoxDecoration(
        color: colors.digitEmptyOutline,
        borderRadius: BorderRadius.circular(12),
      ),
    );

    return ExcludeSemantics(
      child: Stack(
        fit: StackFit.expand,
        children: [
          ColoredBox(
            color: colors.sheet,
            child: ListView(
              physics: const NeverScrollableScrollPhysics(),
              padding: const EdgeInsets.fromLTRB(32, 72, 32, 0),
              children: [block(28), block(56), block(56), block(56)],
            ),
          ),
          BackdropFilter(
            filter: ImageFilter.blur(sigmaX: 12, sigmaY: 12),
            child: ColoredBox(color: colors.scrim),
          ),
        ],
      ),
    );
  }
}

class _OtpSheet extends StatelessWidget {
  const _OtpSheet({required this.otp});

  final OtpController otp;

  @override
  Widget build(BuildContext context) {
    final colors = OtpColors.of(context);
    final isDark = Theme.of(context).brightness == Brightness.dark;

    return Material(
      color: colors.sheet,
      borderRadius: BorderRadius.circular(28),
      clipBehavior: Clip.antiAlias,
      child: Padding(
        padding: const EdgeInsets.fromLTRB(20, 32, 20, 20),
        child: Column(
          children: [
            // Only this part scrolls, which keeps Submit pinned and on screen
            // when the keyboard leaves too little height for everything.
            Expanded(
              child: SingleChildScrollView(
                child: Column(
                  children: [
                    Image.asset(
                      isDark
                          ? 'assets/thamani-logo-white.png'
                          : 'assets/thamani-logo.png',
                      height: 56,
                      semanticLabel: 'Thamani logo',
                    ),
                    const SizedBox(height: 20),
                    Text(
                      _title,
                      textAlign: TextAlign.center,
                      style: TextStyle(
                        color: colors.textPrimary,
                        fontSize: 24,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                    const SizedBox(height: 8),
                    ConstrainedBox(
                      constraints: const BoxConstraints(maxWidth: 220),
                      child: Text(
                        _subtitle,
                        textAlign: TextAlign.center,
                        style: TextStyle(
                          color: colors.textSecondary,
                          fontSize: 14,
                          height: 1.4,
                        ),
                      ),
                    ),
                    const SizedBox(height: 28),
                    OtpInput(controller: otp),
                    const SizedBox(height: 12),
                    _StatusMessage(otp: otp),
                    const SizedBox(height: 4),
                    _ResendLine(otp: otp),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 12),
            _SubmitButton(otp: otp),
          ],
        ),
      ),
    );
  }
}

class _StatusMessage extends StatelessWidget {
  const _StatusMessage({required this.otp});

  final OtpController otp;

  @override
  Widget build(BuildContext context) {
    final colors = OtpColors.of(context);

    return ListenableBuilder(
      listenable: otp,
      builder: (context, _) {
        final (message, color) = switch (otp.status) {
          OtpStatus.idle => ('', colors.textSecondary),
          OtpStatus.success => (_successMessage, colors.success),
          OtpStatus.error => (_errorMessage, colors.error),
        };
        // The reserved height stops the layout jumping when a message appears.
        return ConstrainedBox(
          constraints: const BoxConstraints(minHeight: 20),
          child: Semantics(
            liveRegion: true,
            child: Text(
              message,
              key: const Key('otp_status'),
              textAlign: TextAlign.center,
              style: TextStyle(
                color: color,
                fontSize: 14,
                fontWeight: FontWeight.w500,
              ),
            ),
          ),
        );
      },
    );
  }
}

class _ResendLine extends StatelessWidget {
  const _ResendLine({required this.otp});

  final OtpController otp;

  @override
  Widget build(BuildContext context) {
    final colors = OtpColors.of(context);

    return ValueListenableBuilder(
      valueListenable: otp.resendSecondsLeft,
      builder: (context, secondsLeft, _) {
        final canResend = secondsLeft == 0;
        final countdown = secondsLeft.toString().padLeft(2, '0');

        return Wrap(
          alignment: WrapAlignment.center,
          crossAxisAlignment: WrapCrossAlignment.center,
          children: [
            if (canResend)
              Text(
                _resendPrompt,
                style: TextStyle(color: colors.textSecondary, fontSize: 14),
              ),
            TextButton(
              key: const Key('otp_resend'),
              onPressed: canResend ? otp.resend : null,
              style: TextButton.styleFrom(
                foregroundColor: colors.link,
                disabledForegroundColor: colors.textSecondary,
                textStyle: Theme.of(context).textTheme.labelLarge,
                padding: EdgeInsets.zero,
                minimumSize: const Size(0, 44),
              ),
              child: Text(canResend ? _resendAction : 'Resend in 0:$countdown'),
            ),
          ],
        );
      },
    );
  }
}

class _SubmitButton extends StatelessWidget {
  const _SubmitButton({required this.otp});

  final OtpController otp;

  @override
  Widget build(BuildContext context) {
    final colors = OtpColors.of(context);

    return ListenableBuilder(
      listenable: otp,
      builder: (context, _) => FilledButton(
        key: const Key('otp_submit'),
        onPressed: otp.isComplete ? otp.submit : null,
        style: FilledButton.styleFrom(
          backgroundColor: colors.buttonBg,
          foregroundColor: colors.buttonText,
          disabledBackgroundColor: colors.buttonDisabledBg,
          disabledForegroundColor: colors.buttonText,
          minimumSize: const Size.fromHeight(52),
          shape: const StadiumBorder(),
          textStyle: Theme.of(
            context,
          ).textTheme.labelLarge?.copyWith(fontSize: 16),
        ),
        child: const Text(_submitLabel),
      ),
    );
  }
}
