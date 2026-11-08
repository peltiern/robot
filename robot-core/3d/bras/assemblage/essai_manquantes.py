#!/usr/bin/env python3
"""
Copies imprimées des pièces goBILDA qui manquent encore, pour essayer le bras avant de les commander (demande de
Nicolas, 2026-10-11) :

  - l'engrenage laiton 2305 du D85MG, couché sur une face, en deux versions : cannelure de 25 dents (celle du
    modèle) et de 24 dents. On ne sait pas encore laquelle porte le D85MG ; c'est celle qui s'enfile sans jeu ni
    forcer qu'il faudra commander ;
  - les deux entretoises 1501-0006-0430 entre les plaques de 96 : Ø 6 × 43, debout pour que le trou sorte rond.
    Le taraudage M4 ne s'imprime pas : un trou de 3,5 mm de bout en bout, où la vis M4 se taille son filet.

Le pignon Slip-Fit 2322 est déjà écrit par essai_embout.py (alésage exact et agrandi de 0,2 mm).

Écrit impression/essai_engrenage_2305-0025-0020_IMPRESSION.stl, essai_engrenage_2305-0024-0020_IMPRESSION.stl et
essai_entretoises_1501-0006-0430_IMPRESSION.stl sur le disque externe.
"""
import os, sys
import numpy as np, trimesh, cascadio
import cadquery as cq

ICI = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, ICI)
from recaler import SORTIE, GLB, modele

DOSSIER = os.path.join(SORTIE, 'impression')
CATALOGUE = os.path.join(os.path.dirname(SORTIE), 'catalogue_gobilda', 'modeles_3d')
ENTRETOISE = dict(d=6.0, longueur=43.0, trou=3.5, nombre=2, ecart=12.0)


def couche(m):
    """Posée sur sa face la plus large, centrée sur le plateau."""
    m.merge_vertices()                             # le GLB sépare les faces : sans fusion, le maillage n'est pas fermé
    m.apply_transform(trimesh.geometry.align_vectors(np.eye(3)[int(np.argmin(m.extents))], [0, 0, 1]))
    lo, hi = m.bounds
    m.apply_translation([-(lo[0] + hi[0]) / 2, -(lo[1] + hi[1]) / 2, -lo[2]])
    return m


def engrenage(sku):
    # le GLB du catalogue n'existe que pour les références déjà employées : on convertit le STEP la première fois
    glb = os.path.join(GLB, sku + '.glb')
    if not os.path.exists(glb):
        cascadio.step_to_glb(os.path.join(CATALOGUE, sku, sku + '.STEP'), glb, tol_linear=0.01, tol_angular=0.1)
    m = couche(modele(sku, None))
    m.export(os.path.join(DOSSIER, 'essai_engrenage_%s_IMPRESSION.stl' % sku))
    return m


def entretoises():
    e = ENTRETOISE
    une = (cq.Workplane("XY").circle(e['d'] / 2).circle(e['trou'] / 2).extrude(e['longueur']))
    piece = une
    for i in range(1, e['nombre']):
        piece = piece.union(une.translate((i * e['ecart'], 0, 0)))
    chemin = os.path.join(DOSSIER, 'essai_entretoises_1501-0006-0430_IMPRESSION.stl')
    cq.exporters.export(piece, chemin, tolerance=0.01, angularTolerance=0.05)
    return trimesh.load(chemin)


def main():
    os.makedirs(DOSSIER, exist_ok=True)
    for nom, m in (('engrenage 2305, cannelure 25 dents', engrenage('2305-0025-0020')),
                   ('engrenage 2305, cannelure 24 dents', engrenage('2305-0024-0020')),
                   ('2 entretoises 1501 de 43 mm', entretoises())):
        print('%-36s %5.1f × %5.1f × %5.1f mm  %5.2f cm³  étanche %s' % (nom, *m.extents, m.volume / 1000, m.is_watertight))


if __name__ == '__main__':
    main()
