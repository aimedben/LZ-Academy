# SDD ledger — plan: docs/superpowers/plans/2026-11-14-final-sms-optim.md

## Setup
- Ruling: pas de dépôt git dans ce projet → pas de worktree ni de scripts git (task-start/task-done/review-package indisponibles). Le ledger est tenu ici, dans `docs/superpowers/ledgers/`, et chaque tâche est clôturée par la commande de test du plan. Coût si erreur : pas d'historique de commits, retour arrière manuel.
- Spec lue : `docs/superpowers/specs/2026-11-14-final-sms-optim-design.md`.
- Pre-flight : interfaces partagées vérifiées — Task 2 consomme `SmsComposer` (Task 1), Task 5 consomme `SmsRecipient` (Task 2), Task 6 consomme `SmsRecipients`/`SmsComposer`/`PreparedSms`/`LZMultilineInput`/`recipients` (Tasks 1,2,3,4,5). Aucun conflit.

## Tasks
- Task 1: complete (tests: `:app:testDebugUnitTest --tests com.lzacademy.app.SmsComposerTest` → BUILD SUCCESSFUL, 5 tests).
- Task 2: complete (tests: `:app:testDebugUnitTest --tests com.lzacademy.app.SmsRecipientsTest` → BUILD SUCCESSFUL, 3 tests).
- Task 3: complete (tests: `SmsHistoryStoreTest` → PASS, 2 tests; incl. JSON corrompu ignoré).
- Task 4: complete (`LZMultilineInput` ajouté, compile OK via le run groupé des tests).
- Task 5: complete (`recipients: StateFlow<List<SmsRecipient>>` sur AdminReportsViewModel, compile OK).
- Task 6: complete (`AdminSmsContentTest` → PASS après scroll LazyColumn; tag `admin_sms_list` ajouté).
  - Ruling: le test Compose ne peut pas voir les items hors écran d'un LazyColumn → ajout d'un `testTag("admin_sms_list")` + `performScrollToNode`. Aucun impact runtime. Coût si erreur : un tag inerte de plus.
- Task 7: complete (`SyncThrottleTest` → PASS, 4 tests ; snapshot 12 GET parallèles ; throttle 60 min).
- Task 8: complete (sonde `fetchLevels` supprimée, 12 GET parallèles, retry unique sur 401, garde 60 s).
  - Vérification Logcat sur appareil : `LZSync: refresh sauté (dernière sync il y a 2 min)` → le throttle 60 min coupe bien le snapshot automatique au lancement. Aucun appel réseau de synchro au démarrage sur miroir frais.
  - Vérification du retry unique sur 401 non reproduite en direct (le JWT en cours est encore valide) : couverte par la structure `loadDataWithRefresh` (1 refresh + 1 retry), donc pas de sonde, pas de 3e appel.
- Task 9: complete — `assembleRelease` → BUILD SUCCESSFUL (17 min), APK 4 019 171 octets, R8 + shrinkResources actifs.
  - Ruling signature/install : l'APK release signé avec `debug.keystore` du projet a EXACTEMENT le même certificat que l'app installée (SHA-256 `8187944e…efe8`, vérifié à l'`apksigner` sur l'APK extrait du téléphone) → mise à jour en place, aucune perte de profil admin ni d'historique local.
  - Ruling install : `adb install` en mode *streamed* échoue sur ce MIUI avec un message vide ; `adb install -r --no-streaming` réussit (`Success`). À réutiliser pour toutes les installations futures sur cet appareil.
  - Smoke test R8 : app 1.1 (versionCode 2) lancée sur `e25ffc230521` → dashboard élève rendu correctement (36 séances, 2 matières, prochain cours, enseignants), Logcat propre : aucun `FATAL`/`ClassNotFoundException`/`NoSuchMethodError`, miroir 381 élèves chargé, avatars distants résolus. Aucune règle `-keep` supplémentaire nécessaire.
- Task 10: complete (index `teacher_payments_teacher_id_idx` créé et vérifié via `pg_indexes`).
- Task 11: complete (3 `take(180)` restants = messages d'erreur admin, conservés ; logs de fetch réduits à `bytes=`). États vides/erreurs : `Aucun élève synchronisé` (miroir vide vs filtre sans résultat), bouton d'envoi désactivé sans destinataire ni texte, `Réessayer` de l'écran Gestion passe `force = true` pour contourner la garde 60 s. Prouvé par `AdminSmsContentTest` + revue de code.

## Correctifs après revue (non planifiés, demandés sur appareil)

- Photos enseignants (espace élève) : `TeacherAvatar` ignorait `photo_url`. Ruling : centralisation dans `teacherPhotoUrl(teacher)` (chemin `<bucket>/<object>` → URL publique, URL absolue conservée), utilisée par `TeacherAvatar` et `TeacherShowcaseCard`. Prouvé par `TeacherPhotoUrlTest` (3 tests, RED puis GREEN). Coût si erreur : un chemin de bucket mal préfixé → avatar en initiales (dégradation douce).
- Plan `docs/superpowers/plans/2026-11-14-profile-avatar-progress-ring.md` (bordure = 4 séances) :
  - Task 1: complete (`SubscriptionRingTest` → 4 tests RED puis GREEN).
  - Task 2: complete (`LZAvatarProgressRing` dans `LZProgress.kt` ; couleurs lues hors du `DrawScope`, sinon appel composable interdit — erreur de compilation rencontrée puis corrigée).
  - Task 3: complete (écran Profil : anneau + libellé « N/4 séances », anneau blanc statique retiré pour ne pas doubler la bordure).
  - Task 4: complete (`SubscriptionRealignTest` → 4 tests RED puis GREEN ; `alignPack` ajouté au DAO ; réalignement seulement si le total du pack change).
  - Task 5: en attente — appareil déconnecté (`adb devices` vide) au moment de l'installation. `assembleDebug` OK.
  - Suite complète `:app:testDebugUnitTest --rerun` : BUILD SUCCESSFUL, 0 échec.
- Ruling données : `public.subscriptions` = 4 séances pour 381/381 lignes ; l'ancien total 48 ne vient que du compteur local. Décision utilisateur : « pas 48, uniquement 4 » → le miroir gagne dès que la taille du pack change.

## Plan `docs/superpowers/plans/2026-11-14-profile-pages-and-settings.md` (pages de profil + Paramètres)

Spec : `docs/superpowers/specs/2026-11-14-profile-pages-and-settings-design.md`. Approche retenue : B (modèle de page commun aux 3 rôles + écran Paramètres plein écran).

- Task 1: complete — catalogue pur `data/settings/NotificationSettings.kt` (`NotificationSwitch`, `SettingsSection`, `notificationSwitchesFor`), clés `student.notifications` / `admin.alerts` / `admin.autoSync`. `NotificationSettingsTest` → 6 tests PASS.
  - Ruling « real data only » : l'enseignant n'a **aucun** interrupteur local (ses deux réglages sont des colonnes serveur `public.teachers`), donc `notificationSwitchesFor(Teacher)` = liste vide. Aucun interrupteur inventé (pas de rappel de séance, pas de push, pas de `POST_NOTIFICATIONS`).
- Task 2: complete — `AppPreferences.notificationEnabled/setNotificationEnabled/notificationFlow(id, default)` (`callbackFlow` + `OnSharedPreferenceChangeListener`, valeur initiale émise immédiatement).
- Task 3: complete — `components/LZSettings.kt` (`LZSettingsSection`, `LZSettingsSwitchRow`, `LZSettingsActionRow`) ; `TeacherSwitchRow` supprimé de `LZTeachers.kt` (remplacé par `LZSettingsSwitchRow`).
- Task 4: complete — `ui/settings/SettingsScreen.kt` : Compte (avatar/nom/e-mail/rôle + déconnexion), Apparence (segmenté Système/Clair/Sombre), Notifications (interrupteurs locaux du rôle), Données & synchronisation (admin : synchro auto), Diffusion & contact (enseignant : 2 colonnes serveur déplacées depuis le profil).
- Task 5: complete — route `LZRoutes.Settings`, déclarée dans `LZNavHost` (reçoit `themeMode`/`onThemeModeSelected`/`onLogout`), `LZApp.onAvatarClick` → Paramètres ; `ui/auth/AccountSheet.kt` supprimé (son contenu vit dans Paramètres).
- Task 6: complete — les 3 profils partagent la structure en-tête « Mon profil » + engrenage → carte d'identité → cartes du rôle → carte « Paramètres ». Élève : cartes existantes conservées. Enseignant : carte « Diffusion & contact » retirée du profil (déplacée dans Paramètres). Admin : identité en lecture seule + « Modifier mes informations » qui bascule le formulaire existant (champs vides → « Aucune donnée — non renseigné »).
- Task 7: complete — effets réels branchés : élève `badge = if (notificationsEnabled) unreadNotifications else 0` (flux réactif) ; admin section « Alertes » rendue seulement si `admin.alerts` ; `shouldSync(..., autoSyncEnabled)` renvoie `false` quand `admin.autoSync` est désactivé. `SyncThrottleTest` étendu (5 tests).
- Task 8: complete — `:app:testDebugUnitTest --rerun` → BUILD SUCCESSFUL, **65 tests, 0 échec, 0 erreur** (17 suites). `assembleRelease` (R8 + shrinkResources) → BUILD SUCCESSFUL in 9 min 10 s, APK `app-release.apk` **4 035 555 octets**. Install `-r --no-streaming` → `Success`.
  - Vérification appareil : `versionName=1.1 versionCode=2`, `firstInstallTime=2026-11-06` conservé, `lastUpdateTime=2026-11-14 17:42:08` → **mise à jour en place**, données locales conservées.
  - Vérification signature : `apksigner verify --print-certs` sur l'APK release → `CN=Android Debug`, SHA-256 `8187944eef3f52377ef42c662c9f2c168a63c0acf7dcee1efab44a8e9f6eefe8` = exactement le certificat de l'app déjà installée.
  - Smoke test profils/Paramètres non exécuté : l'appareil est en appel vidéo (consigne utilisateur : aucun tap/launch tant que l'appel dure). À faire dès que l'appareil est libre.


