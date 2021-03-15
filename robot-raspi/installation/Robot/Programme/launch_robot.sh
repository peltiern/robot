#!/bin/bash
export ROBOT_HOME="/home/pi/Robot/Programme"
export GOOGLE_APPLICATION_CREDENTIALS="$ROBOT_HOME/google-credentials/Robot-bb7a2da6474d.json"

java -jar robot-raspi-1.0-SNAPSHOT.jar
