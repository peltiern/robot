#!/usr/bin/env python3
"""
La main du bras : doigts et pouce du « WALL•E Motion Arm » (wallebuilders.club, v2025) à l'échelle 0,7, sur une
paume dessinée ici, vissée au bout du goTUBE du bras.

Ce qui vient du Motion Arm, réduit à 0,7 : les deux doigts (jointure et phalange fusionnées, phalange figée), leurs
caches, et le pouce d'une pièce (sauf son moyeu). Ce qui est dessiné ici : le poignet (base vissée au goTUBE, chambre du fléau),
la paume (deux joues qui tiennent l'axe), le moyeu du pouce avec sa poulie, la poulie des doigts sur la jointure B,
le fléau.

Le mécanisme (choix B1 de Nicolas, 2026-11-08) : doigts et pouce tournent sur le même axe Ø4 fixe, en sens
contraires. Un seul câble, celui du HS-65MG, tire le milieu d'un fléau flottant dans le poignet ; chaque bout du
fléau tire une poulie (doigts, pouce). Le fléau partage l'effort en deux et laisse un côté continuer quand l'autre
touche l'objet. Deux ressorts de torsion sur l'axe rouvrent la main.

Repère de travail H : origine au milieu de l'axe des doigts, u = x du bras (vers le bout des doigts), v = y,
w = z (le long de l'axe). Côté +w (côté du canal +z du goTUBE, par où arrive le câble) : les deux poulies et le
fléau. Dans H, le pouce est côté +v, comme dans le Motion Arm ; fermer y fait tourner les doigts de +RAPPORT·c autour
de +w, le pouce de −RAPPORT·c.

Le repère du bras s'en déduit par une symétrie v → −v (COTE_POUCE) puis une translation de X_AXE en x : Nicolas veut
le pouce à l'intérieur, côté corps (y−). Un demi-tour autour du goTUBE l'y mettait aussi, mais emmenait poulies et
fléau sous le canal −z, alors que la liaison imprimée amène le câble dans le +z. La main est presque symétrique le
long de son axe (écart médian 0,85 mm), si bien que la symétrie a l'allure du demi-tour ; elle inverse en revanche
le sens de fermeture vu du bras.

Écrit dans SORTIE/pince/ : <pièce>_position.stl (repère du bras) et .step pour les pièces imprimées.
"""
import json, math, os, sys
import numpy as np
import cadquery as cq
from OCP.gp import gp_Trsf
from OCP.BRepBuilderAPI import BRepBuilderAPI_Transform
from OCP.ShapeFix import ShapeFix_FixSmallFace
import trimesh

ICI = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, ICI)
from recaler import SORTIE

DOSSIER = os.path.join(SORTIE, 'pince')
MA = os.path.join(DOSSIER, 'ma')                  # solides du Motion Arm extraits de Full-Arm-Assembly-v2025.step
ECHELLE = 0.7
# Dans l'assemblage du Motion Arm : axe des doigts en (x 88,93 ; z 371,06), le long de y ; milieu des doigts en y
MA_AXE = (88.93, -7.15, 371.06)

BOUT_TUBE = 265.6
COTE_POUCE = -1                                   # pouce côté corps (y−) ; +1 = pose du Motion Arm
# Le long du bras, depuis l'axe des doigts : fond du poignet (contre le bout du goTUBE), chambre du fléau, plancher
# de la paume. Le plancher reste sous le bas des jointures (u −15,7) et entre elles (|w| < 17).
FOND = (-37.0, -34.0)
PLANCHER = (-20.0, -17.0)
X_AXE = BOUT_TUBE - FOND[0]
R_POIGNET = 17.0                                  # Ø34 : le fléau tient sous le canal +z (le goTUBE fait Ø32)

# Empilement le long de l'axe (w). Jeu libre entre les jointures, près de l'axe : −18,5 → +17,25.
JOINTURE_A, JOINTURE_B = -18.5, 17.25
POULIE_DOIGTS = (13.75, 17.25)                    # sur la jointure B, d'une pièce avec elle
JOUE_B = (8.5, 13.25)
POULIE_POUCE = (4.5, 8.0)                         # d'une pièce avec le moyeu du pouce
MOYEU_POUCE = (-4.5, 4.5)
RESSORT_POUCE = (-8.0, -4.5)
JOUE_A = (-13.5, -8.5)
RESSORT_DOIGTS = (-18.0, -14.0)
R_JOUE = 6.0
R_POULIE, R_GORGE, GORGE = 10.5, 9.0, 1.2         # flasques, fond de gorge, largeur de gorge
R_ENROULEMENT = R_GORGE + 0.2                     # tresse PE de 0,4 mm au fond de la gorge
# Ancrage d'un brin dans sa poulie : la tresse quitte la gorge au bout de son enroulement (φ = ±90° dans H, pose du
# Motion Arm) par un trou Ø1 tiré en corde dans le plan de la gorge, et ressort 60° plus loin dans une poche radiale
# Ø2,6 où loge son nœud. Poche et trou restent dans l'arc que la tresse ne couvre jamais (elle s'enroule sur 175° à
# 216°). Le premier ancrage (2026-11-08) était un trou parallèle à l'axe, bouché d'un côté par le moyeu ou la
# jointure, à 0,5 mm de la joue de l'autre, et sous le fond de gorge : la tresse ne pouvait ni y entrer ni s'y nouer.
ANCRAGE = dict(r_trou=0.5, r_poche=1.3, ecart=60.0, r_entree=9.6, r_poche_fond=6.8, r_bout=7.3)

# Roulements goBILDA 1600-0410-0004 (4 × 10 × 4) : un dans chaque jointure, deux dans le moyeu du pouce. L'axe Ø4
# est fixe, serré dans les joues ; il est enfilé par la jointure B, sous le cache.
ROULEMENT = dict(r=5.0, ep=4.0)
LOGEMENTS_DOIGTS = [(JOINTURE_B, JOINTURE_B + 4.0), (JOINTURE_A - 4.0, JOINTURE_A)]
LOGEMENTS_POUCE = [(MOYEU_POUCE[0], MOYEU_POUCE[0] + 4.0), (MOYEU_POUCE[1] - 4.0, MOYEU_POUCE[1])]
AXE = (JOINTURE_A - 4.0, JOINTURE_B + 4.0)        # tige inox Ø4 × 44
R_AXE_SERRE, R_AXE_LIBRE = 1.95, 2.3

# Le fléau : sous le canal +z du goTUBE (z 6,8 → 15,2, milieu 11), entre les deux plans de poulie.
# Ses trous des bouts sont presque à l'aplomb des poulies : un brin qui dérive en w frotte sur la joue B (contrôle du
# 2026-11-08 : avec des trous à 8,9 et 13,1, les deux brins passaient au ras de ses faces). Le milieu (10,5) reste
# sous le canal (11). Ses bouts dépassent les trous de 0,3 mm : tangents, ils ouvraient le maillage.
FLEAU = dict(w=(6.1, 14.9), v=1.6, u=3.0, trous=(7.0, 10.5, 14.0))
CHAMBRE = dict(w=(5.0, 15.5), v=6.0)
CANAL_Z = 11.0

# Vis : poignet → goTUBE (motif taraudé du bout du tube), paume → poignet (entre les joues, sous le moyeu du pouce)
VIS_TUBE = [(8.0, 8.0), (-8.0, 8.0), (8.0, -8.0), (-8.0, -8.0)]
VIS_PAUME = [(12.0, 0.0), (-12.0, 0.0)]          # loin des têtes des vis du tube (bossage Ø8 / tête Ø7,6)

# Course : du palonnier à 0° (main ouverte) à +60° (fermée), le câble s'allonge de 6,56 mm (animation.L).
COURSE_CABLE = 6.56
RAPPORT = math.degrees(COURSE_CABLE / R_ENROULEMENT) / 60.0
# La pose du Motion Arm est presque fermée : à +5,5° de chaque côté, le bout du pouce touche les phalanges (contrôle
# exact du 2026-11-08). Fermée (palonnier à +60°) = pose du Motion Arm + 4,5° ; le modèle est dessiné ouvert.
AU_DELA = 4.5
OUVERTURE = RAPPORT * 60.0 - AU_DELA              # ≈ 36° de chaque côté, 72° entre pouce et doigts


def tourner_ma():
    T = gp_Trsf(); T.SetValues(0, 0, 1, 0, -1, 0, 0, 0, 0, -1, 0, 0)      # (x, y, z) du MA → (z, −x, −y)
    return T


def ma(nom):
    """Un solide du Motion Arm, réduit à l'échelle et posé dans le repère H."""
    s = cq.importers.importStep(os.path.join(MA, nom + '.step')).val()
    # Upper-Finger-A porte des faces dégénérées (aire nulle) : le maillage n'était pas fermé, et les contrôles de
    # trajet de la notice se seraient rabattus sur l'enveloppe convexe
    f = ShapeFix_FixSmallFace(); f.Init(s.wrapped); f.SetPrecision(0.05); f.Perform()
    s = cq.Shape.cast(f.FixShape())
    s = s.translate(cq.Vector(-MA_AXE[0], -MA_AXE[1], -MA_AXE[2])).scale(ECHELLE)
    return cq.Shape.cast(BRepBuilderAPI_Transform(s.wrapped, tourner_ma(), True).Shape())


def wcyl(r, w0, w1, u=0.0, v=0.0):
    return cq.Workplane("XY").workplane(offset=w0).center(u, v).circle(r).extrude(w1 - w0).val()


def ucyl(r, u0, u1, v=0.0, w=0.0):
    return cq.Workplane("YZ").workplane(offset=u0).center(v, w).circle(r).extrude(u1 - u0).val()


def boite(u0, u1, v0, v1, w0, w1):
    return cq.Workplane("XY").box(u1 - u0, v1 - v0, w1 - w0, centered=False).translate((u0, v0, w0)).val()


def ancrage(ancre_v):
    """(entrée du trou dans la gorge, centre de la poche) en (u, v), pose du Motion Arm."""
    a = math.radians(90.0 * ancre_v)
    b = a + math.radians(ANCRAGE['ecart'] * ancre_v)
    return ((ANCRAGE['r_entree'] * math.cos(a), ANCRAGE['r_entree'] * math.sin(a)),
            (ANCRAGE['r_bout'] * math.cos(b), ANCRAGE['r_bout'] * math.sin(b)), b)


def poulie(w0, w1, r_alesage, ancre_v):
    """Poulie à gorge ; ancre_v : côté (±v) où le brin finit son enroulement, pose du Motion Arm."""
    p = wcyl(R_POULIE, w0, w1).cut(wcyl(r_alesage, w0 - 1, w1 + 1))
    m = (w0 + w1) / 2
    return p.cut(wcyl(R_POULIE + 1, m - GORGE / 2, m + GORGE / 2).cut(wcyl(R_GORGE, m - 1, m + 1)))


def percer_ancrage(piece, w0, w1, ancre_v):
    """Trou en corde et poche du nœud, percés après la fusion de la poulie avec sa pièce."""
    (ua, va), (ub, vb), b = ancrage(ancre_v)
    m = (w0 + w1) / 2
    A, B = cq.Vector(ua, va, m), cq.Vector(ub, vb, m)
    trou = cq.Solid.makeCylinder(ANCRAGE['r_trou'], (B - A).Length, A, (B - A).normalized())
    rad = cq.Vector(math.cos(b), math.sin(b), 0)
    poche = cq.Solid.makeCylinder(ANCRAGE['r_poche'], R_POULIE + 1 - ANCRAGE['r_poche_fond'],
                                  cq.Vector(0, 0, m) + rad * ANCRAGE['r_poche_fond'], rad)
    return piece.cut(trou).cut(poche)


# ---------------------------------------------------------------------------------------------- pièces dessinées

def poignet():
    """Base vissée au bout du goTUBE : fond, centrage dans l'alésage Ø14, chambre du fléau, bossages des vis de la
    paume. Les quatre vis M4 du tube se posent par le haut, chambre ouverte, avant la paume."""
    p = ucyl(R_POIGNET, FOND[0], PLANCHER[0])
    p = p.fuse(ucyl(6.9, FOND[0] - 3.0, FOND[0]))                       # centrage dans l'alésage du goTUBE
    # chambre : oblongue sous le canal, et assez large pour poser les vis du tube
    chambre = ucyl(R_POIGNET - 1.6, FOND[1], PLANCHER[0] + 0.01)
    p = p.cut(chambre)
    for v, w in VIS_PAUME:                                                # bossages Ø8 contre la paroi
        p = p.fuse(ucyl(4.0, FOND[1], PLANCHER[0], v, w).intersect(ucyl(R_POIGNET, FOND[1], PLANCHER[0])))
        p = p.cut(ucyl(1.75, FOND[1] + 2, PLANCHER[0] + 0.01, v, w))   # avant-trou M4 dans le plastique
    for v, w in VIS_TUBE:
        p = p.cut(ucyl(2.2, FOND[0] - 1, FOND[1] + 0.01, v, w))
    # Passage de la tresse du servo, chanfreiné des deux côtés : c'est, avec la lumière du berceau, le seul bord
    # qu'elle touche. Pas de gaine dans le canal (abandonnée le 2026-11-08) : le servo tourne avec le goTUBE, la
    # tresse y file droit à plus de 4 mm des parois, et rien ne tenait la gaine en place.
    p = p.cut(ucyl(1.2, FOND[0] - 4, FOND[1] + 0.01, 0.0, CANAL_Z))
    for x, sens in ((FOND[0], 1), (FOND[1], -1)):
        p = p.cut(cq.Solid.makeCone(2.2, 1.2, 1.0, cq.Vector(x - sens * 0.01, 0.0, CANAL_Z), cq.Vector(sens, 0, 0)))
    return p


def paume():
    """Plancher posé sur le poignet et deux joues qui serrent l'axe."""
    p = ucyl(R_POIGNET, PLANCHER[0], PLANCHER[1])
    for w0, w1 in (JOUE_A, JOUE_B):
        profil = (cq.Workplane("XY").workplane(offset=w0).center(0, 0).circle(R_JOUE).extrude(w1 - w0).val()
                  .fuse(boite(PLANCHER[1] - 0.5, 0.0, -R_JOUE, R_JOUE, w0, w1)))
        p = p.fuse(profil)
    p = p.cut(wcyl(R_AXE_SERRE, JOUE_A[0] - 1, JOUE_B[1] + 1))
    for v, w in VIS_PAUME:
        p = p.cut(ucyl(2.2, PLANCHER[0] - 1, PLANCHER[1] + 1, v, w))
    # fentes des deux brins : sous chaque plan de poulie, étirées en v (le brin descend en biais vers le fléau)
    for w0, w1 in ((POULIE_POUCE[0] + 0.15, JOUE_B[0] - 0.65), (JOUE_B[1] + 0.25, POULIE_DOIGTS[1] - 0.65)):
        p = p.cut(boite(PLANCHER[0] - 1, PLANCHER[1] + 1, -R_GORGE - 1.5, R_GORGE + 1.5, w0, w1))
    return p


def fleau():
    u0 = PLANCHER[0] - 1.5 - FLEAU['u']
    f = boite(u0, u0 + FLEAU['u'], -FLEAU['v'], FLEAU['v'], *FLEAU['w'])
    for w in FLEAU['trous']:
        f = f.cut(ucyl(0.6, u0 - 1, u0 + FLEAU['u'] + 1, 0.0, w))
    return f


def moyeu_pouce():
    """Le pivot A du pouce, réduit et rogné entre les joues, autour de deux roulements, et la poulie du pouce."""
    pivot = ma('Thumb-Pivot-A_1').intersect(boite(-60, 60, -60, 60, *MOYEU_POUCE))
    # Un seul logement traversant : les deux roulements entrent par le côté A, le second jusqu'à la poulie, dont
    # l'alésage Ø7 ne porte que sur leur bague extérieure (la bague intérieure reste avec l'axe fixe).
    m = pivot.fuse(wcyl(7.5, *MOYEU_POUCE)).fuse(poulie(*POULIE_POUCE, 3.5, +1))
    m = percer_ancrage(m, *POULIE_POUCE, +1)
    return m.cut(wcyl(ROULEMENT['r'], MOYEU_POUCE[0] - 0.01, MOYEU_POUCE[1]))


def jointure(nom, cote):
    """Jointure d'un doigt : le logement du moyeu 1314 du Motion Arm est rebouché autour de l'axe, puis repercé
    pour un roulement et pour enfiler l'axe depuis l'extérieur (sous le cache)."""
    j = ma(nom)
    if cote > 0:
        # la poulie laisse passer le roulement (alésage Ø10,2) jusqu'à son logement dans la jointure
        j = j.fuse(wcyl(9.0, JOINTURE_B, 40.5)).fuse(poulie(*POULIE_DOIGTS, ROULEMENT['r'] + 0.1, -1))
        j = percer_ancrage(j, *POULIE_DOIGTS, -1)
        w0, w1 = LOGEMENTS_DOIGTS[0]
        j = j.cut(wcyl(ROULEMENT['r'], w0, w1)).cut(wcyl(R_AXE_LIBRE, POULIE_DOIGTS[0] - 1, 46))
    else:
        j = j.fuse(wcyl(9.0, -41.5, JOINTURE_A))
        w0, w1 = LOGEMENTS_DOIGTS[1]
        j = j.cut(wcyl(ROULEMENT['r'], w0, w1)).cut(wcyl(R_AXE_LIBRE, -47, w1))
    return j


# Liaison des deux doigts. Dans le Motion Arm, ils étaient serrés sur un même moyeu ; ici chacun tourne sur son
# roulement et seule la jointure B porte une poulie : sans liaison, le doigt A ne fermait pas (vu le 2026-11-08, après
# une première notice qui les disait « collés » alors qu'un jeu de 1,3 mm les séparait). Les faces planes du doigt B
# tournées vers A sont prolongées jusqu'à elles (cale), et deux chevilles en filament 1,75 mm collées dans des trous
# Ø1,9 portent l'effort ; la colle ne fait que tenir les deux doigts serrés l'un contre l'autre.
# Les jointures ne sont pas pleines derrière la face : les deux chevilles sont aux points (pose du Motion Arm, repère
# H) où les deux doigts ont de la matière tout autour du trou sur toute sa profondeur, les plus éloignés l'un de
# l'autre (28,5 mm ; recherche sur une grille de 1,5 mm le 2026-11-08). Le premier choix, géométrique, tombait dans
# des creux.
CHEVILLE = dict(r=0.95, profondeur=6.0, axes=[(46.14, -13.27), (19.14, -4.27)])


def _faces_regard(s, signe):
    """Faces planes d'un doigt tournées vers l'autre doigt (normale signe·w), près du plan médian."""
    return [f for f in s.Faces() if f.geomType() == 'PLANE' and abs(f.normalAt().z * signe - 1) < 1e-3
            and abs(f.Center().z) < 2.0]


def _empreinte(f):
    from shapely.geometry import Polygon
    pts = []
    for e in f.outerWire().Edges():
        pts += [(p.x, p.y) for p in e.positions(np.linspace(0, 1, 12))]
    return Polygon(pts).buffer(0)


def liaison_doigts():
    """(cales à fusionner au doigt B, axes des chevilles en (u, v), plan de contact w) — dans H."""
    from shapely.ops import unary_union
    A = [ma('Upper-Finger-A_1'), ma('Lower-Finger-A_1')]
    B = [ma('Upper-Finger-B_1'), ma('Lower-Finger-B_1')]
    fa = [(f, _empreinte(f)) for s in A for f in _faces_regard(s, +1)]
    cales, contacts = [], []
    for s in B:
        for fb in _faces_regard(s, -1):
            pb = _empreinte(fb)
            # la face de A en regard : la plus proche dessous, dont l'empreinte recouvre au moins la moitié de celle-ci
            cibles = [(f, p) for f, p in fa if f.Center().z < fb.Center().z - 0.3 and p.intersection(pb).area > 0.5 * pb.area]
            if not cibles:
                continue
            f, p = max(cibles, key=lambda c: c[0].Center().z)
            cales.append(cq.Solid.extrudeLinear(fb, cq.Vector(0, 0, f.Center().z - fb.Center().z)))
            contacts.append((p.intersection(pb), f.Center().z))
    # les cales s'arrêtent sur A : sur la face des phalanges, elles mordaient de 0,2 mm³ sur un arrondi
    A = A[0].fuse(A[1])
    cales = [c.cut(A) for c in cales]
    _, w = max(contacts, key=lambda c: c[0].area)
    return cales, CHEVILLE['axes'], w


def doigt(cote):
    """Un doigt d'une pièce : jointure, phalange et cache de phalange fusionnés (la phalange est figée ; collée sur
    le cylindre d'articulation du Motion Arm, seule la colle l'aurait empêchée de pivoter)."""
    l, k = ('A', 2) if cote < 0 else ('B', 1)
    d = jointure('Upper-Finger-%s_1' % l, cote).fuse(ma('Lower-Finger-%s_1' % l)).fuse(ma('Knuckel-Rod-Cap_%d' % k))
    cales, chevilles, w = liaison_doigts()
    if cote > 0:
        for c in cales:
            d = d.fuse(c)
    for u, v in chevilles:
        w0, w1 = (w - CHEVILLE['profondeur'], w + 0.01) if cote < 0 else (w - 0.01, w + CHEVILLE['profondeur'])
        d = d.cut(wcyl(CHEVILLE['r'], w0, w1, u, v))
    return d.clean()


def pouce():
    """Le pouce d'une pièce : pivot, bases, bouts, goupille et son cache fusionnés (le bout s'articulait sur la base
    par un cylindre et la goupille : collé, seule la colle l'aurait tenu). Le pivot entre dans la fente du moyeu,
    tenon dans une mortaise : collé là, il ne peut pas tourner.

    Les pièces du Motion Arm gardent leurs jeux de fonctionnement (0,14 mm dans l'articulation base/bout autour de
    la goupille, 0,04 mm entre pivot et base) : fusionnées telles quelles, elles restaient trois morceaux.
    L'épaississement d'OCC échoue sur ces formes ; les jeux sont donc comblés à la main."""
    S = {n: ma(n) for n in ('Thumb-Pivot-B_1', 'Upper-Thumb-A_1', 'Upper-Thumb-B_1', 'Lower-Thumb-A_1',
                            'Lower-Thumb-B_1', 'Thumb-Pin_1', 'Thumb-Pin-Cap_1')}
    p = S['Thumb-Pivot-B_1']
    for n, s in S.items():
        if n != 'Thumb-Pivot-B_1':
            p = p.fuse(s)
    # articulation : un cylindre plein dans l'axe de la goupille, 0,2 mm plus gros qu'elle, ferme le jeu de 0,14
    f = max((f for f in S['Thumb-Pin_1'].Faces() if f.geomType() == 'CYLINDER'), key=lambda f: f.Area())
    c = f._geomAdaptor().Cylinder()
    axe = cq.Vector(c.Axis().Direction().X(), c.Axis().Direction().Y(), c.Axis().Direction().Z())
    lg = f.Area() / (2 * math.pi * c.Radius())
    p = p.fuse(cq.Solid.makeCylinder(c.Radius() + 0.2, lg, f.Center() - axe * (lg / 2), axe))
    # pivot/base : là où leurs copies grossies de 2 % se recouvrent, c'est-à-dire dans le jeu qui les sépare ;
    # le tenon garde sa taille dans la mortaise du moyeu
    def grossi(s):
        g = s.Center()
        return s.translate(-g).scale(1.02).translate(g)
    p = p.fuse(grossi(S['Thumb-Pivot-B_1']).intersect(grossi(S['Upper-Thumb-A_1'])))
    return p.clean()


IMPRIMEES = {
    # nom : (fabrique, ensemble)
    'poignet': (poignet, 'tube'),
    'paume': (paume, 'tube'),
    'fleau': (fleau, 'tube'),
    'pouce_moyeu': (moyeu_pouce, 'pouce'),
    'doigt_A': (lambda: doigt(-1), 'doigts'),
    'doigt_B': (lambda: doigt(+1), 'doigts'),
    'doigt_A_cache': (lambda: ma('Finger-End-Cap_2'), 'doigts'),
    'doigt_B_cache': (lambda: ma('Finger-End-Cap_1'), 'doigts'),
    'pouce': (pouce, 'pouce'),
}


def vers_bras(s, ensemble):
    """Pose ouverte dans H (doigts −OUVERTURE, pouce +OUVERTURE autour de l'axe), puis repère du bras."""
    a = {'doigts': -OUVERTURE, 'pouce': OUVERTURE}.get(ensemble, 0.0)
    if a:
        s = s.rotate(cq.Vector(0, 0, 0), cq.Vector(0, 0, 1), a)
    # poignet, paume et fléau sont symétriques en v : les retourner ne change rien, sinon d'ouvrir le maillage du
    # fléau (ses trous tombent sur le plan de symétrie)
    if COTE_POUCE < 0 and ensemble in ('doigts', 'pouce'):
        s = s.mirror('XZ')
    return s.translate(cq.Vector(X_AXE, 0, 0))


def exporter_stl(s, chemin):
    """Maillage fermé : sommets arrondis au 1/10 000 de mm puis fusionnés (l'export STL direct laisse des coutures)."""
    v, f = s.tessellate(0.02, 0.1)
    m = trimesh.Trimesh(np.round(np.array([x.toTuple() for x in v]), 4), np.array(f))
    m.merge_vertices(); m.update_faces(m.nondegenerate_faces()); m.remove_unreferenced_vertices()
    if not m.is_watertight:
        print('   maillage non fermé :', os.path.basename(chemin))
    m.export(chemin)


def construire(seulement=None):
    os.makedirs(DOSSIER, exist_ok=True)
    resume = {}
    for nom, (fab, ens) in IMPRIMEES.items():
        if seulement and nom not in seulement:
            continue
        s = vers_bras(fab(), ens)
        exporter_stl(s, os.path.join(DOSSIER, nom + '_position.stl'))
        cq.exporters.export(cq.Workplane().add(s), os.path.join(DOSSIER, nom + '_position.step'))
        resume[nom] = dict(ensemble=ens, volume_cm3=round(s.Volume() / 1000, 2), valide=s.isValid())
        print('%-24s %-7s %6.2f cm³ %s' % (nom, ens, s.Volume() / 1000, '' if s.isValid() else 'INVALIDE'))
    if not seulement:
        json.dump(dict(pieces=resume, x_axe=X_AXE, rapport=RAPPORT, ouverture=OUVERTURE), open(os.path.join(DOSSIER, 'pince.json'), 'w'),
                  ensure_ascii=False, indent=1)


# ------------------------------------------------------------------------------------------- dans le modèle v5

NOMS = {
    'poignet': 'main, poignet vissé au goTUBE',
    'paume': 'main, paume (joues de l\'axe)',
    'fleau': 'main, fléau du câble de pince',
    'pouce_moyeu': 'main, pouce : moyeu et poulie',
    'doigt_A': 'main, doigts : doigt A (jointure et phalange)',
    'doigt_B': 'main, doigts : doigt B (jointure, phalange et poulie)',
    'doigt_A_cache': 'main, doigts : cache de l\'axe, côté A',
    'doigt_B_cache': 'main, doigts : cache de l\'axe, côté B',
    'pouce': 'main, pouce : pouce (pivot, base et bout)',
}
U_FLEAU = PLANCHER[0] - 1.5 - FLEAU['u']           # dessous du fléau, main ouverte


def en_bras(u, v, w):
    return np.array([X_AXE + u, COTE_POUCE * v, w], float)


def brins():
    """Les deux brins, main ouverte : du dessus du fléau à la poulie, puis enroulés jusqu'à l'ancrage.
    Doigts : le brin descend côté +v (tirer ferme vers +w) ; pouce : côté −v."""
    out = {}
    for nom, (w0, w1), w_fleau, signe, ouvert in (
            ('doigts', POULIE_DOIGTS, FLEAU['trous'][2], +1, -OUVERTURE),
            ('pouce', POULIE_POUCE, FLEAU['trous'][0], -1, +OUVERTURE)):
        w = (w0 + w1) / 2
        pts = [en_bras(U_FLEAU + FLEAU['u'], 0.0, w_fleau)]
        # de la tangente (φ = ±90°) par-dessus la poulie (φ = 0) jusqu'à l'ancrage (φ = ∓90° + rotation d'ouverture)
        fin = -signe * 90.0 + ouvert
        for phi in np.linspace(signe * 90.0, fin, 16):
            a = math.radians(phi)
            pts.append(en_bras(R_ENROULEMENT * math.cos(a), R_ENROULEMENT * math.sin(a), w))
        # dans le trou en corde jusqu'à la poche du nœud (l'ancrage tourne avec la poulie, ouverte de `ouvert`)
        _, (ub, vb), _ = ancrage(-signe)
        c, s = math.cos(math.radians(ouvert)), math.sin(math.radians(ouvert))
        pts.append(en_bras(c * ub - s * vb, s * ub + c * vb, w))
        out[nom] = pts
    return out


def cable_servo():
    """Du bout du goTUBE au dessous du fléau (les points d'avant sont ceux d'animation.cable_de_pince)."""
    return [np.array([BOUT_TUBE, 0.0, CANAL_Z]), en_bras(FOND[1], 0.0, CANAL_Z), en_bras(U_FLEAU, 0.0, FLEAU['trous'][1])]


def ressort(w0, w1):
    """Ressort de torsion, dessiné en spires jointives (fil 0,6, Ø intérieur 4,5) ; ses branches restent à poser
    quand Nicolas aura les ressorts en main."""
    spires = []
    n = max(1, int((w1 - w0) / 0.6))
    for k in range(n):
        t = trimesh.creation.torus(major_radius=2.55, minor_radius=0.3, major_sections=24, minor_sections=8)
        t.apply_translation((X_AXE, 0, w0 + 0.3 + k * 0.6)); spires.append(t)
    return trimesh.util.concatenate(spires)


def pieces_main():
    """(nom, nature, maillage, référence) des pièces de la main, posées main ouverte, pour la v5."""
    from v5 import poser_vis
    from recaler import modele
    out = []
    for cle, nom in NOMS.items():
        m = trimesh.load(os.path.join(DOSSIER, cle + '_position.stl'))
        out.append((nom, 'imprimée', m, 'IMPRIME:' + nom))
    rlt = modele('1600-0410-0004', None)
    rlt.apply_translation(-rlt.bounds.mean(0))
    rlt.apply_transform(trimesh.geometry.align_vectors(np.eye(3)[int(np.argmin(rlt.extents))], [0, 0, 1]))
    for nom, (w0, w1) in (('main, doigts : roulement de la jointure B', LOGEMENTS_DOIGTS[0]),
                          ('main, doigts : roulement de la jointure A', LOGEMENTS_DOIGTS[1]),
                          ('main, pouce : roulement côté A', LOGEMENTS_POUCE[0]),
                          ('main, pouce : roulement côté B', LOGEMENTS_POUCE[1])):
        m = rlt.copy(); m.apply_translation(en_bras(0, 0, (w0 + w1) / 2))
        out.append((nom, 'achetée', m, '1600-0410-0004'))
    axe = trimesh.creation.cylinder(radius=2.0, segment=[en_bras(0, 0, AXE[0]), en_bras(0, 0, AXE[1])], sections=24)
    out.append(('main : axe des doigts et du pouce', 'visserie ajoutée', axe,
                'VIS:tige inox Ø4 mm, %d mm (axe de la main)' % round(AXE[1] - AXE[0])))
    for nom, (w0, w1) in (('main : ressort de rappel du pouce', RESSORT_POUCE),
                          ('main : ressort de rappel des doigts', RESSORT_DOIGTS)):
        out.append((nom, 'visserie ajoutée', ressort(w0, w1), 'VIS:ressort de torsion, fil 0,6 mm, Ø intérieur 4,5 mm'))
    for v, w in VIS_TUBE:
        out.append(('vis 2802-0004-0008 — main, poignet ↔ goTUBE', 'visserie ajoutée',
                    poser_vis('2802-0004-0008', en_bras(FOND[1] + 2.2, v, w), [-1, 0, 0]), '2802-0004-0008'))
    for v, w in VIS_PAUME:
        out.append(('vis 2802-0004-0010 — main, paume ↔ poignet', 'visserie ajoutée',
                    poser_vis('2802-0004-0010', en_bras(PLANCHER[1] + 2.2, v, w), [-1, 0, 0]), '2802-0004-0010'))
    from cables import cordon
    for nom, pts in brins().items():
        out.append(('câble de pince, brin %s' % ('des doigts' if nom == 'doigts' else 'du pouce'), 'câblage',
                    cordon(pts, r=0.2), 'CABLE:câble de pince, brin %s' % nom))
    return out


if __name__ == '__main__':
    construire(sys.argv[1:] or None)
