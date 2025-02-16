FROM ubuntu:22.04

ENV DEBIAN_FRONTEND=noninteractive
ENV LANG C.UTF-8

# Install package dependencies
RUN apt-get update -y && \
    apt-get install -y --no-install-recommends \
        alsa-base \
        alsa-utils \
        libsndfile1-dev && \
    apt-get install -y sox && \
    apt-get install -y libjpeg8 && \
    apt-get -y install v4l-utils && \
    apt-get install -y openjdk-11-jre && \
    apt-get install -y curl && \
    curl -fsSL https://www.phidgets.com/downloads/setup_linux | bash -  && \
    apt-get install -y libphidget22-dev \
            libphidget22java && \
    apt-get install -y udev && \
    apt-get clean

COPY /installation/Robot/third-party/phidgets/99-libphidget22.rules /etc/udev/rules.d/

EXPOSE 8080

CMD udevadm trigger && \
    java -jar /Robot/Programme/robot-core.jar
