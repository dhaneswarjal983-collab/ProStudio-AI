# Lumen Photo Studio

An original Android photo-editing app built with Kotlin and Jetpack Compose. This project uses Lightroom Android feature research as a product requirements reference; it does not use Adobe source code, branding, or proprietary assets.

## Current implementation (0.2.0)
- Home, Library, Editor, Looks, and Profile navigation
- Android system photo picker
- On-device photo preview and non-destructive in-memory adjustment state
- Light controls: exposure, contrast, highlights, shadows, whites, blacks
- Color controls: temperature, tint, saturation, vibrance
- Effects controls: clarity-style local tone adjustment, dehaze-style tone adjustment, vignette, grain
- Original built-in looks, before/after preview, undo, reset, JPEG export to Pictures/Lumen Studio

## Important boundaries
This is an early working foundation, not a 327-feature Lightroom replacement. RAW workflow, masks, healing, AI/generative tools, cloud sync, catalog management, HDR, advanced curves, geometry, video, and many audited controls remain unimplemented and require separate engineering and testing. Device compatibility and output quality must be tested on physical Android hardware.

## Build
Open with Android Studio or run:
`./gradlew :app:assembleDebug`

Package: `com.dhanesh.prostudiopro`
