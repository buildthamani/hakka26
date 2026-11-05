import 'package:flutter/material.dart';

import '../theme/otp_colors.dart';
import 'otp_controller.dart';
import 'otp_input.dart';
import 'otp_strings.dart';

/// The OTP verification bottom sheet: logo, copy, digit boxes, status,
/// resend line and Submit button.
class OtpSheet extends StatefulWidget {
  const OtpSheet({super.key, this.verifier = verifyDemoCode});

  final OtpVerifier verifier;

  @override
  State<OtpSheet> createState() => _OtpSheetState();
}

class _OtpSheetState extends State<OtpSheet> {
  late final OtpController _otp = OtpController(verifier: widget.verifier);

  @override
  void dispose() {
    _otp.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final colors = OtpColors.of(context);
    final text = Theme.of(context).textTheme;

    return Material(
      color: colors.sheet,
      borderRadius: const BorderRadius.vertical(top: Radius.circular(32)),
      clipBehavior: Clip.antiAlias,
      child: SafeArea(
        top: false,
        child: CustomScrollView(
          slivers: [
            SliverFillRemaining(
              hasScrollBody: false,
              child: Padding(
                padding: const EdgeInsets.fromLTRB(24, 12, 24, 24),
                child: ListenableBuilder(
                  listenable: _otp,
                  builder: (context, _) => Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      const _DragHandle(),
                      const SizedBox(height: 28),
                      Center(
                        child: Image.asset(
                          'assets/thamani-logo.png',
                          height: 64,
                          semanticLabel: 'Thamani logo',
                        ),
                      ),
                      const SizedBox(height: 24),
                      Text(
                        OtpStrings.title,
                        textAlign: TextAlign.center,
                        style: text.headlineSmall?.copyWith(
                          color: colors.textPrimary,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                      const SizedBox(height: 8),
                      Text(
                        OtpStrings.subtitle,
                        textAlign: TextAlign.center,
                        style: text.bodyMedium?.copyWith(
                          color: colors.textSecondary,
                        ),
                      ),
                      const SizedBox(height: 32),
                      OtpInput(controller: _otp),
                      const SizedBox(height: 20),
                      _StatusMessage(status: _otp.status),
                      const SizedBox(height: 12),
                      _ResendLine(controller: _otp),
                      const Spacer(),
                      const SizedBox(height: 24),
                      _SubmitButton(
                        onPressed: _otp.isComplete ? _otp.submit : null,
                      ),
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

class _DragHandle extends StatelessWidget {
  const _DragHandle();

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Container(
        width: 40,
        height: 4,
        decoration: BoxDecoration(
          color: OtpColors.of(context).digitEmptyOutline,
          borderRadius: BorderRadius.circular(2),
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
    final (message, color) = switch (status) {
      OtpStatus.success => (OtpStrings.success, colors.success),
      OtpStatus.error => (OtpStrings.error, colors.error),
      OtpStatus.idle => ('', colors.textSecondary),
    };
    return Semantics(
      identifier: OtpTestIds.status,
      liveRegion: true,
      child: SizedBox(
        height: 20,
        child: Text(
          message,
          key: const ValueKey(OtpTestIds.status),
          textAlign: TextAlign.center,
          style: Theme.of(context).textTheme.bodyMedium?.copyWith(
            color: color,
            fontWeight: FontWeight.w600,
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

    final button = Semantics(
      identifier: OtpTestIds.resend,
      child: TextButton(
        key: const ValueKey(OtpTestIds.resend),
        onPressed: canResend ? controller.resend : null,
        style: TextButton.styleFrom(
          padding: const EdgeInsets.symmetric(horizontal: 2),
          minimumSize: const Size(0, 36),
          tapTargetSize: MaterialTapTargetSize.shrinkWrap,
          foregroundColor: colors.link,
          disabledForegroundColor: colors.textSecondary,
          textStyle: style?.copyWith(
            fontWeight: canResend ? FontWeight.w600 : null,
          ),
        ),
        child: Text(
          canResend
              ? OtpStrings.resend
              : OtpStrings.resendIn(controller.secondsLeft),
        ),
      ),
    );

    return Wrap(
      alignment: WrapAlignment.center,
      crossAxisAlignment: WrapCrossAlignment.center,
      children: [
        if (canResend) Text(OtpStrings.resendPrompt, style: style),
        button,
      ],
    );
  }
}

class _SubmitButton extends StatelessWidget {
  const _SubmitButton({required this.onPressed});

  final VoidCallback? onPressed;

  @override
  Widget build(BuildContext context) {
    final colors = OtpColors.of(context);
    return Semantics(
      identifier: OtpTestIds.submit,
      child: FilledButton(
        key: const ValueKey(OtpTestIds.submit),
        onPressed: onPressed,
        style: FilledButton.styleFrom(
          minimumSize: const Size.fromHeight(56),
          shape: const StadiumBorder(),
          backgroundColor: colors.buttonBg,
          foregroundColor: colors.buttonText,
          disabledBackgroundColor: colors.buttonDisabledBg,
          disabledForegroundColor: colors.buttonText,
          textStyle: const TextStyle(fontSize: 16, fontWeight: FontWeight.w600),
        ),
        child: const Text(OtpStrings.submit),
      ),
    );
  }
}
