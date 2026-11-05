import 'dart:async';
import 'dart:ui';

import 'package:flutter/material.dart';

import 'otp_colors.dart';
import 'otp_input.dart';

const resendCooldown = 30;

/// Demo verification code from the challenge spec; a real app would ask the server.
const _validCode = '123456';

enum OtpStatus { none, success, error }

class OtpScreen extends StatefulWidget {
  const OtpScreen({super.key});

  @override
  State<OtpScreen> createState() => _OtpScreenState();
}

class _OtpScreenState extends State<OtpScreen> {
  final _controller = TextEditingController();
  final _focusNode = FocusNode();
  Timer? _timer;
  int _secondsLeft = resendCooldown;
  OtpStatus _status = OtpStatus.none;

  @override
  void initState() {
    super.initState();
    _controller.addListener(() => setState(() {}));
    _startCountdown();
  }

  @override
  void dispose() {
    _timer?.cancel();
    _controller.dispose();
    _focusNode.dispose();
    super.dispose();
  }

  void _startCountdown() {
    _timer?.cancel();
    setState(() => _secondsLeft = resendCooldown);
    _timer = Timer.periodic(const Duration(seconds: 1), (timer) {
      setState(() => _secondsLeft--);
      if (_secondsLeft == 0) timer.cancel();
    });
  }

  void _submit() {
    final ok = _controller.text == _validCode;
    setState(() => _status = ok ? OtpStatus.success : OtpStatus.error);
    if (!ok) {
      _controller.clear();
      _focusNode.requestFocus();
    }
  }

  @override
  Widget build(BuildContext context) {
    final c = OtpColors.of(context);
    final text = Theme.of(context).textTheme;
    final complete = _controller.text.length == otpLength;
    final counting = _secondsLeft > 0;

    return Scaffold(
      body: Stack(
        fit: StackFit.expand,
        children: [
          const _BackgroundPage(),
          BackdropFilter(
            filter: ImageFilter.blur(sigmaX: 6, sigmaY: 6),
            child: ColoredBox(color: c.scrim),
          ),
          Align(
            alignment: Alignment.bottomCenter,
            child: FractionallySizedBox(
              heightFactor: 0.85,
              child: Container(
                width: double.infinity,
                decoration: BoxDecoration(
                  color: c.sheet,
                  borderRadius: const BorderRadius.vertical(
                    top: Radius.circular(28),
                  ),
                ),
                child: SafeArea(
                  top: false,
                  // Fills the sheet so Submit sits at the bottom, but still scrolls
                  // when the keyboard leaves too little room.
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
                        child: IntrinsicHeight(
                          child: Column(
                            children: [
                              Image.asset(
                                'assets/thamani-logo.png',
                                width: 64,
                                height: 64,
                                semanticLabel: 'Thamani logo',
                              ),
                              const SizedBox(height: 16),
                              Text(
                                'Enter your code',
                                textAlign: TextAlign.center,
                                style: text.headlineSmall?.copyWith(
                                  color: c.textPrimary,
                                  fontWeight: FontWeight.w600,
                                ),
                              ),
                              const SizedBox(height: 8),
                              Text(
                                'A 6-digit code has been sent to your phone.',
                                textAlign: TextAlign.center,
                                style: text.bodyMedium?.copyWith(
                                  color: c.textSecondary,
                                ),
                              ),
                              const SizedBox(height: 28),
                              OtpInput(
                                controller: _controller,
                                focusNode: _focusNode,
                              ),
                              const SizedBox(height: 20),
                              Wrap(
                                crossAxisAlignment: WrapCrossAlignment.center,
                                alignment: WrapAlignment.center,
                                children: [
                                  Text(
                                    "Didn't receive the code? ",
                                    style: text.bodyMedium?.copyWith(
                                      color: c.textSecondary,
                                    ),
                                  ),
                                  Semantics(
                                    identifier: 'otp_resend',
                                    child: TextButton(
                                      key: const Key('otp_resend'),
                                      onPressed: counting
                                          ? null
                                          : _startCountdown,
                                      style: TextButton.styleFrom(
                                        foregroundColor: c.link,
                                        disabledForegroundColor:
                                            c.textSecondary,
                                        padding: const EdgeInsets.symmetric(
                                          horizontal: 4,
                                        ),
                                        minimumSize: const Size(48, 40),
                                      ),
                                      child: Text(
                                        counting
                                            ? 'Resend in 0:${_secondsLeft.toString().padLeft(2, '0')}'
                                            : 'Resend',
                                      ),
                                    ),
                                  ),
                                ],
                              ),
                              const SizedBox(height: 12),
                              Semantics(
                                identifier: 'otp_status',
                                liveRegion: true,
                                child: SizedBox(
                                  height: 24,
                                  child: Text(
                                    switch (_status) {
                                      OtpStatus.none => '',
                                      OtpStatus.success => "You're in.",
                                      OtpStatus.error =>
                                        "That code didn't work. Try again.",
                                    },
                                    key: const Key('otp_status'),
                                    style: text.bodyMedium?.copyWith(
                                      color: _status == OtpStatus.success
                                          ? c.success
                                          : c.error,
                                      fontWeight: FontWeight.w600,
                                    ),
                                  ),
                                ),
                              ),
                              const Spacer(),
                              const SizedBox(height: 24),
                              Semantics(
                                identifier: 'otp_submit',
                                child: SizedBox(
                                  width: double.infinity,
                                  height: 54,
                                  child: FilledButton(
                                    key: const Key('otp_submit'),
                                    onPressed: complete ? _submit : null,
                                    style: FilledButton.styleFrom(
                                      backgroundColor: c.buttonBg,
                                      foregroundColor: c.buttonText,
                                      disabledBackgroundColor:
                                          c.buttonDisabledBg,
                                      disabledForegroundColor: c.buttonText,
                                      shape: const StadiumBorder(),
                                      textStyle: const TextStyle(
                                        fontSize: 16,
                                        fontWeight: FontWeight.w600,
                                      ),
                                    ),
                                    child: const Text('Submit'),
                                  ),
                                ),
                              ),
                            ],
                          ),
                        ),
                      ),
                    ),
                  ),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

/// The page the sheet sits over; dimmed and blurred behind it.
class _BackgroundPage extends StatelessWidget {
  const _BackgroundPage();

  @override
  Widget build(BuildContext context) {
    return ExcludeSemantics(
      child: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                'Welcome back',
                style: Theme.of(context).textTheme.headlineMedium,
              ),
              const SizedBox(height: 24),
              for (var i = 0; i < 4; i++)
                Container(
                  height: 56,
                  margin: const EdgeInsets.only(bottom: 16),
                  decoration: BoxDecoration(
                    color: Theme.of(
                      context,
                    ).colorScheme.surfaceContainerHighest,
                    borderRadius: BorderRadius.circular(12),
                  ),
                ),
            ],
          ),
        ),
      ),
    );
  }
}
