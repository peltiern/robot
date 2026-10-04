#!/usr/bin/env python3
"""
Copies allégées des modèles pour la visionneuse : bras-assemble-leger.glb et bras-v5-leger.glb.

Les STEP du catalogue dessinent le filetage de chaque vis et écrou : 1,66 million de triangles affichés
à chaque image, et la démo des mouvements saccadait (2026-10-03). L'œil n'en a pas besoin ; les calculs,
si : audit, frottements et câbles relisent toujours les maillages complets (bras-v5.glb, STEP).
"""
import os, sys
import numpy as np, trimesh

ICI = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, ICI)
from recaler import SORTIE

TRIANGLES_VISSERIE = 600          # un filetage devient une surface lisse
PLANCHER = 300                      # en dessous, on ne touche à rien
# Une simplification à l'aveugle (12 % partout) a déchiré les plaques perforées : chaque essai doit rester
# à moins d'ECART_MAX de la pièce d'origine, sinon on essaie moins fort, et à la fin on garde l'original.
ECART_MAX = 0.15
PARTS = (0.12, 0.25, 0.5)


def ecart(a, b):
    """Écart des deux surfaces, dans les deux sens (99e centile, en mm), mesuré à la surface exacte : un
    nuage de points de comparaison donnait un plancher de 0,5 mm sur les grandes plaques."""
    d1 = trimesh.proximity.closest_point(b, trimesh.sample.sample_surface(a, 2000, seed=1)[0])[1]
    d2 = trimesh.proximity.closest_point(a, trimesh.sample.sample_surface(b, 2000, seed=2)[0])[1]
    return float(max(np.percentile(d1, 99), np.percentile(d2, 99)))


def visserie(nom):
    ref = nom.split('#')[0]
    return ref.startswith(('2800-', '2802-', '2812-', '2801-')) or 'vis ' in nom or 'écrou' in nom


def alleger(source, cible, noms=None):
    s = trimesh.load(os.path.join(SORTIE, source), force='scene')
    avant = apres = 0
    for noeud in s.graph.nodes_geometry:
        g = s.graph[noeud][1]
        m = s.geometry[g].copy()
        # Les STEP arrivent avec un sommet par face : sans fusion, la simplification ne peut rien recoudre
        # et déchire la pièce.
        m.merge_vertices()
        n = len(m.faces); avant += n
        nom = noms(noeud) if noms else noeud
        buts = ([TRIANGLES_VISSERIE, 2 * TRIANGLES_VISSERIE, 4 * TRIANGLES_VISSERIE] if visserie(nom)
                else [int(n * p) for p in PARTS])
        for but in buts:
            if n <= PLANCHER or but >= n:
                break
            l = m.simplify_quadric_decimation(face_count=max(but, PLANCHER))
            if ecart(m, l) < ECART_MAX:
                s.geometry[g] = l
                break
        apres += len(s.geometry[g].faces)
    s.export(os.path.join(SORTIE, cible))
    print('%-22s %8d → %7d triangles' % (cible, avant, apres))


if __name__ == '__main__':
    import json
    v5 = json.load(open(os.path.join(SORTIE, 'v5.json')))
    alleger('bras-assemble.glb', 'bras-assemble-leger.glb')
    alleger('bras-v5.glb', 'bras-v5-leger.glb',
            lambda k: '%s %s' % (v5['pieces'][int(k[3:])]['nom'], v5['pieces'][int(k[3:])]['ref']))
