# Changelog

## 0.1.11
- Fixed: System back swipe closes screens with the same animation as the action bar back button
- Added: uBlock Origin Settings on the Settings screen
- Changed: Updated uBlock Origin to 1.74.0
- Changed: Updated GeckoView to 154
- Changed: Raised minimum Android version to 8.0 (API 26)
- Changed: Settings titles use the same system font and unread-post colour as the main feed
- Fixed: Settings screen swipe-down refresh spinner looping forever
- Removed: HTML Provider setting (Instapaper Text and Textise)
- Removed: Android Webview and Custom Tab article viewers

## 0.1.10
- Changed: Targeting Android 16 (API 36) for Google Play
- Fixed: Align Kotlin JVM bytecode with Java 17 so the project builds on JDK 21
- Fixed: Migrate system back handling to OnBackPressedDispatcher (required on Android 16)

## 0.1.9
- Changed: Updated target SDK version to 36
- Changed: Dependencies updated
- Changed: Replace Action bar display helper with fitsSystemWindows=true to prevent content within the app being obscured or hidden by system windows as per Android SDK 35 default edge-to-edge rendering

## 0.1.8
- Fixed: Set an App label so it doesn't use the default appid+name
- Fixed: Spotlight the correct location on the actionbar during the initial usage popup
- Fixed: Assembled release is corrupt. Disabled shrinkResources/minify in releases.

## 0.1.7
- Added: Consistent animation and back button behavior between activities
- Fixed: Action bar overlap issues
- Fixed: URL corrections

## 0.1.6
- Changed: Updated target SDK version to 35
- Changed: Modified app store feature assets
- Removed: Release.properties file

## 0.1.5
- Added: Privacy Policy
- Changed: Enhanced build configuration to minimize requested privileges

## 0.1.4
- Added: New open source components to NOTICE file
- Added: Notice dialog functionality in AboutActivity
- Changed: Modified APK naming convention

## 0.1.3
- Changed: Refactored release workflow to always build Debug APK
- Fixed: Conditional APK build indentation in release workflow
- Changed: Updated CI workflow to conditionally build Debug or Release APK based on tag presence

## 0.1.2
- Added: uBlock Origin integration to GeckoView
- Changed: Updated action bar titles and styles across activities
- Changed: App name changed to HN+
- Changed: Refactored project to Kotlin
- Added: GeckoView support for future extensions
- Changed: Updated dependencies

## 0.1.1
- Added: Custom Tab support for opening links in Chrome
- Added: Preference setting "View Articles within.." in Settings
- Added: Fallback to webview if Chrome or equivalent browser not installed

## 0.1.0
- Added: HTTPS enforcement for article viewers
- Removed: Outdated article readers (Google GWT, View Text)
- Added: Textise provider
- Fixed: Comment parsing for text and color

## Previous
Previous versions were developed in https://github.com/manmal/hn-android