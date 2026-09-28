# CalmFeed

CalmFeed is an Android GeckoView browser prototype for more intentional social browsing.
Social sites open in their real pages; CalmFeed does not collect their passwords.

## What's in the prototype

- A home dashboard with YouTube, Instagram, and Facebook shortcuts, an original daily
  motivational quote, and time used in CalmFeed today.
- One user-selected daily allowance, set once per local calendar day (15 to 240 minutes).
  Unused allowance carries only within that day; it resets the next day.
- Browsing is split into sessions of at most 15 minutes. The on-device timer tracks
  actual browser use, and sites will not open until today's allowance has been set.
- After the allowance is exhausted, the user can request more time from one paired
  trusted person. That person approves or declines and chooses an extra allowance of
  1 to 120 minutes. Each later request needs a new decision.
- A short-lived, one-use invite code pairs the user's device with the trusted person's
  CalmFeed installation. The trusted person opens **Trusted circle → Approval requests**
  to review incoming requests. Their app checks for requests every 15 seconds while
  that screen is open.
- GeckoView filters for selected short-video entry points and suggested videos, with a
  20-video daily limit on Shorts/Reels pages. The limit counts downward swipes and
  detects YouTube Shorts video changes as a fallback. Third-party page changes can
  affect these filters.

Daily allowance and session usage are stored locally on the user's device. Firebase
Authentication and Cloud Firestore (Spark plan) are used only to pair trusted people
and exchange approval requests. Firestore security rules restrict pairing and approval
updates to the linked accounts. The data is limited to anonymous Firebase account IDs,
user-selected display names, and approval requests; CalmFeed never receives social-site
credentials.

## Connect Firebase (required before social browsing)

The Firebase Android app is already registered as `com.calmfeed`; its local
`app/google-services.json` connects the client to project `calm-feed-1934a`. The
configuration file is intentionally excluded from source control. The project-level
Firebase CLI alias is in `.firebaserc`.

1. In this Firebase project, enable the **Anonymous** sign-in provider in Firebase
   Authentication.
2. Create a Cloud Firestore database.
3. With the Firebase CLI signed in to the project, deploy the Firestore security rules:

   ```powershell
   firebase deploy --only firestore:rules
   ```

   This Spark-plan design does not deploy Cloud Functions or require a billing account.
   Firestore currently includes a free daily quota (including 50,000 document reads and
   20,000 writes per day); check the [official quota details](https://firebase.google.com/docs/firestore/quotas)
   and monitor usage in the Firebase console.
4. Sync Gradle and build/run the app on both devices from the same Firebase-connected
   build. On the user's device, open **Trusted circle** and create an invite. On the
   trusted person's device, open **Trusted circle**, enter the invite code and a
   recognizable name, and connect. The invite expires after 10 minutes.

Before a public release, configure Firebase App Check and review account recovery,
data retention, and parental-consent requirements for the intended age group.

## Build

Open this directory in Android Studio with Android SDK 37 and JDK 17 installed, then
sync Gradle and run the `app` configuration on an Android 8.0+ device or emulator.
The current debug build targets arm64 devices such as the Galaxy A15.

## Prototype limitations

Social-site page selectors and swipe counting are best-effort: sites may change their
markup or scroll behavior. The 20-video Reels/Shorts filter is separate from the
on-device time allowance. As this free design has no trusted server, a user who clears
app data, changes the device clock, modifies the app, or bypasses CalmFeed can evade
the local time limit. Firestore approvals require internet access. The trusted person
needs to open the app's approval inbox to see requests; push notifications are not
implemented yet. Anonymous Firebase accounts are tied to the current app install, so
account recovery across reinstall/new devices is not implemented.
