# Visible Glass test candidate 2.0.20.9

Based on the signed 2.0.20.8 test commit 4b68004a202dd0ea838162c26e959bf7d397b699.
The user-verified foundation remains 2.0.20.6 at f90249b466cace478e5a038c1cd96ca0e4fc7aba.

The prior Glass styling was too opaque to show a glass appearance. This candidate
adds one 1280-wide static backdrop, visibly translucent tinted panels, fixed
surface highlights, full-width provider artwork on the existing Home hero, and
a native player bottom bar with inset margins and thin highlights. Existing
navigation and focus targets are retained. No blur, refraction, RenderEffect,
offscreen compositing layer, new dependency, or catalogue limit is introduced.

Background asset: app/src/main/res/drawable-nodpi/glass_backdrop.webp.
Generated using the built-in ImageGen tool, then encoded as a small WebP.
Prompt: a wide cinematic dark navy planetary mountain landscape at blue hour,
a large soft planet arc in the upper right, restrained amber horizon light,
misty distant mountain ridges, subtle scattered stars and dark layered clouds.
Dark calm left/lower areas, subtle highlights for white text over tinted panels.
No people, posters, logos, text, buttons, television frame, or UI.

Physical box and Quest performance still requires device testing. The workflow
checks the current version, existing signing certificate, unit tests, lint
regressions against the exact 2.0.20.6 baseline, and TV/phone instrumentation.
Screenshot evidence includes Glass Home, movies, live, profiles, settings and player.
