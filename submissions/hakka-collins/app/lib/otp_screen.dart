import 'dart:async';

import 'package:flutter/material.dart';

import 'otp_controller.dart';
import 'otp_input.dart';
import 'otp_service.dart';
import 'otp_theme.dart';

enum OtpStatus { none, success, error }

const _codeLength = 6;
const _resendSeconds = 30;

class OtpScreen extends StatefulWidget {
  final OtpColors colors;
  final OtpService service;

  const OtpScreen({
    super.key,
    required this.colors,
    this.service = const MockOtpService(),
  });

  @override
  State<OtpScreen> createState() => _OtpScreenState();
}

class _OtpScreenState extends State<OtpScreen> {
  final _otp = OtpController(length: _codeLength);
  OtpStatus _status = OtpStatus.none;
  bool _verifying = false;
  int _secondsLeft = _resendSeconds;
  Timer? _timer;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) _otp.focus(0);
    });
    _startCountdown();
  }

  @override
  void dispose() {
    _timer?.cancel();
    _otp.dispose();
    super.dispose();
  }

  void _startCountdown() {
    _timer?.cancel();
    setState(() => _secondsLeft = _resendSeconds);
    _timer = Timer.periodic(const Duration(seconds: 1), (timer) {
      setState(() => _secondsLeft--);
      if (_secondsLeft == 0) timer.cancel();
    });
  }

  void _resend() {
    _startCountdown();
    unawaited(widget.service.resendCode());
  }

  void _onCodeEdited(String _) {
    if (_status != OtpStatus.none) setState(() => _status = OtpStatus.none);
  }

  Future<void> _submit() async {
    setState(() => _verifying = true);
    final verified = await widget.service.verify(_otp.code);
    if (!mounted) return;
    setState(() {
      _verifying = false;
      _status = verified ? OtpStatus.success : OtpStatus.error;
    });
    if (!verified) _otp.clear();
  }

  @override
  Widget build(BuildContext context) {
    final colors = widget.colors;
    final textTheme = Theme.of(context).textTheme;

    return Material(
      color: colors.sheet,
      clipBehavior: Clip.antiAlias,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(28),
        side: BorderSide(color: colors.digitEmptyOutline),
      ),
      child: CustomScrollView(
        slivers: [
          SliverFillRemaining(
            hasScrollBody: false,
            child: Padding(
              padding: const EdgeInsets.fromLTRB(20, 40, 20, 40),
              child: Column(
                children: [
                  // The logo artwork only reads on a light surface, so dark mode gets one too.
                  DecoratedBox(
                    decoration: const BoxDecoration(
                      color: Colors.white,
                      shape: BoxShape.circle,
                    ),
                    child: Padding(
                      padding: const EdgeInsets.all(8),
                      child: Image.asset(
                        'assets/otp-logo.png',
                        height: 64,
                        semanticLabel: 'Logo',
                      ),
                    ),
                  ),
                  const SizedBox(height: 16),
                  Text(
                    'Enter your code',
                    textAlign: TextAlign.center,
                    style: TextStyle(
                      color: colors.textPrimary,
                      fontSize: 26,
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                  const SizedBox(height: 10),
                  ConstrainedBox(
                    constraints: const BoxConstraints(maxWidth: 220),
                    child: Text(
                      'A 6-digit code has been sent to your phone.',
                      textAlign: TextAlign.center,
                      style: TextStyle(
                        color: colors.textPrimary,
                        fontSize: 14,
                        height: 1.45,
                      ),
                    ),
                  ),
                  const SizedBox(height: 36),
                  OtpInput(
                    controller: _otp,
                    colors: colors,
                    onChanged: _onCodeEdited,
                  ),
                  const SizedBox(height: 24),
                  _ResendLine(
                    secondsLeft: _secondsLeft,
                    colors: colors,
                    onResend: _secondsLeft == 0 ? _resend : null,
                  ),
                  const SizedBox(height: 20),
                  Semantics(
                    liveRegion: true,
                    child: Text(
                      switch (_status) {
                        OtpStatus.success => "You're in.",
                        OtpStatus.error => "That code didn't work. Try again.",
                        OtpStatus.none => '',
                      },
                      key: const Key('otp_status'),
                      textAlign: TextAlign.center,
                      style: TextStyle(
                        color: _status == OtpStatus.error
                            ? colors.error
                            : colors.success,
                        fontSize: 14,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ),
                  const Spacer(),
                  const SizedBox(height: 24),
                  SizedBox(
                    width: double.infinity,
                    height: 52,
                    child: ListenableBuilder(
                      listenable: _otp,
                      builder: (context, _) => FilledButton(
                        key: const Key('otp_submit'),
                        onPressed: _otp.isComplete && !_verifying
                            ? _submit
                            : null,
                        style: FilledButton.styleFrom(
                          backgroundColor: colors.buttonBg,
                          foregroundColor: colors.buttonText,
                          disabledBackgroundColor: colors.buttonDisabledBg,
                          disabledForegroundColor: colors.buttonText,
                          shape: const StadiumBorder(),
                          textStyle: textTheme.labelLarge?.copyWith(
                            fontSize: 15,
                            fontWeight: FontWeight.w500,
                          ),
                        ),
                        child: _verifying
                            ? SizedBox.square(
                                dimension: 20,
                                child: CircularProgressIndicator(
                                  strokeWidth: 2,
                                  color: colors.buttonBg,
                                  semanticsLabel: 'Verifying code',
                                ),
                              )
                            : const Text('Submit'),
                      ),
                    ),
                  ),
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
  final int secondsLeft;
  final OtpColors colors;
  final VoidCallback? onResend;

  const _ResendLine({
    required this.secondsLeft,
    required this.colors,
    required this.onResend,
  });

  @override
  Widget build(BuildContext context) {
    final style = Theme.of(
      context,
    ).textTheme.bodyMedium!.copyWith(fontSize: 13.5);
    final seconds = secondsLeft.toString().padLeft(2, '0');

    return Wrap(
      alignment: WrapAlignment.center,
      crossAxisAlignment: WrapCrossAlignment.center,
      children: [
        Text(
          "Didn't receive the code? ",
          style: style.copyWith(color: colors.textSecondary),
        ),
        TextButton(
          key: const Key('otp_resend'),
          onPressed: onResend,
          style: TextButton.styleFrom(
            padding: EdgeInsets.zero,
            minimumSize: Size.zero,
            tapTargetSize: MaterialTapTargetSize.shrinkWrap,
            foregroundColor: colors.link,
            disabledForegroundColor: colors.textSecondary,
            textStyle: style.copyWith(fontWeight: FontWeight.w500),
          ),
          child: Text(secondsLeft > 0 ? 'Resend in 0:$seconds' : 'Resend'),
        ),
      ],
    );
  }
}
