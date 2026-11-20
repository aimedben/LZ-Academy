# Plan — Refonte UI « Clean modern SaaS » (13/11/2026)

Design : `docs/superpowers/specs/2026-11-13-ui-redesign-design.md`

## Tâches

### 1. Palette — `theme/LZColors.kt`
- [ ] Light : background #F8FAF9, border #E7ECEA, textPrimary #101A17, textSecondary #66756F,
      primary #059669, primaryDark #047857, surfaceSoft #ECF6F1, shadow ajusté.
- [ ] `studentHeader`/`teacherHeader`/`adminHeader` → dégradés de surface clairs
      (blanc → teinte pâle du rôle) ; `heroPurple` → turquoise→vert ; heroGreen affiné.
- [ ] Dark : headers → surface sombre (teinte douce), reste conservé.
- [ ] Ajouter `onHeader` (couleur de texte/icone sur header) light & dark.

### 2. Composants — `components/LZHeaders.kt`
- [ ] `LZGradientHeader` : fond clair (brush token), supprimer/atténuer les cercles
      décoratifs, bordure basse 1dp `border`, padding identique.
- [ ] `LZGreetingRow` : Color.White → LZ.colors.onHeader (titre + sous-titre).
- [ ] `LZHeaderIconButton` : container translucide → surface bordée, tint = onHeader.

### 3. Status bars — 7 appels `lightIcons = true` → `false`
- [ ] TeacherDashboardScreen (×2), StudentDashboardScreen (×2), AdminDashboardScreen (×2),
      StudentTeacherProfileScreen (×1).

### 4. Nav — `components/LZNavigation.kt`
- [ ] AdminBottomNavigation accent purple → primary.

### 5. Typo — `theme/LZTypography.kt` (léger)
- [ ] pageTitle 20sp/700 tracking -0.4 ; display 30sp ; secondary 12.5→13sp ok ;
      cardTitle 15.5sp ; stat 28sp. (Garder l'API.)

### 6. Skeletons
- [ ] Vérifier `LZDashboardSkeleton(brush)` sur header clair (contraste des items).

### 7. Build & install
- [ ] `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest` vert.
- [ ] `adb install -r` → captures : login, accueil élève, planning, profil, admin dashboard,
      gestion (élèves + groupes), abonnements.
- [ ] Fixer les textes blancs codés en dur trouvés via captures (headers).

### 8. Ledger
- [ ] Append `docs/superpowers/ledgers/2026-11-13-real-data-only-progress.md`.
