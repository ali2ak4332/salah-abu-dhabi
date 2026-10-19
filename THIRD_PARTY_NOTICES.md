# Third-party notices

## Noto Sans Arabic

Copyright 2022 The Noto Project Authors (https://github.com/notofonts/arabic)

The source project retains two static font design instances derived from the official Noto Sans Arabic variable font, version 2.012: Regular (weight 400) and Medium (weight 500), both at width 100. These files are stored outside Android resources in design/fonts and are not packaged as fonts in the v1.9.1 recovery APK. These static instances remove the variable-font axes from the packaged fonts; they are modified versions of the upstream font, not the original variable font.

Upstream source: https://github.com/google/fonts/tree/main/ofl/notosansarabic
Upstream variable TTF SHA-256: 63111b5b2e074dd48cc67692e0a2726d86ee94c1c37fe8598257b7b4e87e869e

The instances were generated with fontTools 4.61.1 using `instantiateVariableFont`, fixing `wdth` to 100 and `wght` to 400/500, with `optimize=True` and `updateFontNames=True`.

- `noto_sans_arabic_regular.ttf` SHA-256: 95c19dd755469ac9a72cc5cfac62331eefb70bbe3e392c6a26e0fb8107ae3b19
- `noto_sans_arabic_medium.ttf` SHA-256: a3c274b54e5a6f3602e49ea31d693228d36a0c6650d5618a20c24201ce0f0930

Licensed under the SIL Open Font License, Version 1.1. See `licenses/NotoSansArabic-OFL.txt` for the complete copyright and license text. The identical copyright/license is bundled in APK assets and remains embedded in the static fonts' metadata. The upstream copyright header specifies no Reserved Font Name.

The vector icons and hero-card motif are original app code.
