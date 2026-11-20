# Plan — Abonnements 4 séances, onglet Groupes, renouvellement, paiements enseignants

Design : docs/superpowers/specs/2026-11-13-subscriptions-groups-payments-design.md
Pas de git → gates = SQL serveur + build/tests + adb. Ledger : 2026-11-13-real-data-only-progress.md (append).

## Task 0 — SQL : données + migration
- Normaliser les 5 abonnements >4 (LZ-894/895/896/900/903) : pack_id = Carnet 4, sessions_total=4,
  sessions_used=0, sessions_remaining=4, starts_on=2026-11-13, ends_on=2027-02-13, status='active'.
- UPDATE packs SET is_active=false WHERE sessions_count IN (16,48,108).
- Migration `teacher_payments` : id uuid PK default gen_random_uuid(), teacher_id uuid NOT NULL
  REFERENCES teachers(id), amount_dzd numeric NOT NULL, method payment_method NOT NULL DEFAULT 'cash',
  note text NULL, paid_at timestamptz NOT NULL DEFAULT now(), created_at timestamptz NOT NULL DEFAULT now().
  RLS : ENABLE ; SELECT (is_admin() OR enseignant via profile_id) ; INSERT/UPDATE/DELETE is_admin().
- Preuves : SELECTs retournés.

## Task 1 — SupabaseApi : paiements + teacher_payments
- RemoteTeacherPayment (top-level, pas niché) + fetchTeacherPayments(token).
- fetchPayments(token) si absent (RemotePayment existe déjà — vérifier champs et fetcher).
- adminWrite marche pour toutes les tables (déjà générique).

## Task 2 — VM (AdminManageViewModel) : groupes + paiements enseignants
- ManageData + `enrollmentCountByGroup: Map<String,Int>` (via fetchEnrollments, left_on null).
- addGroup/updateGroup/deleteGroup (FK error → message « Retirez d'abord les élèves »).
- addTeacherPayment(teacherId, amount, method, note, paidAt) / deleteTeacherPayment(id).
- reloadTable : branches "groups" et "teacher_payments".

## Task 3 — Écran Gestion : onglet Groupes + paiements enseignants
- manageTabs : Élèves/Enseignants/Groupes/Salles/Packs — décaler tous les when (sections, delete
  dispatch, conditions bulk students/teachers).
- groupsSection : cartes (nom, niveau, enseignant, salle, capacité, N élèves) + CRUD dialog
  (nom, niveau dropdown, enseignant dropdown, salle dropdown, capacité).
- teachersSection : ligne « Dernier paiement : … » + bouton « Paiement… » + icône historique →
  dialog historique (liste, delete possible). Dialog paiement : montant, méthode, note, date JJ/MM/AAAA.

## Task 4 — Renouvellement (écran Abonnements admin)
- Lire AdminSubscriptionsScreen + VM ; bouton « Renouveler » par élève (et « Créer » si absent) →
  dialog (méthode, montant prérempli prix pack) → POST payments + PATCH/POST subscriptions → reload.
- « Historique » par élève → dialog paiements (date, montant, méthode, statut).

## Task 5 — Build + install + vérification appareil
- assembleDebug + testDebugUnitTest verts ; install ; vérifs : accueil élève LZ-895 = 4/4 ; Gestion
  onglet Groupes (CRUD salle de test) ; renouvellement réel sur un élève (SQL : payment paid + sub 4/0/4) ;
  paiement enseignant (SQL : ligne teacher_payments).
- SQL confirmations serveur.
- Photos de profil (tâche précédente) : vérifier sur appareil dans le même cycle.

## Task 6 — Ledger + récap final
