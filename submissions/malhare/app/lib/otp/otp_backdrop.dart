import 'package:flutter/material.dart';

import 'palette.dart';

/// Branded backdrop behind the dimmed sheet.
class OtpBackdrop extends StatelessWidget {
  const OtpBackdrop({super.key, required this.brand});

  final BrandColors brand;

  @override
  Widget build(BuildContext context) {
    return DecoratedBox(
      decoration: BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [brand.fogInk, brand.thamaniBlue, brand.signalBlue],
          stops: const [0.0, 0.62, 1.0],
        ),
      ),
      child: Stack(
        fit: StackFit.expand,
        children: [
          Positioned(
            top: -90,
            right: -70,
            child: _Glow(color: brand.clearCyan, size: 280),
          ),
          Positioned(
            bottom: 120,
            left: -90,
            child: _Glow(color: brand.acidLemon, size: 260),
          ),
          Positioned(
            top: 88,
            left: 28,
            right: 28,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Container(
                  height: 14,
                  width: 120,
                  decoration: BoxDecoration(
                    color: Colors.white.withValues(alpha: 0.35),
                    borderRadius: BorderRadius.circular(7),
                  ),
                ),
                const SizedBox(height: 18),
                for (var i = 0; i < 3; i++) ...[
                  Container(
                    height: 64,
                    decoration: BoxDecoration(
                      color: Colors.white.withValues(alpha: 0.10),
                      borderRadius: BorderRadius.circular(18),
                      border: Border.all(
                        color: Colors.white.withValues(alpha: 0.08),
                      ),
                    ),
                  ),
                  const SizedBox(height: 12),
                ],
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _Glow extends StatelessWidget {
  const _Glow({required this.color, required this.size});

  final Color color;
  final double size;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: size,
      height: size,
      decoration: BoxDecoration(
        shape: BoxShape.circle,
        gradient: RadialGradient(
          colors: [
            color.withValues(alpha: 0.5),
            color.withValues(alpha: 0),
          ],
        ),
      ),
    );
  }
}
