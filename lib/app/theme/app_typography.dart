import 'package:flutter/material.dart';

/// AvdiBook typography scale.
///
/// Built on Inter with premium audiobook reading feel:
/// - Generous line height for readability
/// - Negative letter spacing on large display text
/// - Slightly increased tracking on small labels
class AppTypography {
  AppTypography._();

  static TextTheme textTheme(ColorScheme cs) {
    return TextTheme(
      // ─── Display ─────────────────────────────────────────────────────
      displayLarge: _style(
        size: 56,
        weight: FontWeight.w700,
        height: 1.12,
        spacing: -1.5,
        color: cs.onSurface,
      ),
      displayMedium: _style(
        size: 44,
        weight: FontWeight.w700,
        height: 1.15,
        spacing: -1.0,
        color: cs.onSurface,
      ),
      displaySmall: _style(
        size: 36,
        weight: FontWeight.w600,
        height: 1.18,
        spacing: -0.8,
        color: cs.onSurface,
      ),

      // ─── Headline ─────────────────────────────────────────────────────
      headlineLarge: _style(
        size: 32,
        weight: FontWeight.w700,
        height: 1.22,
        spacing: -0.5,
        color: cs.onSurface,
      ),
      headlineMedium: _style(
        size: 28,
        weight: FontWeight.w600,
        height: 1.25,
        spacing: -0.3,
        color: cs.onSurface,
      ),
      headlineSmall: _style(
        size: 24,
        weight: FontWeight.w600,
        height: 1.28,
        spacing: -0.2,
        color: cs.onSurface,
      ),

      // ─── Title ────────────────────────────────────────────────────────
      titleLarge: _style(
        size: 20,
        weight: FontWeight.w600,
        height: 1.3,
        spacing: -0.1,
        color: cs.onSurface,
      ),
      titleMedium: _style(
        size: 16,
        weight: FontWeight.w600,
        height: 1.35,
        spacing: 0.0,
        color: cs.onSurface,
      ),
      titleSmall: _style(
        size: 14,
        weight: FontWeight.w500,
        height: 1.4,
        spacing: 0.1,
        color: cs.onSurface,
      ),

      // ─── Body ─────────────────────────────────────────────────────────
      bodyLarge: _style(
        size: 16,
        weight: FontWeight.w400,
        height: 1.6,
        spacing: 0.1,
        color: cs.onSurface,
      ),
      bodyMedium: _style(
        size: 14,
        weight: FontWeight.w400,
        height: 1.55,
        spacing: 0.1,
        color: cs.onSurface,
      ),
      bodySmall: _style(
        size: 12,
        weight: FontWeight.w400,
        height: 1.5,
        spacing: 0.2,
        color: cs.onSurface.withValues(alpha: 0.72),
      ),

      // ─── Label ────────────────────────────────────────────────────────
      labelLarge: _style(
        size: 14,
        weight: FontWeight.w600,
        height: 1.4,
        spacing: 0.3,
        color: cs.onSurface,
      ),
      labelMedium: _style(
        size: 12,
        weight: FontWeight.w500,
        height: 1.4,
        spacing: 0.4,
        color: cs.onSurface,
      ),
      labelSmall: _style(
        size: 10,
        weight: FontWeight.w500,
        height: 1.4,
        spacing: 0.6,
        color: cs.onSurface.withValues(alpha: 0.72),
      ),
    );
  }

  static TextStyle _style({
    required double size,
    required FontWeight weight,
    required double height,
    required double spacing,
    required Color color,
  }) {
    return TextStyle(
      fontFamily: 'Inter',
      fontSize: size,
      fontWeight: weight,
      height: height,
      letterSpacing: spacing,
      color: color,
    );
  }

  // ─── Utility styles ──────────────────────────────────────────────────────

  /// Chapter title shown in the full player
  static TextStyle playerChapterTitle(ColorScheme cs) => _style(
        size: 13,
        weight: FontWeight.w500,
        height: 1.4,
        spacing: 0.3,
        color: cs.onSurface.withValues(alpha: 0.7),
      );

  /// Book title in the full player (large)
  static TextStyle playerBookTitle(ColorScheme cs) => _style(
        size: 22,
        weight: FontWeight.w700,
        height: 1.25,
        spacing: -0.3,
        color: cs.onSurface,
      );

  /// Author / narrator line
  static TextStyle playerAuthor(ColorScheme cs) => _style(
        size: 14,
        weight: FontWeight.w400,
        height: 1.4,
        spacing: 0.1,
        color: cs.onSurface.withValues(alpha: 0.65),
      );

  /// Timestamp labels (current pos, remaining)
  static TextStyle timestamp(ColorScheme cs) => _style(
        size: 12,
        weight: FontWeight.w500,
        height: 1.0,
        spacing: 0.3,
        color: cs.onSurface.withValues(alpha: 0.6),
      );

  /// Section header in home / library
  static TextStyle sectionHeader(ColorScheme cs) => _style(
        size: 18,
        weight: FontWeight.w700,
        height: 1.25,
        spacing: -0.2,
        color: cs.onSurface,
      );

  /// Greeting headline
  static TextStyle greeting(ColorScheme cs) => _style(
        size: 26,
        weight: FontWeight.w700,
        height: 1.2,
        spacing: -0.4,
        color: cs.onSurface,
      );
}
