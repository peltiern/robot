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

Elle s'imprime en deux pièces, coupées devant la bride arrière (voir COUPE) : la partie arrière (embout, portée,
collerette, bride) et le berceau du HS-65MG, vissé au goTUBE du bras.

Repère du bras (recaler.py). La liaison v5 est écrite dans un repère local d'origine x = −298,85.
"""
import importlib.util, json, math, os, sys
import cadquery as cq

ICI = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, ICI)
from recaler import SORTIE
from v5 import POCHE_HS65

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

# Le vrai HS-65MG n'entrait pas dans la liaison imprimée (Nicolas, 2026-10-17). Le modèle de la v5 n'avait pas de
# passe-fil : celui du servo (6 × 4 mm, 1,4 de saillie, centré, au bas du bout côté cannelure) butait contre la paroi
# y−, à 0,4 mm du servo et juste sous la vis de l'oreille y−. Et les oreilles sont à 17 mm du fond, pas 14 : le servo
# descend de 3 mm, oreilles toujours sur le haut des parois, relevé de 0,5 mm pour garder 0,8 mm de matière sous ses
# coins. Le câble est plat (3 fils côte à côte, 4 mm) et dépasse de 3 à 4 mm : il ne plie bien que vers le haut ou le
# bas. Il remonte au bout du servo, un pli à 45° le renvoie vers l'arrière, debout dans un couloir de la paroi ; contre
# la bride arrière il tourne vers le centre, à plat, et un second pli à 45° le mène à l'alésage (idée de Nicolas).
# Première version imprimée le 2026-10-17 : servo posé par le dessus, paroi y− fendue de part en part, bouchon glissé
# sous l'oreille. Le câble ne se posait pas : le couloir et la gorge de la bride ne s'ouvraient que vers la poche, et le
# servo les couvrait une fois en place (Nicolas, 2026-10-18). La liaison est maintenant coupée devant la bride arrière
# (idée de Nicolas) : le servo entre par l'arrière comme un tiroir, passe-fil et bout du câble dans une fente basse ; le
# câble se pose à découvert, puis la partie arrière, enfilée sur le fil, se visse sur le berceau. Plus de fente jusqu'en
# haut ni de bouchon : la vis de l'oreille y− mord dans le haut de la paroi, redevenu continu.
HS65 = dict(x=POCHE_HS65['x'], fond=POCHE_HS65['fond'], oreilles=POCHE_HS65['fond'] + 17.0, empreinte=(148.3, 160.7))
HAUT_PAROIS = HS65['oreilles'] - 0.3
Z_PLI = -4.5                                     # haut du câble au premier pli, juste sous le haut de la paroi
COULOIR = dict(y0=-12.0, y1=-14.5, z0=Z_PLI - 4.3, z1=Z_PLI + 0.3)
FENETRE = dict(x0=HS65['x'] - 3.2, x1=HS65['x'] + 3.2)          # le câble y remonte et s'y plie, de l'extérieur
FENTE_BASSE = dict(z0=HS65['fond'] + 0.7, z1=COULOIR['z0'])     # passe-fil (1 à 5 mm du fond) et bout du câble
GORGE_BRIDE = 2.9                                # deux épaisseurs de câble au second pli
# Coupe au bout du dégagement des écrous (x 145,4) et non sur la face de la bride (144,5) : là, le dégagement ramène
# la pièce au rayon 18,39, et les coins du servo (18,33) n'auraient pas laissé de paroi. La partie arrière se termine
# par un disque plein, qui ferme aussi les deux petits trous du pied de la bride (vus par Nicolas, 2026-10-17).
COUPE = DEGAGEMENT_ECROUS['x1']
JEU = 0.2                                        # par face, demandé par Nicolas pour l'impression (2026-10-18)
# Jonction : deux pattes de la partie arrière dans des encoches du haut des parois, juste derrière les oreilles, une vis
# M2 × 6 de servo dans chacune, tête noyée ; les pattes s'arrêtent devant les oreilles (x 148,83, 0,3 au-dessus) : la
# partie arrière arrive levée et les aurait heurtées. En bas, un tenon sur le fond et sa dent dans une rainure. Une vis
# par-dessous dépassait du méplat, et Nicolas n'a pas de M2 à tête fraisée.
# La dent tombait d'abord dans une poche fermée côté coupe, ce qui imposait d'arriver levé de 1,5 mm : imprimée, elle ne
# s'emboîtait pas (Nicolas). Le rebord de 1,2 mm devant la poche était un pont dans le sens d'impression, la dent un
# surplomb, et le tenon n'avait aucun jeu sur le fond. La rainure est ouverte jusqu'à la coupe (idée de Nicolas) : la partie
# arrière arrive à plat, et la dent, prolongée jusqu'au disque, s'imprime sans surplomb. La dent ne retient donc plus le
# bas vers l'arrière, elle ne fait plus que le caler en y : ce sont les deux vis qui tiennent la jonction.
PATTE = dict(x1=148.8, z0=-1.5)
VIS_JONCTION = [(146.5, 16.3), (146.5, -16.3)]
TETE_VIS = dict(r=2.2, z0=0.6)                   # tête bombée Ø3,8 × 1,5 sous le haut de la paroi
TENON = dict(x1=148.55, y=5.0, z1=-11.0)
DENT = dict(x1=148.4, y=4.0, h=1.2)
JEU_DENT = 0.3                                   # par face, tenon compris sur le fond


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


def levre_arret(x0_rex):
    """La lèvre au bout de l'embout : rampe à 45° côté arrière, bord franc côté pignon."""
    return xcyl(LEVRE['r'], x0_rex + LEVRE['rampe'], x0_rex + LEVRE['epaisseur']).union(
        cq.Workplane("YZ").workplane(offset=x0_rex).circle(REX_PLATS / 2)
        .workplane(offset=LEVRE['rampe']).circle(LEVRE['r']).loft())


def fendre(piece, x0_rex):
    """Trois fentes aux sommets de l'hexagone (chaque doigt garde deux plats pour entraîner le pignon), du bout de
    l'embout jusqu'à 1 mm de l'épaulement."""
    long_fente = PIGNON[1] - LEVRE['fin_fentes'] - (x0_rex - 0.1)
    for a in (0, 120, 240):
        fente = (cq.Workplane("XY").box(long_fente, 6.0, LEVRE['fente'], centered=(False, False, True))
                 .translate((x0_rex - 0.1, 0, 0))
                 .rotate((0, 0, 0), (1, 0, 0), a))
        piece = piece.cut(fente)
    return piece


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
    piece = corps.union(moyeu).union(levre_arret(x0_rex))
    piece = fendre(piece.cut(xcyl(R_FIL, x0_rex - 1, x_bride + 4.5)), x0_rex)
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
    piece = reprendre_poche(piece, (m.X0 + m.T_R + DECALAGE, m.X1 - m.T_FL + DECALAGE), m.Z_FLOOR)
    return couper(piece)


def box(x0, x1, y0, y1, z0, z1):
    return cq.Workplane("XY").box(x1 - x0, y1 - y0, z1 - z0, centered=False).translate((x0, y0, z0))


def zcyl(r, x, y, z0, z1):
    return cq.Workplane("XY").workplane(offset=z0).center(x, y).circle(r).extrude(z1 - z0)


def reprendre_poche(piece, poche, z_meplat):
    """La poche du HS-65MG reprise pour le vrai servo, d'un seul bloc (voir HS65) ; poche : x des faces intérieures des
    brides."""
    x0, x1 = poche
    enveloppe = xcyl(19.5, x0, x1)
    d = DEGAGEMENT_ECROUS
    piece = piece.union(box(x0, 157.1, -14.7, -12.85, -11.3, -8.4))       # l'ancienne encoche du fil, au bas de la paroi y−
    for s in (1, -1):
        piece = piece.union(box(x0, x1, *sorted((s * 12.85, s * 19.6)), HAUT_PAROIS - 0.6, HAUT_PAROIS).intersect(enveloppe))
    piece = piece.cut(xcyl(25.0, d['x0'], d['x1']).cut(xcyl(d['r'], d['x0'], d['x1'])))
    piece = piece.cut(zcyl(0.9, HS65['x'], 14.0, HAUT_PAROIS - 9, HAUT_PAROIS + 0.1))         # avant-trous des oreilles
    piece = piece.cut(zcyl(0.9, HS65['x'], -14.0, COULOIR['z1'], HAUT_PAROIS + 0.1))
    # le servo glisse de la coupe jusqu'à sa place, 3 mm plus bas que dans la v5
    piece = piece.cut(box(COUPE - 0.1, HS65['empreinte'][1], -12.2, 12.2, HS65['fond'] - 0.2, HAUT_PAROIS + 1))
    piece = piece.cut(box(COUPE - 0.1, FENETRE['x1'], -25, -12.0, FENTE_BASSE['z0'], FENTE_BASSE['z1']))
    piece = piece.cut(box(COUPE - 0.1, FENETRE['x1'], COULOIR['y1'], COULOIR['y0'], FENTE_BASSE['z0'], COULOIR['z1']))
    piece = piece.cut(box(FENETRE['x0'], FENETRE['x1'], -25, -12.0, FENTE_BASSE['z0'], COULOIR['z1']))
    # disque plein jusqu'à la coupe, alésage du fil prolongé, gorge sur sa face jusqu'à l'alésage
    piece = piece.union(xcyl(d['r'], x0 - 0.01, COUPE).cut(box(x0 - 1, COUPE + 1, -30, 30, -30, z_meplat)))
    piece = piece.cut(xcyl(R_FIL, x0 - 1, COUPE + 1))
    piece = piece.cut(box(COUPE - GORGE_BRIDE, COUPE + 0.01, COULOIR['y1'], 2.3, COULOIR['z0'], COULOIR['z1']))
    return piece.cut(box(COUPE - GORGE_BRIDE, COUPE + 0.01, -2.3, 2.3, COULOIR['z0'], 0.5))


def couper(piece):
    """(partie arrière, berceau) : la liaison coupée en COUPE, avec la jonction (voir PATTE)."""
    arriere = piece.intersect(box(100, COUPE, -30, 30, -30, 30))
    berceau = piece.intersect(box(COUPE, 200, -30, 30, -30, 30))
    j, fond = JEU, HS65['fond'] - 0.2
    for s in (1, -1):
        arriere = arriere.union(berceau.intersect(box(COUPE - 0.01, PATTE['x1'] - j, *sorted((s * 12.0, s * 20)), PATTE['z0'] + j, HAUT_PAROIS + 1)))
        berceau = berceau.cut(box(COUPE - 0.1, PATTE['x1'], *sorted((s * 12.0, s * 20.5)), PATTE['z0'], HAUT_PAROIS + 1))
    jd = JEU_DENT
    arriere = arriere.union(box(COUPE - 0.1, TENON['x1'] - j, -TENON['y'] + j, TENON['y'] - j, fond + jd, TENON['z1']))
    arriere = arriere.union(box(COUPE - 0.1, DENT['x1'] - jd, -DENT['y'] + jd, DENT['y'] - jd, fond - DENT['h'] + jd, fond + jd + 0.01))
    berceau = berceau.cut(box(COUPE - 0.1, DENT['x1'], -DENT['y'], DENT['y'], fond - DENT['h'], fond + 0.1))
    for x, y in VIS_JONCTION:
        # le lamage traverse le disque de la partie arrière : il débordait de 0,8 mm au-dessus de la tête, qui ne pouvait
        # pas descendre dans son trou (vu le 2026-10-18 ; la notice tolère 5 mm³ pour une vis, le heurt en faisait 2,5)
        arriere = arriere.cut(zcyl(TETE_VIS['r'], x, y, TETE_VIS['z0'], 25)).cut(zcyl(1.1, x, y, PATTE['z0'] - 1, TETE_VIS['z0'] + 0.1))
        berceau = berceau.cut(zcyl(0.8, x, y, -6.0, PATTE['z0'] + 0.1))
    return arriere, berceau


def main():
    os.makedirs(DOSSIER, exist_ok=True)
    for ancien in ('liaison_moyeu_position.stl', 'liaison_moyeu_position.step', 'bouchon_position.stl'):
        if os.path.exists(os.path.join(DOSSIER, ancien)):
            os.remove(os.path.join(DOSSIER, ancien))       # liaison d'une pièce et bouchon du 2026-10-17
    for nom, piece in zip(('arriere', 'berceau'), construire()):
        cq.exporters.export(piece, os.path.join(DOSSIER, 'liaison_%s_position.stl' % nom), tolerance=0.02, angularTolerance=0.1)
        cq.exporters.export(piece, os.path.join(DOSSIER, 'liaison_%s_position.step' % nom))
        bb = piece.val().BoundingBox()
        print('liaison, %s : x %.1f → %.1f, Ø max %.1f, volume %.1f cm³' % (
            nom, bb.xmin, bb.xmax, 2 * max(abs(bb.ymin), bb.ymax, abs(bb.zmin), bb.zmax), piece.val().Volume() / 1000))


if __name__ == '__main__':
    main()
