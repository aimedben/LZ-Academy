# Pages de profil + écran Paramètres — plan d'implémentation

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Les trois pages de profil (élève, enseignant, admin) partagent la même structure, et un
écran Paramètres plein écran regroupe thème, notifications réelles et compte.

**Architecture:** Un fichier de composants de réglage (`LZSettings.kt`), un catalogue pur des
interrupteurs par rôle (`NotificationSettings.kt`, testable en JVM), une route `settings`, et une
préférence réactive dans `AppPreferences` (`notificationFlow`).

**Tech Stack:** Kotlin, Jetpack Compose, Navigation Compose, SharedPreferences, JUnit.

**Spec:** `docs/superpowers/specs/2026-11-14-profile-pages-and-settings-design.md`

## Global Constraints

- Aucun interrupteur sans effet réel ; aucune donnée inventée (règle « real data only »).
- Aucune nouvelle requête Supabase, aucune permission ajoutée.
- Français pour l'UI, anglais pour les commentaires et les noms de code.
- Pas de git : clôture par la commande de test, ledger dans `docs/superpowers/ledgers/`.

## Review Focus

- Un interrupteur basculé dans Paramètres est visible au retour sur le tableau de bord sans
  redémarrage.
- Le mode sombre choisi dans Paramètres s'applique immédiatement à l'écran Paramètres lui-même.
- « Synchronisation automatique » désactivée : le lancement ne déclenche plus de snapshot.
- L'admin qui n'a jamais ouvert Paramètres garde le comportement actuel (défauts à `true`).

---

### Task 1: Catalogue pur des interrupteurs

**Files:** Create `app/src/main/java/com/lzacademy/app/data/settings/NotificationSettings.kt`,
Test `app/src/test/java/com/lzacademy/app/NotificationSettingsTest.kt`.

**Interfaces:** `data class NotificationSwitch(val id: String, val title: String, val description: String, val default: Boolean)`,
`enum class SettingsSectionKind`, `fun notificationSwitchesFor(role: UserRole): List<NotificationSwitch>`.

- [x] **Step 1:** Test : l'élève a `student.notifications` (défaut `true`) ; l'admin a `admin.alerts` et
  `admin.autoSync` ; l'enseignant n'a **aucun** interrupteur local (ses deux réglages sont serveur) ;
  tous les `id` sont uniques ; aucun défaut n'est `false` sans raison.
- [x] **Step 2:** Lancer le test, vérifier l'échec (`Unresolved reference`).
- [x] **Step 3:** Implémenter le catalogue.
- [x] **Step 4:** Lancer le test, vérifier le succès.

### Task 2: Préférence réactive

**Files:** Modify `app/src/main/java/com/lzacademy/app/data/preferences/AppPreferences.kt`.

- [x] **Step 1:** `notificationEnabled(id, default)`, `setNotificationEnabled(id, enabled)`,
  `notificationFlow(id, default): Flow<Boolean>` (via `OnSharedPreferenceChangeListener` +
  `callbackFlow`, valeur initiale émise immédiatement).
- [x] **Step 2:** `:app:compileDebugKotlin` → BUILD SUCCESSFUL.

### Task 3: Composants de réglage

**Files:** Create `app/src/main/java/com/lzacademy/app/components/LZSettings.kt`.

- [x] **Step 1:** `LZSettingsSection(icon, title, content)` (carte titrée), `LZSettingsSwitchRow(icon, title, description, checked, onCheckedChange, enabled)`,
  `LZSettingsActionRow(icon, title, description, onClick)`.
- [x] **Step 2:** `:app:compileDebugKotlin` → BUILD SUCCESSFUL.

### Task 4: Écran Paramètres

**Files:** Create `app/src/main/java/com/lzacademy/app/ui/settings/SettingsScreen.kt`.

- [x] **Step 1:** Sections Compte / Apparence / Notifications / Données & synchronisation (admin).
  L'enseignant charge sa ligne via `TeachersViewModel.teacherByProfile` pour ses deux réglages serveur.
- [x] **Step 2:** `:app:compileDebugKotlin` → BUILD SUCCESSFUL.

### Task 5: Route + ouverture depuis les avatars

**Files:** Modify `navigation/LZRoutes.kt`, `navigation/LZNavHost.kt`, `LZApp.kt` ; delete `ui/auth/AccountSheet.kt`.

- [x] **Step 1:** `LZRoutes.Settings = "settings"` ; `LZNavHost` reçoit `themeMode`, `onThemeModeChange`,
  `onLogout` et déclare la route ; `LZApp` remplace `AccountSheet` par la navigation vers Paramètres.
- [x] **Step 2:** `:app:compileDebugKotlin` → BUILD SUCCESSFUL.

### Task 6: Restructuration des trois profils

**Files:** Modify `ui/student/StudentProfileScreen.kt`, `ui/teacher/TeacherProfileScreen.kt`, `ui/admin/AdminProfileScreen.kt`.

- [x] **Step 1:** Élève : en-tête « Mon profil » + engrenage, cartes existantes conservées, carte finale
  « Paramètres » → `onOpenSettings`.
- [x] **Step 2:** Enseignant : en-tête « Mon profil » + engrenage, « Diffusion & contact » retirée
  (déplacée dans Paramètres), carte finale « Paramètres ».
- [x] **Step 3:** Admin : en-tête « Mon profil » + engrenage, carte d'identité en lecture seule +
  « Modifier mes informations » (bascule le formulaire existant) + carte finale « Paramètres ».
- [x] **Step 4:** `:app:compileDebugKotlin` → BUILD SUCCESSFUL.

### Task 7: Branchement des effets réels

**Files:** Modify `ui/student/StudentDashboardScreen.kt`, `ui/admin/AdminDashboardScreen.kt`,
`repository/SupabaseSyncRepository.kt`.

- [x] **Step 1:** Élève : `badge = if (notificationsEnabled) data.unreadNotifications else 0`.
- [x] **Step 2:** Admin : la section « Alertes » n'est rendue que si `admin.alerts` est vrai.
- [x] **Step 3:** `shouldSync` reçoit `autoSyncEnabled` et renvoie `false` quand il est désactivé.
- [x] **Step 4:** Test JVM de `shouldSync` (3 cas) ; suite complète verte.

### Task 8: Vérification et livraison

- [x] **Step 1:** `:app:testDebugUnitTest --rerun` → 0 échec.
- [x] **Step 2:** `assembleRelease` (R8), install `--no-streaming`, smoke test.
- [x] **Step 3:** Ledger mis à jour.
