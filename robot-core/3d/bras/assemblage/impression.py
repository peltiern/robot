#!/usr/bin/env python3
"""
Fichiers à imprimer des trois pièces imprimées du bras : chacune est reprise telle qu'elle est dans le bras
(jeux d'impression compris), couchée sur la face qui laisse le moins de surplomb, posée à z = 0 au centre du
plateau.

  - cloison : face arrière (x = 97,2) sur le plateau, comme la cloison de la v5 ; seuls restent des ponts
    courts (13 mm au plus, au-dessus du passage du fil du D85MG) ;
  - liaison : face avant (x = 170, celle du goTUBE) sur le plateau : portée du roulement et embout REX
    sortent ronds. Le fond de la poche du HS-65MG devient un pont de 37 mm : supports dans la poche,
    retirés par la fenêtre du dessus (couchée sur son fond comme la v5, la portée et l'embout seraient
    en surplomb et pas ronds) ;
  - plaque de bout : face extérieure (x = 192) sur le plateau ; les poches d'écrous font des ponts de 7 mm.

Écrit impression/*.stl sur le disque externe.
"""
import os, sys
import numpy as np, trimesh

ICI = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, ICI)
from recaler import SORTIE

DOSSIER = os.path.join(SORTIE, 'impression')
# (fichier dans le repère du bras, direction du bras qui va vers le plateau, fichier à imprimer)
PIECES = [
    ('cloison/cloison_structurelle_position.stl', (-1, 0, 0), 'cloison_IMPRESSION.stl'),
    ('liaison/liaison_moyeu_position.stl', (1, 0, 0), 'liaison_IMPRESSION.stl'),
    ('plaque_bout/plaque_bout_position.stl', (1, 0, 0), 'plaque_bout_IMPRESSION.stl'),
]


def surplombs(m):
    """Surface tournée vers le bas à plus de 45°, hors de la face posée sur le plateau (mm²)."""
    n, c, a = m.face_normals, m.triangles_center, m.area_faces
    plateau = (n[:, 2] < -0.99) & (c[:, 2] < m.bounds[0, 2] + 0.05)
    return a[plateau].sum(), a[(n[:, 2] < -0.707) & ~plateau].sum()


def main():
    os.makedirs(DOSSIER, exist_ok=True)
    for source, vers_plateau, sortie in PIECES:
        m = trimesh.load(os.path.join(SORTIE, source))
        m.apply_transform(trimesh.geometry.align_vectors(vers_plateau, [0, 0, -1]))
        lo, hi = m.bounds
        m.apply_translation([-(lo[0] + hi[0]) / 2, -(lo[1] + hi[1]) / 2, -lo[2]])
        appui, surplomb = surplombs(m)
        m.export(os.path.join(DOSSIER, sortie))
        print('%-28s %5.1f × %5.1f × %5.1f mm  %5.1f cm³  étanche %s  appui %4.0f mm²  surplomb %4.0f mm²' % (
            sortie, *m.extents, m.volume / 1000, m.is_watertight, appui, surplomb))


if __name__ == '__main__':
    main()
