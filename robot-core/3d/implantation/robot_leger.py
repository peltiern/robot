#!/usr/bin/env python3
"""
Le robot autour de la caisse, allégé pour la page d'implantation : robot-leger.glb (≈ 7 Mo, mm, repère du modèle).

La caisse, les chenilles, le cou et la tête viennent de ../robot-walle.glb ; les bras, de ../bras/assemblage/robot-anime.glb,
parce que robot-walle.glb a encore les anciens bras de l'export du 2026-09-13. Les deux fichiers partagent le repère
de la maquette (x à droite du robot, y en haut, z vers l'arrière, origine au sol). L'allègement fusionne les sommets
d'une même maille : assez pour un fantôme, pas pour mesurer.
"""
import json
import os

import numpy as np

from lire_glb import charger, grappe, ecrire

ICI = os.path.dirname(os.path.abspath(__file__))
TROIS_D = os.path.dirname(ICI)
PAS = {'chenilles': 3.0, 'tete': 2.0, 'bras': 2.0}        # maille de fusion, mm ; 1,2 ailleurs


def groupe_walle(nom):
    if nom in ('caisse', 'chenilles'):
        return nom
    if 'coque' in nom or 'oeil' in nom or nom in ('tete', 'pignon_inclinaison', 'panoramique'):
        return 'tete'
    return 'cou'


def main():
    groupes = {g: [] for g in ('caisse', 'chenilles', 'cou', 'bras', 'tete')}
    for nom, (v, f) in charger(os.path.join(TROIS_D, 'robot-walle.glb')).items():
        if not nom.startswith('bras'):
            groupes[groupe_walle(nom)].append((v, f))
    anime = os.path.join(TROIS_D, 'bras', 'assemblage')
    noeuds = json.load(open(os.path.join(anime, 'robot-anime.json')))['noeuds']
    for nom, (v, f) in charger(os.path.join(anime, 'robot-anime.glb')).items():
        if noeuds[nom]['groupe'] in ('bras_droit', 'bras_gauche', 'flexion_corps'):
            groupes['bras'].append((v, f))
    sortie = {}
    for g, morceaux in groupes.items():
        VV, FF, n = [], [], 0
        for v, f in morceaux:
            vn, fn = grappe(v.astype(np.float64), f.astype(np.int64), PAS.get(g, 1.2))
            VV.append(vn)
            FF.append(fn + n)
            n += len(vn)
        V, F = np.vstack(VV), np.vstack(FF).astype(np.uint32)
        sortie[g] = [(None, V, F)]
        print('%-10s %7d triangles' % (g, len(F)))
    taille = ecrire(os.path.join(ICI, 'robot-leger.glb'), sortie)
    print('robot-leger.glb : %.1f Mo' % (taille / 1e6))


if __name__ == '__main__':
    main()
