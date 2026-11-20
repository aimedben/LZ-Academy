# LZ Academy — Base de données Supabase

Schéma PostgreSQL/Supabase du projet LZ Academy, construit à partir de l'analyse des modèles
Kotlin (`Teacher`, `Models.kt`, `DemoData.kt`) et des écrans élèves / enseignants / administration.

| Élément | Valeur |
|---|---|
| Projet Supabase | `qxnuuhjjuzudxyyleplw` |
| URL API | `https://qxnuuhjjuzudxyyleplw.supabase.co` |
| PostgreSQL | 17.11 |
| Tables | 26 |
| Vues | 5 |
| Policies RLS | 99 (une policy permissive par commande : lecture + insert/update/delete séparés) |
| Migrations | 14 (`20261109000100` → `20261110001400`) |
| Buckets Storage | `materials` (privé), `teacher-photos` / `avatars` (publics, 2 Mo, images) |
| Fuseau | `Africa/Algiers` |

---

## 1. Domaines

### Socle
| Table | Rôle |
|---|---|
| `profiles` | Extension de `auth.users` : rôle applicatif (`student`, `teacher`, `admin`), nom, téléphone, avatar. |
| `academy_settings` | Ligne unique : nom, adresse, contacts, fuseau, année scolaire courante. |
| `audit_log` | Journal des actions administratives (acteur, action, entité, détails JSONB). |

### Personnes & structure
| Table | Rôle |
|---|---|
| `academic_years` | Années scolaires, une seule `is_current`. |
| `levels` | Niveaux (`3AM`, `1AS`, `2AS`, `3AS-SCI`, `3AS-MT`). |
| `subjects` | Matières avec code et accent visuel (`green`, `blue`, `violet`, `orange`). |
| `teachers` | Enseignants + champs de la vitrine et deux préférences indépendantes. |
| `teacher_subjects` | Matières enseignées (`is_primary`). |
| `teacher_levels` | Niveaux enseignés. |
| `students` | Élèves, matricule unique, tuteur, niveau, compte Auth facultatif. |
| `rooms` | Salles et capacités. |
| `groups` | Groupes (niveau, enseignant, salle, année, capacité). |
| `group_schedule_slots` | Créneaux hebdomadaires (jour 1–7, début, fin, salle). |
| `enrollments` | Inscriptions élève ↔ groupe, avec date de sortie. |

### Pédagogie
| Table | Rôle |
|---|---|
| `sessions` | Séances datées (groupe, enseignant, matière, salle, statut, thème). |
| `attendance` | Feuille d'appel : `present`, `absent`, `late`, `justified`. |
| `materials` | Métadonnées des supports ; le fichier vit dans Storage, jamais en base. |

### Finances
| Table | Rôle |
|---|---|
| `packs` | Formules et tarifs en DZD. |
| `subscriptions` | Abonnements ; `sessions_remaining` est une colonne générée. |
| `payments` | Paiements (`cash`, `ccp`, `baridimob`, `bank`, `card`), statuts et échéances. |

### Vie scolaire & communication
| Table | Rôle |
|---|---|
| `announcements` | Annonces ciblées (`all`, `students`, `teachers`, `level`, `group`). |
| `announcement_reads` | Accusés de lecture par utilisateur. |
| `broadcasts` | Historique des diffusions SMS / Push / Email (outbox). |
| `teacher_messages` | Messages élève → enseignant, statut `pending` par défaut. |
| `featured_teacher_days` | Page de vitrine persistée pour une date. |
| `featured_teacher_entries` | Enseignants de la page, avec position 1–20. |

---

## 2. Conventions

- **Clés primaires** : `uuid` générées par `gen_random_uuid()` (sauf tables de liaison et `academy_settings`).
- **Dates métier** : `date` ; **horodatages** : `timestamptz` ; **heures de créneau** : `time`.
- **Montants** : `numeric(12,2)` en dinars algériens.
- **Nommage** : `snake_case`, tables au pluriel, contraintes explicites.
- **Enums PostgreSQL** : rôles, statuts de présence, paiement, abonnement, séance, message, audience, canal, type de fichier.
- **`updated_at`** : trigger `set_updated_at()` sur les tables mutables.
- **Suppressions** : `cascade` pour les dépendances fortes, `set null` pour les références facultatives.
- **Aucun BLOB** : les fichiers sont dans le bucket Storage privé `materials`.
- **Commentaires** : documents et libellés en français.

---

## 3. Sécurité (RLS)

RLS est activé sur **toutes** les tables du schéma `public`.

### Helpers d'identité
| Fonction | Usage |
|---|---|
| `is_admin()` | Vérifie le rôle admin du profil courant. |
| `current_student_id()` | Id de l'élève lié au compte Auth. |
| `current_teacher_id()` | Id de l'enseignant lié au compte Auth. |
| `is_teacher_of_student(uuid)` | Un enseignant anime-t-il un groupe de cet élève ? |
| `is_student_self(uuid)` | L'élève appartient-il au compte courant ? |
| `is_teacher_of_group(uuid)` | L'enseignant anime-t-il ce groupe ? |

Ces helpers sont `SECURITY DEFINER` avec `search_path = public` : ils court-circuitent les RLS
croisées (`students` ↔ `enrollments`) et évitent toute récursion.

### Principes
- **Élève** : voit son profil, ses inscriptions, ses séances, ses présences, ses supports publiés,
  ses abonnements et paiements. Ne voit jamais les autres élèves.
- **Enseignant** : voit les élèves de ses groupes, ses séances, ses supports, les messages reçus.
  Peut modifier **uniquement** sa propre fiche.
- **Admin** : gère la structure, les inscriptions, les finances, les annonces et la visibilité
  des enseignants. Ne peut **jamais** modifier `accepts_student_messages`.
- **Anon** : accès en lecture à la vitrine (via RPC), aux enseignants, matières, niveaux, salles,
  packs et paramètres publics. Aucune donnée élève ni message.

### Garde-fous
- `guard_profile_role()` : un utilisateur ne peut pas changer son rôle.
- `guard_teacher_settings()` : l'admin peut changer `is_featured_on_student_home`, mais toute
  tentative de modifier `accepts_student_messages` est rejetée.
- `handle_new_user()` : tout nouvel utilisateur Auth devient `student` par défaut.
- `materials_owner_write` : un enseignant ne publie que pour ses propres groupes ; un élève ne
  peut pas créer de support.
- Storage : bucket `materials` privé, chemin `{user_id}/{group_id}/{fichier}`. La lecture d'un
  objet exige que sa ligne `materials` soit visible par l'appelant.

---

## 4. Vitrine « Nos enseignants »

### Champs
```text
id, full_name, photo_url, professional_description,
is_featured_on_student_home, accepts_student_messages,
featured_rotation_priority, last_featured_at, is_active
```
Les matières et niveaux sont normalisés dans `teacher_subjects` / `teacher_levels`.
Le nombre de groupes est calculé par `v_teacher_directory`.

### Rotation quotidienne
`get_featured_teachers(p_size)` est la seule RPC publique. Elle appelle en interne
`rotate_featured_teachers(current_date, p_size)` :

1. Verrou consultatif par date (pas de double rotation concurrente).
2. Purge immédiate des enseignants devenus inéligibles (`is_active = false` ou retrait de la vitrine).
3. Page persistée dans `featured_teacher_days` / `featured_teacher_entries` : **stable toute la journée**,
   y compris après redémarrage.
4. Sélection équitable : `last_featured_at` nul d'abord, puis `featured_rotation_priority` décroissant,
   puis `full_name`.
5. Mise à jour de `last_featured_at` à chaque sélection.

Un client ne peut pas appeler `rotate_featured_teachers` directement : il ne peut donc pas réécrire
l'historique de rotation ni choisir une autre date. Le service role / postgres conserve ce droit
pour l'administration et les tâches planifiées.

### Données seed
- 21 enseignants, dont **18 affichés** et **3 retirés** (`Samia Kaci`, `Fouad Benmoussa`, `Karima Taleb`).
- **16 disponibles** à la réception de messages, **5 hors ligne** (`Nadia Bensalem`, `Reda Said`,
  `Omar Ferhat`, `Djamila Saidi`, `Fouad Benmoussa`).
- 5 jours de rotation reconstitués : 20 entrées, 4 par jour, **18 enseignants distincts** couverts.
- `last_featured_at` est renseigné pour les 18 enseignants affichés, jamais pour les retirés.

---

## 4bis. Optimisation « offre gratuite » (migration 0014)

Cible : plusieurs centaines d'utilisateurs sans dépasser le Plan gratuit
(500 Mo de base, 1 Go Storage, 5 Go de sortie mensuelle).

### Vitrine : lecture rapide
`get_featured_teachers` appelait la rotation complète (verrou + purge +
écriture) à chaque ouverture de l'accueil élève. Désormais :

1. **Chemin rapide** : la page du jour est déjà complète (préparée par
   `pg_cron` ou le premier élève du matin) → lecture seule, sans verrou ;
   seul un enseignant qui vient de se retirer est purgé (effet immédiat).
2. **Voie d'écriture** : uniquement le premier appel du jour — une
   requête atomique par sélection (choix + insertion + horodatage).

Mesure `EXPLAIN (ANALYZE, BUFFERS)` :

| Cas | Avant | Après |
|---|---|---|
| Lecture (jour courant) | 149,8 ms · 1516 buffers · écritures disque | **0,80 ms · 40 buffers · 0 écriture** |
| Premier appel du jour | voies mixtes | ~4,5 ms (une fois, puis lecture rapide) |

### pg_cron (installé et actif)
| Job | Horaire UTC → Algiers | Effet |
|---|---|---|
| `lz_featured_nightly_rotation` | 23:05 → 00:05 | Pré-construit la page du jour (rotation + équité), la vitrine est prête avant le premier élève. |
| `lz_featured_daily_purge` | 23:15 → 00:15 | Supprime les pages de rotation de plus de 3 jours (cascade sur les entrées) — la table reste minuscule, bloat contrôlé. |

Si le cron échoue, la fallback du chemin d'écriture garantit quand même
la page du jour au premier appel.

### Policies : une policy permissive par commande
Chaque policy « ALL » doublonnant avec une policy « SELECT » (groupe
d'écriture admin appliqué aussi aux lectures) a été scindée en policies
`insert` / `update` / `delete` distinctes. Résultat : **une seule policy
permissive évaluée par commande et par ligne** (sauf `teacher_messages`,
qui a légitimement deux audiences élève / enseignant), ce qui réduit le
coût RLS des lectures chaudes.

### Realtime
Seule `teacher_messages` est publiée à `supabase_realtime` (faible
volume). Les séances et annonces se rechargent en entrant sur l'écran —
pas de connexions websocket superflues (quota Realtime du Plan gratuit).

### Garde-fous
- `statement_timeout` : `anon` 5 s, `authenticated` 10 s — une requête
  toxique d'un client ne peut plus monopoliser le PostgREST.
- Extension `pg_stat_statements` active : requêtes les plus lourdes
  surveillables à tout moment.
- Analyze à jour sur les tables chaudes.

### Egress (sortie réseau)
- Photos servies par le CDN Storage (URLs publiques), jamais via RPC/JSON.
- Les clients doivent paginer (`.limit()`) ; les RPC de vitrine plafonnent
  `p_size` à 12.
- Connexions app : Supavisor mode transaction (port 6543) — pooling
  partagé inclus dans le Plan gratuit.

---

## 4ter. Photos (buckets publics)

| Bucket | Visibilité | Contenu | Dossier imposé |
|---|---|---|---|
| `teacher-photos` | public | portraits d'enseignants, `{teacher_id}/…` | `{auth.uid()}/{teacher_id}/{fichier}` |
| `avatars` | public | avatars `profiles.avatar_url` | `{auth.uid()}/{fichier}` |
| `materials` | privé | supports de cours | `{user_id}/{group_id}/{fichier}` |

- `teacher-photos` / `avatars` : **2 Mo max, `image/png|jpeg|webp`**.
- Écritures limitées au dossier de l'utilisateur : un enseignant ne peut
  publier que dans `{uid}/{son teacher_id}/…` ✓ (testé RLS à l'envers, insertion refusée si dossier étranger).
- L'uploader reste ±17 % lisible en lecture : URLs publiques servies sans
  policy par le CDN.
- RPC `update_teacher_photo(p_photo_path)`
  (authenticated uniquement) : vérifie que l'objet est dans le dossier du
  caller, puis stocke l'URL publique dans `teachers.photo_url` ;
  `p_photo_path = null` retire la photo.
- L'admin passe par le service role / console pour changer une photo
  d'un autre enseignant.

---

## 5. Messagerie élève → enseignant

RPC `send_teacher_message(p_teacher_id uuid, p_body text)` :

- résout l'élève depuis `auth.uid()` (`current_student_id()`) ;
- refuse si l'enseignant est inactif ou si `accepts_student_messages = false` avec le message
  exact : « Cet enseignant n'est actuellement pas disponible pour recevoir des messages. » ;
- insère le message avec le statut `pending` ;
- ne prétend jamais avoir livré : la livraison réelle (push, email, SMS) reste un traitement
  asynchrone à brancher sur `broadcasts` / `teacher_messages`.

Aucune policy `INSERT` n'existe sur `teacher_messages` : la RPC est le seul point d'entrée.

---

## 6. Vues

| Vue | Contenu |
|---|---|
| `v_teacher_directory` | Annuaire complet : matières, niveaux, expérience, réglages, nombre de groupes. |
| `v_showcase_stats` | Compteurs vitrine : affichés / retirés / disponibles / hors ligne / total. |
| `v_monthly_revenue` | Revenus mensuels des paiements `paid`. |
| `v_attendance_summary` | Taux de présence par élève. |
| `v_student_overview` | Synthèse élève : groupes, séances restantes, montant en retard. |

Toutes les vues sont `security_invoker = true` : les RLS de l'appelant s'appliquent.

---

## 7. Données de démonstration

| Table | Lignes |
|---|---|
| `academic_years` | 1 (2025-2026) |
| `levels` | 5 |
| `subjects` | 15 |
| `teachers` | 21 |
| `teacher_subjects` | 21 |
| `teacher_levels` | 44 |
| `students` | 12 |
| `rooms` | 8 |
| `groups` | 4 (M. Karim) |
| `group_schedule_slots` | 7 |
| `enrollments` | 16 |
| `sessions` | 28 (4 semaines × 7 créneaux) |
| `attendance` | 4 (séance déjà terminée) |
| `materials` | 4 |
| `packs` | 5 (dont « Carnet 4 séances » à 6 000 DZD) |
| `subscriptions` | 12 |
| `payments` | 12 |
| `announcements` | 4 |
| `broadcasts` | 2 |
| `featured_teacher_entries` | 20 |

Les identifiants des enseignants et des élèves sont déterministes :
`md5('lz:teacher:t-karim')::uuid`, `md5('lz:student:s1')::uuid`, etc.

---

## 8. Migrations

Les fichiers de `supabase/migrations/` sont la source de vérité :

| Version | Fichier | Contenu |
|---|---|---|
| `20261109000100` | `foundation.sql` | Enums, profils, trigger Auth, audit, paramètres. |
| `20261109000200` | `people.sql` | Années, niveaux, matières, enseignants, élèves, salles, groupes, inscriptions. |
| `20261109000300` | `teaching.sql` | Séances, présences, supports. |
| `20261109000400` | `finance.sql` | Packs, abonnements, paiements. |
| `20261109000500` | `communication.sql` | Annonces, lectures, diffusions, messages, vitrine. |
| `20261109000600` | `functions_views.sql` | Helpers, garde-fous, rotation, messagerie, vues. |
| `20261109000700` | `rls.sql` | RLS, policies, Storage. |
| `20261109000800` | `seed.sql` | Données de référence et de démonstration. |
| `20261109000900` | `hardening.sql` | search_path, extension hors `public`, privilèges, index FK. |
| `20261109001000` | `rls_optimization.sql` | `(select auth.uid())`, RPC publique `get_featured_teachers`. |
| `20261109001100` | `rls_recursion_fix.sql` | Helpers de contrôle croisé, suppression de la récursion. |
| `20261109001200` | `materials_security.sql` | Écriture des supports limitée aux groupes de l'enseignant, lecture Storage alignée. |
| `20261110001300` | `storage_photos.sql` | Buckets publics `teacher-photos` / `avatars` (2 Mo, images), dossiers imposés, RPC `update_teacher_photo`. |
| `20261110001400` | `performance_free_tier.sql` | Lecture rapide de la vitrine, policies scindées par commande, pg_cron (rotation nocturne + purge), Realtime messages, timeouts. |

Les versions appliquées sont enregistrées dans `supabase_migrations.schema_migrations`.
`list_migrations` les retourne dans l'ordre.

### Appliquer via l'API de gestion
```bash
# Exemple : une migration précise
curl -X POST "https://api.supabase.com/v1/projects/qxnuuhjjuzudxyyleplw/database/query" \
  -H "Authorization: Bearer $SUPABASE_PAT" \
  -H "Content-Type: application/json" \
  -d "{\"query\": $(jq -Rs . supabase/migrations/20261109001200_materials_security.sql)}"
```

### Appliquer via la CLI Supabase
```bash
supabase link --project-ref qxnuuhjjuzudxyyleplw
supabase db push
```

---

## 9. Vérifications effectuées

- 26 tables, 5 vues, 99 policies (une policy permissive par commande), 0 table sans RLS.
- Anon : 21 enseignants visibles, 0 élève, 0 message.
- Authentifié sans profil lié : 21 enseignants, 0 élève, 0 inscription, 0 message, 2 annonces `all`.
- `get_featured_teachers(4)` : 4 enseignants, au moins un disponible ;
  **0,80 ms / 40 buffers** en lecture du jour (vs 149,8 ms / 1516 buffers avant optimisation).
- Rotation : écriture testée (page construite + 4 enseignants horodatés) et relecture équitable ;
  purge immédiate d'un enseignant retiré (test : 0 retiré reste dans la page).
- Photos : RPC `update_teacher_photo` enregistrée (URL publique en base) ; insertion hors du
  dossier personnel refusée par la RLS Storage, pour `teacher-photos` comme pour `avatars`.
- `pg_cron` : 2 jobs programmés (rotation nocturne 00:05, purge 00:15 — heure d'Alger).
- Realtime : publication limitée à `teacher_messages`.
- Index GIN `pg_trgm` valides après déplacement de l'extension vers `extensions`.
- Advisors restants :
  - `SECURITY DEFINER` exécutables : volontaires (`get_featured_teachers`, helpers RLS,
    `send_teacher_message`, `update_teacher_photo`, `featured_snapshot`) ; les fonctions trigger
    et la rotation interne sont révoquées.
  - `multiple_permissive_policies` (9) : audiences multiples légitimes (`teacher_messages`
    élève/enseignant, écritures enseignant + admin sur ses fiche-matières/niveaux), sans impact
    fonctionnel.
  - `unused_index` : attendu sur une base neuve sans trafic.

---

## 10. Intégration Android — prochaines étapes

1. Ajouter le SDK Supabase Kotlin et l'authentification (email / téléphone).
2. Lier les comptes :
   - `profiles.id = auth.uid()` ;
   - `students.profile_id` / `teachers.profile_id` renseignés par l'administration.
3. Remplacer progressivement `TeacherRepository` (Room) par les appels Supabase :
   - vitrine : `rpc("get_featured_teachers", { p_size: 4 })` ;
   - annuaire : `v_teacher_directory` ;
   - réglages enseignant : `update teachers set is_featured_on_student_home = ...` ;
   - contact : `rpc("send_teacher_message", { p_teacher_id, p_body })`.
4. Upload des supports : Storage `materials`, chemin `{user_id}/{group_id}/{fichier}`,
   puis insertion dans `materials`.
5. Photo d'un enseignant : Storage `teacher-photos`, chemin
   `{uid}/{teacher_id}/{fichier}` (2 Mo, PNG/JPEG/WebP), puis
   `rpc("update_teacher_photo", { p_photo_path })` pour publier l'URL.
6. Avatars : Storage `avatars`, chemin `{uid}/{fichier}`, puis
   `update profiles set avatar_url = <URL publique>` (lecture publique).
7. Temps réel : souscrire à `teacher_messages` uniquement (publication
   configurée côté base) — `announcements`/`sessions` se rechargent à
   l'entrée sur l'écran.
8. Bibliothèque cliente : URL Supabase + clé publishable, connecter en
   mode transaction (port `6543`) pour bénéficier du pooling partagé ;
   respecter les limites clientes (`.limit()`) pour l'annuaire.
9. Tâche planifiée (pg_cron) optionnelle : passer les séances échues en `completed`
   (les présences restent saisies par l'enseignant).
10. Brancher un fournisseur SMS / Push / Email sur `broadcasts` : la table est un outbox,
    elle ne prétend pas envoyer.

> Le projet n'a pas besoin de `google-services.json` : Supabase n'utilise pas Firebase.
> La clé publishable `sb_publishable_Jqbg5bNg-E7b8z6rKP0Jlw_G0OSCzks` suffit côté client ;
> la clé `service_role` doit rester exclusivement côté serveur.

---

## 11. Points d'attention

- **Deux réglages enseignants indépendants** : `is_featured_on_student_home` et
  `accepts_student_messages`. Ne jamais les fusionner dans l'UI ni dans une policy.
- **L'admin gère la visibilité, pas la messagerie** : le trigger `guard_teacher_settings` le garantit.
- **Pas de fausse livraison** : un message hors ligne est refusé avant insertion ; un message
  inséré reste `pending` jusqu'à un envoi réel.
- **Rotation équitable** : ne pas appeler `rotate_featured_teachers` depuis le client ; utiliser
  `get_featured_teachers`.
- **RLS croisées** : toute nouvelle policy qui référence une table déjà protégée doit passer par
  un helper `SECURITY DEFINER` pour éviter une récursion.
- **Storage** : ne jamais rendre le bucket `materials` public ; les policies alignent la lecture
  sur la visibilité de la ligne `materials`.
