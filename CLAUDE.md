# Conventions du projet Robot Wall-E

## La langue

Tout s'écrit en **français** : noms de classes, de méthodes, de variables, commentaires, messages
de log, messages de commit. Les seules exceptions sont ce qu'impose un cadre extérieur (`getInstance`,
annotations Spring, protobuf, noms de modèles ONNX).

Un commentaire dit **pourquoi**, jamais **quoi** — le code dit déjà ce qu'il fait. Les commentaires
les plus utiles du projet racontent une panne réelle et ce qu'elle a coûté ; c'est le modèle à suivre.

## Les couches

Le premier niveau de packages est la métaphore du robot, et elle ne bouge pas :

| Package           | Ce qu'on y met                                                        |
|-------------------|-----------------------------------------------------------------------|
| `organes/`        | le corps : `capteurs/` et `actionneurs/`, tout ce qui touche au matériel |
| `memoire/`        | ce que le robot retient — voir plus bas                                 |
| `decisionnel/`    | le cerveau : arbitrage des activités, regard, déclencheurs              |
| `activites/`      | ce que le robot sait faire (conversation, accueil, retrouvailles…)      |
| `services/`       | les services techniques : interfaces par domaine, implémentations par techno |
| `systemenerveux/` | le bus d'évènements et les `RobotEvent`                                  |
| `securite/`       | arrêt d'urgence, chien de garde, surveillance des organes               |
| `web/`            | ce qui parle à l'extérieur : contrôleurs REST, DTO, websocket           |
| `spring/`         | configuration et amorçage, rien d'autre                                  |
| `util/`           | outillage sans métier                                                    |

`memoire/` se lit en deux temps, et la distinction compte :

- `memoire/courtterme/` — ce que le robot a en tête là, maintenant : qui est devant lui, quel visage
  il suit, quel apprentissage est en cours. **Rien n'y est persisté**, tout est perdu au redémarrage,
  et c'est voulu.
- `memoire/longterme/` — ce qui doit lui survivre, un sous-package par notion : `personne`, `visage`,
  `rencontre`, `conversation`.

Une règle qui traverse tout : **l'image ne voyage pas**. L'organe de vision extrait ce qu'il faut
pendant qu'il tient le `Mat` (empreinte, vignette) et ne transmet que le résultat. Aucun `Mat` ne
traverse un évènement ni ne s'attarde en mémoire courte.

## Les noms

- **`XxxRepository`** — l'accès à une table, et rien d'autre. Suffixe uniforme, et **jamais la
  technologie dans le nom** : `ConversationRepository`, pas `MapDbChatMemoryRepository`. Un dépôt ne
  connaît que sa table ; ce qui coordonne plusieurs tables n'est pas un dépôt.
- **Le métier prend un nom de la métaphore**, en français, et c'est lui qui orchestre plusieurs
  dépôts : `RegistrePresence`, `RepertoireDesPersonnes`, `JournalDesRencontres`. Un contrôleur ne
  parle jamais à deux dépôts directement — il passe par là.
- **`ServiceXxx`** — l'interface d'un service technique, dans `services/<domaine>/`.
  **`<Techno>ServiceXxx`** — son implémentation, dans `services/providers/<techno>/`.
  Exemple : `ServiceReconnaissanceVisage` et `OpenCvServiceReconnaissanceVisage`.
- **`XxxController`** — REST, dans `web/controller/`. Il traduit HTTP ↔ métier et ne porte aucune
  règle.
- **`XxxEvent`** — un `RobotEvent` publié sur le bus, dans `systemenerveux/event/`.

Ces règles valent pour tout code neuf. L'existant s'y range au fil des chantiers, pas en une grande
passe.

## Les organes

Tout organe suit le même moule, sans exception : il étend `AbstractOrgane` (ou
`AbstractOrganeWithThread`), implémente `SmartLifecycle`, et publie ce qu'il a à dire sous forme de
`RobotEvent`. **Jamais** de `@Scheduled` doublé d'un `SimpMessagingTemplate` en direct — l'erreur a
été commise deux fois, elle contourne le bus et rend l'organe invisible à la surveillance.

Un bean à **plusieurs constructeurs** doit en annoter un avec `@Autowired`, sinon Spring ne choisit
rien, se rabat sur un constructeur vide qui n'existe pas, et **tout** le contexte échoue au
démarrage. `CablageDesBeansTest` monte la garde ; il vérifie aussi que ce qu'un bean réclame par
constructeur est bien un bean.

## La mémoire longue

Un seul fichier SQLite, `$ROBOT_HOME/memoire/memoire.db`, ouvert par `BaseMemoire`. Le schéma vit
dans `src/main/resources/memoire/schema.sql` et se rejoue à chaque démarrage (`IF NOT EXISTS`).

- Les liens entre tables portent `ON DELETE CASCADE`, et les clés étrangères sont **activées**
  explicitement (SQLite les ignore par défaut). Effacer une personne doit emporter ses visages et ses
  rencontres : une empreinte orpheline ferait « reconnaître » au robot quelqu'un qui n'existe plus.
- Pas de pool de connexions : SQLite n'accepte qu'un écrivain à la fois.
- Toute évolution du schéma s'écrit dans `schema.sql`. C'est ce qui a fait abandonner MapDB, où la
  forme des données n'était écrite nulle part et où ajouter un champ à un record relisait l'existant
  sans erreur, le champ à `null`.

## Les tests

Pas de Phidgets, pas de webcam, pas de micro, pas d'appel à une IA payante : un test qui a besoin du
robot n'est pas un test. Ce qui dépend du temps prend une `Clock` (voir `HorlogeReglable`) ; ce qui
dépend d'un modèle ONNX s'ignore proprement avec `assumeTrue`.

## Le rythme de travail

- **Expliquer avant de corriger** : la cause est exposée et validée avant qu'une ligne ne change.
  Jamais de correctif sur une supposition.
- **Commit après validation** : rien n'est commité avant que la fonctionnalité ait été essayée sur le
  robot. Commits locaux, sans push, messages courts en français.
- Préférer le réglage au code, et la solution simple à la solution complète.

## Maven

`mvn` n'est pas dans le `PATH`. Le binaire est ici :
`~/.m2/wrapper/dists/apache-maven-3.9.11/a2d47e15/bin/mvn`
