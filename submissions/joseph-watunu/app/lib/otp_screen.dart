import 'dart:async';
import 'dart:ui';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'otp_colors.dart';
import 'otp_input.dart';

class OtpScreen extends StatefulWidget {
  const OtpScreen({super.key, required this.colors, this.now = DateTime.now});

  final OtpColors colors;
  final DateTime Function() now;

  @override
  State<OtpScreen> createState() => _OtpScreenState();
}

class _OtpScreenState extends State<OtpScreen> with WidgetsBindingObserver {
  final _controller = TextEditingController();
  final _focusNode = FocusNode(debugLabel: 'OTP input');
  Timer? _timer;
  late DateTime _resendAt;
  int _secondsLeft = 30;
  String? _status;
  bool _success = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    _controller.addListener(_refresh);
    _startCountdown();
  }

  void _refresh() => setState(() {});

  void _startCountdown() {
    _timer?.cancel();
    _secondsLeft = 30;
    _resendAt = widget.now().add(const Duration(seconds: 30));
    _timer = Timer.periodic(
      const Duration(seconds: 1),
      (_) => _updateCountdown(),
    );
  }

  void _updateCountdown() {
    final remaining = (_resendAt.difference(widget.now()).inMilliseconds / 1000)
        .ceil()
        .clamp(0, 30);
    if (remaining != _secondsLeft) setState(() => _secondsLeft = remaining);
    if (remaining == 0) _timer?.cancel();
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) _updateCountdown();
  }

  void _submit() {
    if (_controller.text.length != 6) return;
    final valid = _controller.text == '123456';
    if (valid) {
      _focusNode.unfocus();
      TextInput.finishAutofillContext();
    } else {
      _controller.clear();
      _controller.selection = const TextSelection.collapsed(offset: 0);
      _focusNode.requestFocus();
    }
    setState(() {
      _success = valid;
      _status = valid ? "You're in." : "That code didn't work. Try again.";
    });
  }

  void _resend() {
    if (_secondsLeft != 0) return;
    _controller.clear();
    _controller.selection = const TextSelection.collapsed(offset: 0);
    _focusNode.requestFocus();
    setState(() {
      _status = null;
      _startCountdown();
    });
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _timer?.cancel();
    _controller.removeListener(_refresh);
    _controller.dispose();
    _focusNode.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final brightness = Theme.of(context).brightness;
    final palette = widget.colors.of(brightness);
    final keyboard = MediaQuery.viewInsetsOf(context).bottom;
    return AnnotatedRegion<SystemUiOverlayStyle>(
      value: brightness == Brightness.dark
          ? SystemUiOverlayStyle.light
          : SystemUiOverlayStyle.dark,
      child: Scaffold(
        resizeToAvoidBottomInset: false,
        backgroundColor: palette['sheet'],
        body: Stack(
          children: [
            ExcludeSemantics(
              child: ImageFiltered(
                imageFilter: ImageFilter.blur(sigmaX: 5, sigmaY: 5),
                child: Align(
                  alignment: const Alignment(0, -0.80),
                  child: Text(
                    'Verification',
                    style: TextStyle(
                      fontSize: 22,
                      color: palette['text_secondary'],
                    ),
                  ),
                ),
              ),
            ),
            Positioned.fill(child: ColoredBox(color: palette['scrim'])),
            SafeArea(
              child: LayoutBuilder(
                builder: (context, constraints) => Padding(
                  padding: EdgeInsets.fromLTRB(
                    20,
                    keyboard > 0 ? 12 : constraints.maxHeight * 0.125,
                    20,
                    keyboard + (keyboard > 0 ? 8 : 24),
                  ),
                  child: Align(
                    alignment: Alignment.bottomCenter,
                    child: ConstrainedBox(
                      constraints: const BoxConstraints(maxWidth: 480),
                      child: Semantics(
                        scopesRoute: true,
                        namesRoute: true,
                        label: 'Code verification',
                        explicitChildNodes: true,
                        child: Container(
                          key: const ValueKey('otp_sheet'),
                          width: double.infinity,
                          clipBehavior: Clip.antiAlias,
                          decoration: BoxDecoration(
                            color: palette['sheet'],
                            borderRadius: BorderRadius.circular(38),
                          ),
                          child: CustomScrollView(
                            keyboardDismissBehavior:
                                ScrollViewKeyboardDismissBehavior.manual,
                            slivers: [
                              SliverPadding(
                                padding: EdgeInsets.fromLTRB(
                                  18,
                                  constraints.maxHeight - keyboard < 560
                                      ? 24
                                      : 48,
                                  18,
                                  0,
                                ),
                                sliver: SliverToBoxAdapter(
                                  child: _content(palette, brightness),
                                ),
                              ),
                              // This footer fills remaining space on tall screens and
                              // scrolls naturally after the content on compact screens.
                              SliverFillRemaining(
                                hasScrollBody: false,
                                child: Padding(
                                  padding: EdgeInsets.fromLTRB(
                                    18,
                                    12,
                                    18,
                                    keyboard > 0 ? 24 : 56,
                                  ),
                                  child: Align(
                                    alignment: Alignment.bottomCenter,
                                    child: _submitButton(palette),
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
          ],
        ),
      ),
    );
  }

  Widget _content(OtpPalette palette, Brightness brightness) => Column(
    mainAxisSize: MainAxisSize.min,
    children: [
      Image.asset(
        brightness == Brightness.dark
            ? 'assets/thamani-logo-white.png'
            : 'assets/thamani-logo.png',
        width: 72,
        height: 72,
        fit: BoxFit.contain,
        semanticLabel: 'Thamani logo',
      ),
      const SizedBox(height: 14),
      Semantics(
        header: true,
        child: Text(
          'Enter your code',
          textAlign: TextAlign.center,
          style: TextStyle(
            fontSize: 24,
            fontWeight: FontWeight.w500,
            color: palette['text_primary'],
          ),
        ),
      ),
      const SizedBox(height: 10),
      ConstrainedBox(
        constraints: const BoxConstraints(maxWidth: 240),
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
      const SizedBox(height: 28),
      OtpInput(
        controller: _controller,
        focusNode: _focusNode,
        palette: palette,
        onChanged: () => setState(() => _status = null),
        onSubmitted: _submit,
      ),
      const SizedBox(height: 8),
      _resendLine(palette),
      Semantics(
        key: const ValueKey('otp_status'),
        identifier: 'otp_status',
        liveRegion: true,
        child: Padding(
          padding: const EdgeInsets.only(top: 12, bottom: 20),
          child: Text(
            _status ?? '',
            textAlign: TextAlign.center,
            style: TextStyle(
              fontSize: 14,
              height: 1.4,
              color: palette[_success ? 'success' : 'error'],
            ),
          ),
        ),
      ),
    ],
  );

  Widget _resendLine(OtpPalette palette) => Wrap(
    alignment: WrapAlignment.center,
    crossAxisAlignment: WrapCrossAlignment.center,
    children: [
      Text(
        "Didn't receive the code? ",
        style: TextStyle(fontSize: 13, color: palette['text_secondary']),
      ),
      MergeSemantics(
        child: Semantics(
          identifier: 'otp_resend',
          child: TextButton(
            key: const ValueKey('otp_resend'),
            onPressed: _secondsLeft == 0 ? _resend : null,
            style: TextButton.styleFrom(
              foregroundColor: palette['link'],
              disabledForegroundColor: palette['text_secondary'],
              minimumSize: const Size(48, 48),
              padding: const EdgeInsets.symmetric(horizontal: 2),
              textStyle: const TextStyle(fontSize: 13),
            ),
            child: Text(
              _secondsLeft == 0
                  ? 'Resend'
                  : 'Resend in 0:${_secondsLeft.toString().padLeft(2, '0')}',
            ),
          ),
        ),
      ),
    ],
  );

  Widget _submitButton(OtpPalette palette) => MergeSemantics(
    child: Semantics(
      identifier: 'otp_submit',
      child: SizedBox(
        width: double.infinity,
        child: FilledButton(
          key: const ValueKey('otp_submit'),
          onPressed: _controller.text.length == 6 ? _submit : null,
          style: FilledButton.styleFrom(
            backgroundColor: palette['button_bg'],
            disabledBackgroundColor: palette['button_disabled_bg'],
            foregroundColor: palette['button_text'],
            disabledForegroundColor: palette['text_secondary'],
            minimumSize: const Size.fromHeight(48),
            padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 14),
            shape: const StadiumBorder(),
            textStyle: const TextStyle(
              fontSize: 14,
              fontWeight: FontWeight.w500,
            ),
          ),
          child: const Text('Submit'),
        ),
      ),
    ),
  );
}
