# Seha AI

A personal Android app for tracking health and physique, with an AI coach and a meal planner.
English and Arabic, light and dark, four colour themes.

## What it does

- **Set-up questions**: age, height, weight, goal, activity, medical history, food preferences.
  From these it works out daily calories, protein, carbs, fat and water (Mifflin-St Jeor).
- **Daily check-in**: did you work out, how you slept and ate, mood and energy. The coach replies.
- **Meal planner**: say what you feel like ("chicken") and what to avoid ("tomato"); it plans a day or a
  week that adds up to your targets, with ingredients, steps and a link to the recipe it was based on.
  Shopping list included.
- **Coach chat**: answers using your profile, medical notes and the last 7 days.
- **Progress**: weight chart against target, tape measurements, progress photos, weekly summary.
- **Health Connect**: reads steps, sleep, workouts, resting heart rate and weight; writes weight back.
- **Widgets**: "Today" and "Next meal". **Reminder**: an evening "How was your day?".

## AI services

The coach and planner call GPT (OpenAI), Gemini (Google) or Claude (Anthropic) directly from the phone
with your own API key: Settings > AI coach. Keys stay on the phone and are never part of a backup.

## Building

Every push to `main` builds the APK on GitHub Actions and publishes it under **Releases**.
All builds are signed with the key in `ci/seha.keystore`, so a new version installs over the old one
and keeps the data. Raise `versionCode` in `app/build.gradle.kts` with every release.

Locally: `bash ci/fetch_fonts.sh` once, then `./gradlew assembleDebug`.

Seha AI gives general fitness and nutrition guidance. It is not medical advice.
