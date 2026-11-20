# Bordure de progression autour de la photo de profil élève — design

Date : 2026-11-14
Statut : approuvé (approche A choisie par l'utilisateur)

## Intention

Un élève doit voir, d'un coup d'œil sur sa **photo de profil**, combien de séances de son
abonnement il a consommées. Un abonnement réel = **4 séances** (vérifié en base : 381/381
lignes `sessions_total = 4`), donc une séance = **un quart du cercle** : lisible sans chiffre.

## Approches examinées

- **A. Anneaux segmentés (retenu)** : N arcs séparés par un petit vide, un par séance.
  Un arc plein = une séance faite. Très parlant pour 4 séances, reste correct jusqu'à ~12.
- B. Arc continu `consommées / total` : marche pour n'importe quel pack, mais « une séance »
  n'est plus visible quand le total est grand.
- C. Remplacer la carte abonnement par ce seul cercle : moins de doublons, mais perte du
  détail (statut, renouvellement) que l'élève consulte aujourd'hui.

## Décisions

- Le cercle est dessiné **autour** de la photo (84 dp) : photo inchangée, anneau de 4 dp
  à l'extérieur, départ en haut, vide de 10° entre segments.
- Couleurs : segments vides `border` (gris clair), segments pleins `primary` (vert).
  Aucune information portée par la couleur seule → un libellé « 2/4 séances » accompagne.
- Animation d'entrée (remplissage progressif ~600 ms) au premier affichage uniquement.
- Écran concerné : **Profil élève**, carte d'identité. La carte « Abonnement » reste en
  place (mêmes chiffres, autre niveau de détail).
- Source des chiffres : le compteur d'appel local (`subscriptions` Room), c'est lui que le
  professeur décrémente à chaque séance. Si `totalSessions <= 0`, aucun segment n'est dessiné
  (repli sur l'anneau blanc actuel).

## Correction de données liée (validée : « pas 48, uniquement 4 »)

Le compteur local garde un total obsolète (48) prioritaire sur la base (4) → l'élève voit
« 36/48 » et la bordure afficherait 25 %. Règle ajoutée à `hydrateSubscriptions()` : quand le
`totalSessions` miroir diffère du total local, **le pack a changé** → la ligne locale est
réalignée sur le miroir (total, restant, statut). Un appel d'appel en cours n'est donc jamais
écrasé tant que le total ne change pas.

Aucun écran ne doit plus jamais afficher un pack de 48 séances : le total montré à l'élève est
toujours celui du pack réel (4). Le dashboard et la carte « Abonnement » lisent la même source
réalignée.

## Hors périmètre

- Pas de bordure sur les autres avatars (dashboard, enseignants, admin).
- Pas d'écriture `sessions_used` vers Supabase depuis l'app (l'admin gère le renouvellement).
- Pas de changement de la carte d'abonnement ni du schéma.
