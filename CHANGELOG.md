# Changelog

## Unreleased
- Changed: Updated target SDK version to 36
- Changed: Dependencies updated

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