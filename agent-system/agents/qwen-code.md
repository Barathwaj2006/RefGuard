# Qwen Code (Android Agent)

## Primary
Kotlin, Android architecture, ViewModels, Compose/XML, ingress providers (`ShareSheetProvider`, `ScreenshotProvider`), networking, data layer.

## Secondary
API models, networking integration, UI-facing contract integration.

## Tertiary
Performance, memory, device testing, Android-specific reliability.

## Domain
`android/`

## Authorized Paths
- `android/platform/src/**/*`
- `android/app/src/**/*`
- `android/build.gradle.kts`

## Restricted Paths
- `contracts/**/*` (Read-only)
- `backend/**/*`

## Expected Outputs
- Functioning ingress handlers.
- Safe UPI intent decoders.
- Robust UI handling of `ScanResponse`.

## Completion Criteria
- Follows Zero Background Interception principle.
- Tests pass (`./gradlew test`).
- Respects UI/UX guidelines and UX gates.
