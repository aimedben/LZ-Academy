# LZ Academy — synthèse design d'après Mobbin

Date : 20 novembre 2026
Statut : **proposition, aucune modification de code effectuée**
Sources : Mobbin (catégorie Education + pattern Schedule), captures dans `docs/design-refs/`

---

## 1. Objet

Choisir une direction visuelle et interactionnelle pour l'app, à partir de ce qui se fait
de mieux sur Mobbin dans l'éducation et la planification, **sans inventer de données** :
tout ce qui s'affiche doit venir de Supabase, du ledger local ou des préférences réelles.

## 2. Ce que Mobbin expose réellement

| Point | Constat |
|---|---|
| Accès | `browse` et `screens` sont derrière un login. Seules les pages SEO publiques sont exploitables. |
| Pages utiles | `/explore/mobile/app-categories/education`, `/explore/mobile/screens/<pattern>` |
| Ce qu'on obtient | vignettes réelles + `alt` = « <App> iOS <Pattern> screen » + lien `/explore/screens/<uuid>` |
| Ce qu'on n'obtient pas | haute résolution, specs, flows complets, exports Figma |
| Contournement | contact sheets Playwright (`mobbin-edu-sheet.png`, `mobbin-schedule-sheet.png`) |

Conclusion : Mobbin sert de **référence de composition**, pas de source de specs. Les
décisions ci-dessous sont donc argumentées sur des captures réelles, pas sur des suppositions.

### Écrans de référence retenus

| App | Pattern | Ce qu'on retient |
|---|---|---|
| Headway | Home | stat row 3 colonnes, chip « mission du jour », rails horizontaux |
| Brilliant | Home / Progress | série + points de semaine, une grosse carte d'action, « reprendre » avec barre |
| Khan Academy | Class & Lesson Detail | fil d'unité, score de maîtrise, liste de compétences, CTA pleine largeur, « ensuite » |
| Quizlet | Class & Lesson Detail | tuiles d'action (4 max) |
| Fable | Home / Lists | segmented tabs + filter chips + list rows avatar / meta / activité |
| MasterClass | Home | hero image + 2 CTA + rail |
| Babbel | Home | salutation + « activity tracker 1/7 » + grande carte CTA |
| Duolingo | Home | parcours de nœuds (à ne pas copier tel quel, voir §6) |
| Plata Card | Schedule | chips de date (Aujourd'hui / Cette semaine / Autre) + créneaux en pilules + CTA collant |
| KakaoBank | Schedule | bande de semaine + grille mois + jour sélectionné en pastille pleine + CTA collant |
| Saturn Calendar | Schedule | feuille de formulaire en lignes (titre, journée entière, début/fin, répétition, calendrier) |
| stoic. | Schedule | question en titre + 3 lignes avec interrupteur + pilule noire de validation |

Liens d'écran (vérifiés) :
`/explore/screens/8a17b957-b9fa-4480-b956-328db04b432a` (Headway) ·
`/explore/screens/05b3f5f2-735b-4e16-b052-3c2cd900d975` (Brilliant) ·
`/explore/screens/1121c763-f18e-4455-af01-7e3f225cd0ab` (Khan) ·
`/explore/screens/2327f459-e7aa-4ea4-835d-e1a7971c12a5` (Quizlet) ·
`/explore/screens/7ab581a2-48c5-4124-8a75-39070b48241c` (Plata Card) ·
`/explore/screens/eef49b04-fc40-4cb7-8399-e0e6d7c0c2d6` (Fable)

---

## 3. Les quatre patterns qui ressortent

1. **Une seule métrique héros par écran.** Les bons dashboards mettent *une* chose en avant
   (la séance suivante, la mission du jour, la pratique à reprendre) et relèguent le reste
   en lignes compactes. Les nôtres empilent 5 cartes de poids visuel égal.
2. **La ligne compacte remplace la carte.** Partout (Fable, Headway, Saturn, stoic.), les
   listes secondaires sont des lignes : icône 40–44 dp, titre, sous-titre, chevron ou
   pastille. Ça allège énormément un écran long.
3. **Le temps se manipule par bande de semaine, pas par calendrier complet.** KakaoBank et
   Plata montrent 7 jours + un état sélectionné très lisible ; le mois n'apparaît que si on
   en a besoin. C'est exactement notre cas (semaine de séances).
4. **L'action principale est collée en bas.** Plata, Kakao et stoic. ont tous un CTA plein
   en bas d'écran. Nos formulaires admin (créer un élève, une séance) gagneraient à ça.

---

## 4. Trois directions possibles

### Direction 1 — « Carnet » (Headway + Fable)

Fond clair uni, zéro gradient, une seule couleur d'accent, stat row 3 colonnes, listes
compactes, beaucoup de blanc. Identité : calme, adulte, lisible.

- **Pour** : très lisible, excellent pour les parents et les ados, peu de fatigue visuelle.
- **Contre** : on perd le vert/violet actuels, l'app perd sa personnalité « académie » ;
  demande de retoucher tous les écrans d'un coup.
- **Coût** : élevé (refonte transverse).

### Direction 2 — « Tableau de bord à cartes, allégé » (Brilliant + MasterClass)

On garde le header gradient et l'ADN couleur actuel, mais on applique la hiérarchie des
meilleures apps : une métrique héros, une stat row compacte, des rails, des lignes pour le
secondaire.

- **Pour** : continuité totale avec le code existant, risque faible, gain visuel immédiat.
- **Contre** : si on ne supprime pas les doublons, on reste sur l'empilement actuel.
- **Coût** : faible à moyen, écran par écran.

### Direction 3 — « Parcours » (Duolingo + Khan)

Les séances deviennent des étapes, chaque matière a un score de progression, un seul CTA
« continuer ».

- **Pour** : très engageant pour les enfants, forte sensation de progression.
- **Contre** : exige un vrai calcul de progression par matière à partir des présences
  réelles ; sans ça, on tombe immédiatement dans le streak/XP inventé, interdit ici.
- **Coût** : élevé, et risque produit (les enfants ne choisissent pas leurs séances, c'est
  l'admin qui planifie — un « parcours » individualisé n'aurait pas de sens).

### Recommandation : **Direction 2 hybride**

Cartes actuelles conservées, mais :

- hiérarchie « Carnet » (une métrique héros, stat row compacte, lignes pour le secondaire) ;
- planning emprunté à **Plata / KakaoBank** (bande de semaine + créneaux + CTA collant) ;
- gestion admin empruntée à **Fable** (segmented tabs avec compteurs + list rows) ;
- paramètres déjà conformes à **stoic.** (sections + lignes interrupteur + segmented thème),
  on n'y touche pas.

C'est le meilleur rapport gain / risque : l'app reste reconnaissable, mais cesse de répéter
la même information et devient navigable d'un coup d'œil.

---

## 5. Application écran par écran

### 5.1 Dashboard élève — le plus gros gain

**Problème actuel** : `Séances restantes` est affiché **deux fois** (stat card l.216 et
`LZSubscriptionCard` l.272 qui réaffiche `N/total` + « N séances restantes »). Cinq blocs de
même poids visuel se suivent, donc aucun ne ressort.

**Cible (Brilliant + Headway)** :

1. Header gradient conservé : salutation + avatar + cloche.
2. **Stat row 3 colonnes**, en chevauchement du header :
   `Séances restantes N/4` · `Assiduité X/Y` · `Prochaine séance` (jour + heure).
   L'assiduité demande de charger `attendance` côté élève : la table est déjà synchronisée
   (`dao.observeAttendance()`), mais `studentDashboard()` ne la lit pas aujourd'hui — c'est
   un ajout de données, pas de la décoration. Si on ne veut pas y toucher, la 3e colonne
   devient `Matières actives` (déjà calculé) et rien n'est inventé.
3. **Hero unique** = prochaine séance : matière, enseignant, salle, heure, CTA
   « Voir le planning ». C'est le seul bloc qui a le droit d'être gros.
4. **Bande « Cette semaine »** : 6 jours (lundi → samedi, comme le reste de l'app), pastille
   pleine sur aujourd'hui, point coloré si séance réelle. Taper un jour ouvre le planning
   sur ce jour.
5. Rail enseignants (inchangé) + actualités en lignes compactes (déjà le cas avec
   `KidNewsCard`).

`LZSubscriptionCard` disparaît du dashboard et devient l'écran d'abonnement (ou une ligne
dans le profil) : plus de doublon, et l'info « consommée / restante » reste accessible.

### 5.2 Détail d'une séance

**Problème actuel** : le bouton « Ajouter un rappel » affiche
« Rappel programmé 15 min avant le cours » alors qu'aucun rappel n'est programmé. C'est une
donnée inventée, contraire à la règle projet.

**Cible (Khan + Plata)** :

- Feuille plein écran (pas un petit sheet) : bloc date/heure en gros, puis lignes
  Salle · Enseignant · Groupe, puis statut de présence réel s'il existe.
- Actions réelles uniquement :
  - « Contacter l'enseignant » → écran de contact existant ;
  - « Ajouter au calendrier » → `Intent` calendrier (aucune permission) ;
  - « Voir dans le planning ».
- Suppression pure et simple du faux rappel.

### 5.3 Planning élève

La structure actuelle (segmented Jour/Semaine, bande de semaine, chips matière, timeline)
est déjà la bonne. Trois retouches :

- bande de semaine façon **KakaoBank** : 6 colonnes égales (lundi → samedi), jour sélectionné
  en pastille pleine, point de séance, marqueur « aujourd'hui » ;
- remplacer la carte « résumé du jour » par un simple en-tête de section
  (`Jeudi 20 • 3 séances`) pour casser l'empilement de cartes ;
- en mode Semaine, en-têtes de jour **collants** (`stickyHeader`) pendant le scroll.

Le PDF reste tel quel.

### 5.4 Gestion admin (5 onglets)

**Cible (Fable + Plata)** :

- `LZCategoryTabs` → tabs avec **compteurs réels** : `Élèves 381`, `Enseignants N`,
  `Groupes N`, `Salles N`, `Planning`. Le compteur vient des listes déjà chargées.
- Lignes de liste : avatar 44 dp + nom + 2 lignes de méta + pastille de statut à droite
  (Actif / Épuisé / Payé). Aujourd'hui l'information est là mais noyée dans la carte.
- Recherche + **filter chips** par onglet (niveau, statut, groupe).
- Onglet Planning : chips de date (Aujourd'hui / Cette semaine / Autre), créneaux en
  pilules, séances datées en dessous, **CTA collant** « Ajouter une séance ».
- Les formulaires (élève, enseignant, salle, créneau, séance) passent en feuille plein
  écran avec CTA collant, comme Saturn Calendar.

### 5.5 Profils (élève / enseignant / admin)

Déjà restructurés. Deux ajouts seulement :

- **bloc identité** en haut : avatar + nom + pastille de rôle + matricule (mono), au lieu
  d'une simple ligne ;
- sections en lignes avec chevron, déconnexion en bas en couleur erreur (déjà fait dans
  Paramètres, à uniformiser sur les profils).

### 5.6 Paramètres

Rien à changer sur le fond : sections + lignes interrupteur + segmented thème correspondent
déjà au pattern stoic. Seule retouche : passer le sélecteur de thème en 3 tuiles
(Système / Clair / Sombre) avec aperçu, au lieu du seul segmented texte.

---

## 6. Ce qu'on ne copie pas

| Pattern Mobbin | Pourquoi non |
|---|---|
| Série / streak (Duolingo, Brilliant) | il n'existe pas de notion de série quotidienne dans nos données ; la fabriquer = donnée inventée |
| XP, niveaux, badges | aucun modèle serveur, aucun barème validé |
| « Maîtrise 1 300/1 300 » (Khan) | nos séances ne sont pas des exercices notés ; on a des présences, pas des scores |
| Recommandations « pour vous » | rien ne les calcule |
| Feed social / commentaires | hors périmètre |
| Compteur « 3 administrateurs », « sauvegarde 03:00 », « 2FA » déjà présents dans `AdminMoreScreen` | données inventées existantes, à supprimer ou brancher sur du réel (hors périmètre de cette synthèse) |

Règle : **un chiffre affiché = une requête ou un calcul réel**. Un état vide honnête vaut
mieux qu'une jauge décorative.

---

## 7. Lots d'implémentation proposés

| Lot | Contenu | Risque | Vérification |
|---|---|---|---|
| 0 | Supprimer le faux rappel ; supprimer le doublon « séances restantes » | très faible | tests existants + preview |
| 1 | Dashboard : stat row 3 colonnes + hero unique + bande semaine | moyen | preview clair/sombre/360 dp |
| 2 | Planning : bande KakaoBank + en-têtes collants + résumé en ligne | faible | preview + test Compose du tag `student_planning_screen` |
| 3 | Détail séance plein écran + « Ajouter au calendrier » | faible | test unitaire de l'Intent |
| 4 | Gestion admin : tabs à compteurs + list rows + CTA collant | moyen | test Compose `admin_sms_list` + scroll |
| 5 | Profils : bloc identité + tuiles de thème | faible | preview |

Ordre conseillé : 0 → 1 → 2 → 3 → 4 → 5. Le lot 0 corrige deux défauts réels et peut
partir seul.

## 8. Décision attendue

Valider la direction (§4) avant toute écriture de code. Une fois validée : spec courte,
plan en lots, implémentation, `assembleRelease`, installation `--no-streaming`, puis smoke
test quand le téléphone sera libre.
