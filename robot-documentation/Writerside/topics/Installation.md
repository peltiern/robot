# Installation de l'application sur le Jetson Nano

**Sur une machine, à distance :**

Copier le ZIP contenant les dossiers et fichiers nécessaire au robot :
```shell
scp installation/Robot.zip   jetson@192.168.xxx.xxx.:~/
```

Se connecter en ssh sur le Jetson Nano :
```shell
ssh jetson@192.168.xxx.xxx
```

Dézipper le dossier :
```shell
unzip Robot.zip
rm Robot.zip
```

Mettre à jour de la liste des paquets :
```shell
sudo apt-get update
```

Tester l'installation de _Docker_ :
```shell
docker --version
```

Installer _Docker_ s'il n'est pas installé :
```shell
sudo apt-get install -y docker.io
```

Ajouter l'utilisateur au groupe docker pour éviter d'utiliser sudo :
```shell
sudo usermod -aG docker $USER
newgrp docker
```

Vérifier que _Docker_ fonctionne :
```shell
docker run --rm hello-world
```

Tester l'installation de _docker-compose_ :
```shell
docker-compose --version
```

Installer _docker-compose_ s'il n'est pas installé :
```shell
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose
docker-compose --version
```

Lancer le conteneur Docker. Cela lancera l'application JAVA :
```shell
cd ~/
docker-compose -f Robot/docker/docker-compose.yml up
```