# Design court — Icône d'app = vrai logo welcome screen (2026-11-13)

## Intent
L'icône de l'application doit être le vrai logo LZ Academy affiché sur la
welcome/login screen (composable `LZLogo` : losange indigo dégradé
#4A2D63→#1E1230, bordure blanche, « LZ » serif gras blanc, swoosh rouge
#E53935), à la place du JPG actuel (carré vert, casquette dorée).

## Approches
- **A — PNG rendu fidèle (choisi)** : redessin programmatique (PIL) avec la
  recette exacte de `LZBrand.kt` (mêmes couleurs, proportions 0.72, coin 12 %,
  police serif grasse, swoosh quadratique 4 %) → maître 1024 px → toutes les
  densités mipmap + icône adaptative v26.
- **B — Vector XML** : rejeté — VectorDrawable ne rend pas le texte ; les
  lettres « LZ » serif seraient une approximation.

## Décisions
- Fond blanc (comme dans l'app), losange centré ; round legacy = fond blanc
  circulaire.
- Le vieux `lz_launcher_icon_1793965509543.jpg` devient orphelin → supprimé.
- `LZText`/letter-spacing non reproduits au-delà du rendu serif gras standard.
- Gate : build OK + install + screenshot appareil montrant la nouvelle icône.
