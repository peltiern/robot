FROM ubuntu:24.04
# Test avec openJDK 11 en image de base ne fonctionne pas. Les moteurs ne sont pas initialisés (problèmes avec l'USB ?)

ENV DEBIAN_FRONTEND=noninteractive
ENV LANG=C.UTF-8

# libatomic1 : dependance transitive de libvosk.so (Kaldi) en aarch64, absente par defaut.
RUN apt-get update -y && \
    apt-get install -y --no-install-recommends \
        alsa-utils \
        libsndfile1 && \
    apt-get install -y sox && \
    apt-get install -y libjpeg8 && \
    apt-get install -y libatomic1 && \
    apt-get -y install v4l-utils && \
    apt-get install -y openjdk-25-jre && \
    apt-get install -y curl && \
    curl -fsSL https://www.phidgets.com/downloads/setup_linux | bash -  && \
    apt-get install -y libphidget22-dev \
            libphidget22java && \
    apt-get install -y udev && \
    apt-get clean

COPY /installation/Robot/third-party/phidgets/99-libphidget22.rules /etc/udev/rules.d/

EXPOSE 8080

CMD ["/bin/sh", "-c", "udevadm trigger && exec java -jar /Robot/Programme/robot-core.jar"]
