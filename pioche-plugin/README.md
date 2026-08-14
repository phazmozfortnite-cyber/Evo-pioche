# Pioche Évolutive — Plugin Paper 26.2

Plugin Java/Paper implémentant le système décrit précédemment :
- Paliers tous les 10 niveaux (1, 10, 20, ... 100, + le mini-palier 75)
- Une quête de minage (X blocs d'un minerai précis) pour débloquer chaque palier
- Efficacité +1 à chaque palier de 10
- Fortune, zone de minage (AoE), Hâte, portée et bonus de vente qui évoluent avec le niveau
- Progression stockée **sur l'item lui-même** (PersistentDataContainer), donc liée à la pioche physique

## ⚠️ Important — compilation

Ce code a été écrit dans un environnement sans accès à `repo.papermc.io` : **il n'a donc
pas pu être compilé ni testé ici**. Il suit l'API Paper moderne (Adventure `Component`,
`Enchantment` par instance, PDC...) telle que documentée pour la 26.x, mais tu dois :

1. Ouvrir le projet avec ton IDE (IntelliJ IDEA recommandé) et lancer `mvn clean package`
   en étant connecté à Internet, pour que Maven télécharge `paper-api` depuis
   `https://repo.papermc.io/repository/maven-public/`.
2. Vérifier dans `pom.xml` que la version résolue de `paper-api` correspond bien à ta
   version de serveur (la plage `[26.2.build,)` prend normalement la dernière build stable).
3. Vérifier la valeur `api-version` dans `plugin.yml` : Mojang/Paper ayant changé de schéma
   de versioning (année.version), le format attendu peut avoir légèrement évolué — un
   coup d'œil à https://docs.papermc.io suffit pour confirmer.
4. Le point le plus fragile techniquement est `PickaxeEffectsTask` : le nom exact de
   l'`Attribute` gérant la portée d'interaction avec les blocs a changé plusieurs fois
   dans l'API. Le code essaie plusieurs noms connus et désactive proprement le bonus de
   Reach s'il n'en trouve aucun (log d'avertissement, pas de crash) — à vérifier/ajuster
   une fois compilé contre ta vraie version.

Une fois compilé, dépose le `.jar` généré dans `plugins/` sur ton serveur Paper 26.2.

## Utilisation en jeu

- `/pioche give [joueur]` — donne une pioche évolutive niveau 1 (admin)
- `/pioche info` — affiche le niveau et la progression de la pioche en main
- `/pioche level <joueur> <niveau>` — force un niveau (utile pour tester, admin)
- `/pioche reload` — recharge `config.yml` à chaud (admin)

## Personnalisation

Tout se règle dans `src/main/resources/config.yml` : quantités des quêtes, minerais
ciblés, taille des zones de minage, Fortune/Efficacité/Hâte/Reach/bonus de vente par
palier. Ajouter un nouveau palier = ajouter une entrée dans la section `levels`, aucune
recompilation du code Java n'est nécessaire.

## Pistes d'amélioration possibles

- Brancher le bonus de vente sur Vault (économie) pour une vraie commande `/vendre`.
- Remplacer le stockage PDC par une base de données si tu veux que la progression soit
  liée au joueur plutôt qu'à l'objet (empêche la perte de progression si la pioche est
  détruite/volée).
- Ajouter des effets (particules, titre plein écran) différents selon l'ampleur du palier
  franchi (un niveau 100 mérite plus de festivités qu'un niveau 20).
