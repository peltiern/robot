# Installation du robot sur Jetson Nano (version Docker)

Cette procédure couvre le déploiement de l'application Java du robot sur le Jetson Nano
sous forme de conteneur Docker. Elle se déroule en trois étapes :

1. [Construire et publier l'image Docker](#1-construire-et-publier-limage-docker) — depuis le poste de développement ;
2. [Préparer le Jetson Nano](#2-préparer-le-jetson-nano) — copie des fichiers et installation de Docker (une seule fois) ;
3. [Lancer le robot](#3-lancer-le-robot).

Pour les déploiements suivants, voir [Mettre à jour le robot](#4-mettre-à-jour-le-robot).

## Prérequis

- **Poste de développement** : Docker avec `buildx` (build multi-architecture), un compte
  Docker Hub, et le jar `robot-core.jar` construit (voir `create_jar_file.txt`).
- **Jetson Nano** : accessible en SSH sur le réseau local. Son hostname est `wall-e`,
  annoncé en mDNS sous `wall-e.local` (si le nom ne répond pas, retrouver l'IP avec
  `nmap -sn 192.168.0.0/24` ou dans les baux DHCP de la box). Utilisateur `jetson`
  dans les exemples.

## 1. Construire et publier l'image Docker

Depuis le poste de développement, dans le dossier `robot-core/installation/Robot/docker` :

```bash
# Connexion à Docker Hub (ne pas mettre le mot de passe dans la commande : il resterait
# dans l'historique du shell — le saisir à l'invite)
docker login -u <utilisateur>

# Construction multi-architecture (arm64 pour le Jetson, amd64 pour les tests sur PC)
# et publication sur Docker Hub. Le contexte de build est robot-core/ (../../../).
docker buildx build -f Dockerfile --platform linux/arm64,linux/amd64 -t peltiern/robot-wall-e:java-25 --push ../../../
```

> **Important** : le tag doit correspondre à celui référencé par `docker-compose.yml`
> (`peltiern/robot-wall-e:java-25`). Au 2026-07-21, le dépôt Docker Hub `robot-wall-e`
> est vide (les anciens push allaient sur `robot-docker-java:java-21`, incompatible avec
> le jar Java 25) : **ce push est indispensable avant le prochain déploiement**.

## 2. Préparer le Jetson Nano

Cette étape n'est nécessaire qu'à la première installation (ou pour mettre à jour les
fichiers embarqués).

### 2.1. Copier les fichiers du robot

Le zip `robot-core/installation/Robot.zip` contient l'arborescence `Robot/` nécessaire à
l'exécution (voir [Contenu du dossier Robot](#contenu-du-dossier-robot)).

```bash
# Depuis le poste de développement
scp installation/Robot.zip jetson@wall-e.local:~/

# Connexion au Jetson Nano
ssh jetson@wall-e.local

# Sur le Jetson : décompression à la racine du home
unzip Robot.zip
rm Robot.zip
```

### 2.2. Installer Docker

```bash
# Mise à jour de la liste des paquets
sudo apt-get update

# Docker est-il déjà installé ?
docker --version

# Sinon, installation
sudo apt-get install -y docker.io

# Permettre l'utilisation de docker sans sudo
sudo usermod -aG docker $USER
newgrp docker    # ou se déconnecter/reconnecter

# Vérification
docker run --rm hello-world
```

### 2.3. Installer docker-compose

```bash
# docker-compose est-il déjà installé ?
docker-compose --version

# Sinon, installation
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
docker-compose --version
```

### 2.4. Configurer les secrets

Le conteneur lit ses variables d'environnement (clés d'API, configuration) dans
`~/Robot/Programme/configuration/.env`. Ce fichier **ne fait pas partie du zip et ne doit
jamais être versionné** : le créer/compléter à la main sur le Jetson (`OPENAI_API_KEY`,
`GOOGLE_SPEECH_API_KEY`, ...).

## 3. Lancer le robot

```bash
cd ~/
docker-compose -f Robot/docker/docker-compose.yml up
```

Le conteneur :

- monte `~/Robot/Programme` (jar, sons, configuration, ...) dans `/Robot/Programme` ;
- monte `/dev` et tourne en mode `privileged` pour accéder au matériel (Phidgets, micro,
  manette) ;
- expose l'application (et la webapp) sur le port **8080**.

L'application démarre automatiquement (`java -jar /Robot/Programme/robot-core.jar`).

## 4. Mettre à jour le robot

L'image et le jar sont deux artefacts indépendants : l'image ne contient que
l'environnement d'exécution (JRE, sox, Phidgets), le jar est monté par volume depuis
`~/Robot/Programme/`. Selon ce qui a changé, la mise à jour n'est pas la même.

### 4.1. Le code Java a changé → nouveau jar

```bash
# Sur le poste de développement : construire le jar (voir create_jar_file.txt)
mvn clean package

# L'envoyer sur le Jetson (il remplace l'ancien)
scp target/robot-core.jar jetson@wall-e.local:~/Robot/Programme/

# Sur le Jetson : redémarrer le conteneur pour relancer l'application
docker-compose -f Robot/docker/docker-compose.yml restart
```

Pas besoin de toucher à l'image : le conteneur relit le jar depuis le volume au
redémarrage.

### 4.2. Le Dockerfile a changé → nouvelle image

```bash
# Sur le poste de développement : reconstruire et publier l'image
# (commande buildx de l'étape 1)
docker buildx build -f Dockerfile --platform linux/arm64,linux/amd64 -t peltiern/robot-wall-e:java-25 --push ../../../

# Sur le Jetson : récupérer la nouvelle image et recréer le conteneur
docker-compose -f Robot/docker/docker-compose.yml pull
docker-compose -f Robot/docker/docker-compose.yml up -d
```

> Un simple `restart` ne suffit pas ici : il relance l'ancien conteneur avec l'ancienne
> image. C'est `up` qui détecte la nouvelle image et recrée le conteneur.

Si le `docker-compose.yml` lui-même a changé, le recopier aussi sur le Jetson
(`scp installation/Robot/docker/docker-compose.yml jetson@wall-e.local:~/Robot/docker/`)
puis `up -d`. Et si le code **et** l'environnement ont changé (ex. montée de version
Java), enchaîner les deux : nouveau jar, puis pull + up.

## Contenu du dossier Robot

| Dossier | Rôle |
|---|---|
| `Robot/docker/` | `Dockerfile` et `docker-compose.yml` |
| `Robot/Programme/` | jar, `configuration/` (dont `.env`), `sounds/`, `synthese-vocale/`, `reconnaissanceVocale/`, `vision-artificielle/`, `memoire/`, `jinput/` |
| `Robot/third-party/` | règles udev Phidgets, outils ReSpeaker et sixpair |

## Remarques et points à corriger

- `docker-compose` (binaire v1, ancien) pourrait être remplacé par le plugin
  `docker compose` (v2) si le dépôt Docker du Jetson le permet.
- La variable `JAVA_TOOL_OPTIONS=-Dnet.java.games.input.librarypath=/Robot/Programme/jinput`
  du compose sert à la manette (natifs jinput) ; elle disparaîtra avec la migration
  prévue vers input4j (sans natifs).
