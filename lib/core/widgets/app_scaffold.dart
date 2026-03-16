import 'package:flutter/material.dart';

/// Consistent scaffold wrapper used across most AvdiBook screens.
///
/// Provides:
/// - Proper safe area handling
/// - Standard app bar configuration
/// - Optional [floatingActionButton]
/// - Optional [bottomSheet] slot for mini-player
class AppScaffold extends StatelessWidget {
  const AppScaffold({
    super.key,
    this.title,
    this.titleWidget,
    this.body,
    this.actions,
    this.leading,
    this.floatingActionButton,
    this.bottomSheet,
    this.backgroundColor,
    this.extendBodyBehindAppBar = false,
    this.showAppBar = true,
    this.centerTitle = false,
    this.padding = const EdgeInsets.symmetric(horizontal: 16),
  });

  final String? title;
  final Widget? titleWidget;
  final Widget? body;
  final List<Widget>? actions;
  final Widget? leading;
  final Widget? floatingActionButton;
  final Widget? bottomSheet;
  final Color? backgroundColor;
  final bool extendBodyBehindAppBar;
  final bool showAppBar;
  final bool centerTitle;
  final EdgeInsets padding;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    Widget? appBar;
    if (showAppBar) {
      appBar = AppBar(
        title: titleWidget ??
            (title != null
                ? Text(
                    title!,
                    style: theme.textTheme.titleLarge,
                  )
                : null),
        centerTitle: centerTitle,
        leading: leading,
        actions: actions,
        backgroundColor: backgroundColor ?? theme.scaffoldBackgroundColor,
      ) as Widget?;
    }

    return Scaffold(
      backgroundColor: backgroundColor,
      extendBodyBehindAppBar: extendBodyBehindAppBar,
      appBar: appBar as PreferredSizeWidget?,
      floatingActionButton: floatingActionButton,
      bottomSheet: bottomSheet,
      body: body != null
          ? SafeArea(
              bottom: false,
              child: Padding(
                padding: padding,
                child: body,
              ),
            )
          : null,
    );
  }
}
