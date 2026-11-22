# Build the Chonkies APK using only your Android phone

This project includes a GitHub Actions workflow that builds the Android APK in the cloud. You do not need Android Studio or a computer.

## Steps

1. On your Android phone, open https://github.com/ and create a free account or sign in.
2. Create a new **Private** repository named `chonkies-android`.
3. Upload the files and folders from this ZIP to the repository's `main` branch. Include the hidden `.github/workflows/build-apk.yml` file; it is what starts the cloud build. If the mobile upload screen hides dot-folders, enable **Request desktop site** in your browser.
4. Commit the files. Open the repository's **Actions** tab and enable Actions if GitHub asks.
5. Choose **Build Chonkies APK**. If it did not start automatically, tap **Run workflow**.
6. Wait for the run to finish successfully. Open the completed run and find **Artifacts**.
7. Download `chonkies-debug-apk`. Extract the downloaded ZIP and tap `app-debug.apk` to install it.
8. If Android blocks installation, allow your browser or file manager to **Install unknown apps** for this installation, then retry.

## Important

- This produces a debug/test APK, not a signed Play Store release.
- The current app has demo menu/pricing and a starter Stripe backend. It is not ready for live customer payments or live orders until real prices/tax, Stripe account keys, secure backend hosting, order storage, and order fulfillment are configured.
- Keep the GitHub repository private. Never put Stripe secret keys in the Android app or repository.
