Place your app icon PNGs (xxhdpi/xhdpi/hdpi/mdpi) under the matching `res/mipmap-*/` folders.
This project centralizes the launcher foreground/monochrome into drawable wrappers:

- `res/drawable/app_icon_foreground.xml` -> points to `@mipmap/ic_launcher` (change this to your PNG resource)
- `res/drawable/app_icon_monochrome.xml` -> points to `@mipmap/ic_launcher` (monochrome mask if needed)

To swap the icon:
1. Replace the mipmap images (`ic_launcher.webp`/`ic_launcher_round.webp`) in `res/mipmap-*/` with your PNGs (same names).
2. Or update `app_icon_foreground.xml`/`app_icon_monochrome.xml` to point to a different drawable resource.
