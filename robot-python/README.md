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
docker/build.sh nvcr.io/nvidia/l4t-pytorch:r32.7.1
```

### Lancer l'image
```
sudo docker/run.sh -c <image_id> -v /home/jetson/jetson-inference/face-recognition:/face-recognition
```

### Sauvegarder l'image
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

### Récupérer l'image
```
sudo docker pull peltiern/jetson-nano-computer-vision:0.1.0
```

### Lancer l'image
```
sudo docker/run.sh -c peltiern/jetson-nano-computer-vision:0.1.0 -v /home/jetson/Robot/vision-artificielle/ ... :/face-recognition
```