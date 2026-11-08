#!/usr/bin/env python3
"""
Pièces d'essai pour éprouver le clipsage du pignon sur l'embout REX fendu de la liaison, sans imprimer toute la
liaison (demande de Nicolas, 2026-10-10) :

  - l'embout seul : exactement celui de la liaison (mêmes fonctions de liaison_moyeu.py : profil REX, trois doigts,
    lèvre), sur 4 mm de l'épaulement et un socle de Ø 24 × 2 mm pour tenir sur le plateau et à la main. Il s'imprime
    debout comme la liaison (embout vers le haut), pour que les doigts aient la même orientation de couches ;
  - le pignon Slip-Fit 2322-4008-0020 du catalogue, couché sur une face, pour qui n'a pas encore le vrai ; et une
    variante à l'alésage agrandi de 0,2 mm sur plats (7,2 au lieu de 7,0) : un trou imprimé sort plus petit que
    dessiné, et l'essai serait plus sévère qu'avec le pignon en acier.

Écrit impression/essai_embout_IMPRESSION.stl, essai_pignon_2322_IMPRESSION.stl et essai_pignon_2322_alesage_7.2_IMPRESSION.stl
sur le disque externe.
"""
import os, sys
import numpy as np, trimesh
import cadquery as cq

ICI = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, ICI)
from recaler import SORTIE, modele
from liaison_moyeu import PIGNON, R_EPAULEMENT, R_FIL, rex, xcyl, levre_arret, fendre

DOSSIER = os.path.join(SORTIE, 'impression')
EPAULEMENT = 4.0
SOCLE = dict(r=12.0, ep=2.0)


def embout():
    x0_rex = PIGNON[0] - 0.5                       # comme dans liaison_moyeu.construire
    x1 = PIGNON[1] + EPAULEMENT
    piece = (rex(x0_rex, PIGNON[1] + 0.01)
             .union(xcyl(R_EPAULEMENT, PIGNON[1], x1))
             .union(xcyl(SOCLE['r'], x1, x1 + SOCLE['ep']))
             .union(levre_arret(x0_rex)))
    piece = fendre(piece.cut(xcyl(R_FIL, x0_rex - 1, x1 + SOCLE['ep'] + 1)), x0_rex)
    chemin = os.path.join(DOSSIER, 'essai_embout_IMPRESSION.stl')
    cq.exporters.export(piece, chemin, tolerance=0.01, angularTolerance=0.05)
    m = trimesh.load(chemin)
    # debout comme la liaison : le socle sur le plateau, le bout de l'embout en haut
    m.apply_transform(trimesh.geometry.align_vectors([1, 0, 0], [0, 0, -1]))
    lo, hi = m.bounds
    m.apply_translation([-(lo[0] + hi[0]) / 2, -(lo[1] + hi[1]) / 2, -lo[2]])
    m.export(chemin)
    return m


ALESAGE = 7.0                                      # alésage hexagonal du 2322, sur plats (STEP du catalogue)
AGRANDI = 0.2


def pignon():
    m = modele('2322-4008-0020', None)
    m.merge_vertices()
    ax = int(np.argmin(m.extents))                 # l'axe : la plus petite dimension (7 mm)
    m.apply_transform(trimesh.geometry.align_vectors(np.eye(3)[ax], [0, 0, 1]))
    lo, hi = m.bounds
    m.apply_translation([-(lo[0] + hi[0]) / 2, -(lo[1] + hi[1]) / 2, -lo[2]])
    m.export(os.path.join(DOSSIER, 'essai_pignon_2322_IMPRESSION.stl'))
    return m


def pignon_agrandi(m):
    """Le même pignon, l'hexagone de l'alésage agrandi de AGRANDI sur plats, dans la même orientation."""
    v = m.vertices
    pres = np.hypot(v[:, 0], v[:, 1])
    coins = v[np.abs(pres - ALESAGE / np.sqrt(3)) < 0.02]          # les sommets de l'hexagone (r 4,04)
    phase = np.angle(np.mean(np.exp(6j * np.arctan2(coins[:, 1], coins[:, 0])))) / 6
    prisme = trimesh.creation.cylinder(radius=(ALESAGE + AGRANDI) / np.sqrt(3), height=m.extents[2] + 2, sections=6)
    # le cylindre à 6 pans de trimesh a un sommet sur +x : on le tourne sur ceux de l'alésage
    prisme.apply_transform(trimesh.transformations.rotation_matrix(phase, [0, 0, 1]))
    prisme.apply_translation([0, 0, m.extents[2] / 2])
    a = trimesh.boolean.difference([m, prisme], engine='manifold')
    a.export(os.path.join(DOSSIER, 'essai_pignon_2322_alesage_%.1f_IMPRESSION.stl' % (ALESAGE + AGRANDI)))
    return a


def main():
    os.makedirs(DOSSIER, exist_ok=True)
    p = pignon()
    for nom, m in (('essai_embout_IMPRESSION.stl', embout()), ('essai_pignon_2322_IMPRESSION.stl', p),
                   ('essai_pignon_2322_alesage_%.1f_IMPRESSION.stl' % (ALESAGE + AGRANDI), pignon_agrandi(p))):
        print('%-34s %5.1f × %5.1f × %5.1f mm  %5.2f cm³  étanche %s' % (nom, *m.extents, m.volume / 1000, m.is_watertight))


if __name__ == '__main__':
    main()
