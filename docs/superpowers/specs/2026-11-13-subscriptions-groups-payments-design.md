# Design — Abonnements 4 séances, onglet Groupes, renouvellement, paiements enseignants

Date : 2026-11-13 · Approuvé par l'utilisateur (« vasy »).

## Intent & pourquoi
L'académie vend des **carnets de 4 séances maximum**. Les écrans doivent refléter ce modèle
partout, et l'admin doit pouvoir gérer **groupes, renouvellements payés et paiements
enseignants** sans complexité (UI épurée exigée — l'admin trouve l'app trop chargée).

## Données réelles constatées (SQL)
- 381 abonnements, tous « Carnet 4 séances » sauf 5 anciens comptes de test (16/48/108).
- Packs : Carnet 4 (actif) + Mensuel 16, Trimestriel 48, Annuel 108, À la carte 1.
- payments : 12 lignes (enum method: cash/ccp/baridimob/bank/card ; status: paid/pending/overdue/refunded).
- Aucune table paiements enseignants ; RLS pattern = `is_admin()` sur role authenticated.

## Approches considérées
1. **Supprimer les gros packs** — rejeté : FK depuis subscriptions, perte d'historique tarifaire.
2. **Désactiver les gros packs + normaliser les 5 vieux abonnements** — retenu : non destructif, le
   formulaire de création ne listant que les packs actifs, plus rien >4 ne peut être créé.
3. Paiements enseignants dans un nouvel onglet dédié — rejeté : surcharge UI ; intégré à l'onglet
   Enseignants existant (1 ligne par carte + dialogue).

## Design
1. **SQL** : 5 vieux abonnements → Carnet 4 (total 4, used 0, remaining 4, dates aujourd'hui → +3 mois,
   status active, pack_id = Carnet) ; 3 gros packs `is_active=false` ; **migration `teacher_payments`**
   (id, teacher_id FK, amount_dzd, method payment_method, note text, paid_at timestamptz, created_at)
   + RLS (SELECT admin/enseignant propre, INSERT/UPDATE/DELETE is_admin()).
2. **Gestion → onglet Groupes** (5e onglet, ordre Élèves/Enseignants/Groupes/Salles/Packs) : liste live
   (nom • niveau • enseignant • salle • N élèves actifs) + CRUD ; suppression impossible si élèves
   inscrits → message « Retirez d'abord les élèves ». Comptes d'élèves via enrollments (left_on IS NULL).
3. **Renouvellement réel** (écran Abonnements) : bouton « Renouveler » par élève → dialogue (méthode,
   montant prérempli = prix pack) → POST payment (status=paid, paid_at=now) + PATCH subscription
   (nouveau carnet 4, dates recalculées) ; élève sans abonnement → POST subscription puis payment ;
   historique des paiements par élève consultable.
4. **Paiements enseignants** : Gestion → Enseignants : ligne « Dernier paiement : date • montant » +
   bouton « Paiement… » (montant libre, méthode, note, date défaut = aujourd'hui) + historique
   consultable ; suppression d'une ligne possible (correction d'erreur).
5. **Accueil élève** : après correction des données, « Séances restantes » affiche 4/4 — aucun code
   nécessaire, vérification appareil.

## Non-goals
- Génération de reçus PDF, SMS, relances automatiques.
- Salaires calculés automatiquement (montant saisi librement).

## Laissé au builder
- Format dates fr (FrenchDates), réutilisation dialogs/patterns existants (LZCard, one-shot messages,
  dialogs fermés immédiatement, adminWrite + reloadTable).
