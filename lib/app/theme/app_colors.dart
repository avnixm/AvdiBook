import 'package:flutter/material.dart';

/// AvdiBook design system palette.
///
/// Calming, premium audiobook aesthetic:
/// - Soft warm off-white surfaces
/// - Deep blue primary accent
/// - Warm amber secondary for highlights
/// - Muted slate for supporting text
class AppColors {
  AppColors._();

  // ─── Brand ──────────────────────────────────────────────────────────────

  /// Primary seed — calm deep blue
  static const Color primary = Color(0xFF1E5FBF);

  /// Secondary — warm amber for chapter progress, highlights
  static const Color secondary = Color(0xFFBF7E1E);

  /// Tertiary — muted teal for narrator / metadata accents
  static const Color tertiary = Color(0xFF1E9B8A);

  // ─── Semantic ─────────────────────────────────────────────────────────

  static const Color success = Color(0xFF2D9B61);
  static const Color warning = Color(0xFFE5A020);
  static const Color error = Color(0xFFD63B3B);

  // ─── Light surfaces ──────────────────────────────────────────────────

  /// Warm cream background — like soft paper
  static const Color backgroundLight = Color(0xFFF7F4EF);

  /// Slightly elevated surface — cards, list tiles
  static const Color surfaceLight = Color(0xFFFFFCF8);

  /// Card color
  static const Color cardLight = Color(0xFFF0EDE8);

  /// Bottom nav bar
  static const Color navBarLight = Color(0xFFEFECE7);

  /// Bottom sheet / modal
  static const Color sheetLight = Color(0xFFFFFCF8);

  /// On-surface in light mode (primary text)
  static const Color onSurfaceLight = Color(0xFF1A1A1A);

  /// Muted secondary text in light mode
  static const Color subtleLight = Color(0xFF7A7A7A);

  // ─── Dark surfaces ────────────────────────────────────────────────────

  /// Deep slate background
  static const Color backgroundDark = Color(0xFF111318);

  /// Elevated surface — cards, list tiles
  static const Color surfaceDark = Color(0xFF1C1F26);

  /// Card color in dark mode
  static const Color cardDark = Color(0xFF22262E);

  /// Bottom nav bar in dark mode
  static const Color navBarDark = Color(0xFF181B22);

  /// Bottom sheet / modal in dark mode
  static const Color sheetDark = Color(0xFF1E2129);

  /// On-surface in dark mode (primary text)
  static const Color onSurfaceDark = Color(0xFFF0EDE8);

  /// Muted secondary text in dark mode
  static const Color subtleDark = Color(0xFF9A9DA8);

  // ─── Cover / Player tints ────────────────────────────────────────────

  /// Overlay gradient on cover art (bottom scrim)
  static const Color coverScrimDark =
      Color(0xCC000000); // 80% black

  static const Color coverScrimLight =
      Color(0x66000000); // 40% black

  // ─── Player specific ─────────────────────────────────────────────────

  /// Progress track fill
  static const Color progressTrack = Color(0xFF1E5FBF);

  /// Progress track background
  static const Color progressTrackBg = Color(0x301E5FBF);

  // ─── Semantic cover placeholder gradient ─────────────────────────────

  static const List<Color> placeholderGradient = [
    Color(0xFF2D3561),
    Color(0xFF1A1F3A),
  ];

  // ─── Convenience getters ─────────────────────────────────────────────

  /// Returns subtle text color for current brightness
  static Color subtle(BuildContext context) {
    return Theme.of(context).brightness == Brightness.light
        ? subtleLight
        : subtleDark;
  }

  /// Returns card background for current brightness
  static Color card(BuildContext context) {
    return Theme.of(context).brightness == Brightness.light
        ? cardLight
        : cardDark;
  }
}
