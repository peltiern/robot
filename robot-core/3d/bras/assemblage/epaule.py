#!/usr/bin/env python3
"""
L'épaule reculée de 8 mm et l'abduction par engrenages métal (2026-11-24), comme sur le WALL•E Motion Arm.

Pourquoi : la couronne de 72 dents est en acétal, module 0,8, en prise sur 4 mm ; servo bloqué à fond (25,2 kg·cm),
une dent prenait ~93 MPa, l'acétal plie vers 65. Le Motion Arm (manuel v2.4, étapes 110 et 131) entraîne son épaule
par une roue alu goBILDA 2302 de 80 dents et un pignon laiton 2305 : tout métal. Chez nous, une roue alu ne tenait
pas à 24 mm des flancs du caisson (la 80 dents mordait 0,6 mm le bout des plaques latérales) ; les bielles goBILDA
non plus (étude du 2026-11-22, abandonnée : rien de goBILDA ne s'y loge sans percer).

Idée de Nicolas : reculer les deux plaques 1108 d'un pas de trous (8 mm) vers l'épaule. Le pivot est dans ces
plaques : l'axe d'abduction recule de 8 mm par rapport au caisson, l'épaule le suit. Les flancs commencent à 32 mm de
l'axe, le servo est à 55,85 mm. À cet entraxe, roue alu 2302-0014-0090 (90 dents, Ø73,6) sur l'épaule et pignon
laiton 2305-0025-0048 sur le servo : 55,2 mm, atteints en poussant le cadre du servo de 0,65 mm vers l'épaule dans
ses trous oblongs (0,7 de course). Rapport 90/48 = 1,875 ; servo bloqué à fond, ~60 MPa dans une dent alu.
Le bras s'allonge de 8 mm vers l'extérieur ; derrière l'épaule, rien ne change.

Plaques 1108 reculées : les vis M4 × 10 qui les tenaient aux profilés en x = 64 passent en x = 56 (trous en face,
vérifié) ; celles de x = 32 restent dans le même trou du profilé, un trou plus loin dans la plaque.

La roue alu n'a qu'un moyeu de 3 mm (la couronne en faisait 4), sa denture fait 6 mm et dépasse d'un côté : moyeu en
bas sur l'entretoise 1526, à la place de la couronne, denture de −22,1 à −16,1 en face de celle du pignon (−22 à −16).
Même motif de trous que la couronne. Aux quatre coins, entretoises de 5 mm (au lieu de 4) jusqu'aux cales sous les
blocs : mêmes vis M4 × 16. Les quatre vis M4 × 14 de l'entretoise 1526, et leurs rondelles, descendent de 1 mm.

Repère du bras inchangé (origine à l'ancien axe) : l'axe d'épaule, de flexion comme d'abduction, est en x = −8.
"""
import json, os, sys
import numpy as np, trimesh

ICI = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, ICI)
from recaler import SORTIE, modele

RECUL = 8.0                                    # l'épaule et le pivot, vers −x
POUSSE_SERVO = 0.65                            # le servo d'abduction et son cadre, vers −x, dans les oblongs
AXE = (-RECUL, 0.0, 0.0)                       # axe de l'épaule dans le repère du bras
AXE_SERVO = (47.85 - POUSSE_SERVO, 0.0)        # cannelure du servo d'abduction (axe Z)
ROUE = dict(sku='2302-0014-0090', dents=90, dessous=-22.1, moyeu=3.0, denture=6.0)
PIGNON = dict(sku='2305-0025-0048', dents=48)
RAPPORT = ROUE['dents'] / PIGNON['dents']

# Unités de assemblage.json qui reculent avec l'épaule (règles de animation.groupe, au 2026-11-24) et avec le pivot
EPAULE = ['1116-0024-0040#1', '1201-0027-0001#1', '1201-0027-0001#2', '1311-0016-4008#1', '1311-0016-4008#2',
          '1526-0032-0100#1', '1526-0032-0100#2', '2312-0414-0072#1', '2312-0414-0072#2']
EPAULE += ['1502-0006-0040#%d' % k for k in range(1, 9)] + ['1601-0014-0004#%d' % k for k in range(1, 5)]
EPAULE += ['2800-0004-0014#%d' % k for k in range(1, 5)] + ['2801-0004-0008#%d' % k for k in (1, 2, 3, 5, 8, 9, 10, 11)]
EPAULE += ['2802-0004-00%s#%d' % (l, k) for l in ('14', '16', '18') for k in range(1, 9)]
EPAULE += ['2807-0407-0500#%d' % k for k in range(1, 11)]
EPAULE += ['2812-0004-0007#%d' % k for k in (1, 3, 9, 10, 12, 14, 17, 18)]
PIVOT = ['1108-0001-0002#1', '1108-0001-0002#2', '2904-0004-0014#1', '2904-0004-0014#2', '2904-0004-0014#3',
         '2904-0004-0014#4', '2800-0004-0020#1', '2800-0004-0020#2', '2812-0004-0007#13', '2812-0004-0007#15',
         # vis 1108 ↔ profilés de x = 64, et leurs écrous : x = 56
         '2802-0004-0010#1', '2802-0004-0010#2', '2802-0004-0010#4', '2802-0004-0010#5',
         '2812-0004-0007#4', '2812-0004-0007#5', '2812-0004-0007#6', '2812-0004-0007#7']
SERVO = ['1802-0043-0001#1', '2000-0025-0002#1', '2800-0004-0010#1', '2800-0004-0010#2', '2800-0004-0010#3',
         '2800-0004-0010#4', '2801-0004-0008#4', '2801-0004-0008#6', '2801-0004-0008#7', '2801-0004-0008#12']
# sous la roue (moyeu de 3 mm au lieu de 4) : vis de l'entretoise 1526 du bas et leurs rondelles
DESCENTE_1526 = ['2802-0004-0014#3', '2802-0004-0014#4', '2802-0004-0014#6', '2802-0004-0014#8',
                 '2801-0004-0008#2', '2801-0004-0008#8', '2801-0004-0008#10', '2801-0004-0008#11']
# remplacées : la couronne du bas par la roue alu, ses quatre entretoises de 4 mm par des 5 mm
COURONNE_BAS = '2312-0414-0072#2'
ENTRETOISES_BAS = ['1502-0006-0040#3', '1502-0006-0040#6', '1502-0006-0040#7', '1502-0006-0040#8']


def decalage(nom):
    """(dx, dy, dz) d'une unité de assemblage.json dans le bras à épaule reculée."""
    d = np.zeros(3)
    if nom in EPAULE or nom in PIVOT:
        d[0] -= RECUL
    if nom in SERVO:
        d[0] -= POUSSE_SERVO
    if nom in DESCENTE_1526:
        d[2] -= ROUE['dessous'] + 4.0 - (ROUE['dessous'] + ROUE['moyeu'])    # 1 mm : 4 de couronne → 3 de moyeu
    return d


def unites():
    """assemblage.json (le bras de Nicolas recalé), pièces de l'épaule, du pivot et du servo déplacées."""
    a = json.load(open(os.path.join(SORTIE, 'assemblage.json')))
    for u in a['unites']:
        d = decalage(u['nom'])
        if d.any():
            M = np.array(u['matrice'], float); M[:3, 3] += d; u['matrice'] = M.round(6).tolist()
    return a


def reculer(m):
    """Une pièce de l'épaule posée en coordonnées fixes (goTUBE, arbre, vis de la plaque 1116…)."""
    m.apply_translation([-RECUL, 0, 0])
    return m


def _tube(d_int, d_ext, z0, z1, x, y):
    m = trimesh.creation.annulus(r_min=d_int / 2, r_max=d_ext / 2, height=z1 - z0, sections=48)
    m.apply_translation([x, y, (z0 + z1) / 2])
    return m


def pieces():
    """(nom, nature, maillage, référence) : la roue alu et les entretoises de 5 mm des coins."""
    out = []
    r = modele(ROUE['sku'], None)
    ax = int(np.argmin(r.extents))
    r.apply_transform(trimesh.geometry.align_vectors(np.eye(3)[ax], [0, 0, 1]))
    r.apply_translation(-r.bounds.mean(0))
    # le moyeu est du côté +z de la denture dans le modèle : on retourne la roue, moyeu en bas
    r.apply_transform(trimesh.transformations.rotation_matrix(np.pi, [1, 0, 0]))
    r.apply_translation([AXE[0], AXE[1], ROUE['dessous'] + ROUE['denture'] / 2 - r.bounds.mean(0)[2]])
    out.append(('roue alu 90 dents de l\'abduction, à la place de la couronne du bas', 'achetée', r, ROUE['sku']))
    haut = ROUE['dessous'] + ROUE['moyeu']
    for x, y in ((16, 16), (16, -16), (-16, 16), (-16, -16)):
        out.append(('entretoise 1502-0006-0050 — roue alu ↔ bloc d\'épaule', 'visserie ajoutée',
                    _tube(4.0, 6.0, haut, haut + 5.0, x + AXE[0], y), '1502-0006-0050'))
    return out
