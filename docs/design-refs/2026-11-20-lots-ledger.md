# SDD ledger — plan: docs/design-refs/2026-11-20-mobbin-synthese-design.md (lots 0→5)

Mode: exécution inline, vérification statique/build uniquement. Aucun adb tap/text, aucun smoke test sans accord explicite.

Pre-flight: no shared interfaces formels ; interfaces réelles vérifiées par relecture (`whenLabel`, `WeekDayUi`, `LZCalendarDayItem`, `LZHeroCard.action`, `lessonsByDay`).

Ruling: lot 1 « Prochaine séance » — `whenLabel` est une phrase (`Aujourd'hui à 14:00`) ; je dérive l'heure en valeur et le jour en label plutôt que d'ajouter un champ au modèle. Coût si faux : libellé moins riche, aucune donnée inventée.

Ruling: lot 1 CTA hero — j'utilise le slot `action` existant de `LZHeroCard` au lieu de modifier sa signature. Coût si faux : mise en page légèrement plus dense.

Ruling: vérification — l'utilisateur a interdit tout test adb/smoke ; la porte de chaque lot est la compilation Kotlin + grep de non-régression, pas un test d'UI. Coût si faux : un défaut purement visuel peut passer jusqu'au build final.

Lot 0: complete (déjà appliqué avant ce ledger : faux rappel, doublon subscription, token `textPrimary`).

Ruling: lot 1 3e KPI — « Matières actives » retenu car `studentDashboard()` ne lit pas `attendance` ; aucune donnée de présence inventée. Coût si faux : KPI moins « rotation » que souhaité, mais honnête.

Ruling: lot 1 navigation — `onDayClick` et CTA next-lesson naviguent vers `LZRoutes.StudentPlanning` sans argument ; le planning conserve son `todayIndex` réel via `selectedDayIndex`. Coût si faux : pas de sélection automatique du jour cliqué, mais aucune route modifiée.

Ruling: lot 1 CTA — `LZSecondaryButton` sur `heroGreenBrush` pour contraste. Coût si faux : bouton plus discret qu'un primary, mais lisible.

Lot 1: complete (3 KPI dérivés de `StudentDashboardData`, bande « Cette semaine », CTA hero, navigation planning). Gate: `compileDebugKotlin` vert (JDK zulu17), grep non-régression OK.

Lot 2: complete (bande KakaoBank, résumé réel `Jeudi 20 • 3 séances`, en-têtes Semaine collants, grille 6 colonnes lundi→samedi, pastille pleine, point de séance, marqueur aujourd'hui). PDF et `PlanningLevels.all()` inchangés. Gate: `compileDebugKotlin` vert (JDK zulu17), grep non-régression OK.

Ruling: lot 2 `stickyHeader` — import invalide supprimé, `ExperimentalFoundationApi` importé et opt-in ajouté sur `StudentPlanningContent`. Coût si faux : en-têtes non collants, mais compilation cassée évitée.
