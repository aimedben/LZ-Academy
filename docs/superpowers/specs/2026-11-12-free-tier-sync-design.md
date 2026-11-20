# Design court — Restreindre l'usage Supabase au niveau gratuit (2026-11-12)

## Intent & pourquoi
L'app LZ Academy doit rester 100 % fonctionnelle sur l'offre gratuite Supabase
(500 Mo base, 1 Go storage, 5 Go egress/mois, ~50k MAU) et ne jamais surcharger
le serveur, même avec 381 élèves. L'app est offline-first (miroir Room) : le
serveur n'est qu'une source de rafraîchissement rare.

## Objectifs / non-objectifs
- Objectif : minimiser requêtes + egress ; token jamais expiré inutilement ;
  photos téléchargées une seule fois par appareil.
- Non-objectif : temps réel (Realtime), sync en arrière-plan permanent.
- Anti-objectif : que l'app dépende du réseau pour fonctionner.

## Approches considérées
1. **Sync sobre (choisi)** : throttling 15 min minimum entre auto-syncs, sync
   forcée manuelle, refresh token au lieu de re-login, cache disque des photos.
2. Realtime/websockets : rejetté — connexions permanentes, inutile pour un
   miroir offline.
3. Edge function agrégeant élèves+abonnements en 1 réponse : micro-gain (2
   requêtes → 1), complexité supplémentaire — rejeté.

## Décisions
- `AppPreferences.lastSyncAt` : auto-sync (démarrage app, ouverture écran)
  sautée si < 15 min et si le miroir n'est pas vide. `loginAndSync` sync
  toujours. Bouton « Actualiser » admin = sync forcée.
- `SupabaseApi` : stocke le `refresh_token` ; en cas de 401 sur un fetch,
  un seul `grant_type=refresh_token` puis retry unique. Fini les syncs mortes.
- `RemoteAvatar` : cache disque (`cacheDir/avatars`, bitmap 512 px max, JPEG)
  en plus du cache mémoire → chaque photo téléchargée 1 fois par appareil.
- Payloads déjà minimaux (colonnes sélectionnées, pas de count) — inchangé.
- Écritures rares par nature (renouvellement admin, futur appel) — inchangé.

## Laissé au builder
- Emplacement exact du bouton Actualiser ; nettoyage du cache photos (LRU
  simple si > 50 Mo) ; logs de diagnostic à garder en Log.d.
