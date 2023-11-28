Commandes à faire une fois que le dossier Robot a été copier sur le Jetson Nano

### Définir le runtime NVIDIA par défaut pour le démon DOCKER
https://github.com/dusty-nv/jetson-containers/tree/bc8d0264ef25aa0d1d25a54e4658f491d2fa130f#docker-default-runtime
`sudo gedit /etc/docker/daemon.json`

```
{
    "runtimes": {
        "nvidia": {
            "path": "nvidia-container-runtime",
            "runtimeArgs": []
        }
    },

    "default-runtime": "nvidia"
}

```

### Récupérer le contenu de jetson-inference pour pouvoir lancer le build
```
git clone --recursive --depth=1 https://github.com/dusty-nv/jetson-inference
cd ~/jetson-inference
```

### Remplacer le Dockerfile
```
mv Dockerfile Dockerfile.old
cp ~/Robot/Programme/vision-artificielle/Dockerfile Dockerfile
```

### Lancer le build avec le nouveau Dockerfile
```
docker/build.sh nvcr.io/nvidia/l4t-pytorch:r32.7.1
```

### Lancer l'image
```
sudo docker/run.sh -c <image_id> -v /home/jetson/Robot/Programme/vision-artificielle:/vision-artificielle
```

### Sauvegarder le conteneur
```
sudo docker commit <image_id> jetson-dlib-face-recognition
```

### Tagger l'image
```
sudo docker tag jetson-dlib-face-recognition:latest peltiern/jetson-nano-computer-vision:0.1.0
```

### Pousser l'image
```
sudo docker push peltiern/jetson-nano-computer-vision:0.1.0
```