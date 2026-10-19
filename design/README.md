# Approved Figma design assets

Source: https://www.figma.com/design/qUiRjBYfMHu0CNmyAjStrC?node-id=2-840
Estimated-iqamah state: https://www.figma.com/design/qUiRjBYfMHu0CNmyAjStrC?node-id=3-403

The main screen was implemented from Figma's high-fidelity design context and checked against its rendered pixels. Sample dates/times are illustrative and never replace the production timetable.

`figma-assets/` contains exact exported SVGs, original node mappings and SHA-256 checksums. The SVGs were exported directly from their completed Figma nodes using its supported export API. Wrapper slots preserve the intended padding: more/chevrons 24dp, calendar 18dp, sunrise 20dp, bell 22dp, estimate clock 16dp, phase dot 6dp.

Android framework Views do not natively decode SVG files. Unmodified SVG exports were rasterized by Inkscape at 384dpi (4×), then bundled in `res/drawable-xxxhdpi/`. Their density-aware intrinsic dimensions remain identical to the design slots. No temporary Figma URL is used at runtime. Theme tinting preserves the exact icon geometry.

The design's web-oriented Material 3 Switch Code Connect reference is not an Android framework dependency and cannot be imported into this Java/View app. `DesignSwitch` extends Android's native Switch, retaining its checked state, click behavior and accessibility, while matching the design's 52×32 track and 48dp touch target with native drawing. No external UI runtime was added.

Native system status and navigation bars remain live system UI, not static icons. Safe platform fonts are used to preserve the v1.9.1 startup recovery; exact font metrics can vary by Android device. Existing navy/sand/dark preferences use the same layout with their established palettes. Smaller phones compress flexible spacing and text while preserving the five prayer rows and controls without a scroll container.
