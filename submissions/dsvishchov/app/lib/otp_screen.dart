import 'dart:math' as math;
import 'dart:ui' show ImageFilter;
import 'package:flutter/material.dart';
import 'otp_controller.dart';
import 'otp_input.dart';
import 'otp_palette.dart';
import 'resend_cooldown.dart';

class OtpScreen extends StatefulWidget {
  const OtpScreen({super.key, required this.palette, this.now});
  final OtpPalette palette;
  final DateTime Function()? now;
  @override
  State<OtpScreen> createState() => _OtpScreenState();
}

class _OtpScreenState extends State<OtpScreen> with WidgetsBindingObserver {
  final _otp = OtpController();
  late final ResendCooldown _cooldown;
  String _lastCode = '';
  String _status = '';
  bool _success = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _cooldown = ResendCooldown(now: widget.now)..addListener(_tick);
    _otp.addListener(_edited);
  }

  void _tick() {
    if (mounted) setState(() {});
  }

  void _edited() => setState(() {
    if (_lastCode != _otp.code) _status = '';
    _lastCode = _otp.code;
  });

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) _cooldown.refresh();
  }

  void _submit() {
    if (!_otp.isComplete) return;
    final success = _otp.code == '123456';
    if (!success) _otp.clear();
    setState(() {
      _success = success;
      _status = success ? "You're in." : "That code didn't work. Try again.";
    });
    if (success) FocusScope.of(context).unfocus();
  }

  void _resend() {
    if (_cooldown.canResend) _cooldown.restart();
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _otp.removeListener(_edited);
    _otp.dispose();
    _cooldown.removeListener(_tick);
    _cooldown.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final palette = widget.palette;
    final size = MediaQuery.sizeOf(context);
    final dark = Theme.of(context).brightness == Brightness.dark;
    final narrow = size.width < 360;
    return Scaffold(
      resizeToAvoidBottomInset: false,
      body: Stack(
        fit: StackFit.expand,
        children: [
          ExcludeSemantics(
            child: IgnorePointer(
              child: Stack(
                fit: StackFit.expand,
                children: [
                  ColoredBox(
                    color: dark
                        ? const Color(0xFF19231D)
                        : const Color(0xFFF4F6F5),
                  ),
                  ImageFiltered(
                    imageFilter: ImageFilter.blur(sigmaX: 4, sigmaY: 4),
                    child: Align(
                      alignment: const Alignment(0, -.78),
                      child: Text(
                        'Welcome back',
                        style: TextStyle(
                          fontSize: 24,
                          fontWeight: FontWeight.w400,
                          color: palette['text_secondary'],
                        ),
                      ),
                    ),
                  ),
                  ColoredBox(color: palette['scrim']),
                ],
              ),
            ),
          ),
          SafeArea(
            minimum: EdgeInsets.fromLTRB(
              narrow ? 8 : 24,
              12,
              narrow ? 8 : 24,
              32,
            ),
            child: Padding(
              padding: EdgeInsets.only(
                bottom: MediaQuery.viewInsetsOf(context).bottom,
              ),
              child: LayoutBuilder(
                builder: (context, constraints) => Align(
                  alignment: Alignment.bottomCenter,
                  child: SizedBox(
                    width: math.min(560, constraints.maxWidth),
                    height: math.min(size.height * .82, constraints.maxHeight),
                    child: Material(
                      color: palette['sheet'],
                      borderRadius: BorderRadius.circular(36),
                      clipBehavior: Clip.antiAlias,
                      child: LayoutBuilder(
                        builder: (context, sheet) => SingleChildScrollView(
                          child: ConstrainedBox(
                            constraints: BoxConstraints(
                              minHeight: sheet.maxHeight,
                            ),
                            child: IntrinsicHeight(
                              child: Padding(
                                padding: EdgeInsets.fromLTRB(
                                  narrow ? 8 : 20,
                                  40,
                                  narrow ? 8 : 20,
                                  60,
                                ),
                                child: Column(
                                  crossAxisAlignment:
                                      CrossAxisAlignment.stretch,
                                  children: [
                                    Image.asset(
                                      dark
                                          ? 'assets/thamani-logo-white.png'
                                          : 'assets/thamani-logo.png',
                                      width: 76,
                                      height: 76,
                                      excludeFromSemantics: true,
                                    ),
                                    const SizedBox(height: 18),
                                    Semantics(
                                      header: true,
                                      child: Text(
                                        'Enter your code',
                                        textAlign: TextAlign.center,
                                        style: TextStyle(
                                          fontSize: 26,
                                          fontWeight: FontWeight.w400,
                                          color: palette['text_primary'],
                                        ),
                                      ),
                                    ),
                                    const SizedBox(height: 10),
                                    Padding(
                                      padding: const EdgeInsets.symmetric(
                                        horizontal: 28,
                                      ),
                                      child: Text(
                                        'A 6-digit code has been sent to your phone.',
                                        textAlign: TextAlign.center,
                                        style: TextStyle(
                                          fontSize: 14,
                                          height: 1.4,
                                          color: palette['text_primary'],
                                        ),
                                      ),
                                    ),
                                    const SizedBox(height: 32),
                                    OtpInput(
                                      controller: _otp,
                                      palette: palette,
                                      availableWidth:
                                          sheet.maxWidth - (narrow ? 16 : 40),
                                    ),
                                    const SizedBox(height: 8),
                                    Wrap(
                                      alignment: WrapAlignment.center,
                                      crossAxisAlignment:
                                          WrapCrossAlignment.center,
                                      children: [
                                        Text(
                                          "Didn't receive the code?",
                                          style: TextStyle(
                                            fontSize: 13,
                                            color: palette['text_secondary'],
                                          ),
                                        ),
                                        Semantics(
                                          identifier: 'otp_resend',
                                          liveRegion: _cooldown.canResend,
                                          child: TextButton(
                                            key: const ValueKey('otp_resend'),
                                            onPressed: _cooldown.canResend
                                                ? _resend
                                                : null,
                                            style: TextButton.styleFrom(
                                              foregroundColor: palette['link'],
                                              disabledForegroundColor:
                                                  palette['text_secondary'],
                                              padding:
                                                  const EdgeInsets.symmetric(
                                                    horizontal: 4,
                                                  ),
                                              minimumSize: const Size(48, 48),
                                              textStyle: const TextStyle(
                                                fontFamily: 'Roboto',
                                                fontSize: 13,
                                              ),
                                            ),
                                            child: Text(
                                              _cooldown.canResend
                                                  ? 'Resend'
                                                  : 'Resend in 0:${_cooldown.seconds.toString().padLeft(2, '0')}',
                                            ),
                                          ),
                                        ),
                                      ],
                                    ),
                                    Semantics(
                                      key: const ValueKey('otp_status'),
                                      identifier: 'otp_status',
                                      liveRegion: true,
                                      child: Text(
                                        _status,
                                        textAlign: TextAlign.center,
                                        style: TextStyle(
                                          fontSize: 14,
                                          height: 1.4,
                                          color:
                                              palette[_success
                                                  ? 'success'
                                                  : 'error'],
                                        ),
                                      ),
                                    ),
                                    const Spacer(),
                                    const SizedBox(height: 24),
                                    Semantics(
                                      identifier: 'otp_submit',
                                      child: FilledButton(
                                        key: const ValueKey('otp_submit'),
                                        onPressed: _otp.isComplete
                                            ? _submit
                                            : null,
                                        style: FilledButton.styleFrom(
                                          minimumSize: const Size(
                                            double.infinity,
                                            52,
                                          ),
                                          padding: const EdgeInsets.symmetric(
                                            horizontal: 24,
                                            vertical: 14,
                                          ),
                                          backgroundColor: palette['button_bg'],
                                          foregroundColor:
                                              palette['button_text'],
                                          disabledBackgroundColor:
                                              palette['button_disabled_bg'],
                                          disabledForegroundColor:
                                              palette['button_text'],
                                          textStyle: const TextStyle(
                                            fontFamily: 'Roboto',
                                            fontSize: 16,
                                          ),
                                          shape: const StadiumBorder(),
                                        ),
                                        child: const Text('Submit'),
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
              ),
            ),
          ),
        ],
      ),
    );
  }
}
