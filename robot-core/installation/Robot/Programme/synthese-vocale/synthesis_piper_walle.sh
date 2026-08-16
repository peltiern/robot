#!/bin/bash

# Coloration "Wall-E" de la voix Piper (fr_FR-siwis-medium), validee a l'oreille le 2026-08-16.
#
#   pitch -200   : -2 demi-tons, la voix siwis est feminine et aigue
#   highpass 450 : coupe les graves que le haut-parleur du robot ne rend pas de toute facon
#   lowpass 3200 : bande passante d'un petit haut-parleur de jouet
#   overdrive    : grain sature, le gresillement electronique
#   chorus       : dedouble legerement la voix (effet "machine")
#   flanger      : le balayage lent qui donne le tremblement de Wall-E
#   gain -n -2   : renormalise, les effets ci-dessus font saturer

echo $1
play $1 pitch -200 highpass 450 lowpass 3200 overdrive 12 8 chorus 0.7 0.9 45 0.5 0.4 2 -t flanger 0 2 0 71 0.5 sin 25 gain -n -2
