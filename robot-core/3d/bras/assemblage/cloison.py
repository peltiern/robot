#!/usr/bin/env python3
"""
La cloison imprimée devient la pièce qui tient le caisson : sans la barrette 1147, rien ne relie plus
les profilés, les plaques latérales, le module du tube et le cadre du servo d'abduction (voir
fixations.py : aucune liaison goBILDA directe n'existe sans perçage).

On part de la cloison de la v5 (poche du D85MG, passage du fil du micro-servo) et on lui ajoute :
  - des écrous M4 Nylstop logés, en face des trous existants des profilés et des plaques de 96 mm ;
    la vis entre de l'extérieur, l'écrou se glisse par une fente qui débouche sur la face la plus proche ;
  - deux bras, côté y−, qui viennent s'appuyer sur la face arrière du palier avancé et s'y vissent
    par ses trous d'angle ;
  (la languette posée un temps sur le cadre 1802 est retirée le 2026-10-03 : le cadre se visse dans les
  trous oblongs des plaques de 96 mm, que la recherche de trous ne voyait pas — voir v5.py, FIXATIONS_CADRE)

Écrit cloison/ sur le disque externe : la pièce dans le repère du bras, la version à imprimer, et la
visserie ajoutée (json).
"""
import json, math, os, sys
import numpy as np, trimesh

ICI = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, ICI)
from recaler import SORTIE, repere_bras, pieces_du_bras
from construire import maillage

DOSSIER = os.path.join(SORTIE, 'cloison')
X0, X1 = 97.2, 114.0                      # faces arrière et avant de la cloison v5
JEU = 0.3                                  # jeu d'impression sur les logements
ECROU_PLATS, ECROU_EP = 7.0, 4.9           # 2812-0004-0007
VIS_D = 4.4                                # passage M4 dans le plastique
TETE_D = 8.4                               # tête bombée 2802 (Ø7,6) et la clé
X_PALIER = 132.4                           # face arrière du bloc 1604 avancé de 8 mm (v5)


def boite(x0, x1, y0, y1, z0, z1):
    b = trimesh.creation.box(extents=(x1 - x0, y1 - y0, z1 - z0))
    b.apply_translation(((x0 + x1) / 2, (y0 + y1) / 2, (z0 + z1) / 2))
    return b


def cylindre(r, a, b, sections=48):
    """Cylindre de rayon r de a à b (points 3D)."""
    a, b = np.asarray(a, float), np.asarray(b, float)
    return trimesh.creation.cylinder(radius=r, segment=[a, b], sections=sections)


def hexagone(centre, axe, plats, epaisseur, tour=0.0):
    """Prisme hexagonal d'axe donné, centré sur centre (logement d'écrou) ; tour : rotation sur son axe, en
    degrés (0 : un angle dans la direction x locale)."""
    h = trimesh.creation.cylinder(radius=plats / np.sqrt(3), height=epaisseur, sections=6)
    h.apply_transform(trimesh.transformations.rotation_matrix(np.radians(tour), [0, 0, 1]))
    h.apply_transform(trimesh.geometry.align_vectors([0, 0, 1], axe))
    h.apply_translation(centre)
    return h


def union(*ms):
    return trimesh.boolean.union([m for m in ms if m is not None], engine='manifold')


def moins(a, *ms):
    return trimesh.boolean.difference([a] + [m for m in ms if m is not None], engine='manifold')


# ── fixations ────────────────────────────────────────────────────────────────────────────────────────
# (nom, point sur la face intérieure de la paroi, direction vers l'intérieur, épaisseur de la paroi,
#  face par où glisser l'écrou : vecteur)
PAROIS = [
    ('profilé haut',  (104.0, -16.0, 31.1), (0, 0, -1), 2.5, (-1, 0, 0)),
    ('profilé haut',  (104.0, 16.0, 31.1), (0, 0, -1), 2.5, (-1, 0, 0)),
    ('profilé bas',   (104.0, -16.0, -31.1), (0, 0, 1), 2.5, (-1, 0, 0)),
    ('profilé bas',   (112.0, -16.0, -31.1), (0, 0, 1), 2.5, (1, 0, 0)),
    ('plaque 96 y−',  (104.0, -21.45, 16.0), (0, 1, 0), 2.57, (-1, 0, 0)),
    ('plaque 96 y−',  (104.0, -21.45, -16.0), (0, 1, 0), 2.57, (-1, 0, 0)),
    ('plaque 96 y+',  (104.0, 21.45, 16.0), (0, -1, 0), 2.57, (-1, 0, 0)),
    # La 2e vis côté y+ (x = 112) mordait de 0,8 mm sur le D85MG : la plaque y+ est tenue en plus par
    # deux entretoises de 43 mm qui la relient à la plaque y− (voir ENTRETOISES_43).
]
RETRAIT_ECROU = 2.5                        # matière entre la paroi et l'écrou
TROUS_REBOUCHES = [(-21.3, 21.3, 25.53), (-21.3, -13.6, -26.1), (16.5, 21.3, -26.1)]   # (y0, y1, z) à x = 104
# Colonnes libres entre les deux plaques de 96 mm (fixations.py) : entretoise taraudée de 43 mm et une
# vis M4 × 8 de chaque côté.
ENTRETOISES_43 = [(87.9, 16.0), (87.9, -16.0)]      # (x, z)


def redresser(base):
    """La cloison d'origine vient de bras.stl : 0,11° de roulis autour de x hérité du recalage (ses faces
    latérales penchaient, et tout ce qu'on lui ajoutait d'équerre y faisait des marches de quelques
    centièmes à 0,2 mm). On la remet d'équerre sur sa grande face y+, autour de son centre, et on relève la
    position de ses faces."""
    base = base.copy(); base.merge_vertices()
    o = np.argsort(base.facets_area)[::-1]
    n = next(base.facets_normal[i] for i in o if abs(base.facets_normal[i][1]) > 0.99)
    n = n * np.sign(n[1])
    angle = -np.arctan2(n[2], n[1])                          # roulis autour de x, à annuler
    base.apply_transform(trimesh.transformations.rotation_matrix(angle, [1, 0, 0], base.bounds.mean(0)))
    o = np.argsort(base.facets_area)[::-1]                   # les facettes se recalculent après rotation
    def position(axe, signe, pres=None):
        best = None
        for i in o:
            nn = base.facets_normal[i]
            if nn[axe] * signe < 0.999:
                continue
            v = base.vertices[np.unique(base.faces[base.facets[i]])][:, axe].mean()
            if pres is None or abs(v - pres) < 1.0:
                return float(v)
        return best
    faces = {'y-': position(1, -1), 'y+': position(1, 1), 'z+': position(2, 1, 31.0),
             'épaulement': position(2, 1, 21.4), 'poche y+': position(1, 1, 3.7)}
    print('cloison d\'origine redressée de %.3f° ; faces : %s' % (np.degrees(angle), {k: round(v, 2) for k, v in faces.items()}))
    return base, faces


def combler_cote(piece, axe, signe, plan, profondeur=1.0):
    """Comble les retraits peu profonds (moins de « profondeur ») d'une face latérale jusqu'à son plan :
    le contour des facettes tournées vers l'extérieur, extrudé jusqu'au plan."""
    import shapely.geometry as sg, shapely.ops as so
    u, v = [i for i in range(3) if i != axe]
    polys = []
    for i in range(len(piece.facets)):
        n = piece.facets_normal[i]
        if n[axe] * signe < 0.999:
            continue
        tri = piece.triangles[piece.facets[i]]
        retrait = (plan - tri[:, :, axe].mean()) * signe
        if 0.01 < retrait < profondeur:
            polys += [(sg.Polygon(t[:, [u, v]]), tri[:, :, axe].mean()) for t in tri]
    morceaux = []
    for poly, pos in polys:
        if poly.area < 1e-4:
            continue
        m = trimesh.creation.extrude_polygon(poly, abs(plan - pos) + 0.02)
        M = np.zeros((4, 4)); M[3, 3] = 1
        M[u, 0] = 1; M[v, 1] = 1; M[axe, 2] = signe; M[axe, 3] = pos - signe * 0.01
        if np.linalg.det(M[:3, :3]) < 0:
            m.invert()
        m.apply_transform(M)
        morceaux.append(m)
    return union(*morceaux) if morceaux else None


def combler_retrait(base, x_retrait, x_face):
    """Comble un niveau en retrait de la face avant : le contour de ses facettes, extrudé jusqu'au plan."""
    import shapely.geometry as sg, shapely.ops as so
    polys = []
    for i in range(len(base.facets)):
        n = base.facets_normal[i]
        if n[0] < 0.999:
            continue
        tri = base.triangles[base.facets[i]]
        if abs(tri[:, :, 0].mean() - x_retrait) > 0.1:
            continue
        polys += [sg.Polygon(t[:, 1:]) for t in tri]
    zone = so.unary_union(polys).buffer(0.01)
    morceaux = []
    for g in getattr(zone, 'geoms', [zone]):
        m = trimesh.creation.extrude_polygon(g, x_face - x_retrait + 0.02)   # extrudé selon z local
        m.apply_transform(np.array([[0, 0, 1, x_retrait - 0.01], [1, 0, 0, 0], [0, 1, 0, 0], [0, 0, 0, 1.]]))
        morceaux.append(m)
    return union(*morceaux) if morceaux else None


def logement(p, d, glisse, longueur_fente, plat_devant=False):
    """Trou de vis + logement d'écrou + fente d'introduction vers la face indiquée.

    Par défaut l'écrou glisse un angle devant (fente large de ses plats). plat_devant : il glisse un plat
    devant (fente large de ses angles) ; sert quand deux logements se font face sur la même ligne : angle
    contre angle, leurs poches se touchaient (fente de 0,8 mm sous le profilé bas, 2026-10-04)."""
    p, d, g = np.asarray(p, float), np.asarray(d, float), np.asarray(glisse, float)
    c = p + d * (RETRAIT_ECROU + ECROU_EP / 2)
    tour = 30.0 if plat_devant else 0.0
    trou = cylindre(VIS_D / 2, p - d * 0.5, p + d * (RETRAIT_ECROU + ECROU_EP + 4))
    ecrou = hexagone(c, d, ECROU_PLATS + JEU, ECROU_EP + JEU, tour)
    fente = hexagone(c + g * longueur_fente / 2, d, ECROU_PLATS + JEU, ECROU_EP + JEU, tour)
    largeur = (ECROU_PLATS + JEU) * (2 / np.sqrt(3) if plat_devant else 1.0)
    fente = union(fente, boite(*_boite_fente(c, d, g, longueur_fente, largeur)))
    return union(trou, ecrou, fente), c


def _boite_fente(c, d, g, L, largeur):
    """Boîte qui relie le logement à la face : largeur donnée, hauteur = épaisseur de l'écrou."""
    e = np.abs(d) * (ECROU_EP + JEU) + np.abs(np.cross(d, g)) * largeur + np.abs(g) * L
    m = c + g * L / 2
    return m[0] - e[0] / 2, m[0] + e[0] / 2, m[1] - e[1] / 2, m[1] + e[1] / 2, m[2] - e[2] / 2, m[2] + e[2] / 2


def main():
    os.makedirs(DOSSIER, exist_ok=True)
    T = repere_bras()
    base = trimesh.load(os.path.join(ICI, '..', 'v5', 'cloison_position_assemblage.stl')); base.apply_transform(T)
    base, faces = redresser(base)
    # côté y+, 0,1 mm de moins : l'aile du profilé du bas, qui garde le roulis du recalage, y vient à 21,43
    Y_MOINS, Y_PLUS = faces['y-'], faces['y+'] - 0.1

    # Le tunnel du fil du D85MG au fond (z ≈ −28, de y = −14 jusqu'à la poche du servo) ne sert plus : le fil
    # sort par le passage à l'arrière de la poche (x = 97 à 102) et ressort derrière la cloison. Rebouché en
    # entier (Nicolas, 2026-10-04 ; d'abord jusqu'à y = −7 seulement, pour loger les écrous du profilé bas).
    corps = union(base, boite(X0, X1, Y_MOINS, faces['poche y+'], -29.4, -27.2))
    # Face avant remise à plat (Nicolas, 2026-10-04) : la cloison d'origine y avait un second niveau,
    # 0,8 mm en retrait (x = 113,2) ; on le comble jusqu'au plan de la face (x = 114,0).
    corps = union(corps, combler_retrait(base, 113.2, X1))

    # Bossage sous l'oreille du bas du D85MG (z = −22,1) : le passage du fil de la v5 évidait toute
    # l'épaisseur de la cloison, l'oreille n'avait rien où se visser. Le fil sort par le bas, à l'arrière
    # du servo (embout à x ≈ 97–100, Nicolas le 2026-10-03) : on garde le passage sur x = 97–102 et on
    # remplit le reste, en épousant le servo.
    d85 = trimesh.load(os.path.join(ICI, '..', 'v5', 'REF_servo_rose_position_assemblage.stl')); d85.apply_transform(T)
    bossage = moins(boite(102.0, X1, faces['poche y+'], 16.5, -31.0, -18.3), d85)
    corps = union(corps, bossage)
    # Bras d'appui sur le palier, aux coins y = −16, z = ±16, hors du rayon de ce qui tourne (moyeu)
    bras = []
    for z in (16.0, -16.0):
        # dessus au ras de l'épaulement de la cloison (|z| = 21,4), et non 0,4 mm plus bas
        section = boite(X1 - 0.5, X_PALIER, Y_MOINS, -11.0, *sorted((np.sign(z) * 11.0, np.sign(z) * faces['épaulement'])))
        bras.append(moins(section, cylindre(18.3, (X1 - 1, 0, 0), (X_PALIER + 1, 0, 0), sections=96)))
    corps = union(corps, *bras)
    # Trous de Ø4 hérités de la cloison d'origine, à x = 104 en face des trous des ailes des profilés :
    # plus aucune vis n'y passe (la cloison tient par le fond des profilés et par les plaques), ils restaient
    # « au milieu de nulle part » (Nicolas, 2026-10-04). Rebouchés ; les logements d'écrous sont recreusés après.
    for y0, y1, z in TROUS_REBOUCHES:
        # un peu plus longs que le trou : l'arasement final les coupe au ras des faces (plus de cuvette)
        corps = union(corps, cylindre(2.15, (104.0, y0 - (0.5 if y0 < -21 else 0), z), (104.0, y1 + (0.5 if y1 > 21 else 0), z), sections=24))

    # Logements et visserie
    vis = []
    vides = []
    for nom, p, d, ep, g in PAROIS:
        L = 9.0
        v, c = logement(p, d, g, L, plat_devant=(nom == 'profilé bas'))
        vides.append(v)
        p = np.asarray(p, float); d = np.asarray(d, float)
        vis.append(dict(paroi=nom, sku='2802-0004-0012', tete=(p - d * (ep + 2.2)).round(2).tolist(),
                        direction=d.tolist(), ecrou='2812-0004-0007', centre_ecrou=c.round(2).tolist()))
    # Avant-trou de la vis d'oreille du bas du D85MG (M2 autotaraudeuse fournie avec le servo)
    vides.append(cylindre(1.25, (105.9, 10.63, -22.14), (X1 + 0.5, 10.63, -22.14), sections=24))
    # Bras : vis par les trous d'angle du palier, tête dans le bras, écrou côté avant du palier.
    # La clé passe par un alésage dans l'axe, depuis la face arrière de la cloison.
    for z in (16.0, -16.0):
        epaulement = np.array([X_PALIER - 3.0, -16.0, z])      # 3 mm de plastique serrés contre le palier
        vides.append(union(cylindre(VIS_D / 2, epaulement, (X_PALIER + 1, -16.0, z)),
                           cylindre(TETE_D / 2, (X0 - 1, -16.0, z), epaulement)))
        tete = epaulement - np.array([2.2, 0, 0])               # dessus de la tête bombée
        vis.append(dict(paroi='palier 1604 avancé (bras)', sku='2802-0004-0016', tete=tete.round(2).tolist(),
                        direction=[1, 0, 0], ecrou='2812-0004-0007', centre_ecrou=[X_PALIER + 7.1 + ECROU_EP / 2, -16.0, z]))
    # Les profilés ont un congé intérieur entre fond et aile : la cloison v5, à arêtes vives, y mordait
    # de 0,6 mm. Chanfrein de 1,5 mm sur les quatre arêtes qui longent les profilés.
    for sy in (1, -1):
        for sz in (1, -1):
            coin = trimesh.creation.box(extents=(X1 - X0 + 40, 1.5 * np.sqrt(2), 1.5 * np.sqrt(2)))
            coin.apply_transform(trimesh.transformations.rotation_matrix(np.pi / 4, [1, 0, 0]))
            coin.apply_translation(((X0 + X1) / 2, sy * 21.45, sz * 31.1))
            vides.append(coin)
    # La cloison v5 descendait à z = −31,64 sous le fond du profilé bas (−31,11) : rabotée à −31,0.
    vides.append(boite(X0 - 30, X_PALIER + 10, -40, 40, -45, -31.0))
    piece = moins(corps, *vides)
    # Côtés plats (Nicolas, 2026-10-04) : les petits retraits (< 1 mm) des faces latérales de la cloison
    # d'origine sont comblés jusqu'au plan, et tout ce qui dépasse des plans est arasé.
    for axe, signe, plan in ((1, -1, Y_MOINS), (1, 1, Y_PLUS)):
        piece = union(piece, combler_cote(piece, axe, signe, plan))
    # Le D85MG entre par le côté y+ (notice.py, 2026-10-04) : par l'avant, la sortie de son câble, qui dépasse
    # de 6 mm sous le boîtier juste sous la fente de l'oreille du bas, butait dans le bossage. Une encoche
    # comme celle du cadre goBILDA aurait coupé l'avant-trou de cette vis, et Nicolas veut ses deux vis dans
    # la matière. Glissé de côté, le servo ne passe sur aucun des deux avant-trous. On creuse son chemin,
    # avec 0,3 mm de jeu (pas de rebord : il barrerait la sortie du câble).
    chemin = [d85.copy().apply_translation((dx, dy, dz)) for dy in np.arange(0.0, 8.01, 0.5)
              for dx in (-0.3, 0.3) for dz in (-0.3, 0.3)]
    piece = moins(piece, union(*chemin))
    # dessus et dessous à ±30,95 : le fond des profilés, qui garde le roulis du recalage, descend à 30,97 ;
    # face arrière à 97,21 : la cloison d'origine y avait deux niveaux à 0,01 mm l'un de l'autre.
    piece = trimesh.boolean.intersection([piece, boite(X0 + 0.01, X_PALIER + 1, Y_MOINS, Y_PLUS, -30.95, 30.95)],
                                         engine='manifold')
    for x, z in ENTRETOISES_43:
        for y, d in ((-24.04 - 2.2, 1.0), (24.05 + 2.2, -1.0)):
            vis.append(dict(paroi='plaque 96 ↔ plaque 96 (entretoise 43 mm)', sku='2802-0004-0008', tete=[x, y, z],
                            direction=[0, d, 0], ecrou=None))
        vis.append(dict(paroi='plaque 96 ↔ plaque 96', sku=None, entretoise='1501-0006-0430', centre_entretoise=[x, 0.0, z],
                        direction=[0, 1, 0]))
    vis = [v for v in vis if v.get('sku') or v.get('entretoise')]
    print('cloison : étanche', piece.is_watertight, ' volume %.1f cm³' % (piece.volume / 1000),
          ' bornes', np.round(piece.bounds, 1).tolist())
    piece.export(os.path.join(DOSSIER, 'cloison_structurelle_position.stl'))
    json.dump(vis, open(os.path.join(DOSSIER, 'visserie.json'), 'w'), ensure_ascii=False, indent=1)
    controler(piece)


def controler(piece, exclure=('cloison',), tourne=False):
    """Collisions avec tout ce qui reste dans le bras (v5 comprise, ancienne cloison exclue).

    Économe en mémoire : le 2026-10-03, un contrôle par distance signée sur toutes les pièces a pris
    9,6 Go et fait tomber la session. On ne charge que les modèles du catalogue (pas bras.stl), on ne
    garde que les voisins proches, et on teste « dedans / dehors » par paquets.
    """
    from recaler import modele
    a = json.load(open(os.path.join(SORTIE, 'assemblage.json')))
    v5 = json.load(open(os.path.join(SORTIE, 'v5.json')))
    masquees = {n for l in v5['masquer'].values() for n in l}
    lo, hi = piece.bounds
    proche = lambda m: np.all(m.bounds[0] < hi + 1) and np.all(m.bounds[1] > lo - 1)
    voisins = {}
    for u in a['unites']:
        if u['nom'] in masquees or u['sku'].startswith(('IMPRIME:', 'BRAS:')) or u['nom'].startswith(tuple(exclure)):
            continue
        m = modele(u['source'], u['filtre']); m.apply_transform(np.array(u['matrice']))
        if proche(m):
            voisins[u['nom']] = m
    s5 = trimesh.load(os.path.join(SORTIE, 'bras-v5.glb'), force='scene')
    for k, p in enumerate(v5['pieces']):
        if p['nom'].startswith(tuple(exclure)) or p['nature'] == 'visserie ajoutée' and 'cloison' in exclure:
            continue
        Tn, g = s5.graph['v5-%d' % k]; m = s5.geometry[g].copy(); m.apply_transform(Tn)
        if proche(m):
            voisins['v5 ' + p['nom']] = m
    del s5
    # Les maillages des STEP goBILDA ne sont pas fermés : pas de booléen possible. On regarde quels
    # points de leur surface tombent dans la cloison (qui, elle, est fermée), et à quelle profondeur.
    print('pénétrations (au-delà de 0,05 mm) :')
    rien = True
    angles = range(0, 360, 30) if tourne else [0]
    for n, m in voisins.items():
      for a_deg in angles:
        # une pièce qui tourne autour de l'axe du tube : on fait tourner les voisins en sens inverse
        if a_deg:
            m = m.copy(); m.apply_transform(trimesh.transformations.rotation_matrix(-math.radians(30), [1, 0, 0]))
        pts, _ = trimesh.sample.sample_surface(m, 8000, seed=3)
        pts = pts[np.all((pts > lo - 0.1) & (pts < hi + 0.1), axis=1)]
        dedans = np.zeros(len(pts), bool)
        for k in range(0, len(pts), 300):            # par petits paquets : le lancer de rayons coûte cher
            dedans[k:k + 300] = piece.contains(pts[k:k + 300])
        if not dedans.any():
            continue
        _, d, _ = trimesh.proximity.closest_point(piece, pts[dedans])
        if d.max() > 0.05:
            rien = False
            c = pts[dedans][np.argmax(d)]
            print('   %-45s %3d°  jusqu\'à %.2f mm, vers (%.1f, %.1f, %.1f)' % (n, a_deg, d.max(), *c))
    if rien:
        print('   aucune')


if __name__ == '__main__':
    if sys.argv[1:] == ['controle']:
        controler(trimesh.load(os.path.join(DOSSIER, 'cloison_structurelle_position.stl')))
    elif sys.argv[1:] == ['controle-liaison']:
        # ce qui tourne avec la liaison n'est pas un obstacle : pignon, micro-servo, goTUBE du bras
        controler(trimesh.load(os.path.join(SORTIE, 'liaison', 'liaison_moyeu_position.stl')),
                  exclure=('liaison', 'moyeu', 'pignon Slip-Fit', 'HS-65MG', 'vis vis M2 du HS-65MG', '4103-0032-0096'), tourne=True)
    else:
        main()
