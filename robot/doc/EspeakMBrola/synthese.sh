#!/bin/bash

echo $1

espeak -v mb-fr4 -q -s150  --pho "$1" | mbrola  -t 1.2 -f 1.3  -e -C "n n2" /usr/share/mbrola/voices/fr4 - -.au | play  -t au  - pitch 200 tremolo 700  echo 0.9 0.8 60 0.9
