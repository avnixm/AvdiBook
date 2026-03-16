/// Formats Dart [Duration] values into human-readable strings
/// for the audiobook player UI.
library;

class DurationFormatter {
  DurationFormatter._();

  /// `01:23:45` for long durations, `23:45` for under an hour.
  static String format(Duration duration) {
    final h = duration.inHours;
    final m = duration.inMinutes.remainder(60);
    final s = duration.inSeconds.remainder(60);

    if (h > 0) {
      return '${_pad(h)}:${_pad(m)}:${_pad(s)}';
    }
    return '${_pad(m)}:${_pad(s)}';
  }

  /// `1h 23m` — used for total book duration, time remaining.
  static String formatHuman(Duration duration) {
    final h = duration.inHours;
    final m = duration.inMinutes.remainder(60);
    final s = duration.inSeconds.remainder(60);

    if (h > 0 && m > 0) return '${h}h ${m}m';
    if (h > 0) return '${h}h';
    if (m > 0 && s > 0) return '${m}m ${s}s';
    if (m > 0) return '${m}m';
    return '${s}s';
  }

  /// `+30m remaining` — used for chapter time left.
  static String formatRemaining(Duration remaining) {
    if (remaining <= Duration.zero) return 'Done';
    return '${formatHuman(remaining)} left';
  }

  /// Percentage string `42%` for progress indicators.
  static String formatPercent(double fraction) {
    final pct = (fraction.clamp(0.0, 1.0) * 100).round();
    return '$pct%';
  }

  /// Parses a `HH:MM:SS` or `MM:SS` string back into a [Duration].
  /// Returns [Duration.zero] on parse failure.
  static Duration parse(String s) {
    try {
      final parts = s.split(':').map(int.parse).toList();
      if (parts.length == 3) {
        return Duration(
          hours: parts[0],
          minutes: parts[1],
          seconds: parts[2],
        );
      }
      if (parts.length == 2) {
        return Duration(minutes: parts[0], seconds: parts[1]);
      }
    } catch (_) {
      // intentionally ignored
    }
    return Duration.zero;
  }

  static String _pad(int n) => n.toString().padLeft(2, '0');
}
