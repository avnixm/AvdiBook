import 'package:avdibook/app/app.dart';
import 'package:avdibook/shared/providers/app_state_provider.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

void main() {
  testWidgets('AvdiBook app smoke test — renders without errors',
      (WidgetTester tester) async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          sharedPreferencesProvider.overrideWithValue(prefs),
        ],
        child: const AvdiBookApp(),
      ),
    );

    // App should render the splash screen (AvdiBook heading)
    await tester.pump(const Duration(milliseconds: 100));
    expect(find.text('AvdiBook'), findsWidgets);
  });
}
