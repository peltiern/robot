#!/usr/bin/env python3
"""
Liaison du tube et moyeu creux en une seule pièce imprimée : le moyeu creux de la v5 aurait été une
troisième pièce imprimée, et aucune combinaison goBILDA ne le remplace sans réduire la course du tube
(engrenage à alésage de 14 mm : 45 dents au minimum, rapport 2,25 à 3,75).

La liaison de la v5 (v5/liaison_29mm.py) est reprise telle quelle, sans ses 4 trous M3 vers le moyeu ; on
lui ajoute vers l'arrière, à travers le roulement du palier avancé (collerette à l'avant, dans le
lamage du bloc 1604) :
  - une collerette contre la face avant de la bague intérieure (elle arrête la pièce vers l'arrière) ;
  - une portée Ø31,8 dans le roulement ;
  - un épaulement et un embout au profil REX pour le pignon Slip-Fit goBILDA de 20 dents (2322-4008-0020) ;
  - un alésage Ø4,5 de bout en bout pour le fil du micro-servo.

Repère du bras (recaler.py). La liaison v5 est écrite dans un repère local d'origine x = −298,85.
"""
import importlib.util, json, math, os, sys
import cadquery as cq

ICI = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, ICI)
from recaler import SORTIE

DOSSIER = os.path.join(SORTIE, 'liaison')
DECALAGE = 298.85                       # repère local de la v5 -> repère du bras

ROULEMENT = (134.5, 139.55)             # bague intérieure du roulement du palier avancé (collerette à l'avant)
R_PORTEE, R_COLLERETTE = 15.9, 17.3
# Pignon Slip-Fit 2322-4008-0020 (7 mm, sans moyeu) : la v5 dessinait un disque de 5 mm à x 126–131 sous la
# référence 2303-4008-0020, un pignon de 14 mm à moyeu qui n'a pas la place (2026-10-08). Sa denture est en face de
# celle de l'engrenage du D85MG (v5.ENGRENAGE_D85) ; la lèvre d'arrêt reste au-dessus du boîtier du D85MG, qui
# monte jusqu'à x 124,75 autour de sa sortie.
PIGNON = (125.3, 132.3)
# Profil REX : hexagone ∩ cercle. L'alésage du vrai pignon 2322 mesure 7,00 mm sur plats et 8,08 sur les arrondis
# (STEP du catalogue) ; la v5 donnait 7,15 sur plats, plus gros que l'alésage (2026-10-08). 0,1 mm de jeu sur les
# plats, à essayer à l'impression.
REX_PLATS, REX_D = 6.8, 7.9
# Lèvre d'arrêt du pignon sur des doigts à ressort : 0,3 mm de prise sur les plats de l'alésage (rayon 3,5) ; en
# montant, chaque doigt (6,5 mm de long, 1,2 à 1,7 d'épaisseur) plie de 0,35 mm, ~1,5 % d'allongement (PLA ~2,5 %).
LEVRE = dict(r=3.8, rampe=0.4, epaisseur=0.5, fente=0.6, fin_fentes=1.0)
# 6,8 et non 7,4 : la tête des dents de l'engrenage du D85MG passe à 7,36 mm de l'axe du tube, et
# l'épaulement frottait à plat contre sa face en tournant en sens inverse (balayage du 2026-10-03).
R_EPAULEMENT = 6.8
# Les écrous du palier 1604 avancé finissent à x = 144,38 et descendent au rayon 18,75 ; les parois de la
# liaison (rayon 19,5) commençaient à 144,40 et les frôlaient à 0,02 mm en tournant. On les recule de 1 mm
# au-delà du rayon 18,5.
DEGAGEMENT_ECROUS = dict(r=18.39, x0=144.3, x1=145.4)   # r ≈ celui de la bride arrière complétée (18,4) : pas de rebord ; 0,01 de moins, sinon deux cylindres confondus font échouer la CAO
R_FIL = 2.25


def xcyl(r, x0, x1, y=0.0, z=0.0):
    return cq.Workplane("YZ").workplane(offset=x0).center(y, z).circle(r).extrude(x1 - x0)


def rex(x0, x1):
    hexa = cq.Workplane("YZ").workplane(offset=x0).polygon(6, REX_PLATS / math.cos(math.pi / 6)).extrude(x1 - x0)
    return hexa.intersect(xcyl(REX_D / 2, x0, x1))


def liaison_v5():
    spec = importlib.util.spec_from_file_location('liaison_v5', os.path.join(ICI, '..', 'v5', 'liaison_29mm.py'))
    m = importlib.util.module_from_spec(spec); spec.loader.exec_module(m)
    corps = m.body
    # les 4 trous M3 vers l'ancien moyeu (bride arrière) sont rebouchés : la pièce est maintenant d'un bloc
    for (y, z) in m.PCD_HUB:
        corps = corps.union(xcyl(3.2, m.X0, m.X0 + m.T_R, y, z))
    return corps.translate((DECALAGE, 0, 0)), m.X0 + DECALAGE, m


def construire():
    corps, x_bride, m = liaison_v5()
    x0_rex = PIGNON[0] - 0.5                       # l'embout dépasse un peu derrière le pignon
    moyeu = (rex(x0_rex, PIGNON[1] + 0.01)
             .union(xcyl(R_EPAULEMENT, PIGNON[1], ROULEMENT[0]))
             .union(xcyl(R_PORTEE, ROULEMENT[0], ROULEMENT[1] + 0.05))
             .union(xcyl(R_COLLERETTE, ROULEMENT[1] + 0.05, x_bride + 0.5)))
    # La collerette appuie sur la face avant de la bague intérieure : elle arrête la pièce vers l'arrière.
    # Arrêt du pignon vers l'arrière, sans frotter sur le D85MG (0,55 mm derrière lui) : une lèvre au bout de l'embout,
    # rampe à 45° côté arrière, bord franc côté pignon. Elle accroche les plats de l'alésage (rayon 3,5) sans atteindre
    # ses arrondis (4,04) ni le boîtier du servo (4,26). L'ancienne lèvre, un anneau sur un embout plein, n'avait aucun
    # ressort : l'embout est maintenant fendu en trois doigts qui plient vers le trou du fil (Nicolas, 2026-10-08).
    levre = xcyl(LEVRE['r'], x0_rex + LEVRE['rampe'], x0_rex + LEVRE['epaisseur']).union(
        cq.Workplane("YZ").workplane(offset=x0_rex).circle(REX_PLATS / 2)
        .workplane(offset=LEVRE['rampe']).circle(LEVRE['r']).loft())
    piece = corps.union(moyeu).union(levre)
    piece = piece.cut(xcyl(R_FIL, x0_rex - 1, x_bride + 4.5))
    # trois fentes aux sommets de l'hexagone (chaque doigt garde deux plats pour entraîner le pignon), du bout de
    # l'embout jusqu'à 1 mm de l'épaulement
    long_fente = PIGNON[1] - LEVRE['fin_fentes'] - (x0_rex - 0.1)
    for a in (0, 120, 240):
        fente = (cq.Workplane("XY").box(long_fente, 6.0, LEVRE['fente'], centered=(False, False, True))
                 .translate((x0_rex - 0.1, 0, 0))
                 .rotate((0, 0, 0), (1, 0, 0), a))
        piece = piece.cut(fente)
    d = DEGAGEMENT_ECROUS
    piece = piece.cut(xcyl(25.0, d['x0'], d['x1']).cut(xcyl(d['r'], d['x0'], d['x1'])))
    # Les coins bas de la poche du servo (y ±12,85, z −13,9) dépassent le disque des brides (r 17) : deux
    # fenêtres de 2 × 2 mm traversaient la pièce de bout en bout (Nicolas, 2026-10-04). On complète chaque
    # bride jusqu'au profil des parois et du fond : en entier à l'avant ; à l'arrière jusqu'à r 18,4
    # seulement, pour rester hors des écrous du palier (r ≥ 18,75, voir DEGAGEMENT_ECROUS).
    profil = lambda x0, x1: (cq.Workplane("XY").box(x1 - x0, 2 * m.R_ENV, m.EAR_Z - 0.3 - m.Z_FLOOR, centered=False)
                             .translate((x0, -m.R_ENV, m.Z_FLOOR)))
    for (x0, x1), r_max in (((m.X1 - m.T_FL + DECALAGE, m.X1 + DECALAGE), m.R_ENV),
                            ((x_bride, x_bride + m.T_R), 18.4)):
        anneau = xcyl(r_max, x0, x1).cut(xcyl(m.R_FL - 0.05, x0, x1))
        piece = piece.union(anneau.intersect(profil(x0, x1)))
    return piece


def main():
    os.makedirs(DOSSIER, exist_ok=True)
    piece = construire()
    cq.exporters.export(piece, os.path.join(DOSSIER, 'liaison_moyeu_position.stl'), tolerance=0.02, angularTolerance=0.1)
    cq.exporters.export(piece, os.path.join(DOSSIER, 'liaison_moyeu_position.step'))
    bb = piece.val().BoundingBox()
    print('liaison + moyeu : x %.1f → %.1f, Ø max %.1f, volume %.1f cm³' % (
        bb.xmin, bb.xmax, 2 * max(abs(bb.ymin), bb.ymax, abs(bb.zmin), bb.zmax), piece.val().Volume() / 1000))


if __name__ == '__main__':
    main()
