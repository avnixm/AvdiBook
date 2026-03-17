import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import 'package:avdibook/app/theme/app_spacing.dart';
import 'package:avdibook/core/constants/app_constants.dart';
import 'package:avdibook/core/widgets/expressive_bounce.dart';
import 'package:avdibook/features/player/data/services/audio_fx_service.dart';
import 'package:avdibook/features/player/presentation/providers/cast_session_provider.dart';
import 'package:avdibook/features/setup/presentation/providers/setup_controller.dart';
import 'package:avdibook/shared/providers/app_state_provider.dart';

class SettingsScreen extends ConsumerStatefulWidget {
  const SettingsScreen({super.key});

  @override
  ConsumerState<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends ConsumerState<SettingsScreen>
  with SingleTickerProviderStateMixin {
  static const _skipOptions = [5, 10, 15, 20, 30, 45, 60];
  static const _smartRewindOptions = [0, 3, 5, 7, 10, 15, 20];
  static const _themeLabels = ['System', 'Light', 'Dark'];
  final AudioFxService _audioFxService = AudioFxService();
  late final Future<AudioFxCapabilities> _capabilitiesFuture;
  late final AnimationController _entranceController;

  @override
  void initState() {
    super.initState();
    _capabilitiesFuture = _audioFxService.getCapabilities();
    _entranceController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 700),
    )..forward();
  }

  @override
  void dispose() {
    _entranceController.dispose();
    super.dispose();
  }

  Future<void> _pickOption<T>({
    required String title,
    required List<T> options,
    required T current,
    required String Function(T) label,
    required Future<void> Function(T) onSelect,
  }) async {
    final selected = await showDialog<T>(
      context: context,
      builder: (ctx) => SimpleDialog(
        title: Text(title),
        children: options.map((opt) {
          final isSelected = opt == current;
          return SimpleDialogOption(
            onPressed: () => Navigator.of(ctx).pop(opt),
            child: Row(
              children: [
                Expanded(child: Text(label(opt))),
                if (isSelected)
                  Icon(
                    Icons.check_rounded,
                    size: 18,
                    color: Theme.of(ctx).colorScheme.primary,
                  ),
              ],
            ),
          );
        }).toList(),
      ),
    );
    if (selected != null && selected != current) {
      await onSelect(selected);
    }
  }

  @override
  Widget build(BuildContext context) {
    final skipFwd = ref.watch(skipForwardSecsProvider);
    final skipBwd = ref.watch(skipBackwardSecsProvider);
    final themeMode = ref.watch(themeModeProvider);
    final speed = ref.watch(globalPlaybackSpeedProvider);
    final trimSilence = ref.watch(trimSilenceProvider);
    final preservePitch = ref.watch(preservePitchProvider);
    final pitch = ref.watch(playbackPitchProvider);
    final smartRewindSecs = ref.watch(smartRewindSecsProvider);
    final volumeBoost = ref.watch(volumeBoostProvider);
    final stereoBalance = ref.watch(stereoBalanceProvider);
    final equalizerEnabled = ref.watch(equalizerEnabledProvider);
    final equalizerPreset = ref.watch(equalizerPresetProvider);
    final reducedMotion = ref.watch(reducedMotionProvider);
    final castState = ref.watch(castSessionProvider);
    final savedFolder = ref.watch(scanFolderPathProvider);
    final setupState = ref.watch(setupControllerProvider);
    final isBusy = setupState.isBusy;
    final useSplitLayout =
        MediaQuery.sizeOf(context).width >= AppSpacing.mediumMaxWidth;

    final playbackSection = _SettingsSection(
      title: 'Playback',
      children: [
        _SettingsTile(
          icon: Icons.speed_rounded,
          label: 'Default Speed',
          value: '${speed % 1 == 0 ? speed.toInt() : speed}×',
          onTap: isBusy
              ? null
              : () => _pickOption<double>(
                  title: 'Default Playback Speed',
                  options: AppDefaults.speedOptions,
                  current: speed,
                  label: (v) => '${v % 1 == 0 ? v.toInt() : v}×',
                  onSelect: (v) =>
                      ref.read(globalPlaybackSpeedProvider.notifier).set(v),
                ),
        ),
        _SettingsTile(
          icon: Icons.fast_forward_rounded,
          label: 'Skip Forward',
          value: '${skipFwd}s',
          onTap: isBusy
              ? null
              : () => _pickOption<int>(
                  title: 'Skip Forward Duration',
                  options: _skipOptions,
                  current: skipFwd,
                  label: (v) => '${v}s',
                  onSelect: (v) =>
                      ref.read(skipForwardSecsProvider.notifier).set(v),
                ),
        ),
        _SettingsTile(
          icon: Icons.fast_rewind_rounded,
          label: 'Skip Backward',
          value: '${skipBwd}s',
          onTap: isBusy
              ? null
              : () => _pickOption<int>(
                  title: 'Skip Backward Duration',
                  options: _skipOptions,
                  current: skipBwd,
                  label: (v) => '${v}s',
                  onSelect: (v) =>
                      ref.read(skipBackwardSecsProvider.notifier).set(v),
                ),
        ),
        _SettingsTile(
          icon: Icons.replay_10_rounded,
          label: 'Smart Rewind',
          value: '${smartRewindSecs}s',
          subtitle: 'When resuming after a pause',
          onTap: isBusy
              ? null
              : () => _pickOption<int>(
                  title: 'Smart Rewind',
                  options: _smartRewindOptions,
                  current: smartRewindSecs,
                  label: (v) => v == 0 ? 'Off' : '${v}s',
                  onSelect: (v) =>
                      ref.read(smartRewindSecsProvider.notifier).set(v),
                ),
        ),
        SwitchListTile.adaptive(
          value: trimSilence,
          onChanged: isBusy
              ? null
              : (value) => ref.read(trimSilenceProvider.notifier).set(value),
          title: const Text('Trim silence'),
          subtitle: const Text('Auto-skip silent gaps for faster listening.'),
          secondary: const Icon(Icons.graphic_eq_rounded),
        ),
        SwitchListTile.adaptive(
          value: preservePitch,
          onChanged: isBusy
              ? null
              : (value) => ref.read(preservePitchProvider.notifier).set(value),
          title: const Text('Preserve pitch'),
          subtitle: const Text('Keep voice tone natural at different speeds.'),
          secondary: const Icon(Icons.tune_rounded),
        ),
        _SettingsTile(
          icon: Icons.multitrack_audio_rounded,
          label: 'Voice Pitch',
          value: '${pitch.toStringAsFixed(1)}×',
          subtitle: preservePitch
              ? 'Used while preserve pitch is on'
              : 'Enable preserve pitch to customize',
          onTap: isBusy
              ? null
              : () => _pickOption<double>(
                  title: 'Voice Pitch',
                  options: AppDefaults.pitchOptions,
                  current: pitch,
                  label: (v) => '${v.toStringAsFixed(1)}×',
                  onSelect: (v) =>
                      ref.read(playbackPitchProvider.notifier).set(v),
                ),
        ),
      ],
    );

    final appearanceSection = _SettingsSection(
      title: 'Appearance',
      children: [
        ListTile(
          leading: const Icon(Icons.dark_mode_rounded),
          title: const Text('Theme'),
          subtitle: Text(_themeLabels[themeMode.clamp(0, 2)]),
        ),
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 0, 16, 12),
          child: SegmentedButton<int>(
            segments: const [
              ButtonSegment<int>(value: 0, label: Text('System')),
              ButtonSegment<int>(value: 1, label: Text('Light')),
              ButtonSegment<int>(value: 2, label: Text('Dark')),
            ],
            selected: {themeMode.clamp(0, 2)},
            onSelectionChanged: isBusy
                ? null
                : (selection) {
                    final mode = selection.first;
                    ref.read(themeModeProvider.notifier).setMode(mode);
                  },
            showSelectedIcon: false,
          ),
        ),
        SwitchListTile.adaptive(
          value: reducedMotion,
          onChanged: isBusy
              ? null
              : (value) => ref.read(reducedMotionProvider.notifier).set(value),
          title: const Text('Reduced motion'),
          subtitle: const Text('Use fewer movement animations across the app.'),
          secondary: const Icon(Icons.motion_photos_off_rounded),
        ),
      ],
    );

    final audioEffectsSection = _SettingsSection(
      title: 'Audio Effects',
      children: [
        FutureBuilder<AudioFxCapabilities>(
          future: _capabilitiesFuture,
          builder: (context, snapshot) {
            final capabilities =
                snapshot.data ?? AudioFxCapabilities.unsupported;
            return Column(
              children: [
                ListTile(
                  leading: const Icon(Icons.volume_up_rounded),
                  title: const Text('Volume boost'),
                  subtitle: Text(
                    capabilities.loudnessSupported
                        ? '${(volumeBoost * 100).round()}%'
                        : 'Not supported on this device',
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 16),
                  child: Slider(
                    value: volumeBoost,
                    onChanged: isBusy || !capabilities.loudnessSupported
                        ? null
                        : (value) =>
                              ref.read(volumeBoostProvider.notifier).set(value),
                    min: 0,
                    max: 1,
                    divisions: 10,
                    label: '${(volumeBoost * 100).round()}%',
                  ),
                ),
                ListTile(
                  leading: const Icon(Icons.surround_sound_rounded),
                  title: const Text('Stereo balance'),
                  subtitle: Text(
                    capabilities.stereoBalanceSupported
                        ? (stereoBalance == 0
                              ? 'Centered'
                              : stereoBalance < 0
                              ? 'Left ${(stereoBalance.abs() * 100).round()}%'
                              : 'Right ${(stereoBalance * 100).round()}%')
                        : 'Not supported by current playback engine',
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 16),
                  child: Slider(
                    value: stereoBalance,
                    onChanged: isBusy || !capabilities.stereoBalanceSupported
                        ? null
                        : (value) => ref
                              .read(stereoBalanceProvider.notifier)
                              .set(value),
                    min: -1,
                    max: 1,
                    divisions: 20,
                  ),
                ),
                const ListTile(
                  leading: Icon(Icons.equalizer_rounded),
                  title: Text('Equalizer'),
                ),
                SwitchListTile.adaptive(
                  value: equalizerEnabled,
                  onChanged: isBusy || !capabilities.equalizerSupported
                      ? null
                      : (value) => ref
                            .read(equalizerEnabledProvider.notifier)
                            .set(value),
                  title: const Text('Enable equalizer'),
                  subtitle: Text(
                    capabilities.equalizerSupported
                        ? 'Android native EQ presets'
                        : 'Equalizer not available on this device',
                  ),
                  secondary: const Icon(Icons.graphic_eq_rounded),
                ),
                _SettingsTile(
                  icon: Icons.library_music_rounded,
                  label: 'Equalizer preset',
                  value: 'Preset #$equalizerPreset',
                  onTap: isBusy || !capabilities.equalizerSupported
                      ? null
                      : () async {
                          final presets = await _audioFxService
                              .getEqualizerPresets();
                          if (!mounted || presets.isEmpty) return;
                          final options = <int>[];
                          for (var i = 0; i < presets.length; i++) {
                            options.add(i);
                          }
                          await _pickOption<int>(
                            title: 'Equalizer preset',
                            options: options,
                            current: equalizerPreset.clamp(
                              0,
                              presets.length - 1,
                            ),
                            label: (v) => presets[v],
                            onSelect: (v) => ref
                                .read(equalizerPresetProvider.notifier)
                                .set(v),
                          );
                        },
                ),
              ],
            );
          },
        ),
      ],
    );

    final castSection = _SettingsSection(
      title: 'Cast',
      children: [
        _SettingsTile(
          icon: castState.connectedDevice == null
              ? Icons.cast_rounded
              : Icons.cast_connected_rounded,
          label: castState.connectedDevice == null ? 'Chromecast' : 'Connected',
          subtitle:
              castState.connectedDevice?.name ??
              (castState.devices.isEmpty
                  ? 'No devices found'
                  : '${castState.devices.length} device(s) available'),
          onTap: isBusy
              ? null
              : () async {
                  final notifier = ref.read(castSessionProvider.notifier);
                  await notifier.refreshDevices();
                  if (!mounted) return;

                  final latest = ref.read(castSessionProvider);
                  if (latest.connectedDevice != null) {
                    final disconnect = await showDialog<bool>(
                      context: context,
                      builder: (ctx) => AlertDialog(
                        title: const Text('Disconnect Chromecast'),
                        content: Text(
                          'Disconnect from ${latest.connectedDevice!.name}?',
                        ),
                        actions: [
                          TextButton(
                            onPressed: () => Navigator.of(ctx).pop(false),
                            child: const Text('Cancel'),
                          ),
                          FilledButton(
                            onPressed: () => Navigator.of(ctx).pop(true),
                            child: const Text('Disconnect'),
                          ),
                        ],
                      ),
                    );
                    if (disconnect == true) {
                      await notifier.disconnect();
                    }
                    return;
                  }

                  if (latest.devices.isEmpty) {
                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(
                        content: Text('No Chromecast devices discovered yet.'),
                      ),
                    );
                    return;
                  }

                  final selected = await showDialog<String>(
                    context: context,
                    builder: (ctx) => SimpleDialog(
                      title: const Text('Cast to device'),
                      children: [
                        for (final device in latest.devices)
                          SimpleDialogOption(
                            onPressed: () => Navigator.of(ctx).pop(device.id),
                            child: Text(device.name),
                          ),
                      ],
                    ),
                  );

                  if (selected == null) return;
                  final match = latest.devices.firstWhere(
                    (d) => d.id == selected,
                  );
                  await notifier.connect(match);
                },
        ),
        if (castState.isDiscovering)
          const Padding(
            padding: EdgeInsets.fromLTRB(16, 0, 16, 12),
            child: LinearProgressIndicator(),
          ),
        if (castState.error != null)
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 0, 16, 12),
            child: Text(
              castState.error!,
              style: Theme.of(context).textTheme.bodySmall?.copyWith(
                color: Theme.of(context).colorScheme.error,
              ),
            ),
          ),
      ],
    );

    final librarySection = _SettingsSection(
      title: 'Library',
      children: [
        _SettingsTile(
          icon: Icons.folder_rounded,
          label: 'Add Folder',
          subtitle: savedFolder != null ? savedFolder.split('/').last : null,
          onTap: isBusy
              ? null
              : () => ref
                    .read(setupControllerProvider.notifier)
                    .importDirectory(),
        ),
        _SettingsTile(
          icon: Icons.refresh_rounded,
          label: 'Rescan Library',
          subtitle: savedFolder == null ? 'No folder selected yet' : null,
          onTap: isBusy
              ? null
              : () async {
                  final controller = ref.read(setupControllerProvider.notifier);
                  final done = await controller.rescanLibrary();
                  if (!done && mounted) {
                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(
                        content: Text(
                          'No folder saved. Use "Add Folder" first.',
                        ),
                      ),
                    );
                  } else if (done && mounted) {
                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(content: Text('Library rescanned.')),
                    );
                  }
                },
        ),
      ],
    );

    final aboutSection = _SettingsSection(
      title: 'About',
      children: [
        _SettingsTile(
          icon: Icons.info_outline_rounded,
          label: 'About AvdiBook',
          value: '0.1.0',
          onTap: () => context.push(AppRoutes.about),
        ),
      ],
    );

    return Scaffold(
      appBar: AppBar(
        title: const Text('Settings'),
        elevation: 0,
      ),
      backgroundColor: Theme.of(context).scaffoldBackgroundColor,
      body: CustomScrollView(
        slivers: [
          SliverPadding(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            sliver: SliverToBoxAdapter(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  _reveal(
                    index: 0,
                    reduceMotion: reducedMotion,
                    child: _SettingsComponentBar(
                      isBusy: isBusy,
                      hasSavedFolder: savedFolder != null,
                      onRescan: () async {
                        if (isBusy) return;
                        final controller = ref.read(setupControllerProvider.notifier);
                        await controller.rescanLibrary();
                      },
                      onAddFolder: () {
                        if (isBusy) return;
                        ref.read(setupControllerProvider.notifier).importDirectory();
                      },
                      onOpenAbout: () => context.push(AppRoutes.about),
                    ),
                  ),
                  const SizedBox(height: 16),
                  if (useSplitLayout)
                    _SettingsSplitColumns(
                      leftChildren: [
                        _reveal(
                          index: 0,
                          reduceMotion: reducedMotion,
                          child: playbackSection,
                        ),
                        _reveal(
                          index: 1,
                          reduceMotion: reducedMotion,
                          child: audioEffectsSection,
                        ),
                        _reveal(
                          index: 2,
                          reduceMotion: reducedMotion,
                          child: librarySection,
                        ),
                      ],
                      rightChildren: [
                        _reveal(
                          index: 1,
                          reduceMotion: reducedMotion,
                          child: appearanceSection,
                        ),
                        _reveal(
                          index: 2,
                          reduceMotion: reducedMotion,
                          child: castSection,
                        ),
                        _reveal(
                          index: 3,
                          reduceMotion: reducedMotion,
                          child: aboutSection,
                        ),
                      ],
                    )
                  else ...[
                    _reveal(
                      index: 0,
                      reduceMotion: reducedMotion,
                      child: playbackSection,
                    ),
                    const SizedBox(height: 16),
                    _reveal(
                      index: 1,
                      reduceMotion: reducedMotion,
                      child: appearanceSection,
                    ),
                    const SizedBox(height: 16),
                    _reveal(
                      index: 2,
                      reduceMotion: reducedMotion,
                      child: audioEffectsSection,
                    ),
                    const SizedBox(height: 16),
                    _reveal(
                      index: 3,
                      reduceMotion: reducedMotion,
                      child: castSection,
                    ),
                    const SizedBox(height: 16),
                    _reveal(
                      index: 4,
                      reduceMotion: reducedMotion,
                      child: librarySection,
                    ),
                    const SizedBox(height: 16),
                    _reveal(
                      index: 5,
                      reduceMotion: reducedMotion,
                      child: aboutSection,
                    ),
                  ],
                  if (setupState.errorMessage != null) ...[
                    const SizedBox(height: 12),
                    Padding(
                      padding: const EdgeInsets.symmetric(horizontal: 4),
                      child: Text(
                        setupState.errorMessage!,
                        style: Theme.of(context).textTheme.bodySmall?.copyWith(
                          color: Theme.of(context).colorScheme.error,
                        ),
                      ),
                    ),
                  ],
                  if (isBusy) ...[
                    const SizedBox(height: 12),
                    const LinearProgressIndicator(),
                  ],
                  const SizedBox(height: 100),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Animation<double> _staggered(int index) {
    final start = math.min(index * 0.1, 0.8);
    final end = math.min(start + 0.28, 1.0);
    return CurvedAnimation(
      parent: _entranceController,
      curve: Interval(start, end, curve: Curves.easeOutCubic),
    );
  }

  Widget _reveal({
    required int index,
    required bool reduceMotion,
    required Widget child,
  }) {
    if (reduceMotion) return child;
    final animation = _staggered(index);
    return FadeTransition(
      opacity: animation,
      child: SlideTransition(
        position: Tween<Offset>(
          begin: const Offset(0, 0.03),
          end: Offset.zero,
        ).animate(animation),
        child: child,
      ),
    );
  }
}

class _SettingsSplitColumns extends StatelessWidget {
  const _SettingsSplitColumns({
    required this.leftChildren,
    required this.rightChildren,
  });

  final List<Widget> leftChildren;
  final List<Widget> rightChildren;

  @override
  Widget build(BuildContext context) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Expanded(
          child: Column(
            children: [
              for (var i = 0; i < leftChildren.length; i++) ...[
                leftChildren[i],
                if (i != leftChildren.length - 1) const SizedBox(height: 16),
              ],
            ],
          ),
        ),
        const SizedBox(width: 16),
        Expanded(
          child: Column(
            children: [
              for (var i = 0; i < rightChildren.length; i++) ...[
                rightChildren[i],
                if (i != rightChildren.length - 1) const SizedBox(height: 16),
              ],
            ],
          ),
        ),
      ],
    );
  }
}

class _SettingsSection extends StatelessWidget {
  const _SettingsSection({required this.title, required this.children});

  final String title;
  final List<Widget> children;

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final disableAnimations = MediaQuery.maybeOf(context)?.disableAnimations ?? false;

    final container = AnimatedContainer(
      duration: disableAnimations
          ? Duration.zero
          : const Duration(milliseconds: 220),
      curve: Curves.easeOutCubic,
      decoration: BoxDecoration(
        color: cs.surfaceContainerLow,
        borderRadius: BorderRadius.circular(20),
      ),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(20),
        child: Column(children: children),
      ),
    );

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.only(left: 4, bottom: 8),
          child: Text(
            title,
            style: Theme.of(context).textTheme.labelLarge?.copyWith(
              color: cs.primary,
              letterSpacing: 0.5,
            ),
          ),
        ),
        container,
      ],
    );
  }
}

class _SettingsComponentBar extends StatelessWidget {
  const _SettingsComponentBar({
    required this.isBusy,
    required this.hasSavedFolder,
    required this.onRescan,
    required this.onAddFolder,
    required this.onOpenAbout,
  });

  final bool isBusy;
  final bool hasSavedFolder;
  final Future<void> Function() onRescan;
  final VoidCallback onAddFolder;
  final VoidCallback onOpenAbout;

  @override
  Widget build(BuildContext context) {
    return Card(
      margin: EdgeInsets.zero,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
        child: Wrap(
          spacing: 8,
          runSpacing: 8,
          children: [
            ActionChip(
              avatar: const Icon(Icons.refresh_rounded, size: 16),
              label: const Text('Rescan'),
              onPressed: isBusy || !hasSavedFolder ? null : () => onRescan(),
            ),
            ActionChip(
              avatar: const Icon(Icons.folder_open_rounded, size: 16),
              label: const Text('Add folder'),
              onPressed: isBusy ? null : onAddFolder,
            ),
            ActionChip(
              avatar: const Icon(Icons.info_outline_rounded, size: 16),
              label: const Text('About'),
              onPressed: onOpenAbout,
            ),
          ],
        ),
      ),
    );
  }
}

class _SettingsTile extends StatelessWidget {
  const _SettingsTile({
    required this.icon,
    required this.label,
    this.value,
    this.subtitle,
    required this.onTap,
  });

  final IconData icon;
  final String label;
  final String? value;
  final String? subtitle;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final cs = Theme.of(context).colorScheme;
    final tt = Theme.of(context).textTheme;

    return ExpressiveBounce(
      enabled: onTap != null,
      child: ListTile(
        leading: Icon(icon, size: 22, color: cs.onSurfaceVariant),
        title: Text(label, style: tt.bodyMedium),
        subtitle: subtitle != null
            ? Text(
                subtitle!,
                style: tt.bodySmall?.copyWith(color: cs.onSurfaceVariant),
              )
            : null,
        trailing: value != null
            ? Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(
                    value!,
                    style: tt.bodyMedium?.copyWith(color: cs.onSurfaceVariant),
                  ),
                  if (onTap != null) ...[
                    const SizedBox(width: 4),
                    Icon(
                      Icons.chevron_right_rounded,
                      size: 18,
                      color: cs.onSurfaceVariant,
                    ),
                  ],
                ],
              )
            : (onTap == null
                  ? null
                  : Icon(
                      Icons.chevron_right_rounded,
                      size: 18,
                      color: cs.onSurfaceVariant,
                    )),
        enabled: onTap != null,
        onTap: onTap,
        contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 2),
      ),
    );
  }
}
