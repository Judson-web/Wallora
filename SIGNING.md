# Wallora release signing

The Android application ID is `app.wallora.premium`. The release signing identity uses the alias `wallora_release` in `signing/wallora-release.jks`.

## Configure

1. Keep the separately supplied signing ZIP in a private location.
2. Extract its contents into the Wallora project root. The project should contain `keystore.properties` and `signing/wallora-release.jks`.
3. Do not commit either file. They are covered by `.gitignore`.
4. Build the release variant from Android Studio or run `./gradlew :app:assembleRelease`.

Gradle reads the signing values from `keystore.properties`; no credentials are hard-coded in the build script.

## Critical security rules

- The keystore and passwords are the app's long-term update identity. Back them up in at least two secure, private locations.
- Anyone who obtains both the keystore and passwords can sign an APK as the same publisher identity. Treat the signing ZIP as a secret.
- Never upload the signing ZIP, `.jks`, or `keystore.properties` to GitHub, public cloud storage, chat groups, or issue trackers.
- Do not regenerate the key for routine releases. Use the same signing identity for every update of this app ID.
- The key is self-signed. It establishes continuity between releases, not a verified real-world identity.

## Algorithm

RSA 4096-bit with SHA256withRSA, valid for 10,000 days. Android Gradle Plugin handles the APK signing schemes supported by the configured build tools. A signed build has not yet been produced or device-tested by this signing setup alone.
