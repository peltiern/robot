#!/bin/bash
export ROBOT_HOME="/home/jetson/Robot/Programme"
export GOOGLE_APPLICATION_CREDENTIALS="$ROBOT_HOME/google-credentials/Robot-bb7a2da6474d.json"
export OPENAI_API_KEY="<YOUR_OPENAI_API_KEY>"

java -jar robot-core.jar
