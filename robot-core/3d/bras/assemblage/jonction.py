#!/usr/bin/env python3
"""
La jonction bras ↔ corps : ce qui porte et entraîne la flexion, dans le corps du robot.

Le côté corps du bras de Nicolas (servo dans l'axe, cadre, accouplement 4001) ne tenait à rien dans l'export Onshape, et
il barrait la sortie des câbles au bout du goTUBE de flexion. La jonction (2026-11-25, choix de Nicolas : tout dans le
corps, en goBILDA, entraînement vissé et non serré) :
  - deux plaques 1123 de 43 × 144 mm, horizontales, à 43 mm l'une de l'autre ; celle du haut pend sous le goRAIL du haut
    du flanc par trois entretoises de 16 mm, vissées dans des écrous Hurricane de la fente du rail ;
  - entre elles, deux paliers 1604 (et leurs roulements 1601) sur le goTUBE de flexion, à 32 mm l'un de l'autre, et le
    cadre 1802 du servo de flexion ;
  - le servo devant le tube : pignon laiton de 40 dents sur sa cannelure, roue alu de 90 dents (alésage 14) vissée au bout
    du goTUBE par ses quatre taraudages. Sur la grille de 8 mm des plaques, la cannelure ne peut tomber qu'à 44, 52 ou
    60 mm de l'axe : 90 + 40 dents au module 0,8 font juste 52,0 mm.
Rapport 2,25 : le servo est un 25-2 « cinq tours » (2000-0025-0502), car 180° de bras demandent 405° de servo.

La position du bras en découle : l'axe de flexion est à 40 mm sous le rail (16 d'entretoise, 2,5 de plaque, 21,5 de demi-
palier : 64 mm sous le haut du cadre, comme sur les plans), et l'épaule s'écarte du corps jusqu'à ce que le bout du goTUBE
de 72 mm vienne contre la roue.

Géométrie écrite dans le repère du robot, côté droit (export du 2026-10-05 : −y vers l'avant, z vers le haut) ; le côté
gauche en est le miroir par le plan médian des deux rails. pieces_bras() la ramène dans le repère du bras (bras droit),
pour la visionneuse et la notice.
"""
import json, os, sys
import numpy as np, trimesh

ICI = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, ICI)
from recaler import SORTIE, modele

# goRAILs du haut des flancs dans l'export (1109-0024-0288_2 à droite, _9 à gauche) : bornes relevées le 2026-11-25
RAIL_DROIT = ((-143.25708709657758, -114.5749371498839, 314.3774811178299),
              (-119.25708688795, 173.42505045235316, 338.3774813264755))
RAIL_GAUCHE_X = (168.74290443956275, 192.74290464819055)
X_FENTE = (RAIL_DROIT[0][0] + RAIL_DROIT[1][0]) / 2         # fente du dessous du rail, où glissent les écrous Hurricane
RAIL_BAS = RAIL_DROIT[0][2]
PLAN = (X_FENTE + sum(RAIL_GAUCHE_X) / 2) / 2                # plan médian des deux rails (24,743)
# Le bras droit de l'export : axes du repère du bras dans le robot (X le long du bras, Y axe de flexion, −Y vers le corps),
# relevés par robot_complet.epaule() sur les plaques 1108 et l'arbre REX de Nicolas.
X_BRAS = np.array([0.0, -0.9351088497790554, -0.3543606059720689])
Y_BRAS = np.array([-1.0, 0.0, 0.0])
R_BRAS = np.column_stack([X_BRAS, Y_BRAS, np.cross(X_BRAS, Y_BRAS)])
Y_AXE = 49.375058412552704                                  # arbre REX de Nicolas, avancé de 30 mm (proportions des plans)

ENTRETOISE, EP, DEMI_PALIER = 16.0, 2.5, 21.5
Z_AXE = RAIL_BAS - ENTRETOISE - EP - DEMI_PALIER
XC = X_FENTE + 16.0                    # milieu des plaques (43 mm selon x) : leur rangée de trous w = −16 sous la fente
UC = Y_AXE - 40.0                      # milieu des plaques (144 mm selon y)
W_CADRE = 16.0                         # le cadre du servo sur la rangée de trous ronds w = +16, comme le palier intérieur
ENTRAXE = (90 + 40) * 0.8 / 2
GOTUBE_FLEXION = dict(sku='4103-0032-0072', debut=32.1, longueur=72.0)   # depuis l'axe d'épaule, le long de −Y
VIS_RAIL_U = (8.0, -24.0, -56.0)       # vis du rail : rangée w = −16, hors du palier extérieur (u 18,5 → 61,5)
# La traverse (2026-11-25) : accrochée par ses seules entretoises du haut, alignées sous la fente, la jonction pivotait
# autour du rail sous le poids du bras, qui pend à 56 mm de cette ligne (Nicolas : « ce n'est pas stable »). Un second
# goRAIL de 288, entre les montants avant et arrière du flanc, la tient par-dessous : les deux lignes de vis, l'une
# au-dessus de l'autre, bloquent ce pivotement. Pas d'appui à plat : les têtes des vis du palier extérieur dépassent de
# 2,2 mm sous la plaque, pile sur la fente (Ø7,6 pour 7,8). Entretoises de 8 et non 3 : l'oreille du collier de butée
# balaie jusqu'à 29,8 mm de l'axe, soit z 244,5 ; il lui reste 2,2 mm.
ENTRETOISE_BAS = 8.0
TRAVERSE_HAUT = Z_AXE - DEMI_PALIER - EP - ENTRETOISE_BAS
VIS_TRAVERSE_U = (-8.0, -32.0, -64.0)  # rangée w = −16, devant le palier extérieur ; décalées des vis du rail pour la clé


def M(R, t):
    T = np.eye(4); T[:3, :3] = R; T[:3, 3] = t
    return T


def _pose(m, T):
    m = m.copy(); m.apply_transform(T); return m


def _servo():
    """Matrice du servo (modèle 2000-0025-0002/0502 : cannelure en (−9,97 ; 0) selon z), du cadre et du pignon autour
    de lui, repris du servo d'abduction du bras (même cadre 1802, pignon laiton sur la cannelure)."""
    from v5 import PIGNON_LAITON
    a = json.load(open(os.path.join(SORTIE, 'assemblage.json'))); U = {u['nom']: u for u in a['unites']}
    Ms_bras = np.array(U['2000-0025-0002#1']['matrice']); Mf_bras = np.array(U['1802-0043-0001#1']['matrice'])
    pg = modele(PIGNON_LAITON[0], None)
    Mp_bras = M(trimesh.transformations.rotation_matrix(np.pi, [1, 0, 0])[:3, :3],
                [PIGNON_LAITON[1][0], PIGNON_LAITON[1][1], PIGNON_LAITON[2] - pg.extents[2] / 2]) @ M(np.eye(3), -pg.bounds.mean(0))
    rel_cadre, rel_pignon = np.linalg.inv(Ms_bras) @ Mf_bras, np.linalg.inv(Ms_bras) @ Mp_bras
    # cannelure selon +x (vers la roue), corps du servo vers l'avant (−y)
    R = np.array([[0, 0, 1], [-1, 0, 0], [0, -1, 0]], float)
    def en(xs):
        return M(R, np.array([xs, Y_AXE - ENTRAXE, Z_AXE]) - R @ np.array([-9.97, 0.0, 0.0]))
    cadre = modele('1802-0043-0001', '^1802-0043-0001$')     # le STEP contient aussi ses vis
    xs = (XC + W_CADRE) - _pose(cadre, en(0.0) @ rel_cadre).bounds.mean(0)[0]
    Ms = en(xs)
    return Ms, Ms @ rel_cadre, Ms @ rel_pignon, cadre


_MS, _MF, _MP, _CADRE = None, None, None, None


def _servo_memo():
    global _MS, _MF, _MP, _CADRE
    if _MS is None:
        _MS, _MF, _MP, _CADRE = _servo()
    return _MS, _MF, _MP, _CADRE


def debut_roue():
    """x de la face de la roue côté épaule : celle du pignon, posé au bout de la cannelure."""
    Ms, Mf, Mp, _ = _servo_memo()
    return _pose(modele('2305-0025-0040', None), Mp).bounds[0][0]


def x_epaule():
    """x de l'axe d'épaule (bras droit) : le bout du goTUBE de flexion contre la roue."""
    return debut_roue() - GOTUBE_FLEXION['longueur'] - GOTUBE_FLEXION['debut']


def epaule(cote):
    """Origine du repère du bras (intersection des axes de flexion et d'abduction), dans le robot."""
    x = x_epaule()
    return np.array([x if cote == 'droit' else 2 * PLAN - x, Y_AXE, Z_AXE])


def pieces_robot():
    """(nom, référence, maillage) de la jonction droite, repère du robot. Les noms commencent par « jonction »."""
    from v5 import poser_vis, poser_ecrou, poser_entretoise
    out = []
    R_PL = np.array([[0, 0, -1], [0, 1, 0], [1, 0, 0]], float)          # plaque : épaisseur → z, longueur → y
    pl = modele('1123-0043-0144', None); c = pl.bounds.mean(0)
    Z_HAUT, Z_BAS = Z_AXE + DEMI_PALIER + EP, Z_AXE - DEMI_PALIER - EP   # face du dessus de la plaque du haut, dessous du bas
    for nom, zm in (('plaque du haut', Z_AXE + DEMI_PALIER + EP / 2), ('plaque du bas', Z_AXE - DEMI_PALIER - EP / 2)):
        out.append(('jonction, ' + nom, '1123-0043-0144', _pose(pl, M(R_PL, [XC, UC, zm]) @ M(np.eye(3), -c))))
    R_PA = np.array([[0, -1, 0], [1, 0, 0], [0, 0, 1]], float)          # palier : alésage → x, chants taraudés → ±z
    bloc = modele('1604-0043-0032', 'NAUO1'); roul = modele('1601-0039-0032', None)
    cb = modele('1604-0043-0032', None).bounds.mean(0)                  # centre de l'alésage (bloc et roulement)
    cr = roul.bounds.mean(0)
    for cote, w in (('extérieur', -16.0), ('intérieur', 16.0)):
        x = XC + w
        out.append(('jonction, palier 1604 %s' % cote, '1604-0043-0032', _pose(bloc, M(R_PA, [x, Y_AXE, Z_AXE]) @ M(np.eye(3), -cb))))
        R_RO = trimesh.geometry.align_vectors([0, 0, 1], [1, 0, 0])[:3, :3] if roul.extents.argmin() == 2 else R_PA
        r = roul.copy(); r.apply_translation(-cr)
        ax = np.eye(3)[int(np.argmin(r.extents))]
        r.apply_transform(trimesh.geometry.align_vectors(ax, [1, 0, 0])); r.apply_translation([x, Y_AXE, Z_AXE])
        out.append(('jonction, roulement du palier %s' % cote, '1601-0039-0032', r))
        for u in (-16.0, 16.0):                                         # deux vis dans chaque chant
            out.append(('jonction, vis 2802-0004-0008 — plaque du haut ↔ palier %s' % cote, '2802-0004-0008',
                        poser_vis('2802-0004-0008', [x, Y_AXE + u, Z_HAUT + 2.2], [0, 0, -1])))
            out.append(('jonction, vis 2802-0004-0008 — plaque du bas ↔ palier %s' % cote, '2802-0004-0008',
                        poser_vis('2802-0004-0008', [x, Y_AXE + u, Z_BAS - 2.2], [0, 0, 1])))
    Ms, Mf, Mp, cadre = _servo_memo()
    out.append(('jonction, servo de flexion 25-2 cinq tours', '2000-0025-0502', _pose(modele('2000-0025-0502', None), Ms)))
    out.append(('jonction, cadre du servo de flexion', '1802-0043-0001', _pose(cadre, Mf)))
    out.append(('jonction, pignon laiton 40 dents sur le servo de flexion', '2305-0025-0040', _pose(modele('2305-0025-0040', None), Mp)))
    for u in (0.0, -24.0, -48.0):                                       # trois taraudages dans chaque chant du cadre
        y = UC + u
        out.append(('jonction, vis 2802-0004-0008 — plaque du haut ↔ cadre du servo', '2802-0004-0008',
                    poser_vis('2802-0004-0008', [XC + W_CADRE, y, Z_HAUT + 2.2], [0, 0, -1])))
        out.append(('jonction, vis 2802-0004-0008 — plaque du bas ↔ cadre du servo', '2802-0004-0008',
                    poser_vis('2802-0004-0008', [XC + W_CADRE, y, Z_BAS - 2.2], [0, 0, 1])))
    # Sous le rail : vis M4 × 25 par-dessous la plaque du haut, entretoise de 16, écrou Hurricane dans la fente (au-dessus
    # de ses lèvres de 1,5 mm : la vis le traverse et dépasse de 2 mm dans le creux du rail).
    for u in VIS_RAIL_U:
        y = UC + u
        out.append(('jonction, vis 2802-0004-0025 — plaque du haut → écrou du rail', '2802-0004-0025',
                    poser_vis('2802-0004-0025', [X_FENTE, y, Z_HAUT - EP - 2.2], [0, 0, 1])))
        out.append(('jonction, entretoise 1502-0006-0160 — plaque du haut ↔ rail', '1502-0006-0160',
                    poser_entretoise('1502-0006-0160', [X_FENTE, y, Z_HAUT + ENTRETOISE / 2], [0, 0, 1])))
        out.append(('jonction, écrou Hurricane 2805-0004-0108 dans la fente du rail', '2805-0004-0108',
                    _ecrou_fente(y, RAIL_BAS, 1)))
    # Sur la traverse : vis M4 × 16 entre les deux plaques, tête sur la plaque du bas (glissée par le côté, clé coudée par
    # les trous libres de la plaque du haut), entretoise de 8, écrou Hurricane dans la fente du dessus de la traverse.
    for u in VIS_TRAVERSE_U:
        y = UC + u
        out.append(('jonction, vis 2802-0004-0016 — plaque du bas → écrou de la traverse', '2802-0004-0016',
                    poser_vis('2802-0004-0016', [X_FENTE, y, Z_BAS + EP + 2.2], [0, 0, -1])))
        out.append(('jonction, entretoise 1502-0006-0080 — plaque du bas ↔ traverse', '1502-0006-0080',
                    poser_entretoise('1502-0006-0080', [X_FENTE, y, Z_BAS - ENTRETOISE_BAS / 2], [0, 0, 1])))
        out.append(('jonction, écrou Hurricane 2805-0004-0108 dans la fente de la traverse', '2805-0004-0108',
                    _ecrou_fente(y, TRAVERSE_HAUT, -1)))
    return out


def _ecrou_fente(y, face, sens):
    """Écrou Hurricane dans la fente d'un goRAIL dont la face est en z = face, au-delà de ses lèvres de 1,5 mm : sens +1
    pour la fente du dessous (écrou au-dessus de la face), −1 pour celle du dessus."""
    ec = modele('2805-0004-0108', None); ec.apply_translation(-ec.bounds.mean(0))
    e = np.argsort(ec.extents)                                          # épaisseur → z, longueur → y (le long de la fente)
    R_EC = np.zeros((3, 3)); R_EC[2, e[0]] = 1; R_EC[1, e[2]] = 1; R_EC[0, e[1]] = 1
    if np.linalg.det(R_EC) < 0: R_EC[0, e[1]] = -1
    if sens < 0:
        R_EC = np.diag([-1.0, 1.0, -1.0]) @ R_EC                         # retourné : ses ailes vers le bas
    ec.apply_transform(M(R_EC, [X_FENTE, y, face + sens * (1.5 + ec.extents[e[0]] / 2)]))
    return ec


def vers_bras(m):
    """Un maillage du repère du robot (côté droit) vers le repère du bras droit."""
    T = np.linalg.inv(M(R_BRAS, epaule('droit')))
    return _pose(m, T)


def pieces_bras():
    """La jonction dans le repère du bras : (nom, référence, maillage)."""
    return [(n, r, vers_bras(m)) for n, r, m in pieces_robot()]


def axe_pignon_bras():
    """Point de l'axe du pignon de flexion, repère du bras (pour l'animation : il tourne de −90/40 de la flexion)."""
    return vers_bras(trimesh.Trimesh(vertices=[[debut_roue(), Y_AXE - ENTRAXE, Z_AXE]], faces=[])).vertices[0]


def pieces_cote_bras():
    """Ce qui tourne avec la flexion, dans le repère du bras : le goTUBE de flexion (vissé au moyeu 1311 retourné),
    la roue de 90 dents vissée à son bout et ses quatre vis. (nom, référence, maillage)"""
    from v5 import poser_vis
    out = []
    tu = modele(GOTUBE_FLEXION['sku'], None)
    # modèle : axe y (0 → 72), taraudages en (±8 ; ±8) ; demi-tour autour de x : y → −y, bout côté épaule en y = −32,1
    tu.apply_transform(trimesh.transformations.rotation_matrix(np.pi, [1, 0, 0]))
    tu.apply_translation([0, -GOTUBE_FLEXION['debut'] - tu.bounds[1][1], 0])
    out.append(('goTUBE de flexion, 72 mm, vissé sur le moyeu', GOTUBE_FLEXION['sku'], tu))
    fin = -(GOTUBE_FLEXION['debut'] + GOTUBE_FLEXION['longueur'])
    ro = modele('2302-0014-0090', None); ro.apply_translation(-ro.bounds.mean(0))
    ax = np.eye(3)[int(np.argmin(ro.extents))]
    ro.apply_transform(trimesh.geometry.align_vectors(ax, [0, 1, 0]))
    ro.apply_translation([0, fin - ro.extents[1] / 2, 0])
    out.append(('roue alu 90 dents de flexion, vissée au bout du goTUBE', '2302-0014-0090', ro))
    for x, z in ((8.0, 8.0), (-8.0, 8.0), (8.0, -8.0), (-8.0, -8.0)):    # taraudages du bout du goTUBE
        out.append(('vis 2802-0004-0012 — roue de flexion ↔ goTUBE', '2802-0004-0012',
                    poser_vis('2802-0004-0012', [x, fin - ro.extents[1] - 2.2, z], [0, 1, 0])))
    return out


# Maintien axial du goTUBE de flexion (2026-11-25, choix de Nicolas) : il n'est vissé qu'à ses bouts (moyeu de l'épaule,
# roue) ; entre les deux, rien ne l'arrêtait dans les paliers. Deux pièces imprimées, toutes deux dans le corps :
#  - l'entretoise de la roue, entre la roue et le roulement du palier intérieur : bras tiré, la roue (vissée) la pousse
#    contre le roulement ;
#  - le collier de butée, contre le palier extérieur côté flanc, serré sur le tube : bras poussé vers le corps, il bute.
# Les roulements sont en retrait d'1 mm dans leur bloc (alésage Ø39) : chaque pièce n'y appuie que par une lèvre Ø34, sur
# la bague intérieure seule, comme la bague fendue du goTUBE du bras. Le collier ne fait que 6 mm entre le flanc
# (x −143,26) et les plaques : une vis M4 n'y laisserait pas 1 mm de matière, d'où une vis M3 × 12 dans un trou de Ø2,5.
# Le couple passe toujours par la roue vissée : si le collier glissait, le zéro du bras ne bougerait pas.
JEU_AXIAL = 0.3                        # jeu laissé entre la roue et l'entretoise (réglable par cales 1500-0010-0032)
BUTEES = dict(r_alesage=16.15, r_levre=17.0, r_entretoise=19.0, r_collier=22.0, r_oreille=29.0, fente=1.2,
              ep_collier=6.0, levre=1.5, vis=(25.0, 1.6, 1.25))   # vis M3 : rayon de l'axe, passage, avant-trou


def _anneau(r0, r1, z0, z1):
    m = trimesh.creation.annulus(r_min=r0, r_max=r1, height=z1 - z0, sections=96)
    m.apply_translation([0, 0, (z0 + z1) / 2]); return m


def _vers_bras_axe(m, y0, sens):
    """Pièce dessinée autour de l'axe z (z = 0 contre le roulement) vers le repère du bras : z → sens·Y, origine en y0."""
    R = np.array([[1, 0, 0], [0, 0, sens], [0, -sens, 0]], float)          # x → X, y → −sens·Z, z → sens·Y
    m = m.copy(); m.apply_transform(M(R, [0, y0, 0])); return m


def butees():
    """(nom, référence, maillage) des deux butées et de la vis du collier, repère du bras ; écrit leurs STL."""
    from v5 import poser_vis
    B, xe = BUTEES, x_epaule()
    y_de = lambda x: -(x - xe)                                          # x du robot (côté droit) → y du bras
    out = []
    # entretoise : du roulement du palier intérieur (face côté roue) à la roue, moins le jeu
    x_roul, x_bloc = XC + 16 + 2.5, XC + 16 + 3.5
    L = debut_roue() - JEU_AXIAL - x_roul
    e = trimesh.boolean.union([_anneau(B['r_alesage'], B['r_levre'], 0, x_bloc - x_roul + 0.5),
                               _anneau(B['r_alesage'], B['r_entretoise'], x_bloc - x_roul + 0.5 - 0.01, L)], engine='manifold')
    out.append(('entretoise de la roue de flexion', 'IMPRIME:entretoise de la roue de flexion', _vers_bras_axe(e, y_de(x_roul), -1)))
    # collier : col Ø34 du roulement du palier extérieur jusqu'aux plaques, puis l'anneau serré entre plaques et flanc
    x_roul, x_pl = XC - 16 - 2.5, XC - 21.5
    col = x_roul - (x_pl - 0.3)                                         # le col dépasse des plaques de 0,3 mm
    z1 = col + B['ep_collier']
    pieces = [_anneau(B['r_alesage'], B['r_levre'], 0, col + 0.01), _anneau(B['r_alesage'], B['r_collier'], col, z1)]
    oreille = trimesh.creation.box(extents=(14.0, B['r_oreille'] - 18.0, B['ep_collier']))
    oreille.apply_translation([0, -(18.0 + B['r_oreille']) / 2, col + B['ep_collier'] / 2])
    c = trimesh.boolean.union(pieces + [oreille], engine='manifold')
    fente = trimesh.creation.box(extents=(B['fente'], 20.0, z1 + 2)); fente.apply_translation([0, -(B['r_alesage'] + 9.0), z1 / 2])
    alesage = trimesh.creation.cylinder(radius=B['r_alesage'], height=z1 + 2, sections=96); alesage.apply_translation([0, 0, z1 / 2])
    ry, zv = -B['vis'][0], col + B['ep_collier'] / 2
    passage = trimesh.creation.cylinder(radius=B['vis'][1], segment=[(-8, ry, zv), (0, ry, zv)], sections=24)
    avant_trou = trimesh.creation.cylinder(radius=B['vis'][2], segment=[(0, ry, zv), (8, ry, zv)], sections=24)
    c = trimesh.boolean.difference([c, fente, alesage, passage, avant_trou], engine='manifold')
    out.append(('collier de butée du goTUBE de flexion', 'IMPRIME:collier de butée du goTUBE de flexion',
                _vers_bras_axe(c, y_de(x_roul), +1)))
    vis = trimesh.creation.cylinder(radius=1.5, segment=[(-7, ry, zv), (5, ry, zv)], sections=16)
    tete = trimesh.creation.cylinder(radius=2.75, segment=[(-10, ry, zv), (-7, ry, zv)], sections=24)
    out.append(('vis M3 × 12 du collier de butée', 'VIS:vis M3 × 12 à tête cylindrique, vissée dans le plastique (avant-trou Ø2,5)',
                _vers_bras_axe(trimesh.util.concatenate([vis, tete]), y_de(x_roul), +1)))
    d = os.path.join(SORTIE, 'jonction'); os.makedirs(d, exist_ok=True)
    out[0][2].export(os.path.join(d, 'entretoise_roue_position.stl'))
    out[1][2].export(os.path.join(d, 'collier_butee_position.stl'))
    return out


if __name__ == '__main__':
    print('axe de flexion z %.2f ; épaule droite x %.2f, gauche x %.2f ; roue à partir de x %.2f' % (
        Z_AXE, x_epaule(), epaule('gauche')[0], debut_roue()))
    for n, r, m in pieces_robot():
        print('%-62s %-15s %s' % (n, r, np.round(m.bounds, 1).tolist()))
    for n, r, m in pieces_cote_bras():
        print('%-62s %-15s %s' % (n, r, np.round(m.bounds, 1).tolist()))
