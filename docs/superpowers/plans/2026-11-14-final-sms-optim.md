# Sélection SMS manuelle + version finale Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Permettre à l'admin de choisir manuellement les destinataires d'un SMS puis d'ouvrir l'app Messages préremplie, et livrer un APK release R8 prêt pour des dizaines d'utilisateurs sur l'offre gratuite Supabase.

**Architecture:** Les destinataires viennent du miroir Room (instantané, hors ligne). Une couche Kotlin pure (`data/sms`) normalise les numéros, construit l'URI `smsto:` et découpe en lots ; l'écran admin ne fait que de l'affichage et un `startActivity`. Côté réseau, le snapshot passe en parallèle, le throttle monte à 60 min, la sonde de token disparaît. Aucune nouvelle permission, aucune migration de schéma.

**Tech Stack:** Kotlin, Jetpack Compose (Material 3), Room (KSP), Robolectric + JUnit4, Gradle (R8).

**Spec:** `docs/superpowers/specs/2026-11-14-final-sms-optim-design.md`

## Global Constraints

- Seule permission du manifeste : `android.permission.INTERNET`. Aucune permission `SEND_SMS`, aucun `ACTION_SEND` direct.
- Séparateur multi-destinataires : `;` dans l'URI `smsto:`.
- Extras : `Intent.EXTRA_TEXT` n'est pas utilisé ; le corps passe par `putExtra("sms_body", body)`.
- Lot de destinataires : **20** maximum par ouverture.
- Estimation d'affichage : **160 caractères** par SMS.
- Throttle de sync : **60 min** (`60 * 60 * 1000L`).
- `versionCode = 2`, `versionName = "1.1"`.
- Historique SMS local : **20 entrées** max, libellé « préparé », jamais « envoyé ».
- Pas de git dans ce projet : chaque tâche se termine par un build/test de vérification, pas par un commit.
- Ne jamais lancer de tap/text adb sans accord explicite de l'utilisateur.

## Review Focus

- Un numéro avec indicatif (`+213 555 12 34 56`) ou espaces/points doit produire un destinataire correct, pas deux numéros tronqués.
- Un élève avec un téléphone invalide (`"12"`, `"abc"`, `"—"`) doit être non sélectionnable et ne doit jamais partir dans l'intent.
- Un miroir vide ou une app jamais synchronisée doit afficher un état vide honnête et un bouton désactivé, pas « 0 destinataire » trompeur ni crash.
- Un appareil sans app SMS (tablette) doit afficher un message clair au lieu d'une `ActivityNotFoundException` non gérée.
- Un token expiré pendant une action admin doit déclencher un seul refresh puis un seul retry, sans perdre l'action ni boucler.
- Le build R8 doit démarrer et synchroniser normalement : aucune classe conservée par réflexion ne doit être retirée.

---

### Task 1: `SmsComposer` — normalisation, URI, lots

**Files:**
- Create: `app/src/main/java/com/lzacademy/app/data/sms/SmsComposer.kt`
- Test: `app/src/test/java/com/lzacademy/app/SmsComposerTest.kt`

**Interfaces:**
- Produces:
  - `object SmsComposer { fun normalizePhone(raw: String?): String?; fun smsUri(phones: List<String>): String; fun batches(phones: List<String>, size: Int = 20): List<List<String>> }`
  - `normalizePhone` retourne null si moins de 6 chiffres après nettoyage.
  - `smsUri(listOf("0555123456","0666778899")) == "smsto:0555123456;0666778899"`.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.lzacademy.app

import com.lzacademy.app.data.sms.SmsComposer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SmsComposerTest {

    @Test
    fun `normalise espaces points tirets et parentheses`() {
        assertEquals("0555123456", SmsComposer.normalizePhone("0555 12.34-56"))
        assertEquals("0555123456", SmsComposer.normalizePhone("(0) 555 12 34 56"))
    }

    @Test
    fun `conserve l indicatif international`() {
        assertEquals("+213555123456", SmsComposer.normalizePhone("+213 555 12 34 56"))
    }

    @Test
    fun `refuse un numero trop court ou vide`() {
        assertNull(SmsComposer.normalizePhone(null))
        assertNull(SmsComposer.normalizePhone(""))
        assertNull(SmsComposer.normalizePhone("12"))
        assertNull(SmsComposer.normalizePhone("abc"))
        assertNull(SmsComposer.normalizePhone("+"))
    }

    @Test
    fun `construit l uri smsto dans l ordre recu`() {
        assertEquals("smsto:", SmsComposer.smsUri(emptyList()))
        assertEquals("smsto:0555123456", SmsComposer.smsUri(listOf("0555123456")))
        assertEquals(
            "smsto:0555123456;+213666778899",
            SmsComposer.smsUri(listOf("0555123456", "+213666778899")),
        )
    }

    @Test
    fun `decoupe en lots de vingt`() {
        val phones = (1..45).map { "05550000" + it.toString().padStart(2, '0') }
        val lots = SmsComposer.batches(phones)
        assertEquals(listOf(20, 20, 5), lots.map { it.size })
        assertEquals(phones, lots.flatten())
        assertEquals(emptyList<List<String>>(), SmsComposer.batches(emptyList()))
        assertEquals(1, SmsComposer.batches(phones.take(20)).size)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.lzacademy.app.SmsComposerTest" --console=plain`
Expected: FAIL — `Unresolved reference: SmsComposer`.

- [ ] **Step 3: Implement `SmsComposer`**

`normalizePhone` : `trim`, supprimer tout sauf `+` et chiffres, retirer un `+` non initial, compter les chiffres ; si `digits < 6` → null, sinon reconstruire `(if (leadingPlus) "+" else "") + digits`.
`smsUri` : `"smsto:" + phones.joinToString(";")`.
`batches` : `if (size <= 0) emptyList() else phones.chunked(size)`.

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.lzacademy.app.SmsComposerTest" --console=plain`
Expected: PASS (5 tests).

---

### Task 2: `SmsRecipients` — liste, recherche, niveaux

**Files:**
- Create: `app/src/main/java/com/lzacademy/app/data/sms/SmsRecipients.kt`
- Test: `app/src/test/java/com/lzacademy/app/SmsRecipientsTest.kt`

**Interfaces:**
- Produces:
  ```kotlin
  data class SmsRecipient(
      val matricule: String,
      val fullName: String,
      val phone: String?,          // déjà normalisé, null si invalide/absent
      val levelName: String,       // "Non affecté" si null
  ) { val canReceive: Boolean get() = phone != null }

  object SmsRecipients {
      fun fromStudents(students: List<SyncedStudentEntity>): List<SmsRecipient>
      fun filter(recipients: List<SmsRecipient>, query: String, level: String?): List<SmsRecipient>
      fun levels(recipients: List<SmsRecipient>): List<String>
  }
  ```
- `fromStudents` ne garde que `isActive`, trie par `fullName`, normalise via `SmsComposer.normalizePhone`.
- `filter` : `level == null` = tous ; query insensible à la casse sur `fullName`, `matricule` et `phone`.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.lzacademy.app

import com.lzacademy.app.data.sms.SmsRecipients
import com.lzacademy.app.data.supabase.SyncedStudentEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsRecipientsTest {

    private fun student(
        matricule: String,
        name: String,
        phone: String?,
        level: String? = "3AM",
        active: Boolean = true,
    ) = SyncedStudentEntity(
        remoteId = "r_$matricule",
        matricule = matricule,
        fullName = name,
        phone = phone,
        levelName = level,
        avatarPath = null,
        isActive = active,
        profileId = null,
        createdAt = "2026-01-01T00:00:00Z",
    )

    @Test
    fun `ne garde que les eleves actifs et normalise les numeros`() {
        val rows = listOf(
            student("LZ-1", "Amine Bensalem", "0555 12 34 56"),
            student("LZ-2", "Inactif Test", "0555000000", active = false),
            student("LZ-3", "Sans Numero", null, level = null),
        )
        val out = SmsRecipients.fromStudents(rows)
        assertEquals(listOf("LZ-1", "LZ-3"), out.map { it.matricule })
        assertEquals("0555123456", out[0].phone)
        assertTrue(out[0].canReceive)
        assertNull(out[1].phone)
        assertEquals("Non affecté", out[1].levelName)
    }

    @Test
    fun `un miroir vide ne produit aucun destinataire`() {
        val out = SmsRecipients.fromStudents(emptyList())
        assertTrue(out.isEmpty())
        assertTrue(SmsRecipients.levels(out).isEmpty())
    }

    @Test
    fun `filtre par recherche et par niveau`() {
        val out = SmsRecipients.fromStudents(
            listOf(
                student("LZ-1", "Amine Bensalem", "0555123456", "3AM"),
                student("LZ-2", "Nadia Cherif", "0666778899", "2AS"),
            ),
        )
        assertEquals(listOf("LZ-1"), SmsRecipients.filter(out, "amine", null).map { it.matricule })
        assertEquals(listOf("LZ-2"), SmsRecipients.filter(out, "0666", null).map { it.matricule })
        assertEquals(listOf("LZ-2"), SmsRecipients.filter(out, "", "2AS").map { it.matricule })
        assertEquals(2, SmsRecipients.filter(out, "", null).size)
        assertEquals(listOf("2AS", "3AM"), SmsRecipients.levels(out))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.lzacademy.app.SmsRecipientsTest" --console=plain`
Expected: FAIL — `Unresolved reference: SmsRecipients`.

- [ ] **Step 3: Implement `SmsRecipients.kt`**

`levels` : `map { it.levelName }.distinct().sorted()` (tri alphabétique, « Non affecté » en dernier via `sortedWith(compareBy({ it == "Non affecté" }, { it }))`).

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.lzacademy.app.SmsRecipientsTest" --console=plain`
Expected: PASS (3 tests).

---

### Task 3: `SmsHistoryStore` — historique local persistant

**Files:**
- Create: `app/src/main/java/com/lzacademy/app/data/preferences/SmsHistoryStore.kt`
- Test: `app/src/test/java/com/lzacademy/app/SmsHistoryStoreTest.kt` (Robolectric)

**Interfaces:**
- Produces:
  ```kotlin
  data class PreparedSms(
      val id: String,
      val atMillis: Long,
      val recipientCount: Int,
      val preview: String,   // 80 premiers caractères du corps
  )

  class SmsHistoryStore(context: Context) {
      fun load(): List<PreparedSms>   // plus récent d'abord
      fun add(entry: PreparedSms)     // récents d'abord, 20 max
  }
  ```
- Clé SharedPreferences : `"prepared_sms_history"`, valeur : `JSONArray` d'objets `{id, atMillis, recipientCount, preview}`.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.lzacademy.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.lzacademy.app.data.preferences.PreparedSms
import com.lzacademy.app.data.preferences.SmsHistoryStore
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SmsHistoryStoreTest {

    @Test
    fun `conserve les vingt derniers du plus recent au plus ancien`() {
        val store = SmsHistoryStore(ApplicationProvider.getApplicationContext<Context>())
        (1..25).forEach {
            store.add(PreparedSms("id$it", it.toLong(), it, "message $it"))
        }
        val out = store.load()
        assertEquals(20, out.size)
        assertEquals("id25", out.first().id)
        assertEquals("id6", out.last().id)
        assertEquals(25, out.first().recipientCount)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.lzacademy.app.SmsHistoryStoreTest" --console=plain`
Expected: FAIL — unresolved references.

- [ ] **Step 3: Implement `SmsHistoryStore`**

Lecture tolérante : entrée JSON invalide ignorée (`runCatching`), jamais d'exception remontée. Écriture `apply()`.

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.lzacademy.app.SmsHistoryStoreTest" --console=plain`
Expected: PASS.

---

### Task 4: `LZMultilineInput` — champ message

**Files:**
- Modify: `app/src/main/java/com/lzacademy/app/components/LZInputs.kt` (après `LZTextInput`, ligne ~133)

**Interfaces:**
- Produces: `@Composable fun LZMultilineInput(value: String, onValueChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier, minHeight: Dp = 120.dp)`
- Mêmes couleurs/bordure/placeholder que `LZTextInput`, mais `Row` remplacé par un `Box` avec `heightIn(min = minHeight)`, `verticalAlignment = Top`, `singleLine = false`.

- [ ] **Step 1: Implement the composable**

Réutiliser `animateColorAsState` sur `focused`/`error` comme `LZTextInput` ; placeholder aligné en haut (`Alignment.TopStart`), `inner()` remplissant la largeur (`fillMaxWidth`).

- [ ] **Step 2: Verify compilation**

Run: `.\gradlew.bat :app:compileDebugKotlin --console=plain`
Expected: `BUILD SUCCESSFUL`.

---

### Task 5: ViewModel — exposer les destinataires

**Files:**
- Modify: `app/src/main/java/com/lzacademy/app/viewmodel/AdminReportsViewModel.kt`

**Interfaces:**
- Consumes: `SmsRecipients.fromStudents`, `SmsRecipient`.
- Produces: `val recipients: StateFlow<List<SmsRecipient>>` sur `AdminReportsViewModel` (liste vide tant que le miroir est vide).

- [ ] **Step 1: Add the flow**

Dans `init` existant, ajouter :
```kotlin
val recipients: StateFlow<List<SmsRecipient>> = dao.observeStudents()
    .map { SmsRecipients.fromStudents(it) }
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
```
(où `dao = database.syncDao()` est hissé en propriété) ; importer `kotlinx.coroutines.flow.map` et `SharingStarted`.

- [ ] **Step 2: Verify compilation**

Run: `.\gradlew.bat :app:compileDebugKotlin --console=plain`
Expected: `BUILD SUCCESSFUL`.

---

### Task 6: Écran Communication — sélection + ouverture Messages

**Files:**
- Modify: `app/src/main/java/com/lzacademy/app/ui/admin/AdminCommunicationScreen.kt` (réécriture complète, `BroadcastMessage` supprimé)
- Test: `app/src/test/java/com/lzacademy/app/AdminSmsContentTest.kt`

**Interfaces:**
- Consumes: `viewModel.recipients`, `SmsComposer`, `SmsRecipients`, `SmsHistoryStore`, `PreparedSms`, `LZMultilineInput`.
- Produces:
  - `@Composable fun AdminCommunicationScreen(onNavigateBack, onShowMessage, onAvatarClick, modifier, viewModel)` — conserve le `testTag("admin_communication_screen")` et fait le lien avec le VM, le `context` et `SmsHistoryStore`.
  - `@Composable fun AdminSmsContent(recipients: List<SmsRecipient>, body: String, onBodyChange: (String) -> Unit, query: String, onQueryChange: (String) -> Unit, level: String?, onLevelChange: (String?) -> Unit, selected: Set<String>, onToggle: (String) -> Unit, onSelectAll: (Boolean) -> Unit, history: List<PreparedSms>, onOpenMessages: () -> Unit, modifier: Modifier = Modifier)` — **sans état Android ni VM**, donc testable en Compose.

Contenu exact :
1. Header gradient admin inchangé (`LZGradientHeader` + `LZGreetingRow`, titre « Centre de Communication », sous-titre « Envoi de SMS • Administration »).
2. **Carte message** : `LZMultilineInput` (placeholder « Corps du SMS… »), compteur `"${body.length} caractères • ${parts} SMS"` avec `parts = max(1, (body.length + 159) / 160)`, 3 modèles rapides (`LZFilterChip`) qui remplissent le corps.
3. **Carte destinataires** :
   - `LZTextInput` recherche (`leadingIcon = Icons.Rounded.Search`, placeholder « Nom, matricule ou téléphone »).
   - `LazyRow` de `LZFilterChip` : « Tous » + `SmsRecipients.levels(...)`.
   - Ligne de contrôle : « N sélectionnés / M avec numéro » + `LZLinkButton` « Tout sélectionner » / « Tout désélectionner » agissant sur la liste **filtrée**.
   - `LZEmptyState` si `recipients` vide : « Aucun élève synchronisé. Ouvrez l'app connecté pour synchroniser. »
   - Ligne par élève (`items(filtered, key = { it.matricule })`) : `Checkbox` Material3, nom, `"${levelName} • ${phone ?: "sans numéro"}"` ; `enabled = it.canReceive`, texte grisé et mention « sans numéro » si non sélectionnable.
4. **Carte envoi** :
   - Si `selected.size > 20` : bandeau `LZStatusPill("${selected.size} destinataires", warning…)` + texte « Ouverture par lots de 20 (limite de certaines app Messages) » et compteur « Lot ${lotIndex + 1}/${lots.size} ».
   - `LZPrimaryButton(text = "Ouvrir Messages ($n)", enabled = n > 0 && body.isNotBlank(), leadingIcon = Icons.Rounded.Send)`.
   - Action : prendre `phones = SmsComposer.batches(selectedPhones, 20)` ; si `size > 20` n'ouvrir que `lots[lotIndex]` puis incrémenter/cycler ; construire `Intent(Intent.ACTION_SENDTO, Uri.parse(SmsComposer.smsUri(phones)))` + `putExtra("sms_body", body)`, `context.startActivity(intent)` dans `runCatching` → `onShowMessage("Aucune application SMS sur cet appareil.")` en cas d'échec ; sur succès `historyStore.add(PreparedSms(...))` et recharger l'historique local.
   - Reset `lotIndex = 0` quand `selected` ou `body` changent.
5. **Historique** : liste des `PreparedSms` (max 20) — titre = extrait, sous-titre `"${recipientCount} destinataires • ${date}"`, pastille `LZStatusPill("Préparé", warning…)`. Aucune notion de « taux de succès », aucun canal Push/Email.
6. Supprimer : `BroadcastMessage`, `broadcastTitle`, `broadcastChannel`, `channels`, `successRate`, et l'import inutilisé `Icons.Rounded.Campaign` s'il ne sert plus.

- [ ] **Step 1: Write the failing Compose test**

```kotlin
package com.lzacademy.app

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.lzacademy.app.ui.admin.AdminSmsContent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AdminSmsContentTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `miroir vide affiche un etat vide et desactive l envoi`() {
        compose.setContent {
            AdminSmsContent(
                recipients = emptyList(),
                body = "Rappel: cours maintenu",
                onBodyChange = {},
                query = "",
                onQueryChange = {},
                level = null,
                onLevelChange = {},
                selected = emptySet(),
                onToggle = {},
                onSelectAll = {},
                history = emptyList(),
                onOpenMessages = {},
            )
        }
        compose.onNodeWithText("Aucun élève synchronisé").assertExists()
        compose.onNodeWithText("Ouvrir Messages (0)").assertIsNotEnabled()
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.lzacademy.app.AdminSmsContentTest" --console=plain`
Expected: FAIL — `Unresolved reference: AdminSmsContent`.

- [ ] **Step 3: Rewrite the screen**

Règles de style : garder `LZCard`, `LZText`, `LZ.type.*`, `LZ.colors.*` ; aucun texte en dur en anglais ; aucun tap simulé. La carte « envoi » et le badge de lot vivent dans `AdminSmsContent` ; `AdminCommunicationScreen` ne calcule que `filtered`, `selectedPhones`, `lots` et lance l'intent.

- [ ] **Step 4: Verify the test passes**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.lzacademy.app.AdminSmsContentTest" --console=plain`
Expected: PASS.

- [ ] **Step 5: Verify compilation and launch**

Run: `.\gradlew.bat :app:compileDebugKotlin :app:testDebugUnitTest --tests "com.lzacademy.app.AppLaunchTest" --console=plain`
Expected: `BUILD SUCCESSFUL`, PASS.

---

### Task 7: Réseau — snapshot parallèle + throttle 60 min

**Files:**
- Modify: `app/src/main/java/com/lzacademy/app/repository/SupabaseSyncRepository.kt:188-202, 398`
- Test: `app/src/test/java/com/lzacademy/app/SyncThrottleTest.kt`

**Interfaces:**
- Produces: `internal fun shouldSync(lastSyncAtMillis: Long, nowMillis: Long, hasMirror: Boolean): Boolean` dans le `companion object` :
  `nowMillis - lastSyncAtMillis >= MIN_SYNC_INTERVAL_MS || !hasMirror`
- Consumes: `MIN_SYNC_INTERVAL_MS = 60 * 60 * 1000L`.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.lzacademy.app

import com.lzacademy.app.repository.SupabaseSyncRepository
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncThrottleTest {

    @Test
    fun `saute la sync si moins de soixante minutes et miroir present`() {
        assertFalse(SupabaseSyncRepository.shouldSync(0L, 59 * 60 * 1000L, true))
    }

    @Test
    fun `synchronise apres soixante minutes`() {
        assertTrue(SupabaseSyncRepository.shouldSync(0L, 60 * 60 * 1000L, true))
    }

    @Test
    fun `synchronise toujours si le miroir est vide`() {
        assertTrue(SupabaseSyncRepository.shouldSync(0L, 1L, false))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.lzacademy.app.SyncThrottleTest" --console=plain`
Expected: FAIL — `Unresolved reference: shouldSync`.

- [ ] **Step 3: Implement**

1. Ajouter `shouldSync` au companion et l'utiliser dans `refresh()` à la place de la condition inline.
2. `MIN_SYNC_INTERVAL_MS = 60 * 60 * 1000L`.
3. `snapshot()` :
```kotlin
private suspend fun snapshot(token: String): RemoteSnapshot = coroutineScope {
    val students = async { api.fetchStudents(token) }
    val subscriptions = async { api.fetchSubscriptions(token) }
    val teachers = async { api.fetchTeachers(token) }
    val subjects = async { api.fetchSubjects(token) }
    val groups = async { api.fetchGroups(token) }
    val payments = async { api.fetchPayments(token) }
    val announcements = async { api.fetchAnnouncements(token) }
    val materials = async { api.fetchMaterials(token) }
    val slots = async { api.fetchSlots(token) }
    val sessions = async { api.fetchSessions(token) }
    val attendance = async { api.fetchAttendance(token) }
    val enrollments = async { api.fetchEnrollments(token) }
    RemoteSnapshot(
        students = students.await(), subscriptions = subscriptions.await(),
        teachers = teachers.await(), subjects = subjects.await(),
        groups = groups.await(), payments = payments.await(),
        announcements = announcements.await(), materials = materials.await(),
        slots = slots.await(), sessions = sessions.await(),
        attendance = attendance.await(), enrollments = enrollments.await(),
    )
}
```
Imports : `kotlinx.coroutines.async`, `kotlinx.coroutines.coroutineScope`.

- [ ] **Step 4: Run tests**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.lzacademy.app.SyncThrottleTest" --console=plain`
Expected: PASS (3 tests).

---

### Task 8: Écran Gestion — plus de sonde, chargement parallèle

**Files:**
- Modify: `app/src/main/java/com/lzacademy/app/viewmodel/AdminManageViewModel.kt:69-124`

**Interfaces:**
- Produces: `fun load(force: Boolean = false)` ; `private suspend fun loadData(token: String): ManageData` parallèle ; `private suspend fun freshToken(): String?` sans appel réseau.
- Consumes: `api.refreshSession(refreshToken)` existant.

- [ ] **Step 1: Implement**

1. `load(force: Boolean = false)` : si `!force && _state.value is UiState.Success && now - lastLoadedAt < 60_000` → `return` ; sinon poser `lastLoadedAt = System.currentTimeMillis()` après succès.
2. `freshToken()` : retourner simplement `preferences.supabaseToken` (plus d'appel `fetchLevels`).
3. Gérer l'expiration une seule fois : envelopper `loadData` dans
```kotlin
private suspend fun loadDataWithRefresh(): ManageData {
    val token = preferences.supabaseToken ?: error("Non connecté")
    return try {
        loadData(token)
    } catch (e: AuthExpiredException) {
        val refreshed = preferences.supabaseRefreshToken?.let { api.refreshSession(it) } ?: throw e
        preferences.supabaseToken = refreshed.accessToken
        preferences.supabaseRefreshToken = refreshed.refreshToken.takeIf { it.isNotBlank() }
        loadData(refreshed.accessToken)
    }
}
```
4. `loadData` : `coroutineScope` + `async` pour les 12 fetch (dont `fetchEnrollments` réutilisé pour `enrollmentCountByGroup` et `enrollments`).

- [ ] **Step 2: Verify compilation**

Run: `.\gradlew.bat :app:compileDebugKotlin --console=plain`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Manual check of the single-retry path**

Token expiré (attendre > 1 h ou vider `supabase_access_token`) : ouvrir Gestion une fois et vérifier dans Logcat **une seule** ligne `token expiré — tentative de refresh`, puis un chargement réussi. Aucune boucle, aucune action perdue.

---

### Task 9: Build release — R8, version, signature

**Files:**
- Modify: `app/build.gradle.kts:19-52`
- Modify (si nécessaire): `app/proguard-rules.pro`

- [ ] **Step 1: Version + R8**

`versionCode = 2`, `versionName = "1.1"`, dans `release` : `isMinifyEnabled = true`, `isShrinkResources = true`.
Signature : conserver la ligne AI Studio et remplacer l'affectation par
```kotlin
signingConfig = signingConfigs.getByName(
    if (System.getenv("KEYSTORE_PATH") != null) "release" else "debugConfig"
)
```
(commenter en une ligne : keystore debug en repli pour que la mise à jour s'installe par-dessus l'app existante).

- [ ] **Step 2: Build release**

Run: `.\gradlew.bat :app:assembleRelease --console=plain`
Expected: `BUILD SUCCESSFUL`, APK dans `app/build/outputs/apk/release/`.

- [ ] **Step 3: Smoke test R8 (après accord de l'utilisateur pour les commandes adb)**

Installer `app-release.apk` avec `adb -s e25ffc230521 install -r <apk>`, lancer l'app, vérifier dashboard + ouverture de l'écran Communication. En cas de crash (`ClassNotFoundException`/`NoSuchMethodError`), ajouter la règle `-keep` minimale correspondante dans `proguard-rules.pro` puis reconstruire. Ne jamais utiliser `adb shell input tap/text` sans accord explicite.

- [ ] **Step 4: Verify tests still pass in release-friendly mode**

Run: `.\gradlew.bat :app:testDebugUnitTest --console=plain`
Expected: `BUILD SUCCESSFUL`, 0 failure.

---

### Task 10: Hygiène base — index manquant

**Files:** aucun fichier app ; migration SQL via MCP (`execute_sql`).

- [ ] **Step 1: Add the index**

```sql
create index if not exists teacher_payments_teacher_id_idx
  on public.teacher_payments (teacher_id);
```

- [ ] **Step 2: Verify**

`get_advisors(type = "performance")` → plus de lint `missing_index` sur `teacher_payments`. Ne pas toucher aux policies RLS ni à `get_featured_teachers` dans cette tâche.

---

### Task 11: Nettoyage final + vérification complète

**Files:**
- Modify: `app/src/main/java/com/lzacademy/app/data/supabase/SupabaseApi.kt` (logs)
- Modify: `docs/superpowers/ledgers/2026-11-13-real-data-only-progress.md`

- [ ] **Step 1: Nettoyer les logs de données personnelles**

Dans les fetchers, remplacer les logs qui contiennent `response.text.take(120)` par `"students http=${response.code} bytes=${response.text.length}"` (idem pour élève/enseignant/paiements). Aucun nom d'élève dans Logcat.

- [ ] **Step 2: Vérifier les états vides**

Communication : miroir vide → `LZEmptyState` + bouton désactivé. Gestion : erreur réseau → message existant + bouton « Réessayer » qui appelle `load(force = true)`.

- [ ] **Step 3: Full verification**

Run: `.\gradlew.bat :app:assembleDebug :app:assembleRelease :app:testDebugUnitTest --console=plain`
Expected: `BUILD SUCCESSFUL`, tous les tests verts.

- [ ] **Step 4: Update the ledger**

Ajouter une section « 2026-11-14 — Sélection SMS + version finale » : ce qui est livré, les valeurs mesurées (322 Ko / ~30 Ko gzip), la raison de l'absence d'ETag, les commandes de build et le chemin de l'APK release.
