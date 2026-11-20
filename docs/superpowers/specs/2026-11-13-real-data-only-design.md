# Design court — Données 100 % réelles, zéro démo (2026-11-13)

## Intent
Supprimer définitivement toutes les données de démonstration de l'app
(`DemoData`, `AttendanceSeed`, comptes/élèves/paiements fictifs) et faire
afficher partout les valeurs réelles de la base Supabase. Le tableau de bord
admin n'affiche plus jamais « 320 élèves » figé.

## État réel de la base (vérifié à l'instant)
students 381 (381 actifs) · subscriptions 381 (1 épuisée) · teachers 21 ·
groups 4 · subjects 15 · payments 12 (6000–15000 DA, paid/pending) ·
announcements 4 · materials 4 · attendance 4 · sessions 28 ·
group_schedule_slots 7 · levels 5.

## Approches

### A — Intégration complète (recommandé)
1. Étendre le miroir Room : + tables `teachers`, `groups`, `subjects`,
   `payments`, `announcements`, `materials`, `group_schedule_slots`,
   `attendance` (8 GET par cycle de sync, throttlés 15 min — reste très sobre).
2. Dashboard admin : stats d'en-tête (Élèves=381, Enseignants=21, Groupes=4,
   Présence=réelle), KPI, alertes (« 1 abonnement épuisé »), revenu mensuel
   (somme réelle des payments), paiements récents, répartition matières —
   tout calculé depuis le miroir.
3. Dashboard élève : abonnement réel (36/48), annonces réelles
   (audience all/students), emploi du temps réel (schedule slots), supports réels.
4. Dashboard enseignant : ses séances/groupes réels.
5. Suppression : `DemoData`, `AttendanceSeed`, section « Espace de démonstration
   local », comptes démo Room — et les écrans affichent « Aucune donnée »
   si la table est vide, jamais d'invention.
- Effort : gros (~2-3 h de travail, beaucoup de fichiers).
- Risque : surfaces d'affichage à redessiner pour les états vides.

### B — Progressive (plus rapide)
1. Admin uniquement : stats/KPI/alertes réelles via miroir étendu minimal
   (teachers, groups, attendance en 3 GET).
2. Suppression de la démo Room et de la section démo.
3. Dashboards élève/enseignant gardent DemoData (nettoyage plus tard).
- Effort : modéré (~1 h).
- Risque : la demande « tout en vrai » n'est que partiellement remplie.

## Décisions ouvertes
- Revenu mensuel : somme des payments du mois (payés) — assumption à valider.
- « Présence 98 % » : % de sessions_done? Présent/Retard sur le total d'attendance
  réelle (4 lignes seulement aujourd'hui).
- Les écrans Rapports/Communication admin ont aussi des valeurs figées (320…)
  → inclus dans A (reports via miroir, cibles de diffusion réelles).
