# Bordure de progression sur la photo de profil élève — plan d'implémentation

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** La photo de profil de l'élève est entourée de 4 arcs (un par séance du pack réel) qui se remplissent à mesure que les séances sont consommées, et aucun écran n'affiche plus un pack de 48 séances.

**Architecture:** Une fonction pure calcule les segments (`SubscriptionRing`), un composable `LZAvatarProgressRing` les dessine autour de n'importe quel avatar, l'écran Profil l'utilise. En parallèle, `hydrateSubscriptions()` réaligne le compteur local sur le total réel du miroir quand le pack change.

**Tech Stack:** Kotlin, Jetpack Compose (Canvas), Room, JUnit.

**Spec:** `docs/superpowers/specs/2026-11-14-profile-avatar-progress-ring-design.md`

## Global Constraints

- Pack réel = 4 séances (`sessions_total = 4` en base, 381/381 lignes). Aucun écran ne doit afficher 48.
- Aucune information portée par la couleur seule : le libellé « N/M séances » accompagne toujours le cercle.
- Aucun nouveau dataset inventé : si le total est 0 ou inconnu, repli sur l'anneau simple (pas de faux segments).
- Français pour l'UI, anglais pour les commentaires et les noms de code.
- Pas de git : les tâches se clôturent par la commande de test, le ledger est tenu dans `docs/superpowers/ledgers/`.

## Review Focus

- Un élève avec `remaining > total` (compteur incohérent) : le cercle doit être plein, jamais un arc négatif.
- Un total supérieur à 12 : les segments restent lisibles (épaisseur minimale), pas de beignet.
- `total = 0` (aucun abonnement) : anneau simple, libellé absent, pas de division par zéro.
- Le compteur local d'un élève déjà à 2 appels sur un ancien pack de 48 : après réalignement, il repart de 4 sans planter.
- Rotation de l'écran : le remplissage animé ne rejoue pas indéfiniment et l'état affiché reste le même.

---

### Task 1: Règle pure des segments

**Files:**
- Create: `app/src/main/java/com/lzacademy/app/components/SubscriptionRing.kt`
- Test: `app/src/test/java/com/lzacademy/app/SubscriptionRingTest.kt`

**Interfaces:**
- Produces: `data class RingSegments(val total: Int, val filled: Int, val empty: Int, val hasSegments: Boolean)` et `fun ringSegments(remaining: Int, total: Int): RingSegments` dans `com.lzacademy.app.components`.

- [ ] **Step 1: Écrire le test qui échoue**

```kotlin
class SubscriptionRingTest {
    @Test fun `quatre seances dont deux faites donnent deux segments pleins`() {
        assertEquals(RingSegments(4, 2, 2, true), ringSegments(remaining = 2, total = 4))
    }
    @Test fun `remaining negatif ou superieur au total est borne`() {
        assertEquals(RingSegments(4, 4, 0, true), ringSegments(remaining = -3, total = 4))
        assertEquals(RingSegments(4, 0, 4, true), ringSegments(remaining = 9, total = 4))
    }
    @Test fun `total nul ou negatif ne dessine aucun segment`() {
        assertEquals(RingSegments(0, 0, 0, false), ringSegments(remaining = 0, total = 0))
        assertFalse(ringSegments(remaining = 2, total = -1).hasSegments)
    }
    @Test fun `au dela de douze seances le total est plafonne a douze`() {
        assertEquals(12, ringSegments(remaining = 20, total = 20).total)
    }
}
```

- [ ] **Step 2: Lancer le test, vérifier l'échec**

Run: `.\gradlew.bat :app:testDebugUnitTest --tests "com.lzacademy.app.SubscriptionRingTest" --console=plain --no-parallel --max-workers=2 "-Dorg.gradle.jvmargs=-Xmx3g -Dfile.encoding=UTF-8"`
Expected: FAIL, `Unresolved reference 'ringSegments'`.

- [ ] **Step 3: Implémenter `ringSegments`**

`total` borné à `0..12` ; `remaining` borné à `0..total` ; `filled = total - remaining` ; `hasSegments = total > 0`.

- [ ] **Step 4: Lancer le test, vérifier le succès**

Run: la commande du Step 2. Expected: BUILD SUCCESSFUL, 4 tests.

---

### Task 2: Anneau dessiné autour de l'avatar

**Files:**
- Modify: `app/src/main/java/com/lzacademy/app/components/LZProgress.kt` (ajout du composable)

**Interfaces:**
- Consumes: `ringSegments` (Task 1).
- Produces: `@Composable fun LZAvatarProgressRing(remaining: Int, total: Int, modifier: Modifier = Modifier, ringWidth: Dp = 4.dp, content: @Composable () -> Unit)`.

- [ ] **Step 1: Implémenter le composable**

`Box` de taille `content` + marge de `ringWidth + 3.dp` ; `Canvas` qui dessine, pour `i in 0 until total`, un arc de `(360f / total) - gap` degrés à partir de `-90f + i * (360f / total)`, couleur `LZ.colors.primary` pour `i < filled`, sinon `LZ.colors.border` à 60 % d'alpha ; `Stroke(width = ringWidth, cap = StrokeCap.Round)`. `animateFloatAsState` 0→1 sur 600 ms pour le balayage d'entrée. `if (!segments.hasSegments)` → pas de Canvas (l'anneau blanc existant de `LZAvatar` reste visible).

- [ ] **Step 2: Vérifier la compilation**

Run: `.\gradlew.bat :app:compileDebugKotlin --console=plain --no-parallel --max-workers=2 "-Dorg.gradle.jvmargs=-Xmx3g -Dfile.encoding=UTF-8"`
Expected: BUILD SUCCESSFUL.

---

### Task 3: Intégration dans l'écran Profil élève

**Files:**
- Modify: `app/src/main/java/com/lzacademy/app/ui/student/StudentProfileScreen.kt:221-226`

**Interfaces:**
- Consumes: `LZAvatarProgressRing` (Task 2), `remainingSessions` / `totalSessions` déjà passés à `StudentProfileScreen`.

- [ ] **Step 1: Remplacer l'avatar nu de la carte d'identité**

`LZAvatarProgressRing(remaining = remainingSessions, total = totalSessions) { LZAvatar(initials = …, size = 84.dp, photo = photo) }` puis, sous la photo, `LZText("$remainingSessions/$totalSessions séances", style = LZ.type.label)` en `textSecondary` (masqué si `totalSessions <= 0`). L'anneau blanc statique (`ring = true`) disparaît de cet avatar pour ne pas doubler la bordure.

- [ ] **Step 2: Vérifier la compilation + tests existants**

Run: `.\gradlew.bat :app:testDebugUnitTest --console=plain --no-parallel --max-workers=2 "-Dorg.gradle.jvmargs=-Xmx3g -Dfile.encoding=UTF-8"`
Expected: BUILD SUCCESSFUL, 0 échec.

---

### Task 4: Réalignement du compteur local sur le pack réel

**Files:**
- Modify: `app/src/main/java/com/lzacademy/app/repository/AttendanceRepository.kt:72-94`
- Test: `app/src/test/java/com/lzacademy/app/SubscriptionRealignTest.kt`

**Interfaces:**
- Produces: `fun realignedCounter(localTotal: Int, localRemaining: Int, remoteTotal: Int, remoteRemaining: Int): Pair<Int, Int>?` dans `com.lzacademy.app.repository`, retourne `null` quand rien ne change.

- [ ] **Step 1: Écrire le test qui échoue**

```kotlin
class SubscriptionRealignTest {
    @Test fun `un ancien pack de 48 est ramene au pack reel de 4`() {
        assertEquals(4 to 4, realignedCounter(localTotal = 48, localRemaining = 36, remoteTotal = 4, remoteRemaining = 4))
    }
    @Test fun `un pack inchange conserve le compteur local deja consomme`() {
        assertNull(realignedCounter(localTotal = 4, localRemaining = 2, remoteTotal = 4, remoteRemaining = 4))
    }
    @Test fun `un nouveau pack remplace l ancien`() {
        assertEquals(8 to 8, realignedCounter(localTotal = 4, localRemaining = 0, remoteTotal = 8, remoteRemaining = 8))
    }
}
```

- [ ] **Step 2: Lancer le test, vérifier l'échec**

Run: `--tests "com.lzacademy.app.SubscriptionRealignTest"`. Expected: FAIL, `Unresolved reference 'realignedCounter'`.

- [ ] **Step 3: Brancher dans `hydrateSubscriptions()`**

Pour chaque abonnement miroir ayant déjà une ligne locale, appeler `realignedCounter` ; si non nul, `dao.updateSessions(...)`/insertion équivalente avec `totalSessions = remoteTotal`, `remainingSessions = remoteRemaining`, statut recalculé (`Exhausted` si 0, sinon `Active`). Les lignes absentes restent semées comme aujourd'hui.

- [ ] **Step 4: Lancer le test + la suite**

Expected: BUILD SUCCESSFUL, `SubscriptionRealignTest` PASS, suite complète sans régression.

---

### Task 5: Vérification sur l'appareil

- [ ] **Step 1:** `assembleDebug`, installer (`--no-streaming`), ouvrir Profil élève, vérifier : 4 segments lisibles, « 4/4 séances », dashboard cohérent (plus de 48).
- [ ] **Step 2:** Consigner le résultat dans le ledger.
