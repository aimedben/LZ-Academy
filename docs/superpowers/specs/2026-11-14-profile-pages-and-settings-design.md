# Pages de profil (élève / enseignant / admin) et écran Paramètres — design

## Intention

Aujourd'hui les trois espaces « profil » ne se ressemblent pas et ne ressemblent à aucune page
profil classique :

- **Élève** : en-tête + carte d'identité + abonnement + cours + assiduité + une carte « Mon compte »
  qui ouvre une *feuille* (`AccountSheet`).
- **Enseignant** : en-tête + carte d'identité + « Diffusion & contact » + « Mes groupes » + la même
  feuille « Mon compte ».
- **Admin** : l'écran « profil » est en réalité un **formulaire d'édition** ; le thème n'existe que
  dans la feuille « Mon compte », et les réglages système vivent dans un écran « Paramètres &
  Système » contenant des données inventées (sauvegarde « 03:00 », 2FA « Obligatoire », « 3 comptes
  administrateurs »).

L'utilisateur veut une **vraie page de profil**, bien structurée, avec un **bouton Paramètres** qui
mène au **thème** et aux **notifications**, pour les trois rôles.

## Objectifs

1. Une structure de page identique pour les trois rôles : en-tête dégradé → carte d'identité →
   cartes d'information du rôle → section « Paramètres ».
2. Un **écran Paramètres** unique, plein écran, atteignable depuis chaque profil (icône engrenage
   dans l'en-tête **et** bouton explicite en bas de page) et depuis n'importe quel avatar d'en-tête.
3. Le thème (Système / Clair / Sombre) y est réglable, persisté, comme aujourd'hui.
4. Une section **Notifications** par rôle dont chaque interrupteur a un **effet réel et visible**.

## Non-objectifs

- Pas de notifications push, pas de canal de notification, pas de permission supplémentaire
  (`POST_NOTIFICATIONS` n'est pas demandée), pas de rappels planifiés (WorkManager).
- Pas de nouvelle requête Supabase : les interrupteurs sont des préférences locales (ou des colonnes
  déjà existantes côté serveur pour l'enseignant).
- On ne touche pas à l'écran « Paramètres & Système » de l'admin dans cette passe (il reste tel
  quel, hors périmètre).

## Anti-objectifs

- Un interrupteur décoratif sans effet : interdit (règle « real data only » du projet).
- Des données de démonstration codées en dur dans le nouvel écran Paramètres.

## Approches examinées

| # | Approche | Ce qu'elle coûte / rapporte |
|---|---|---|
| A | Enrichir chaque profil sur place, garder les réglages dans la feuille `AccountSheet` | Le moins de travail, mais la feuille est à l'étroit, n'a pas la place pour des notifications, et les trois profils restent dissemblables. Écartée. |
| B | **Un modèle de page commun aux trois rôles + un écran Paramètres plein écran** | Structure identique partout, de la place pour thème + notifications, un seul endroit à faire évoluer ; les cartes d'information restent propres à chaque rôle. **Retenue.** |
| C | Transformer les profils en « hub » à onglets (Profil / Paramètres) | Plus lourd, cache l'information derrière un onglet, ajoute de l'état de navigation sans rien apporter. Écartée. |

**Choix : B.** Raison : c'est la seule qui rende les trois pages cohérentes tout en gardant la
richesse propre à chaque rôle (abonnement et assiduité de l'élève, groupes de l'enseignant), et
elle donne à « Paramètres » l'espace dont il a besoin.

## Conception

### Structure commune d'une page profil

```
[ En-tête dégradé (couleur du rôle) ]
   avatar (photo ou initiales)  |  « Mon profil »        [ ⚙ Paramètres ]
                                |  sous-titre du rôle

[ Carte d'identité ]  avatar + nom + pastille de rôle/statut + faits clés (niveau, téléphone, e-mail)
[ Cartes d'information du rôle ]
[ Carte « Paramètres » ]  titre + description + bouton « Ouvrir les paramètres »
```

- Élève : identité (anneau de progression + « N/4 séances »), Abonnement, Mes cours, Assiduité.
- Enseignant : identité (matières, expérience, présentation), Mes groupes (chiffres réels).
- Admin : identité (nom, fonction, téléphone, e-mail — 100 % local) + bouton « Modifier mes
  informations » qui bascule la même page en mode édition (le formulaire actuel).

### Écran Paramètres

Route `settings`, atteignable par :
- l'icône engrenage de l'en-tête de chaque profil,
- le bouton « Ouvrir les paramètres » en bas de chaque profil,
- n'importe quel avatar d'en-tête (`onAvatarClick`), qui ouvre désormais Paramètres au lieu de la
  feuille `AccountSheet` (la feuille est supprimée : son contenu vit dans Paramètres).

Sections :

1. **Compte** — carte d'identité (avatar, nom, e-mail, pastille de rôle) + bouton « Se déconnecter ».
2. **Apparence** — contrôle segmenté Système / Clair / Sombre (réel, persisté dans
   `AppPreferences.themeMode`).
3. **Notifications** — interrupteurs propres au rôle (voir ci-dessous).
4. **Données & synchronisation** — admin uniquement : « Synchronisation automatique ».

### Interrupteurs de notifications : chaque effet est réel

| Rôle | Interrupteur | Effet réel |
|---|---|---|
| Élève | « Notifications de l'académie » | Affiche ou masque la pastille de non-lus sur la cloche du tableau de bord (le compteur reste celui du miroir). |
| Enseignant | « Recevoir les messages des élèves » | Colonne serveur `acceptsStudentMessages` (déjà existante) : les élèves voient ou non le bouton de contact. |
| Enseignant | « Afficher mon profil sur l'accueil des élèves » | Colonne serveur `isFeaturedOnStudentHome` (déjà existante). |
| Admin | « Alertes de gestion » | Affiche ou masque la section « Alertes » du tableau de bord admin (données réelles du miroir). |
| Admin | « Synchronisation automatique » | Quand elle est désactivée, `SupabaseSyncRepository.shouldSync` renvoie `false` : plus de snapshot automatique au lancement, seuls les rafraîchissements demandés passent. |

Les deux interrupteurs de l'enseignant sont des colonnes serveur : ils quittent la page profil
(« Diffusion & contact ») pour Paramètres, pour qu'il n'y ait qu'une seule surface de réglage.

**Ce qui n'est pas fait, et pourquoi** : aucun interrupteur « rappels de séance », « nouveau
élève », « rapport hebdomadaire » — l'app n'a aucun mécanisme de notification, donc ces
interrupteurs n'auraient aucun effet. On ne les invente pas.

### Lecture réactive des préférences

Un interrupteur changé dans Paramètres doit être visible immédiatement sur le tableau de bord.
`AppPreferences` expose donc un `Flow<Boolean>` par clé (`notificationFlow(id, default)`),
construit sur `OnSharedPreferenceChangeListener`. Les écrans consommateurs font
`collectAsStateWithLifecycle()`. Aucun redémarrage nécessaire.

### Préférences ajoutées

- `notificationEnabled(id, default)` / `setNotificationEnabled(id, enabled)` — stockage `notif_<id>`.
- Clés : `student.notifications` (défaut `true`), `admin.alerts` (défaut `true`),
  `admin.autoSync` (défaut `true`).

### Contrainte « real data only »

Tout ce que l'écran Paramètres affiche est soit une préférence locale réellement lue par l'app,
soit une colonne serveur réellement écrite. Aucune valeur inventée.

## Ce qui reste au constructeur

- Le détail visuel exact des cartes (espacements, icônes) : suivre les jetons existants
  (`LZCard`, `LZDivider`, `LZText`, `LZSize.ScreenPadding`).
- Le libellé exact des sous-titres de rôle.
