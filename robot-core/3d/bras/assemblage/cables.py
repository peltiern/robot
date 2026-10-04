#!/usr/bin/env python3
"""
Les câbles des trois servos (3 fils chacun), de leur sortie jusqu'au corps, par l'intérieur du bras.

Passage clé trouvé le 2026-10-03 (repéré par Nicolas, confirmé par voxels) : l'échancrure du moyeu 1311
côté corps laisse découverte une fenêtre de 6,7 × 16,9 mm sur les canaux ±x du goTUBE de flexion ; une
prise de servo (7,85 × 2,42 × 15 mm, standard JR/Futaba/TJC8) y passe. Les canaux ±z, eux, sont bouchés.

Le fil du HS-65MG tourne avec le tube : il passe dans l'axe (embout REX, Ø4,5) sans sa prise, qui est
sertie après passage (option A retenue par Nicolas).

Chaque câble est une suite de points ; on vérifie le jeu avec toutes les pièces (sauf son propre servo et,
pour le HS-65MG, ce qui tourne avec lui), on mesure les longueurs, et on dessine fils et prises.
"""
import json, os, sys
import numpy as np, trimesh
from scipy.spatial import cKDTree

ICI = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, ICI)
from recaler import SORTIE

PRISE = (7.85, 2.42, 15.0)        # prise de servo (contacts femelles), mesurée : mitat.tuu.fi
# Chaque câble est une nappe de 3 fils (masse, +, signal). Section d'après les fiches (2026-10-04) :
# 28 AWG pour les deux Hitec, 22 AWG pour le goBILDA 2000 ; diamètre gainé courant, à confirmer au pied à
# coulisse : 1,0 et 1,5 mm. Jusqu'au 2026-10-04, un cordon rond de Ø2 pour tous : la nappe du 2000 fait
# 4,5 mm de large, et les jeux de ce câble étaient trop optimistes.
FIL = {'D85MG': 1.0, 'HS-65MG': 1.0, "servo d'abduction": 1.5}
# Dans les passages communs, les nappes du D85MG et du HS-65MG sont empilées (3,0 × 2,2 mm, D85MG dessus) au
# lieu de se superposer. Avec la nappe du 2000 en plus (6,3 × 2,85), le faisceau mordait de 0,3 mm sur le bloc
# 1201 et la vis du moyeu à la sortie de la lumière : le 2000 passe donc par le côté −x (plan B, 2026-10-04) ;
# côte à côte, les deux Hitec mordaient encore de 0,1 mm, empilées elles passent au large.
# (décalage en largeur, décalage en hauteur), en mm, dans le repère de la nappe
DISPOSITION = {'D85MG': (0.0, 0.6), 'HS-65MG': (0.0, -0.6), "servo d'abduction": (0.0, 0.0)}
# Le fil d'origine du HS-65MG ne fait que 160 mm pour 259 mm de chemin : il est coupé et soudé à une nappe
# 28 AWG plus longue, la soudure sous gaine logée dans le couloir au-dessus du cadre 1802 (assez de place).
SOUDURE_HS65 = dict(x=(68.0, 86.0), section=(4.2, 2.2), decalage_z=-0.6)

# Tronc commun, du dessous du servo d'abduction jusqu'au corps : chemin calculé dans les voxels de l'épaule,
# puis le canal +x du goTUBE de flexion (centre à x = 11,2, z = 0), jusqu'au bout du tube (y = −80) et au-delà.
# Tronc commun : on remonte hors de la couronne du bas, on rentre entre les deux couronnes, puis on file
# dans la lumière de 8,7 × 4,1 du bloc d'épaule 1201 (x = 13,6, z = 0), dans le prolongement du canal +x
# du goTUBE — passage proposé par Nicolas le 2026-10-03, plus direct que le contournement du bloc.
TRONC = [(28, -9, -26), (29.5, -12.5, -22), (30, -13, -17), (27, -11, -14), (25, -8, -11), (22, -6, -8),
         (18, -7, -4), (15, -10, -0.5), (14.5, -14, 0), (14, -20, 0), (14.0, -23.5, 0), (15.6, -25.0, 0), (16.2, -26.5, 0), (15.9, -28.0, 0), (14.7, -29.6, 0),
         (13.73, -31.24, 0), (12.2, -33.8, 0), (11.5, -36, 0), (11.5, -40, 0), (11.5, -80, 0), (11.5, -88, 0)]
# Recentrés le 2026-10-04 pour trois nappes (carte d'occupation au pas de 0,5 mm) : lumière du bloc 1201
# libre de x 10 à 18 ; puis le moyeu 1311 et sa vis de serrage occupent tout jusqu'à x 12,5 (y −24 à −31) ;
# puis le canal du goTUBE, une lentille libre de x 7,5 à 15,5 sur ±5 mm de haut. Le faisceau traverse la
# lumière à plat, se met sur la tranche en longeant le moyeu (la place ne manque pas en z), puis passe entre
# le moyeu et le bout du goTUBE : 3,5 mm d'ouverture mesurés, au milieu (13,7 ; −31,2), à traverser dans
# l'axe (−0,51 ; −0,86). Sur la tranche, le faisceau y fait 2,75 mm ; à plat, il n'y entrait pas.
# Le faisceau pivote de 90° (à plat → sur la tranche) entre y −23,6 et −29,6, par étapes : d'un coup, d'un
# point au suivant, les fils des nappes se croisaient au milieu du segment.
PIVOT = (-23.6, -29.6)
# Vers l'avant-bras, les câbles passent AU-DESSUS du cadre 1802 du servo d'abduction (Nicolas, 2026-10-03 :
# au fond du caisson ils passaient à 5 mm du pignon laiton, qui tourne avec le servo). Couloir entre le
# servo (y ±10,3) et la plaque y− (−21,4), à z ≈ 9 : au-dessus du cadre (z 3) et sous les entretoises de
# 43 mm (z 13–19 à x = 85–91). Derrière la cloison, ils montent à x ≈ 93,5, entre cadre et cloison ; devant
# le cadre (x < 26,7), ils redescendent rejoindre le tronc entre les couronnes.
DESSUS = [(91.0, -16.0, 9.0), (60.0, -16.0, 9.0), (32.0, -16.0, 9.0), (27.5, -12.0, 7.0), (25.5, -7.0, 3.0),
          (22.0, -6.0, -3.0)]

CABLES = {
    'D85MG': dict(servo='D85MG', points=[
        (98.5, 10.5, -24.5), (99.5, 10.5, -28.5), (95.0, 10.0, -28.5), (93.5, 8.0, -20.0), (93.5, 0.0, 6.0),
        (93.0, -10.0, 9.5)] + DESSUS + TRONC[6:],
        prise_au_bout=True),
    'HS-65MG': dict(servo='HS-65MG', tourne=('liaison', 'HS-65MG', 'pignon Slip-Fit', '4103-0032-0096'), points=[
        (155.0, -13.5, -10.0), (145.0, -13.5, -10.0), (143.0, -5.0, -3.0), (142.0, 0.0, 0.0), (125.0, 0.0, 0.0),
        (116.0, -2.0, 0.0), (97.0, -2.5, 0.0), (94.0, -4.0, 2.0), (93.0, -15.0, 7.0)] + DESSUS + TRONC[6:],
        prise_au_bout=False),
    # Descend hors du rayon de la couronne du haut (r > 29,6), passe devant le cadre 1802 (x < 26,7) et
    # rejoint le tronc entre les couronnes.
    # Par l'autre côté (carte d'occupation au pas de 1 mm) : à mi-hauteur, l'intérieur de l'épaule est libre
    # entre les couronnes, de x −21 à +27 ; on le traverse, puis le chemin est le miroir de celui des Hitec :
    # lumière −x du bloc 1201 (x −17 → −10), contour du moyeu, canal −x du goTUBE de flexion. L'arbre REX de
    # flexion (Ø8, axe y) occupant le milieu, la traversée passe au-dessus, à z = +6.
    'servo d\'abduction': dict(servo='2000-0025-0002', points=[
        (33.0, 0.0, 19.3), (31.5, -2.0, 14.0), (29.0, -4.0, 8.0), (24.0, -3.0, 4.0), (16.0, -3.5, 6.0),
        (6.0, -5.0, 6.0), (-6.0, -7.0, 6.0), (-11.5, -9.5, 3.0), (-13.5, -12.0, 0.0)] + [(-x, y, z) for x, y, z in TRONC[9:]], prise_au_bout=True),
}


def longueur(pts):
    p = np.asarray(pts, float)
    return float(np.linalg.norm(np.diff(p, axis=0), axis=1).sum())


COMMUNS = {tuple(map(float, p)) for p in DESSUS + TRONC}


def reperes(p, w_prec=None):
    """(w, h) en chaque point d'une polyligne : w la largeur de la nappe, à plat (perpendiculaire à z)
    tant que le câble n'est pas vertical, h l'épaisseur."""
    out = []
    for i in range(len(p)):
        t = p[min(i + 1, len(p) - 1)] - p[max(i - 1, 0)]; t = t / np.linalg.norm(t)
        w = np.cross(t, [0, 0, 1.0])
        if np.linalg.norm(w) < 0.35 and w_prec is not None:
            w = w_prec - np.dot(w_prec, t) * t
        w = w / np.linalg.norm(w)
        if w_prec is not None and np.dot(w, w_prec) < 0:
            w = -w
        out.append((w, np.cross(w, t))); w_prec = w
    return out


# Le repère des passages communs se calcule sur le chemin commun lui-même, pas câble par câble : sinon deux
# nappes qui arrivent sous des angles différents ne se rangent pas dans le même sens et se chevauchent.
_CHEMIN = np.asarray(DESSUS + TRONC[6:], float)
REPERE_COMMUN = {}
for _q, (_w, _h) in zip(_CHEMIN, reperes(_CHEMIN)):
    _a = np.pi / 2 * float(np.clip((PIVOT[0] - _q[1]) / (PIVOT[0] - PIVOT[1]), 0, 1))
    REPERE_COMMUN[tuple(map(float, _q))] = (np.cos(_a) * _w + np.sin(_a) * _h, np.cos(_a) * _h - np.sin(_a) * _w)


def fils(nom):
    """Les 3 fils d'un câble (tableaux de points), et l'axe de sa nappe. Dans les passages communs, la nappe
    est décalée selon DISPOSITION, dans le repère du chemin commun."""
    p = np.asarray(CABLES[nom]['points'], float)
    d = FIL[nom]
    a, b = DISPOSITION[nom]
    centres, largeurs = [], []
    for q, (w, h) in zip(p, reperes(p)):
        cle = tuple(map(float, q))
        if cle in REPERE_COMMUN:
            w, h = REPERE_COMMUN[cle]
            q = q + a * w + b * h
        elif abs(q[0]) < 25:
            # même pivot que le faisceau commun : sur la tranche pour passer entre moyeu et goTUBE
            r = np.pi / 2 * float(np.clip((PIVOT[0] - q[1]) / (PIVOT[0] - PIVOT[1]), 0, 1))
            w, h = np.cos(r) * w + np.sin(r) * h, np.cos(r) * h - np.sin(r) * w
        centres.append(q); largeurs.append(w)
    centres, largeurs = np.array(centres), np.array(largeurs)
    return [centres + (k - 1) * d * largeurs for k in range(3)], centres


def cordon(pts, r):
    morceaux = []
    p = np.asarray(pts, float)
    for a, b in zip(p[:-1], p[1:]):
        morceaux.append(trimesh.creation.cylinder(radius=r, segment=[a, b], sections=12))
    for a in p[1:-1]:
        s = trimesh.creation.icosphere(subdivisions=1, radius=r); s.apply_translation(a); morceaux.append(s)
    return trimesh.util.concatenate(morceaux)


def prise(bout, avant):
    """Boîtier de prise au bout du câble, dans l'axe du dernier segment."""
    d = np.asarray(bout, float) - np.asarray(avant, float); d /= np.linalg.norm(d)
    b = trimesh.creation.box(extents=(PRISE[0], PRISE[1], PRISE[2]))
    b.apply_transform(trimesh.geometry.align_vectors([0, 0, 1], d))
    b.apply_translation(np.asarray(bout, float) + d * PRISE[2] / 2)
    return b


def soudure_hs65():
    """Gaine de la soudure du HS-65MG, sur sa nappe, dans le couloir au-dessus du cadre."""
    _, c = fils('HS-65MG')
    x0, x1 = SOUDURE_HS65['x']
    # le couloir est droit de x 91 à 60 ; la gaine, plus épaisse que la nappe, déborde vers le bas, à l'opposé
    # du D85MG empilé dessus
    y, z = c[[i for i, q in enumerate(CABLES['HS-65MG']['points']) if tuple(map(float, q)) == DESSUS[1]][0]][1:]
    z += SOUDURE_HS65['decalage_z']
    g = trimesh.creation.box(extents=(x1 - x0, SOUDURE_HS65['section'][0], SOUDURE_HS65['section'][1]))
    g.apply_translation(((x0 + x1) / 2, y, z))
    return g


def pieces_cables():
    """(nom, maillage, référence) des câbles et des prises, pour le calque v5."""
    out = []
    for nom, c in CABLES.items():
        f, centres = fils(nom)
        out.append(('câble du %s : 3 fils de Ø%.1f (%.0f mm dans le bras)' % (nom, FIL[nom], longueur(centres)),
                    trimesh.util.concatenate([cordon(x, FIL[nom] / 2) for x in f]), 'CABLE:câble du %s' % nom))
        if c['prise_au_bout']:
            out.append(('prise du câble du %s' % nom, prise(centres[-1], centres[-2]), 'CABLE:prise de servo'))
        else:
            out.append(('bout du câble du %s : prise à sertir après passage' % nom,
                        trimesh.creation.icosphere(radius=1.8, subdivisions=1).apply_translation(centres[-1]),
                        'CABLE:prise à sertir'))
    out.append(('soudure du fil du HS-65MG sous gaine (rallonge 28 AWG)', soudure_hs65(), 'CABLE:soudure sous gaine'))
    return out


def echantillons(f, pas=0.5):
    return np.concatenate([a + np.outer(np.linspace(0, 1, max(2, int(np.linalg.norm(b - a) / pas))), b - a)
                           for a, b in zip(f[:-1], f[1:])])


def controler():
    """Jeu entre chaque fil et les pièces du bras (au repos), puis entre les fils de câbles différents."""
    from audit import pieces_nouveau_bras
    P = pieces_nouveau_bras()
    bilan, tous = {}, {}
    nuages = {}
    for nom, c in CABLES.items():
        exclus = (c['servo'],) + c.get('tourne', ())
        f, centres = fils(nom)
        r = FIL[nom] / 2
        pts = np.concatenate([echantillons(x) for x in f])
        tous[nom] = pts
        pire = []
        for k, (m, ref, role) in P.items():
            if any(e in k or e == ref for e in exclus) or ref.startswith('CABLE:'):
                continue
            lo, hi = m.bounds
            sel = np.all((pts > lo - 4) & (pts < hi + 4), axis=1)
            if not sel.any():
                continue
            if k not in nuages:
                nuages[k] = cKDTree(trimesh.sample.sample_surface(m, max(3000, int(m.area * 3)), seed=5)[0])
            d, _ = nuages[k].query(pts[sel])
            j = int(d.argmin())
            pire.append((float(d[j]) - r, k, pts[sel][j]))
        pire.sort(key=lambda t: t[0])
        bilan[nom] = dict(longueur=round(longueur(centres)), jeu=[(round(j, 2), k, np.round(q, 1).tolist()) for j, k, q in pire[:5]])
        print('%-20s %4.0f mm, fils Ø%.1f ; jeux les plus faibles : %s' % (nom, longueur(centres), FIL[nom],
              ' ; '.join('%s %.1f mm à (%.0f, %.0f, %.0f)' % (k[:28], j, *q) for j, k, q in pire[:4])), flush=True)
    noms = list(tous)
    for i, a in enumerate(noms):
        for b in noms[i + 1:]:
            d, j = cKDTree(tous[b]).query(tous[a])
            k = int(d.argmin())
            print('entre %-18s et %-18s : %.2f mm (en %s)' % (a, b, d[k] - FIL[a] / 2 - FIL[b] / 2,
                                                            np.round(tous[a][k], 1).tolist()))
    g = soudure_hs65()
    for k, (m, ref, role) in P.items():
        if ref.startswith('CABLE:') or np.any(g.bounds[0] > m.bounds[1] + 3) or np.any(m.bounds[0] > g.bounds[1] + 3):
            continue
        d, _ = cKDTree(trimesh.sample.sample_surface(m, max(3000, int(m.area * 3)), seed=5)[0]).query(
            trimesh.sample.sample_surface(g, 3000, seed=6)[0])
        print('soudure du HS-65MG ↔ %-40s %.2f mm' % (k[:40], d.min()))
    d, _ = cKDTree(tous['D85MG']).query(trimesh.sample.sample_surface(g, 3000, seed=6)[0])
    print('soudure du HS-65MG ↔ fils du D85MG %.2f mm' % (d.min() - FIL['D85MG'] / 2))
    json.dump(bilan, open(os.path.join(SORTIE, 'cables.json'), 'w'), ensure_ascii=False, indent=1)


if __name__ == '__main__':
    controler()
