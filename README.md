# Sec+ Quest

A Duolingo-style study app for the CompTIA Security+ (SY0-701) exam, with a home-screen widget.

- 21 lessons and 168 questions across the 5 exam domains
- XP, daily goal, streaks, hearts, and spaced-repetition review
- 30-question timed practice exam weighted like the real one
- Android home-screen widget: streak, today's XP, review cards due, exam countdown

## Install on Android
Open the latest release on your phone, download `SecPlusQuest.apk`, and open it.
If Android asks, allow installs from your browser.

## How it's built
`android/app/src/main/assets/index.html` is the whole app (HTML/CSS/JS). A small
Android wrapper (`MainActivity`) shows it full screen. The widget (`StatsWidget`)
reads a progress summary the page hands over through a JavaScript bridge.
Every push to `main` builds a signed APK with GitHub Actions and publishes it as a release.
