#!/usr/bin/env python3
"""
Le robot complet animé : chaque pièce de robot-complet-leger.glb (robot_complet.py) est rangée dans l'ensemble
mobile qui la porte, et la cinématique est écrite à côté pour la visionneuse robot-complet.html.

  - cou et tête : la cinématique relevée le 2026-09-13 (mesures/cotes-robot.json, releve_robot.py), validée sur
    le robot. Le nouvel export reprend les mêmes pièces au même endroit (chariot, barres, panoramique, tête à
    1 mm près) : chaque pièce va dans le groupe de l'ancien modèle (robot-walle.glb) dont elle est la plus proche.
    La pose de l'export est supposée la même qu'en septembre (le parallélogramme est à la même place au
    centième ; le panoramique et l'inclinaison ne se mesurent pas assez bien sur la tête pour la vérifier) ;
  - yeux : le mécanisme est nouveau. Chaque œil tourne sur l'arbre de 8 mm et porte son servo goBILDA, dont le
    palonnier tire par une tringle à rotules sur un point fixe de la tête. Rien n'est encore validé sur le robot :
    la tringlerie est relevée dans le modèle, l'angle de l'œil se compte comme dans robot-walle.html (0 : à plat) ;
  - bras : le nouveau bras, avec les ensembles d'animation.json (flexion, abduction, tube, pince).

Repère de la maquette (celui de robot-walle.glb) : x droite du robot, y haut, z arrière, origine au sol dans le
plan de symétrie, mm.

Écrit robot_complet/robot-anime.glb et robot_complet/robot-anime.json sur le disque externe.
"""
import json, os, re, sys
import numpy as np, trimesh
from scipy.spatial import cKDTree

ICI = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, ICI)
from recaler import SORTIE
import robot_complet as rc

RACINE_3D = os.path.join(ICI, '..', '..')
ANCIEN = os.path.join(RACINE_3D, 'robot-walle.glb')
COTES = os.path.join(RACINE_3D, 'mesures', 'cotes-robot.json')
CENTRE = np.array([24.7, 29.4, -108.06])
# export → maquette : x = −x, y = z, z = y, après avoir retiré le centre
VERS_MAQUETTE = np.array([[-1, 0, 0, CENTRE[0]], [0, 0, 1, -CENTRE[2]], [0, 1, 0, -CENTRE[1]], [0, 0, 0, 1.0]])
GROUPES_COU = ('chariot', 'panoramique', 'tete', 'pignon_inclinaison', 'barre_avant', 'barre_milieu',
               'barre_arriere', 'palonnier_monter')
# La tige du monter du CAD n'est pas celle du robot (voir robot-walle.html) : la visionneuse dessine la vraie.
TIGE_CAD = ('2808-0004-0070', '2913-0004-0241')
VISSERIE = re.compile(r'^(28\d\d-|15\d\d-|Spacer)')


def t3(p):
    return trimesh.transform_points(np.atleast_2d(p), VERS_MAQUETTE)


def anciens_groupes(C):
    """Les groupes du cou et de la tête de robot-walle.glb, ramenés à la pose de l'export (maquette)."""
    s = trimesh.load(ANCIEN); g = s.graph
    R = lambda ax, a, c: trimesh.transformations.rotation_matrix(np.radians(a), ax, c)
    e = C['export']; PT = C['pivots']['tete']
    pose = {'panoramique': R([0, 1, 0], -e['panoramique'], PT)}
    pose['tete'] = pose['panoramique'] @ R([1, 0, 0], e['inclinaison'], PT)
    pose['pignon_inclinaison'] = pose['tete'] @ R([1, 0, 0], C['pignon']['rapport'] * e['inclinaison'],
                                                  C['pivots']['pignon_inclinaison'])
    out = {}
    for n in GROUPES_COU + ('caisse',):
        T, gg = g[n]
        out[n] = cKDTree(trimesh.transform_points(s.geometry[gg].vertices, pose.get(n, np.eye(4)) @ T))
    return out


def ranger(pieces, C):
    """{nom: groupe} pour tout ce qui n'est pas le nouveau bras."""
    kd = anciens_groupes(C)
    groupe = {}
    cote = lambda m: 'droit' if t3(m.bounds.mean(0))[0, 0] > 6.6 else 'gauche'
    attente = []
    for p in pieces:
        n, c, m = p['nom'], p['chaine'], p['maillage']
        if len(c) > 1 and c[1] == 'Wall-E GoBilda Tracks <1>':
            groupe[n] = 'chenilles'
        elif len(c) > 1 and c[1] == 'Wall-E Neck Gearbox <1>':
            if n.startswith(TIGE_CAD) and t3(m.bounds.mean(0))[0, 1] < 450:
                groupe[n] = 'tige_cad'
                continue
            q = t3(trimesh.sample.sample_surface(m, 150, seed=1)[0])
            d = {k: np.median(t.query(q)[0]) for k, t in kd.items()}
            groupe[n] = min(d, key=d.get)
        elif len(c) > 1 and c[1] == 'Wall-E Eye <1>':
            # Chaque servo est monté sur son œil (cadre 1801 au même angle que l'œil) : il tourne avec lui et tire,
            # par son palonnier et la tringle, sur une rotule vissée dans la tête (Nicolas, 2026-10-07).
            servo = [x for x in c if x.startswith('Servo-motor-with-arm')]
            if n.startswith('2913-'):
                if 'housing' in n:
                    groupe[n] = 'tringle_oeil_' + cote(m)          # vissé au bout de la tringle
                else:
                    groupe[n] = ('palonnier_oeil_' if servo else 'tete') + ('' if not servo else cote(m))
            elif servo:
                gros = n.startswith('2000-0025-0002') and max(m.extents) > 20
                groupe[n] = 'oeil_' + cote(m) if gros else 'palonnier_oeil_' + cote(m)
            elif n.startswith('2808-0004-0060'):
                groupe[n] = 'tringle_oeil_' + cote(m)
            elif n.startswith(('Support_Tilt', '4100-', '1600-0516', '1605-0024', 'Spacer')):
                groupe[n] = 'tete'
            elif VISSERIE.match(n):
                attente.append(p)
            else:
                groupe[n] = 'oeil_' + cote(m)
        elif any(b in c for b in rc.ANCIENS_BRAS):
            # la flexion côté corps de Nicolas, gardée : immobile comme la caisse, mais à part, car le nouveau bras la
            # traverse encore (goTUBE de flexion dans le support 1222 : côté corps pas encore conçu)
            groupe[n] = 'flexion_corps'
        else:
            groupe[n] = 'caisse'
    # La visserie et les rotules des yeux suivent la pièce la plus proche (déjà rangée) qu'elles tiennent.
    rangees = [p for p in pieces if groupe.get(p['nom'], '').startswith(('oeil_', 'palonnier_oeil_', 'tringle_oeil_'))
               or (groupe.get(p['nom']) == 'tete' and 'Wall-E Eye <1>' in p['chaine'])]
    nuages = [(groupe[p['nom']], cKDTree(trimesh.sample.sample_surface(p['maillage'], 400, seed=2)[0])) for p in rangees]
    for p in attente:
        q = p['maillage'].vertices.mean(0)[None]
        groupe[p['nom']] = min(nuages, key=lambda gk: gk[1].query(q)[0][0])[0]
    return groupe


def tringlerie(pieces, groupe):
    """Tringlerie de chaque œil, dans le plan de la maquette (x, y) : axe de l'œil O ; axe du servo S, sur l'œil ;
    rotule du palonnier A ; rotule fixe F, vissée dans la tête ; et l'angle de l'œil dans l'export."""
    arbre = next(p['maillage'] for p in pieces if p['nom'].startswith('4100-0008-0100'))
    O = t3(arbre.bounds.mean(0))[0]
    out = {}
    for cote, s in (('droit', 1), ('gauche', -1)):
        du_servo = [p for p in pieces if groupe.get(p['nom']) == 'palonnier_oeil_' + cote]
        S = t3(next(p['maillage'] for p in du_servo if p['nom'].startswith('2000-0025-0002')).bounds.mean(0))[0]
        A = t3(next(p['maillage'] for p in du_servo if p['nom'].startswith('2913-0004-0241 ball')).bounds.mean(0))[0]
        boules = [t3(p['maillage'].bounds.mean(0))[0] for p in pieces
                  if p['nom'].startswith('2913-0004-0241 ball') and groupe.get(p['nom']) == 'tete'
                  and ('droit' if t3(p['maillage'].bounds.mean(0))[0, 0] > 6.6 else 'gauche') == cote]
        F = boules[0]
        # Angle de l'œil dans l'export : sa plaque arrière, à plat quand l'œil est à 0 (même convention que
        # robot-walle.html : positif, bord extérieur en haut).
        plaque = next(p['maillage'] for p in pieces if p['nom'] == ('Droit' if cote == 'droit' else 'Gauche') + '-Plaque-Arrière')
        mq = plaque.copy(); mq.apply_transform(VERS_MAQUETTE)
        nr, a = mq.face_normals, mq.area_faces; sel = np.abs(nr[:, 2]) < 0.3
        h, e = np.histogram(np.degrees(np.arctan2(nr[sel, 1], nr[sel, 0])) % 180, bins=1800, range=(0, 180), weights=a[sel])
        export = (e[np.argmax(h)] + 0.05 - 90.0) / s
        out[cote] = dict(O=O.round(3).tolist(), S=S.round(3).tolist(), A=A.round(3).tolist(), F=F.round(3).tolist(),
                         export=round(float(export), 2),
                         palonnier=round(float(np.linalg.norm((A - S)[:2])), 2),
                         tringle=round(float(np.linalg.norm((F - A)[:2])), 2))
    return out


# ── les chenilles ─────────────────────────────────────────────────────────────────────────────────────
# De chaque côté : motoréducteur 5203 (axe dans le sens de la marche) → pignon conique de 14 dents (2306) →
# couronne conique de 28 dents (2307) sur l'axe arrière → deux roues dentées 2401 qui entraînent les maillons.
RENVOI = 28 / 14


def enrouler(dense, c, R, approche=40.0):
    """Le chemin fermé (points) passe autour du cercle (c, R) : là où il s'en approche à moins de 3 mm, il suit
    exactement le cercle, entre les points où le cercle prend la direction du chemin qui arrive et qui repart ; avant et
    après, sur `approche` mm, une courbe d'Hermite raccorde le chemin au cercle en tangente. Une tangente tirée d'un
    point trop proche faisait un coude de 17° (2026-10-07)."""
    m = len(dense)
    r = np.linalg.norm(dense - c, axis=1)
    dedans = r < R + 3
    if not dedans.any():
        return dense
    debut = next(i for i in range(m) if dedans[i] and not dedans[i - 1])
    fin = debut
    while dedans[(fin + 1) % m]:
        fin += 1
    cum = np.r_[0, np.cumsum(np.linalg.norm(np.diff(np.vstack([dense, dense[:1]]), axis=0), axis=1))]
    pas_moyen = cum[-1] / m
    k = int(approche / pas_moyen)
    ang = np.unwrap(np.arctan2(*(dense[[j % m for j in range(debut, fin + 1)]] - c).T[::-1]))
    s = np.sign(ang[-1] - ang[0])
    sur_cercle = lambda a: c + R * np.array([np.cos(a), np.sin(a)])
    tan_cercle = lambda a: s * np.array([-np.sin(a), np.cos(a)])
    def direction(i):
        d = dense[(i + 1) % m] - dense[(i - 1) % m]; return d / np.linalg.norm(d)
    # L'arc commence et finit là où le cercle a la direction du chemin qui arrive et qui repart : le faire partir du
    # premier point proche du cercle faisait une bosse de 8 mm sous la roue avant.
    d_in, d_out = direction(debut - k), direction(fin + k)
    a1 = max((np.arctan2(-d_in[0], d_in[1]) + e for e in (0, np.pi)), key=lambda a: np.dot(tan_cercle(a), d_in))
    a2 = max((np.arctan2(-d_out[0], d_out[1]) + e for e in (0, np.pi)), key=lambda a: np.dot(tan_cercle(a), d_out))
    a2 = a1 + s * ((s * (a2 - a1)) % (2 * np.pi))
    def hermite(P0, m0, P1, m1, n):
        L = np.linalg.norm(P1 - P0)
        t = np.linspace(0, 1, n, endpoint=False)[:, None]
        h00, h10, h01, h11 = 2*t**3 - 3*t**2 + 1, t**3 - 2*t**2 + t, -2*t**3 + 3*t**2, t**3 - t**2
        return h00 * P0 + h10 * m0 * L + h01 * P1 + h11 * m1 * L
    P_in, P_out = dense[(debut - k) % m], dense[(fin + k) % m]
    n_ent = max(4, int(np.linalg.norm(sur_cercle(a1) - P_in) / 0.5))
    n_arc = max(2, int(abs(a2 - a1) * R / 0.5))
    n_sor = max(4, int(np.linalg.norm(P_out - sur_cercle(a2)) / 0.5))
    entree = hermite(P_in, d_in, sur_cercle(a1), tan_cercle(a1), n_ent)
    arc = np.array([sur_cercle(a1 + (a2 - a1) * t) for t in np.linspace(0, 1, n_arc, endpoint=False)])
    sortie = hermite(sur_cercle(a2), tan_cercle(a2), P_out, d_out, n_sor)
    reste = np.array([dense[j % m] for j in range(fin + k, debut - k + m)])
    return np.vstack([entree, arc, sortie, reste])

def pas_a_pas(points, cum, u0, pas, n):
    """Les n axes d'une chenille posés le long du chemin fermé (points, longueurs cumulées cum) à partir de u0,
    chacun à pas mm à vol d'oiseau du précédent — comme une vraie chaîne : sur l'arc d'une roue dentée, deux axes
    voisins sont alors exactement à une dent l'un de l'autre. Renvoie les positions et l'abscisse du n-ième pas
    (u0 + longueur du chemin si la chenille se referme juste)."""
    L, m = cum[-1], len(points)
    def point(u):
        u = u % L
        i = int(np.searchsorted(cum, u, side='right') - 1)
        f = (u - cum[i]) / (cum[i + 1] - cum[i])
        return i, points[i] + (points[(i + 1) % m] - points[i]) * f
    i, p = point(u0)
    u = u0
    out = [p]
    for k in range(n):
        j = i
        a = p
        while True:
            B = points[(j + 1) % m]
            if np.linalg.norm(B - p) >= pas:
                d = B - a; w = a - p
                A2, B2, C2 = d @ d, 2 * d @ w, w @ w - pas * pas
                t = (-B2 + np.sqrt(max(B2 * B2 - 4 * A2 * C2, 0))) / (2 * A2)
                q = a + d * t
                u = u + np.linalg.norm(q - a) + (0 if j == i else 0)
                break
            u += np.linalg.norm(B - a)
            a = B; j += 1
        i, p = j % m, q
        out.append(p)
    return np.array(out[:-1]), u


def chenilles(pieces):
    """Ce qui bouge dans les chenilles : {nom: rôle}, et le chemin des axes de maillons de chaque côté (plan y, z de
    la maquette ; chaque côté est à x constant).

    Les roues dentées 2401 ont 12 dents et les maillons 24 mm de pas : un axe de maillon engagé tourne à
    R = 24 / (2 sin 15°) = 46,4 mm du centre de la roue, et deux axes voisins y sont à 30° l'un de l'autre. Dans
    l'export, la chenille est mal enroulée (axes posés de 43,9 à 51,1 mm du centre) et les roues sont calées au
    hasard (Nicolas, 2026-10-07). Le chemin suit donc exactement le cercle des axes autour des roues dentées, les
    axes y sont posés à 24 mm à vol d'oiseau les uns des autres, et la chenille, un peu courte une fois enroulée
    juste, reçoit son mou dans la longue portée libre entre la petite roue du haut et la roue dentée arrière."""
    from scipy.interpolate import CubicSpline
    centre = lambda p: t3(p['maillage'].bounds.mean(0))[0]
    roles, chemins = {}, {}
    dents = 12
    des_chenilles = [p for p in pieces if len(p['chaine']) > 1 and p['chaine'][1] == 'Wall-E GoBilda Tracks <1>']
    for cote, signe in (('droit', 1), ('gauche', -1)):
        du_cote = [p for p in des_chenilles if np.sign(centre(p)[0]) == signe and 'body support' not in ' '.join(p['chaine'])]
        axes_m = [p for p in du_cote if p['nom'].startswith('2400-0112-0002 Screw')]
        pts = np.array([centre(p)[1:] for p in axes_m])
        m0 = pts.mean(0)
        ordre = np.argsort(np.arctan2(pts[:, 1] - m0[1], pts[:, 0] - m0[0]))
        export = pts[ordre]
        n = len(export)
        pas = float(np.mean(np.linalg.norm(np.diff(np.vstack([export, export[:1]]), axis=0), axis=1)))
        R = pas / (2 * np.sin(np.pi / dents))
        centres = []
        for p in du_cote:
            if p['nom'].startswith('2401-'):
                c = centre(p)[1:]
                if not any(np.linalg.norm(c - q) < 2 for q in centres):
                    centres.append(c)
        roues_libres = [centre(p)[1:] for p in du_cote if p['nom'].startswith('2405-0014-0054')]
        # la portée libre la plus longue : les axes loin de toute roue
        def libre(q):
            return min([np.linalg.norm(q - c) - R for c in centres] + [np.linalg.norm(q - c) - 27 for c in roues_libres]) > 20
        meilleure, courante = [], []
        for j in list(range(n)) * 2:
            if libre(export[j]):
                courante.append(j)
                if len(courante) > len(meilleure):
                    meilleure = list(courante)
            else:
                courante = []
        portee = meilleure[:n]
        def chemin(fleche):
            cale = export.copy()
            for c in centres:
                for j in range(n):
                    v = cale[j] - c
                    if np.linalg.norm(v) < R + 8:
                        cale[j] = c + v / np.linalg.norm(v) * R
            # le mou : une flèche parabolique vers l'intérieur de la boucle, sur la portée libre
            if fleche and len(portee) > 1:
                a0, a1 = cale[(portee[0] - 1) % n], cale[(portee[-1] + 1) % n]
                t = (a1 - a0) / np.linalg.norm(a1 - a0); nrm = np.array([-t[1], t[0]])
                if np.dot(nrm, m0 - (a0 + a1) / 2) < 0:
                    nrm = -nrm
                for k, j in enumerate(portee):
                    s_ = (k + 1) / (len(portee) + 1)
                    cale[j] = cale[j] + nrm * fleche * 4 * s_ * (1 - s_)
            boucle = np.vstack([cale, cale[:1]])
            t = np.r_[0, np.cumsum(np.linalg.norm(np.diff(boucle, axis=0), axis=1))]
            dense = CubicSpline(t, boucle, bc_type='periodic')(np.linspace(0, t[-1], 4001))[:-1]
            # Autour des roues dentées, exactement le cercle des axes, raccordé en tangente : plaquer sur le cercle les
            # points proches faisait un saut de 3 mm où la chenille arrive sur la roue, et chaque axe y était
            # « aimanté » (Nicolas, 2026-10-07).
            for c in centres:
                dense = enrouler(dense, c, R)
            cum = np.r_[0, np.cumsum(np.linalg.norm(np.diff(np.vstack([dense, dense[:1]]), axis=0), axis=1))]
            u0 = float(cum[np.argmin(np.linalg.norm(dense - cale[0], axis=1))])
            return dense, cum, u0
        def ecart(fleche):
            dense, cum, u0 = chemin(fleche)
            _, u = pas_a_pas(dense, cum, u0, pas, n)
            return u - (u0 + cum[-1])                       # > 0 : la chenille est trop longue pour le chemin
        lo, hi = 0.0, 40.0
        if ecart(lo) <= 0:
            lo = hi = 0.0                                   # déjà assez de mou
        else:
            for _ in range(40):
                mi = (lo + hi) / 2
                (hi, lo) = (mi, lo) if ecart(mi) <= 0 else (hi, mi)
        fleche = hi
        dense, cum, u0 = chemin(fleche)
        repos, u_fin = pas_a_pas(dense, cum, u0, pas, n)
        aire = 0.5 * np.sum(dense[:, 0] * np.roll(dense[:, 1], -1) - np.roll(dense[:, 0], -1) * dense[:, 1])
        sens = 1.0 if aire > 0 else -1.0
        chemins[cote] = dict(x=float(np.mean([centre(p)[0] for p in axes_m])), points=dense.round(3).tolist(),
                             longueur=round(float(cum[-1]), 2), sens=sens, pas=round(pas, 4), n=n, u0=round(u0, 4),
                             rayon_dente=round(float(R), 3), fleche=round(fleche, 2),
                             ecart_fermeture=round(float(u_fin - u0 - cum[-1]), 3), portee=[int(portee[0]), int(portee[-1])])
        kd_axes = cKDTree(export)
        def du_maillon(p):
            if p['nom'].startswith(('2400-0112-0002', '2400-0112-0001 Tread')):
                return True
            if any('Link Assembly' in c for c in p['chaine']):
                return True
            return p['nom'].startswith(('2801-', '2812-')) and kd_axes.query(centre(p)[1:])[0] < 10
        # Chaque pièce d'un maillon suit son axe a et garde son angle par rapport à l'axe voisin b.
        for p in du_cote:
            if not du_maillon(p):
                continue
            q = centre(p)[1:]
            a = int(kd_axes.query(q)[1])
            b = min([(a - 1) % n, (a + 1) % n], key=lambda j: np.linalg.norm(export[j] - q))
            roles[p['nom']] = dict(role='maillon', cote=cote, a=a, b=b,
                                   pa=export[a].round(3).tolist(), pb=export[b].round(3).tolist())
        # Roues dentées : l'angle du milieu des creux, au repos ; la page les tourne pour qu'un creux tombe sous
        # chaque axe engagé.
        def profil(p, c):
            """Rayon du bord de la roue dentée tous les 0,5°, sur une coupe à mi-épaisseur : les sommets du maillage
            sont trop clairsemés sur les flancs des dents pour le dire."""
            from shapely.geometry import LineString, Polygon
            m = p['maillage'].copy(); m.apply_transform(VERS_MAQUETTE)
            sec = m.section(plane_origin=[m.bounds.mean(0)[0], c[0], c[1]], plane_normal=[1, 0, 0])
            bord = Polygon(max((e[:, 1:] - c for e in sec.discrete), key=lambda e: np.ptp(e[:, 0]))).exterior
            r = []
            for al in np.radians(np.arange(0, 360, 0.5)):
                x = LineString([(0, 0), (80 * np.cos(al), 80 * np.sin(al))]).intersection(bord)
                pts_ = [x] if x.geom_type == 'Point' else list(getattr(x, 'geoms', []))
                r.append(max(np.hypot(q.x, q.y) for q in pts_) if pts_ else 0.0)
            return np.array(r)
        def creux(p, c):
            # Milieu des creux : la phase de l'ondulation à 12 dents de tout le profil (un premier essai prenait
            # le premier angle le plus bas d'un fond plat de 10° : 4° d'erreur, dans un sens différent sur les
            # deux disques montés face à face).
            prof = profil(p, c)
            al = np.radians(np.arange(0, 360, 0.5))
            dent = np.angle(np.sum(prof * np.exp(1j * dents * al))) / dents
            return float(dent + np.pi / dents)
        for sous in ('Front Wheel', 'Rear Wheel'):
            ps = [p for p in du_cote if any(c.startswith(sous) for c in p['chaine'])]
            if not ps:
                continue
            c = np.mean([centre(p)[1:] for p in ps if p['nom'].startswith('2401-')], axis=0)
            phases = {p['nom']: creux(p, c) for p in ps if p['nom'].startswith('2401-')}
            ref = next(iter(phases.values()))
            for p in ps:
                roles[p['nom']] = dict(role='dentee', cote=cote, centre=np.round(c, 3).tolist(), rayon=round(float(R), 3),
                                       creux=round(phases.get(p['nom'], ref), 6))
        for p in du_cote:
            if p['nom'].startswith('2405-0014-0054'):
                roles[p['nom']] = dict(role='roue', cote=cote, centre=centre(p)[1:].round(3).tolist(),
                                       rayon=round(float(max(p['maillage'].extents) / 2), 2))
        dentees = {tuple(v['centre']): v for v in roles.values() if v['role'] == 'dentee' and v['cote'] == cote}
        for p in du_cote:
            if p['nom'] in roles or not p['nom'].startswith(('2106-4008-1200', '2307-', '1310-')):
                continue
            for c, v in dentees.items():
                if np.linalg.norm(centre(p)[1:] - np.array(c)) < 5:
                    roles[p['nom']] = dict(v)
        moteur = [p for p in du_cote if p['nom'].startswith(('5203 Shaft', '2306-4008-0014', 'Axle Part'))]
        if moteur:
            axe = t3(next(p['maillage'] for p in du_cote if p['nom'].startswith('Motor Sleeve')).bounds.mean(0))[0]
            for p in moteur:
                roles[p['nom']] = dict(role='moteur', cote=cote, centre=axe[:2].round(3).tolist(), rayon=round(float(R), 3),
                                       rapport=RENVOI)
    return roles, chemins

def main():
    C = json.load(open(COTES))['cinematique']
    pieces = rc.charger_complet()
    corps = [p for p in pieces if not (any(b in p['chaine'] for b in rc.ANCIENS_BRAS) and
                                       abs(p['maillage'].bounds.mean(0)[0] - rc.PLAN_SYMETRIE) > rc.GARDE_CORPS)]
    groupe = ranger(corps, C)
    yeux = tringlerie(corps, groupe)
    roles, chemins = chenilles(corps)
    for c, ch in chemins.items():
        n = sum(1 for r in roles.values() if r['cote'] == c and r['role'] == 'maillon')
        print('chenille %s : chemin de %.1f mm, %d axes au pas de %.2f mm, mou : flèche de %.1f mm sur les axes %s, '
              'écart de fermeture %.3f mm ; %d pièces de maillons' % (c, ch['longueur'], ch['n'], ch['pas'], ch['fleche'],
                                                                     ch['portee'], ch['ecart_fermeture'], n))
    for c, t in yeux.items():
        print('œil %s : %.1f° dans l\'export, palonnier %.1f mm, tringle %.1f mm' % (c, t['export'], t['palonnier'], t['tringle']))

    # maillages allégés, déjà calculés par robot_complet.py
    leger = trimesh.load(os.path.join(rc.DOSSIER, 'robot-complet-leger.glb'))
    scene = trimesh.Scene()
    noeuds = {}
    k = 0
    for n in leger.graph.nodes_geometry:
        if n not in groupe:
            continue                                               # les bras, refaits ci-dessous
        T, g = leger.graph[n]
        m = leger.geometry[g].copy(); m.apply_transform(VERS_MAQUETTE @ T)
        nom = 'p%d' % k; k += 1
        scene.add_geometry(m, node_name=nom, geom_name=nom)
        noeuds[nom] = dict(groupe=groupe[n], piece=n, **({'chenille': roles[n]} if n in roles else {}))
    anim = json.load(open(os.path.join(SORTIE, 'animation.json')))
    bras = {}
    for cote, ancien in (('droit', rc.ANCIENS_BRAS[0]), ('gauche', rc.ANCIENS_BRAS[1])):
        M = VERS_MAQUETTE @ rc.epaule(pieces, ancien)
        bras[cote] = M.round(6).tolist()
        for cle, nom_piece, ref, m in rc.nouveau_bras():
            m = m.copy(); m.apply_transform(M)
            nom = 'p%d' % k; k += 1
            scene.add_geometry(m, node_name=nom, geom_name=nom)
            noeuds[nom] = dict(groupe='bras_' + cote, piece=nom_piece, ref=ref,
                               articulation=anim['noeuds'].get(cle, 'caisson'))
    scene.export(os.path.join(rc.DOSSIER, 'robot-anime.glb'))
    json.dump(dict(cinematique=C, yeux=yeux, chenilles=chemins, bras=bras, bras_groupes=anim['groupes'], courses=anim['courses'],
                   noeuds=noeuds), open(os.path.join(rc.DOSSIER, 'robot-anime.json'), 'w'), ensure_ascii=False)
    compte = {}
    for v in noeuds.values():
        compte[v['groupe']] = compte.get(v['groupe'], 0) + 1
    print('pièces par groupe :', compte)


if __name__ == '__main__':
    main()
