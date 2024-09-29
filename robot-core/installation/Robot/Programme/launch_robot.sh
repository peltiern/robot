#!/bin/bash
export ROBOT_HOME="/home/jetson/Robot/Programme"
export GOOGLE_SPEECH_API_KEY="<YOUR_GOOGLE_SPEECH_API_KEY>"
export OPENAI_API_KEY="<YOUR_OPENAI_API_KEY>"

java -jar robot-core.jar
