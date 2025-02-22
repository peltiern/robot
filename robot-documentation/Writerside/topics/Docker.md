# Docker

Le noyau du robot (_robot-core_) est une application JAVA qui peut être exécuter à l'intérieur d'un conteneur Docker ayant un JDK installé.
Un Dockerfile a été créé spécifiquement pour créer une image Docker contenant le JDK mais écalement toutes les dépendances nécessaires à son utilisation (librairies pour le contrôle des moteurs, la gestion du son, de la webcam, des ports USB, ...).

Pour créer cette image à la fois sur les architectures AMD64 et ARM64 (ex : Jetson Nano), et la pousser sur un Hub, exécuter, à partir du dossier _robot-core/installation/Robot/docker_ :
```shell
docker login -u <login_hub_docker> -p <mot_de_passe>
docker buildx build -f Dockerfile --platform linux/arm64,linux/amd64 -t <votre_hub_docker>/<nom_image> --push ../../../
```
