# Design court — Sélection SMS manuelle + préparation de la version finale (2026-11-14)

## Intent & pourquoi
L'app doit être distribuable à des dizaines d'élèves/enseignants sur l'offre gratuite
Supabase, et l'admin doit pouvoir prévenir un sous-ensemble choisi d'élèves par SMS
depuis son propre téléphone, sans passerelle payante et sans permission sensible.

## Mesures réelles (faites avant de décider)
- Payload complet d'un snapshot (JSON, sans compression) mesuré en base :
  abonnements 144 Ko + élèves 137 Ko + séances 13 Ko + enseignants 10 Ko + le reste ~20 Ko
  → **~322 Ko**.
- `Content-Encoding: gzip` est déjà renvoyé par Supabase et Android décompresse
  automatiquement → **~25–35 Ko réellement transférés** par snapshot.
- Budget gratuit : 5 Go d'egress/mois ≈ 170 Mo/jour.
  Projection 40 utilisateurs × 8 syncs/jour × 30 Ko ≈ **10 Mo/jour ≈ 300 Mo/mois**.
  Marge ~15×, même à 200 utilisateurs.
- **PostgREST ne renvoie aucun header `ETag`** (vérifié sur l'API réelle du projet) :
  le cache conditionnel `If-None-Match` n'est pas disponible. Le `304` n'est donc pas
  une option ; l'équivalent (RPC de version) est reporté, voir « Plus tard ».

## Décisions

### A. Sélection manuelle des destinataires (SMS)
- **Source** : miroir Room (`synced_students`), donc instantané et utilisable hors ligne.
  Seuls les élèves **actifs avec un téléphone** sont sélectionnables ; les autres
  apparaissent grisés « sans numéro » et ne peuvent pas être cochés.
- **Filtres** : recherche texte (nom, matricule, téléphone) + puces par niveau réel
  (« Tous » + niveaux existants).
- **Sélection** : case à cocher par élève, « Tout sélectionner »/« Tout désélectionner »
  portant sur la **liste filtrée**, compteur permanent « N sélectionnés / M avec numéro ».
- **Envoi** : `Intent.ACTION_SENDTO` + `Uri.parse("smsto:<num1>;<num2>;…")` avec
  `sms_body` = message. L'app Messages s'ouvre préremplie, l'admin appuie sur Envoyer.
  Aucune permission `SEND_SMS`, aucun coût, fonctionne hors ligne.
- **Normalisation** des numéros : on retire espaces, points, tirets et parenthèses,
  on conserve le `+` initial, on déduplique. Un numéro invalide (moins de 6 chiffres)
  est ignoré et signalé dans le compteur.
- **Limites honnêtes** : au-delà de 20 destinataires, un avertissement rappelle que
  certaines app Messages tronquent la liste ; on propose d'ouvrir par lots de 20.
  Si aucune app SMS n'est présente, message clair au lieu d'un crash.
- **Historique** : local et persistant (20 derniers), libellé « Message préparé —
  non envoyé par l'app », avec date, nombre de destinataires et extrait du texte.
  Les canaux fictifs « Push » / « Email » sont supprimés : un seul canal réel, SMS.

### B. Réseau (approche A, ajustée aux mesures)
- `snapshot()` : les 12 GET partent **en parallèle** (`coroutineScope` + `async`).
  Même volume, latence divisée.
- `MIN_SYNC_INTERVAL_MS` : 15 → **60 min**. Sync auto uniquement au démarrage de
  l'app et sur action explicite ; plus aucune sync déclenchée par la simple ouverture
  d'un écran.
- **Suppression de la sonde** `fetchLevels()` dans `AdminManageViewModel.freshToken()`.
  À la place : chargement parallèle, et en cas de `AuthExpiredException` un seul
  `refresh_token` puis un seul retry.
- `loadData()` de l'écran Gestion : 12 GET parallèles + **garde 60 s** (une réouverture
  immédiate ne re-télécharge pas). On garde la lecture live pour la Gestion : c'est un
  écran d'écriture serveur, il doit voir l'état réel, et 30 Ko pour un seul admin sont
  négligeables.
- Aucune migration SQL, aucun changement de policy. Rollback = un fichier.

### C. Version finale
- `versionCode = 2`, `versionName = "1.1"`.
- Release : `isMinifyEnabled = true`, `isShrinkResources = true`, règles ProGuard
  ajoutées seulement si un test sur appareil le justifie.
- Signature : si `KEYSTORE_PATH` est défini (CI/AI Studio) on garde le keystore
  `release` existant ; sinon le build release retombe sur le **keystore debug local**.
  Cela permet de mettre à jour l'app déjà installée sans désinstallation (donc sans
  perdre le profil admin local ni l'historique Room). Un keystore neuf obligerait à
  tout réinitialiser chez les utilisateurs actuels.
- Nettoyage : suppression du code mort (canaux fictifs, `successRate`), états vides
  honnêtes, aucune nouvelle permission dans le manifeste.

## Tests & vérification
- Tests unitaires : normalisation/déduplication des numéros, construction de l'URI
  `smsto:` (ordre stable, séparateur `;`), découpage en lots de 20, garde de throttle.
- `:app:testDebugUnitTest`, `:app:assembleDebug`, `:app:assembleRelease`.
- Installation du build release sur l'appareil, parcours manuel : sélection, filtres,
  ouverture de l'app Messages, retour, historique, écran Gestion.

## Risques
- R8 peut casser un chemin runtime non détecté à la compilation → test obligatoire du
  build release sur l'appareil avant distribution.
- Certaines app SMS limitent le nombre de destinataires → avertissement + lots de 20.
- `smsto:` multi-destinataires varie selon le constructeur → repli documenté : lots.

## Plus tard (hors périmètre, documenté)
Si l'egress dépasse ~2 Go/mois : fonction SQL `sync_versions()` renvoyant (table, nb lignes,
checksum) → 1 requête d'~1 Ko, puis téléchargement des seules tables modifiées. Même
bénéfice qu'un ETag, sans exposition de données (hash + compteurs uniquement).

## Laissé au builder
Disposition exacte des lignes destinataires, seuil de lot, libellés, couleur des élèves
sans numéro, et format de l'historique local.
