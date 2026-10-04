#!/usr/bin/env python3
"""
Plaque imprimée qui ferme le bout du caisson, autour du goTUBE du bras (demandée par Nicolas le 2026-10-04).

Le caisson finit à x = 192 : profilés 1121 en haut et en bas (fond à |z| 31,0 → 33,6, ailes à |y| 21,5 → 24),
plaques 1123 sur les côtés (|y| 21,5 → 24). Derrière, le palier 1604 avant s'arrête à x = 187,5.

  - un panneau de 4,2 mm remplit l'ouverture, au ras des bouts (x 187,8 → 192) : la taille extérieure du
    bras ne change pas ; trou de Ø33 pour le tube, qui tourne (0,5 mm de jeu au rayon) ;
  - deux pattes entrent dans les profilés, contre leur fond, au-dessus du palier (|z| 22,4 → 30,8) ; chacune
    se visse par deux trous existants du fond du profilé (x = 184, y = ±16) : vis M4 × 10 de l'extérieur,
    écrou Nylstop enfilé par le dessous de la patte, dans une poche hexagonale ouverte de ce côté (elle
    l'empêche de tourner ; seuls les 2,5 mm côté profilé sont serrés). Aucun perçage.

Écrit plaque_bout/plaque_bout_position.stl et plaque_bout/visserie.json sur le disque externe.
"""
import json, os, sys
import numpy as np, trimesh

ICI = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, ICI)
from recaler import SORTIE
from cloison import boite, cylindre, hexagone, moins, union, RETRAIT_ECROU, ECROU_EP, ECROU_PLATS, VIS_D

DOSSIER = os.path.join(SORTIE, 'plaque_bout')

X_BOUT = 192.0                 # bout des profilés et des plaques
X_PALIER = 187.5               # face avant du palier 1604
JEU = 0.2
R_TUBE = 16.0 + 0.5
Y_INT, Z_INT = 21.5, 31.0      # intérieur du caisson : entre les ailes / plaques, sous le fond des profilés (mesuré à 30,97–31,10)
X_PATTE = 179.0                # bout arrière des pattes
FOND_PROFILE = 31.0            # face intérieure du fond des profilés
VIS = [(184.0, 16.0), (184.0, -16.0)]           # (x, y) des trous existants du fond des profilés
EPAISSEUR_PATTE = RETRAIT_ECROU + ECROU_EP + 1.0
JEU_ECROU = 0.3


def construire():
    y, z = Y_INT - JEU, Z_INT - JEU
    piece = boite(X_PALIER + 0.3, X_BOUT, -y, y, -z, z)
    logements, visserie = [], []
    for s in (1, -1):                                       # patte du haut, patte du bas
        z_fond = s * FOND_PROFILE
        patte = boite(X_PATTE, X_PALIER + 0.5, -y, y, *sorted((z_fond - s * EPAISSEUR_PATTE, s * (Z_INT - JEU))))
        piece = union(piece, patte)
        for xv, yv in VIS:
            d = np.array([0, 0, -s], float)
            p = np.array([xv, yv, s * (Z_INT - JEU)])
            # Poche ouverte vers la face libre de la patte (1 mm plus loin) : pas besoin de la fermer ni de
            # fente latérale, la matière utile est celle entre le profilé et l'écrou (Nicolas, 2026-10-04).
            c = p + d * (RETRAIT_ECROU + ECROU_EP / 2)
            debut = p + d * RETRAIT_ECROU
            long_poche = EPAISSEUR_PATTE - RETRAIT_ECROU + 2.0          # jusqu'au-delà de la face libre
            poche = hexagone(debut + d * long_poche / 2, d, ECROU_PLATS + JEU_ECROU, long_poche)
            logements.append(union(cylindre(VIS_D / 2, p - d * 0.5, p + d * (EPAISSEUR_PATTE + 1.0)), poche))
            visserie.append(dict(sku='2802-0004-0010', paroi='plaque de bout', tete=[xv, yv, s * (33.6 + 2.2)],
                                 direction=d.tolist(), ecrou='2812-0004-0007', centre_ecrou=c.round(2).tolist()))
    piece = moins(piece, cylindre(R_TUBE, [X_PATTE - 1, 0, 0], [X_BOUT + 1, 0, 0], sections=96), *logements)
    return piece, visserie


def main():
    os.makedirs(DOSSIER, exist_ok=True)
    piece, visserie = construire()
    piece.export(os.path.join(DOSSIER, 'plaque_bout_position.stl'))
    json.dump(visserie, open(os.path.join(DOSSIER, 'visserie.json'), 'w'), indent=1)
    print('plaque de bout : étanche %s, volume %.1f cm³, bornes %s' % (
        piece.is_watertight, piece.volume / 1000, np.round(piece.bounds, 1).tolist()))


if __name__ == '__main__':
    main()
