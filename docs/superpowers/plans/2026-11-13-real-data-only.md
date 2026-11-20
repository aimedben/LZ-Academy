# Plan — Données 100 % réelles, zéro démo (2026-11-13)

**Spec :** `docs/superpowers/specs/2026-11-13-real-data-only-design.md` (approche A — intégration complète, validée par l'utilisateur).
**But :** supprimer définitivement toutes les données de démonstration de l'app et faire afficher partout les valeurs réelles de la base Supabase (projet `qxnuuhjjuzudxyyleplw`). Le dashboard admin n'affiche plus jamais « 320 élèves » figé. Tout écran sans source réelle affiche « Aucune donnée » — jamais d'invention.

## État réel de la base (vérifié par SQL le 13/11)

- students 381 (381 actifs, 369 sans `level_id`, `created_at` existe) · subscriptions 381 (1 épuisée, 380 actives)
- teachers 21 (`profile_id` tous NULL → à corriger pour le compte enseignant) · groups 4 · subjects 15
- payments 12 (6000–15000 DA, `status` paid/pending, `amount_dzd`, `created_at`, `paid_at`)
- announcements 4 (`audience` enum, `level_id`, `is_pinned`, `published_at`, `expires_at`)
- materials 4 (`kind`, `storage_path`, `size_bytes`, `subject_id`, `is_published`)
- attendance 4 (`session_id`, `student_id`, `status`, `recorded_at`) · sessions 28 (`group_id`, `teacher_id`, `subject_id`, `room_id`, `starts_at`, `ends_at`, `status`, `topic`)
- group_schedule_slots 7 (`group_id`, `weekday`, `starts_at`, `ends_at`, `room_id`) · rooms 8 (`name`)
- enrollments 16 (`student_id`, `group_id`, `joined_on`, `left_on`)
- Colonnes jointes disponibles via PostgREST : `levels(name)`, `groups(name)`, `subjects(name)`, `rooms(name)`, `teachers(full_name)`.

## Décisions consignées

- **Miroir Room v4** : +10 tables `synced_*` (teachers, groups, subjects, payments, announcements, materials, schedule_slots, sessions, attendance, enrollments) → 12 GET throttlés 15 min par cycle (reste sobre pour le free tier).
- **`synced_students` recréée** avec `createdAt` (drop + recreate dans la migration — simple miroir, perte acceptable) ; `TeacherEntity` gagne `profileId` (ALTER nullable).
- **Dashboards** : calculs purs dans `DashboardComputer` (objet JVM testable, aucun type Compose) → `SupabaseDashboardRepository` mappe vers les modèles UI (icônes/couleurs), lit uniquement le miroir Room.
- **États vides** : `StudentDashboardData.nextLesson` et `TeacherDashboardData.nextSession` deviennent nullables ; écrans affichent « Aucune donnée » / « Aucun cours programmé ».
- **Revenu admin** = somme `amount_dzd` des payments `paid` du mois courant ; tendance = sommes payées des 6 derniers mois ; delta = mois précédent (0 si vide).
- **Croissance admin (Rapports)** = cumul réel des élèves par mois depuis `students.created_at` (6 derniers mois) — remplace `listOf(180f…320f)`.
- **Donut admin** = élèves par `levelName` (« Non affecté » si NULL) — réel.
- **Annonces élève** = `audience IN ('all','students')`, non expirées ; `unreadNotifications = 0` (la table `announcement_reads` n'est pas miroirée — YAGNI).
- **Prochain cours / emploi du temps élève** = via `enrollments` → `group_schedule_slots` + `sessions` ; l'élève LZ-895 n'a probablement aucune inscription → état vide honnête.
- **Appel (roll-call)** = roster 381 élèves syncés ; enregistrement local Room (inchangé) ; POST serveur seulement si une `session` du jour résout (sinon local uniquement — voir tâche 7).
- **Auth locale conservée** (`AuthSeed` 3 comptes) : c'est l'authentification, pas des données de test ; le label « Comptes de démonstration » devient « Comptes de l'académie ».
- **Previews** : les `@Preview` qui utilisaient `DemoData`/`TeacherSeed` reçoivent un fichier `PreviewSamples.kt` (jamais chargé au runtime).
- Pas de git : pas de commits ; vérification par build + tests + appareil.

---

## Tâche 0 — Correction de données : lier le compte enseignant à sa ligne `teachers`

Le dashboard enseignant doit matcher l'utilisateur connecté (`teachers.profile_id`). Tous sont NULL.

1. Écrire `build/sbc/fix_teacher_profile.py` (même pattern que `check_cols.py`, clés `service_role` de `build/sbc/keys.json`) :
   `UPDATE public.teachers SET profile_id = '62ad802d-…' WHERE full_name = 'M. Karim Lebbah';` (l'uuid exact du compte `teacher@lzacademy.dz` est dans le résumé de session ; le vérifier d'abord par `SELECT id, email FROM auth.users WHERE email='teacher@lzacademy.dz';`).
2. Exécuter, puis vérifier : `SELECT full_name, profile_id FROM teachers WHERE profile_id IS NOT NULL;` → 1 ligne.

**Preuve :** la requête de vérification renvoie M. Karim Lebbah avec son profile_id.

## Tâche 1 — Miroir Room étendu (DB v4) : entités, fetchers, sync

### 1a. `data/supabase/SupabaseApi.kt` — classes Remote + fetchers

Ajouter (même pattern que `fetchStudents` : GET, `AuthExpiredException` sur 401, `emptyList()` sinon, log `LZSync` concis) :

```kotlin
data class RemoteTeacher(remoteId, fullName, photoUrl?, description?, yearsOfExperience: Int,
    isFeatured: Boolean, acceptsMessages: Boolean, rotationPriority: Int, lastFeaturedDate: LocalDate?,
    isActive: Boolean, profileId: String?, subjects: List<String>)
// GET /rest/v1/teachers?select=id,full_name,photo_url,professional_description,years_of_experience,
//   is_featured_on_student_home,accepts_student_messages,featured_rotation_priority,last_featured_at,
//   is_active,profile_id,teacher_subjects(subjects(name))&limit=200

data class RemoteSubject(remoteId, code?, name)          // ?select=id,code,name&limit=100
data class RemoteGroup(remoteId, name, levelName?, teacherId?, capacity: Int)
// groups?select=id,name,capacity,teacher_id,levels(name)&limit=100
data class RemotePayment(remoteId, studentId, amountDzd: Double, method, status, reference?, dueOn: LocalDate?,
    paidAtIso: String?, createdAtIso: String?)
// payments?select=id,student_id,amount_dzd,method,status,reference,due_on,paid_at,created_at
//   &order=created_at.desc&limit=200
data class RemoteAnnouncement(remoteId, title, body, audience, levelId?, isPinned: Boolean,
    publishedAtIso: String?, expiresAtIso: String?, createdAtIso: String?)
// announcements?select=id,title,body,audience,level_id,group_id,is_pinned,published_at,expires_at,created_at
//   &order=published_at.desc&limit=100
data class RemoteMaterial(remoteId, title, description?, kind, storagePath?, sizeBytes: Long,
    subjectId?, groupName?, isPublished: Boolean, publishedAtIso?)
// materials?select=id,title,description,kind,storage_path,size_bytes,subject_id,is_published,published_at,
//   groups(name)&limit=100
data class RemoteSlot(remoteId, groupId, groupName?, weekday: Int, start: String, end: String, roomName?)
// group_schedule_slots?select=id,group_id,weekday,starts_at,ends_at,rooms(name),groups(name)&limit=200
data class RemoteSession(remoteId, groupId, groupName?, teacherId?, teacherName?, subjectName?, roomName?,
    startsAtIso, endsAtIso, status, topic?)
// sessions?select=id,group_id,teacher_id,subject_id,room_id,starts_at,ends_at,status,topic,
//   groups(name),subjects(name),rooms(name),teachers(full_name)&order=starts_at.desc&limit=200
data class RemoteAttendance(remoteId, sessionId, studentId, status, recordedAtIso)
// attendance?select=id,session_id,student_id,status,recorded_at&order=recorded_at.desc&limit=500
data class RemoteEnrollment(remoteId, studentId, groupId, joinedOn: LocalDate?, leftOn: LocalDate?)
// enrollments?select=id,student_id,group_id,joined_on,left_on&limit=1000
```

Ajouter aussi `createdAt` à `RemoteStudent` (+ `students?select=…,created_at`).

### 1b. `data/supabase/SyncEntities.kt` — 10 entités miroir + DAO

Chaque entité : `@Entity(tableName = "synced_xxx")`, `@PrimaryKey val remoteId: String`, colonnes TEXT/INTEGER/REAL mappées 1:1 des RemoteX. Liste :

`SyncedTeacherEntity` (fullName, photoUrl, description, yearsOfExperience, isFeatured, acceptsMessages, rotationPriority, lastFeaturedEpochDay: Long?, isActive, profileId, subjects: String CSV) ·
`SyncedSubjectEntity` (code, name) ·
`SyncedGroupEntity` (name, levelName, teacherId, capacity) ·
`SyncedPaymentEntity` (studentId, amountDzd: Double, method, status, reference, dueOn: String?, paidAtIso, createdAtIso) ·
`SyncedAnnouncementEntity` (title, body, audience, levelId, isPinned, publishedAtIso, expiresAtIso, createdAtIso) ·
`SyncedMaterialEntity` (title, description, kind, storagePath, sizeBytes: Long, subjectId, groupName, isPublished, publishedAtIso) ·
`SyncedSlotEntity` (groupId, groupName, weekday: Int, start, end, roomName) ·
`SyncedSessionEntity` (groupId, groupName, teacherId, teacherName, subjectName, roomName, startsAtIso, endsAtIso, status, topic) ·
`SyncedAttendanceEntity` (sessionId, studentId, status, recordedAtIso) ·
`SyncedEnrollmentEntity` (studentId, groupId, joinedOn: String?, leftOn: String?).

`SyncedStudentEntity` : ajouter `createdAt: String` (iso).
`SyncDao` : `@Upsert suspend fun upsertX(rows: List<SyncedXEntity>)` ×10 + `@Query("SELECT * FROM synced_xxx") fun observeX(): Flow<List<SyncedXEntity>>` ×10 + counts utiles (`paymentCount()`, `announcementCount()`… au besoin). Pas de contrainte FK (jointures faites en Kotlin, volumes faibles).

### 1c. `data/teachers/LZDatabase.kt` — v4

- `version = 4`, entités += les 10 nouvelles classes.
- `MIGRATION_3_4` :
  - `DROP TABLE IF EXISTS synced_students;` + recréation avec `createdAt TEXT NOT NULL` (+ index matricule) ;
  - `ALTER TABLE teachers ADD COLUMN profileId TEXT;` ;
  - `CREATE TABLE IF NOT EXISTS …` pour les 10 nouvelles tables (schéma Room exact : TEXT NOT NULL pour non-null, INTEGER pour Boolean/Int, REAL pour Double/Float).
- Exporter le schéma attendu si besoin pour debug (`exportSchema` reste à false).

### 1d. `repository/SupabaseSyncRepository.kt` — store étendu

- `data class RemoteSnapshot(students, subscriptions, teachers, subjects, groups, payments, announcements, materials, slots, sessions, attendance, enrollments)` (dans ce fichier).
- `fetchAndStore` : construire le snapshot avec les 12 fetchers (l'ordre n'importe pas, jointures en Kotlin) ; sur 401 → refresh → 1 retry (mécanisme existant conservé).
- `store(snapshot, userId)` :
  - upsert de tout ; `subjects` du teacher = CSV depuis l'embed `teacher_subjects(subjects(name))` ;
  - `lastFeaturedEpochDay` = `LocalDate.parse(last_featured_at)?.toEpochDay()` ;
  - upsert `teachers` **après** groups (mapping `groups`→compte par teacherId est fait dans la couche lecture, pas au store — donc rien à calculer ici) ;
  - `preferences.ownProfileId = userId` (nouvelle clé dans `AppPreferences`, comme `ownMatricule`) ;
  - log final : `"store terminé: 381 élèves, 381 abonnements, 21 enseignants, 12 paiements, …"`.
- `refresh()` : garde le throttle 15 min existant (condition `dao.studentCount() > 0` inchangée).

**Build de contrôle :** `.\gradlew.bat :app:compileDebugKotlin` (commande du résumé) — aucune UI touchée, tout doit compiler.

## Tâche 2 — `repository/DashboardComputer.kt` (calculs purs) + tests

Nouveau fichier, objet sans dépendance Android/Compose (testable JVM) :

```kotlin
object DashboardComputer {
    data class AdminNumbers(
        val activeStudents: Int, val teachers: Int, val groups: Int,
        val activeSubscriptions: Int, val exhaustedSubscriptions: Int,
        val pendingPayments: Int, val monthRevenue: Double, val prevMonthRevenue: Double,
        val monthlyRevenue: List<Double>,          // 6 mois, du plus ancien au plus récent
        val monthlyNewStudents: List<Int>,          // 6 mois (cumul total élèves), idem
        val studentsByLevel: List<Pair<String, Int>>, // trié décroissant, « Non affecté » inclus
        val recentPayments: List<PaymentRow>,       // 5 derniers, déjà triés
    )
    data class PaymentRow(val id: String, val studentName: String?, val amountDzd: Double,
        val status: String, val createdAtIso: String, val reference: String?)

    fun admin(students: List<SyncedStudentEntity>, subscriptions: List<SyncedSubscriptionEntity>,
        teachers: List<SyncedTeacherEntity>, groups: List<SyncedGroupEntity>,
        payments: List<SyncedPaymentEntity>, sessions: List<SyncedSessionEntity>,
        enrollments: List<SyncedEnrollmentEntity>, today: LocalDate): AdminNumbers

    data class StudentNumbers(val ownMatricule: String?, val firstName: String, val initials: String,
        val remaining: Int, val total: Int, val activeSubjects: Int,
        val nextLesson: NextLesson?,   // data class locale (subject, teacher, whenLabel, room)
        val lessonsByDay: Map<Int, List<Lesson>>, val materialsCount: Int)
    fun student(own: SyncedStudentEntity?, subscription: SyncedSubscriptionEntity?,
        subjects: List<SyncedSubjectEntity>, materials: List<SyncedMaterialEntity>,
        slots: List<SyncedSlotEntity>, sessions: List<SyncedSessionEntity>,
        enrollments: List<SyncedEnrollmentEntity>, now: ZonedDateTime): StudentNumbers
        // groupes de l'élève = enrollments où studentId == own.remoteId ;
        // nextLesson = 1re session (status='scheduled', starts_at > now) de ces groupes ;
        // lessonsByDay = slots de ces groupes (weekday 1..7) ;
        // activeSubjects = subjects distinctes référencées par les materials.

    data class TeacherNumbers(val name: String, val initials: String,
        val sessionsToday: Int, val groups: Int, val students: Int,
        val nextSession: NextSession?, val todaySessions: List<TodaySession>,
        val weeklyHours: List<Float>)   // 7 valeurs Lun..Dim
    fun teacher(teacher: SyncedTeacherEntity?, groups: List<SyncedGroupEntity>,
        sessions: List<SyncedSessionEntity>, enrollments: List<SyncedEnrollmentEntity>,
        attendance: List<SyncedAttendanceEntity>, now: ZonedDateTime): TeacherNumbers
        // sessions du teacher = sessions.teacherId == teacher.remoteId ; groupes = groups.teacherId == id ;
        // students = enrollments de ses groupes ; groupAttendance (taux) trop peu de données → liste vide.
}
```

Tests JVM `app/src/test/java/.../DashboardComputerTest.kt` (style des tests existants) :
1. `admin` : revenu du mois = somme des `paid` du mois ; pending comptés ; 1 abonnement épuisé détecté ; série 6 mois correcte (mois sans paiement = 0.0) ; recentPayments triés desc, max 5 ; `monthlyNewStudents` = cumul depuis `createdAt`.
2. `student` : sans inscription → `nextLesson = null`, `lessonsByDay` vide ; avec inscription → prochaine session et slots corrects.
3. `teacher` : sessions d'aujourd'hui et heures/semaine corrects ; teacher null → valeurs vides.

**Preuve :** `:app:testDebugUnitTest` vert (21 existants + nouveaux).

## Tâche 3 — Dashboards réels (repo + ViewModels + écrans)

### 3a. `repository/DashboardRepository.kt`

- Garder l'interface. Supprimer `DemoDashboardRepository`.
- Nouveau `class SupabaseDashboardRepository(private val database: LZDatabase, private val preferences: AppPreferences) : DashboardRepository` :
  - lit les flows `syncDao` (`.first()` dans chaque méthode suspend, ou combine) → passe à `DashboardComputer` → mappe vers `AdminDashboardData` / `StudentDashboardData` / `TeacherDashboardData` (icônes `Icons.Rounded.*` et couleurs `LZPalette` déjà utilisées par DemoData — mêmes choix visuels) ;
  - **admin** : headerStats = Élèves 381 / Enseignants 21 / Groupes 4 / Séances du jour ; revenue.amount formaté `"12 000 DA"` (même formatage que DemoData) ; delta `"+N vs mois précédent"` ou `"—"` si 0 ; kpis (abonnements actifs, paiements en attente, séances programmées) ; alerts : 1 alerte par abonnement épuisé (« actionLabel » ouvre le renouvellement existant) + 1 alerte paiements en attente ; periods = `listOf(1)` (année courante) ; enrollment = `mapOf(0 to ChartSeriesUi(mensualNewStudents, labels))` ; subjectDistribution = donut `studentsByLevel` (couleurs `LZPalette`) ; recentPayments → `PaymentUi` (join nom élève via `synced_students.remoteId`) ;
  - **student** : `own` via `preferences.ownMatricule` ; nextLesson nullable ; materials → `FileUi` (kind : pdf→Pdf, doc→Doc, slides→Slides, image→Image, sinon Pdf) ; announcements filtrées (`audience == "all" || "students"`, non expirées, `levelId == null || levelId == own.levelId`) → `AnnouncementUi` (icône `Campaign`/`Event` alternée, accent cyclé) ; `scheduleChange = null` ;
  - **teacher** : `teacher` via `preferences.ownProfileId` sur `SyncedTeacherEntity.profileId` ; `nextSession` nullable ; `groupAttendance = emptyList()` (donnée trop faible — écran doit le gérer) ; publications = materials du groupe de l'enseignant ;
  - si le miroir est vide (première lancée avant sync) → données vides cohérentes (« Élève » / 0 / listes vides), jamais de valeurs inventées.

### 3b. Modèles + écrans (états vides)

- `models/Models.kt` : `StudentDashboardData.nextLesson: NextLessonUi?` ; `TeacherDashboardData.nextSession: SessionUi?`.
- `ui/student/StudentDashboardScreen.kt` : si `nextLesson == null` → carte « Aucune donnée — Aucun cours programmé » (style `LZSectionHeader` existant) ; `lessonsByDay` vide → journée vide déjà gérée ? Vérifier et ajouter « Aucune séance » sinon.
- `ui/teacher/TeacherDashboardScreen.kt` : idem pour `nextSession` ; `groupAttendance` vide → section « Aucune donnée ».
- `ui/admin/AdminDashboardScreen.kt` : les listes (alerts, recentPayments) vides → texte « Aucune donnée » dans la section ; supprimer l'import `DemoData` (les 2 `@Preview` passent à `PreviewSamples`, tâche 7 — provisoirement les previews peuvent appeler le constructeur réel avec des listes factices du fichier previews) ; le message ligne 298 « Données de démonstration : aucun paiement réel. » devient « Source : base de données (synchronisée). » ; ligne 122 message quick-action conservé (honnête).

### 3c. ViewModels + NavHost

- `viewmodel/DashboardViewModels.kt` : les 3 VM deviennent `AndroidViewModel(application)` ; constructeur principal `(application)` → `SupabaseDashboardRepository(LZDatabase.get(application), AppPreferences(application))` ; supprimer les constructeurs secondaires sans argument et l'import `DemoDashboardRepository`.
- `navigation/LZNavHost.kt` : adapter les sites d'appel (instanciations actuelles sans argument → passer `application`, ou `viewModel()` avec factory par défaut d'AndroidViewModel — choisir le plus court qui compile).

**Vérification appareil (après tâche 8 seulement) :** dashboard admin affiche 381 / 21 / 4.

## Tâche 4 — Rapports + Communication réels

- `ui/admin/AdminReportsScreen.kt` :
  - ligne 70 `values = listOf(180f…320f)` → série réelle passée en paramètre (nouveau paramètre `growth: ChartSeriesUi` ou lecture du `AdminDashboardViewModel` existant — au choix le plus court) ; calculée par `DashboardComputer.admin().monthlyNewStudents` ;
  - ligne 176 sous-titre → `"… (${"381"} élèves)"` avec le compte réel ;
  - auditer le reste du fichier : toute valeur figée restante (montants, taux) est remplacée par une donnée du miroir ou un état « Aucune donnée ».
- `ui/admin/AdminCommunicationScreen.kt` :
  - lignes 76/80 : cibles calculées depuis le miroir — `"Tous les élèves (381)"`, puis par niveau réel (`"Élèves de 3AS Scientifique (4)"`…), `"Enseignants (21)"` ; `broadcastTarget` initialisé dynamiquement (chaîne vide puis `LaunchedEffect` sur la 1re valeur, pour éviter l'état figé) ;
  - le message d'avertissement SMS (honnête) est conservé.

## Tâche 5 — Annuaire enseignants réel + écrans planning réels

### 5a. Annuaire

- `repository/TeacherRepository.kt` : supprimer `seedIfNeeded`, `seeded`, `seedMutex`, l'import `TeacherSeed` ; `observeFeatured` n'appelle plus le seed.
- `viewmodel/TeachersViewModel.kt` : exposer `fun observeTeacherByProfile(profileId: String?): Flow<Teacher?>` (nouvelle requête DAO `WHERE profileId = :profileId`) ; ajouter `profileId` dans `TeacherEntity` + `toDomain` (mappé vers `Teacher.profileId` — ajouter le champ au modèle `models/Teacher.kt` ou garder interne, au plus court).
- `ui/teacher/TeacherProfileScreen.kt` (lignes 60/122/123) : remplacer `TeacherSeed.CURRENT_TEACHER_ID` par l'id résolu : `LaunchedEffect` lit `preferences.ownProfileId` → `TeachersViewModel.observeTeacherByProfile(...)` ; si null → écran affiche « Aucune donnée » (profil non lié).
- `ui/student/TeacherDirectoryScreen.kt` : la liste réelle vient déjà de Room (`teachers`) — après sync elle contient les 21 vrais ; vide → état « Aucun enseignant pour le moment » (ajouter si absent).

### 5b. Planning réels

- `ui/student/StudentPlanningScreen.kt` (l.73) : `DemoData.student(today, now)` → observer `StudentDashboardViewModel` (déjà réel après tâche 3) ; mêmes données `lessonsByDay`/week.
- `ui/teacher/TeacherPlanningScreen.kt` (l.66) : idem avec `TeacherDashboardViewModel`.
- Vérifier les autres `DemoData` restants (grep) : `AdminReportsScreen` etc. traités en tâche 4/7.

## Tâche 6 — Appel (roll-call) sur le roster réel

- `repository/AttendanceRepository.kt` :
  - supprimer `AttendanceSeed` (import + `seedStudents`/`seedSubscriptions` + valeurs par défaut `GROUP_LABEL`) ;
  - `students` = `synced_students` (actifs) mappés vers `Student` (id = matricule) ; `subscriptions` = `synced_subscriptions` mappées vers `Subscription` (id = matricule) ;
  - `observeRoster` = combine(students, records) sur les vrais élèves ; `groupLabel` = nom du groupe réel de la séance ou `"Séance du jour"` (constante du companion) ;
  - `record`/`undo`/`renew` inchangés en local (Room), `renew` délègue déjà au serveur via `SupabaseSyncRepository.renewSubscription`.
- `viewmodel/AttendanceViewModel.kt` : supprimer l'import `AttendanceSeed` ; `sessionLabel = "Séance du jour"` (ou valeur du repository) ; le roster devient 381 élèves — l'écran d'appel utilise déjà une `LazyColumn` (vérifier `key`) ; la recherche doit rester fluide (déjà virtualisée).
- `ui/admin/AdminSubscriptionsScreen.kt` (l.200) : supprimer la section « Espace de démonstration local » et son contenu (items de démo) — la vraie liste est déjà affichée au-dessus.
- **POST serveur (post-roster)** : tenter `POST /rest/v1/attendance` (`session_id`, `student_id`, `status`) dans `SupabaseApi.addAttendance(token, sessionId, studentId, status)` seulement si une session du jour résout pour le groupe ; échec silencieux (local conservé). Si `session_id` est obligatoire sans session résoluable → local uniquement (consigner la limitation dans le code).

## Tâche 7 — Suppression des fichiers de démo + previews

1. Déplacer `FrenchDates` (de `data/DemoData.kt`) vers `data/FrenchDates.kt` (adapter les imports, grep `FrenchDates`).
2. Supprimer : `data/DemoData.kt`, `data/attendance/AttendanceSeed.kt`, `data/teachers/TeacherSeed.kt`.
3. Créer `ui/previews/PreviewSamples.kt` : `val SampleTeachers: List<Teacher>` (2-3 enseignants factices) + `fun sampleStudentDashboard(...): StudentDashboardData` + `sampleTeacherDashboard()` + `sampleAdminDashboard()` — utilisés UNIQUEMENT dans les blocs `@Preview` (StudentDashboardScreen, TeacherDashboardScreen, AdminDashboardScreen, TeacherDirectoryScreen, TeacherContactScreen, StudentTeacherProfileScreen). Aucune référence runtime.
4. `grep DemoData|AttendanceSeed|TeacherSeed|DemoDashboardRepository` → 0 résultat hors `PreviewSamples.kt`.
5. `ui/auth/LoginScreen.kt` l.140 : « Comptes de démonstration » → « Comptes de l'académie ».

## Tâche 8 — Build, tests, vérification appareil

1. Build complet (commande du résumé avec JAVA_HOME 21 et JVM args) : `:app:assembleDebug :app:testDebugUnitTest` → BUILD SUCCESSFUL, tous les tests verts.
2. Installer : `adb install -r app-debug.apk` (appareil e25ffc230521, package `com.aistudio.lzacademy.bvdqxk`).
3. Scénario admin : login `admin@lzacademy.dz` / `LZ2026!` → dashboard admin affiche **381 élèves / 21 enseignants / 4 groupes** ; abonnement alerte « épuisé » présent ; paiements récents réels (6 000–15 000 DA) ; Rapports : sous-titre avec le compte réel ; Communication : cibles réelles.
4. Scénario élève : login `student@lzacademy.dz` → abonnement réel (36/48), annonces réelles, supports réels, « Aucun cours programmé » si aucune inscription (état vide honnête).
5. Scénario enseignant : login `teacher@lzacademy.dz` → profil M. Karim Lebbah réel, séances réelles.
6. Logs `adb logcat -s LZSync` : « store terminé: … » avec les 12 tables ; pas d'exception.
7. Roll-call : ouvrir l'appel enseignant → roster = vrais élèves (recherche LZ-0025 → BENKHALED Yacine) ; pointer un présent → visible localement.

## Hors périmètre (explicitement)

- Tables non miroirées : `broadcasts`, `teacher_messages`, `featured_teacher_*`, `announcement_reads`, `audit_log`, `packs`, `academic_years`, `academy_settings` (aucun écran ne les requiert aujourd'hui).
- Écritures serveur au-delà du renouvellement et du POST attendance optionnel (création d'annonces/paiements depuis l'app).
- Lecture des photos enseignants (initials avatars conservés).
