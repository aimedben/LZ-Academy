# Accueil élève — retirer le calendrier, cartes enseignants pleine largeur

## Intent
Deux corrections validées par l'utilisateur sur l'accueil élève :
1. Supprimer le bloc « Cette semaine » (calendrier).
2. Les cartes « Nos enseignants » doivent démarrer au bord gauche, sans marge.

## Décisions
- `LZCalendarDayItem` reste dans `LZCards.kt` : utilisé par les plannings prof/élève et l'admin.
- Carrousel conservé, cartes `178.dp` inchangées ; seule la marge de départ disparaît.

## Tâches
1. `StudentDashboardScreen.kt`
   - Supprimer le bloc `Column { LZSectionHeader("Cette semaine") … LZCalendarDayItem … }`.
   - Supprimer l'import `LZCalendarDayItem` et le champ `onDayClick` (+ son câblage dans `StudentDashboardRoute`).
   - Sortir `TeacherShowcaseSection` du `Column` paddé horizontalement : hero card dans un `Column` paddé, section enseignants pleine largeur, actualités dans un `Column` paddé.
2. `LZTeachers.kt`
   - `contentPadding = PaddingValues(horizontal = 0.dp)` sur le `LazyRow` (en-tête « Nos enseignants » reste à 20 dp).
3. Vérifier la compilation.

## Partie admin (validée ensuite)
- Navbar admin : « Plus » → « Profil » (5 onglets : Dashboard, Gestion, Communiquer, Rapports, Profil).
- `AdminProfileScreen` devient un onglet : suppression de `onNavigateBack`, ajout de `onOpenAcademy`.
- Carte « Academy » en bas du profil → `AdminMore` (packs, abonnements, sauvegardes, infos établissement).
- `AdminMoreScreen` : suppression du bouton redondant « Profil admin » et du paramètre `onOpenProfile`.
- Retour de `AdminMore` recâblé vers l'onglet Profil.

