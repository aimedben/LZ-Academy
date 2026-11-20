# Design — Refonte UI/UX LZ Academy (13/11/2026)

## Intent / pourquoi
L'utilisateur trouve les interfaces « pas bien faites » et veut un rendu professionnel,
styles actuels. Après une première passe de ma part (single-accent vert, headers clairs),
**l'utilisateur a fourni la direction artistique détaillée de sa maquette** (inspiration
Dribbble SaaS/EdTech). C'est cette direction qui fait foi — v1 abandonnée.

## Direction validée (spéc maquette utilisateur)
- **Bicolore** : violet foncé = identité, **vert émeraude = actions principales / actifs**.
- **Fond principal : lavande très claire** (~#F3F1F8). Cartes blanches radius 20–24,
  ombres très douces. Textes secondaires gris bleuté. Rouge = suppressions/alertes.
- **Header compact dégradé violet foncé → vert émeraude** : logo LZ cercle vert, nom,
  description courte, cloche avec badge rouge. (Élève/enseignant : même système,
  émeraude dominant ; admin : violet dominant.)
- **Onglets catégories** (Gestion) : sélectionné = pilule émeraude pleine, icône+texte
  blancs ; non-sélectionnés = fond lavande, texte violet, icône.
- **Barre de recherche** : fond lavande, radius 24, loupe à gauche + bouton filtre
  séparé à droite.
- **FAB** : cercle vert émeraude 56dp, « + », bas-droite.
- **Navigation basse** : blanche, fixe, coins arrondis en haut ; onglet actif
  **violet foncé** pour l'admin.
- **Police Inter** (bundlée res/font, fallback SansSerif si téléchargement impossible).
- **Système de composants réutilisables** : header, onglets, recherche, cartes, badges,
  menus d'actions, FAB et nav inférieure partagés entre les 5 onglets de Gestion (et
  re-déclinables partout) — pas cinq designs indépendants.

## Approches considérées
1. **Redesign par écran** — lent, risqué, casse les écrans validés. ❌
2. **Refonte aux sources** (tokens + composants partagés + restructuration ciblée de
   l'écran Gestion) qui se propage partout. Rapide, cohérent, réversible. ✅ choisi
   (conforme à l'exigence « un seul système de composants »).
3. Passe v1 single-accent/headers clairs — supplantée par la spec maquette. ❌

## Hors périmètre (non-goals)
- Aucun changement de logique / VM / données.
- Pas de nouvelle fonctionnalité ; les stats/résumés par onglet n'ajoutent pas de requêtes.

## Détails d'implémentation (décisions)
- Tokens (`LZColors`) : background lavande, surfaces blanches, primary vert émeraude,
  `purple`/DeepPurple conservés comme identité admin, textes gris bleuté.
- Headers : gradients dual-color par rôle ; textes blancs ; `LZStatusBarIcons(true)` sur
  les écrans à header coloré (retour au comportement initial).
- `LZRadius` : standard 20 (cartes), large 24 ; ombres douces existantes conservées.
- Gestion : chips à icônes (5 onglets), search lavande + bouton filtre, FAB émeraude
  câblé aux actions d'ajout existantes de chaque onglet.
- Inter : tentative de bundling statique (regular/medium/semibold/bold) ; sinon SansSerif
  avec scale affinée.
- Ce qui reste au builder : ajustements visuels révélés par les captures d'écran.

