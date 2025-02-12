FROM amazoncorretto:11

WORKDIR /robot-core

COPY /target/*.jar robot-core.jar

EXPOSE 8080

VOLUME [ "/dev/bus/usb" ]

CMD ["java", "-jar", "robot-core.jar"]
