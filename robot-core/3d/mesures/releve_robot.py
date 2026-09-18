#!/usr/bin/env python3
"""
Relevé du robot complet dans « Wall-E GoBilda Full.stl », et construction du modèle simplifié
(robot-walle.glb) que lisent robot-walle.html et la maquette de l'éditeur d'animation.

Le STL fait 1,35 Go (27 M triangles) : il n'est jamais chargé d'un bloc. Les sommets sont recopiés
une fois sur le disque local en float32 (975 Mo), tout le reste travaille sur des tableaux projetés
en mémoire.

    python3 releve_robot.py [STL] [--travail DOSSIER] [--sortie DOSSIER]

Chaque étape reprend son résultat du dossier de travail si elle a déjà tourné : après un nouvel
export, changer de dossier de travail, sinon l'ancien sera relu sans rien dire.

Tout ce qui dépend de l'export — numéros de pièce, points de départ des ajustements, plages — est
regroupé dans le bloc « Cet export » ci-dessous. Onshape écrit les pièces dans l'ordre de l'arbre
d'assemblage et la segmentation les numérote dans l'ordre du fichier : un nouvel export les
renumérote, et c'est ce bloc qu'il faut revoir, pas la méthode.
"""
import argparse, json, math, os, shutil, struct, sys, tempfile
import numpy as np
from scipy.optimize import least_squares
from scipy.sparse import coo_matrix
from scipy.sparse.csgraph import connected_components

ICI = os.path.dirname(os.path.abspath(__file__))
STL_DEFAUT = '/media/npeltier/disque_usb/Robot/Wall-E GoBilda Full.stl'
# Origine de la grille de soudure, figée : la déplacer peut faire basculer un sommet d'un micron d'une
# cellule à l'autre, couper une pièce en deux et décaler toute la numérotation qui suit.
ORIGINE_SOUDURE = np.array([-280.0, -339.0, -109.0])
# Plan de symétrie et sol, pour le repère du modèle
CENTRE = np.array([24.7, 29.4, -108.06])
BLOC = 3_000_000

# ── Cet export (Onshape, 2026-09-13 19:22) ──────────────────────────────────────────────────────────
TETE = range(0, 93)          # coques, optique, palonniers et bielles des yeux, platine
COU = range(93, 200)         # parallélogramme, chariot, panoramique, inclinaison, servo du monter
BRAS = range(200, 613)       # le bras gauche déborde jusqu'à 612 ; la caisse commence à 613
PIECES = {
    'palonnier_monter': [153, 133, 178],                    # moyeu du servo du monter
    'barre_avant': [112, 126], 'barre_milieu': [108, 109, 181, 143], 'barre_arriere': [176],
    # La roue de 80 dents (104) est fixée à l'équerre du panoramique (114) : c'est autour d'elle que roule
    # le pignon du Stingray.
    'panoramique': [114, 189, 113, 140, 185, 104],
    # Le moteur de l'inclinaison (Stingray, 105) tourne avec la tête : ses trous partagent la direction
    # inclinée de la platine.
    'tete': [96, 98, 99, 100, 101, 102, 103, 105, 106],
    'pignon_inclinaison': [107, 97],                        # pignon de 40 dents et sa vis
}
# Roue et pignon de l'inclinaison, module 0,8 : rayons extérieurs 32,8 et 16,8 mm, entraxe 48,00.
DENTS_INCLINAISON = (80, 40)
# Maneton et tige du CAD : il accroche la tige sur un trou du moyeu, à 11,3 mm, avec une tige de
# 92,65 mm. Le robot a un bras goBILDA de 6 trous (rotule à 28 mm) et une tige de 87 mm : le modèle les
# dessine à part (MONTER_REEL), et ces pièces-là n'y figurent pas.
EXCLUS = [124, 197, 166, 118, 171]
PALONNIERS = {'droit': 3, 'gauche': 8}       # donnent la profondeur des broches
BIELLES = {'droit': 60, 'gauche': 51}
GRAINES = {
    'arbre': (list(TETE), (-0.0345, 0.9966, 0.0751), (19.46, 0, 575.53)),
    'inclinaison': ([114, 104, 105, 106, 96, 98], (0.9994, 0.0346, 0), (18.13, 38.38, 556.66)),
    'panoramique': ([114, 189, 113, 140, 185, 93, 94, 156], (0, 0, 1), (18.13, 38.37, 500)),
    'yeux': {'droit': dict(S=([3, 36, 37], (-47.41, 0, 518.87)), P=([3, 38, 44], (-50.81, 0, 486.97)),
                           D=([50, 52, 61, 71, 74], (6.45, 0, 549.49))),
             'gauche': dict(S=([7, 8, 14], (97.89, 0, 536.04)), P=([8, 9, 69], (104.63, 0, 504.66)),
                            D=([22, 31, 50, 63, 67], (32.47, 0, 549.42)))},
    'pignon': ([107, 97], (0.9994, 0.0346, 0), (6.8, -9.91, 553.06)),
    'O2': ([153, 133, 178, 127], (40, -4.57, 290.38)),        # axe du servo du monter
    'R0': ([109, 181], (17, 84.32, 277.45)),                  # rotule du levier
    'bas': ([144, 192, 152, 120, 137, 151, 187, 145, 175, 191, 167, 186, 119, 130, 177, 188], 322.38, (51.43, 67.43, 83.43)),
    'haut': ([155, 184, 159, 117, 121, 123, 125, 134, 138, 146, 149, 154, 160, 162, 179, 180, 194], 442.19, (6.37, 22.37, 38.37)),
    'epaules': {'bras_gauche': ([474], (225, 79.375, 275.777)), 'bras_droit': ([313], (-175, 79.375, 276.977))},
}
X_BRAS_MONTER, X_LEVIER = 52.0, 21.8      # plans latéraux du bras sur le moyeu et de la rotule du levier
# Pièce absente du modèle Onshape : à l'arrière du parallélogramme, les entretoises réservent la place
# d'une plaque en face de la plaque avant 126, mais il n'y a rien. On la reconstitue comme la paire
# avant (126 = 112 décalée de 32 mm) : la plaque arrière 176, même pièce, décalée de 32 mm.
AJOUTS = {'barre_arriere': [(176, np.array([32.0, 0.0, 0.0]))]}

# ── Le vrai monter/descendre, relevé sur le robot le 2026-09-13 ────────────────────────────────────
# Bras goBILDA 1102-0006-0048 vissé dans deux logements d'écrou opposés du moyeu (trous 1 et 4), rotule
# au trou 6 : 12 + 2 × 8 = 28 mm. Tige de 87 mm, rotule à rotule. Servo « 300° Torque » : 1,583 °/unité.
MONTER_REEL = dict(bras=28.0, tige=87.0, deg_par_unite=1.583)
# Hauteur mesurée au réglet selon la valeur du HUD (cm), du dessus du U des axes bas au dessous du bloc
# au-dessus des axes hauts ; le réglet a 6 mm d'origine. Elle cale le servo, qu'aucune cote ne donne.
MESURE_HAUTEUR = dict(zip(range(-10, 61, 2), (
    6.4, 6.5, 6.5, 6.6, 6.7, 6.9, 7.1, 7.3, 7.7, 7.9, 8.2, 8.3, 8.7, 9.0, 9.4, 9.6, 10.0, 10.2,
    10.5, 10.6, 11.0, 11.3, 11.5, 11.8, 12.0, 12.1, 12.4, 12.4, 12.6, 12.7, 12.7, 12.7, 12.8, 12.8, 12.8, 12.8)))
ORIGINE_REGLET_CM = 0.6
# Les deux bouts de la course s'écartent de la loi : le robot y bute. Seul le milieu cale le servo.
PLAGE_CALAGE = (-2, 50)


# ── 1. sommets, aires ────────────────────────────────────────────────────────

def extraire(stl, T):
    fs, fa = os.path.join(T, 'sommets.npy'), os.path.join(T, 'aires.npy')
    if not (os.path.exists(fs) and os.path.exists(fa)):
        n = int(np.fromfile(stl, dtype='<u4', count=1, offset=80)[0])
        if os.path.getsize(stl) != 84 + 50 * n:
            sys.exit('%s : STL binaire attendu' % stl)
        dt = np.dtype([('n', '<f4', 3), ('v', '<f4', (3, 3)), ('a', '<u2')])
        m = np.memmap(stl, dtype=dt, mode='r', offset=84, shape=(n,))
        V = np.lib.format.open_memmap(fs, mode='w+', dtype='<f4', shape=(n, 3, 3))
        A = np.empty(n, '<f4')
        for i in range(0, n, BLOC):
            v = np.array(m['v'][i:i + BLOC])
            V[i:i + len(v)] = v
            A[i:i + len(v)] = 0.5 * np.linalg.norm(np.cross(v[:, 1] - v[:, 0], v[:, 2] - v[:, 0]), axis=1)
        V.flush(); del V
        np.save(fa, A)
    return np.load(fs, mmap_mode='r'), np.load(fa)


# ── 2. pièces ────────────────────────────────────────────────────────────────

def segmenter(V, T):
    """Connexité par ARÊTE partagée : par sommet, les pièces qui se touchent à plat fusionnent."""
    ff, fp = os.path.join(T, 'faces.npy'), os.path.join(T, 'pieces.npy')
    if not (os.path.exists(ff) and os.path.exists(fp)):
        n = V.shape[0]
        cle = np.empty(n * 3, np.int64)
        for i in range(0, n, BLOC):
            q = np.rint((np.asarray(V[i:i + BLOC], np.float64).reshape(-1, 3) - ORIGINE_SOUDURE) * 1000).astype(np.int64)
            cle[i * 3:i * 3 + len(q)] = (q[:, 0] << 42) | (q[:, 1] << 21) | q[:, 2]
        uniq, inv = np.unique(cle, return_inverse=True)
        del cle
        nv = len(uniq); del uniq
        F = inv.astype(np.int32).reshape(n, 3); del inv
        np.save(ff, F)
        a = np.concatenate([F[:, 0], F[:, 1], F[:, 2]]).astype(np.int64)
        b = np.concatenate([F[:, 1], F[:, 2], F[:, 0]]).astype(np.int64)
        ar = np.minimum(a, b) * nv + np.maximum(a, b); del a, b, F
        tri = np.tile(np.arange(n, dtype=np.int32), 3)
        o = np.argsort(ar, kind='stable'); ar = ar[o]; tri = tri[o]; del o
        eq = ar[1:] == ar[:-1]
        g = coo_matrix((np.ones(eq.sum(), np.int8), (tri[:-1][eq], tri[1:][eq])), shape=(n, n)).tocsr()
        del ar, tri, eq
        np.save(fp, connected_components(g, directed=False)[1].astype(np.int32))
    return np.load(ff, mmap_mode='r'), np.load(fp)


# ── 3. fiches ────────────────────────────────────────────────────────────────

def ficher(V, A, L, T):
    f = os.path.join(T, 'fiches.npz')
    if not os.path.exists(f):
        k = L.max() + 1
        cnt = np.bincount(L, minlength=k); ar = np.bincount(L, weights=A, minlength=k)
        mn = np.full((k, 3), np.inf); mx = np.full((k, 3), -np.inf); cg = np.zeros((k, 3))
        for i in range(0, len(L), BLOC):
            v = np.asarray(V[i:i + BLOC], np.float64); l = L[i:i + BLOC]; c = v.mean(1)
            for d in range(3):
                cg[:, d] += np.bincount(l, weights=c[:, d] * A[i:i + BLOC], minlength=k)
                np.minimum.at(mn[:, d], l, v[:, :, d].min(1)); np.maximum.at(mx[:, d], l, v[:, :, d].max(1))
        cg /= np.maximum(ar, 1e-12)[:, None]
        np.savez(f, cnt=cnt, ar=ar, mn=mn, mx=mx, cg=cg)
    z = np.load(f)
    return {k: z[k] for k in z.files}


# ── 4. axes ──────────────────────────────────────────────────────────────────

class Releve:
    def __init__(s, V, F, A, L):
        s.V, s.F, s.A, s.L = V, F, A, L
        s._cyl = {}
        o = np.argsort(L, kind='stable'); s._o = o
        s._deb = np.searchsorted(L[o], np.arange(L.max() + 2))

    def triangles(s, p):
        return s._o[s._deb[p]:s._deb[p + 1]]

    def normales(s, p):
        v = np.asarray(s.V[s.triangles(p)], np.float64)
        n = np.cross(v[:, 1] - v[:, 0], v[:, 2] - v[:, 0]); a = np.linalg.norm(n, axis=1)
        return v, n / (a[:, None] + 1e-15), a

    def cylindres(s, p, mini=16, tol=0.25):
        """Nappes lisses (plis de moins de 25°) dont les normales tiennent dans un plan : l'axe est
        la normale de ce plan (plus petit vecteur propre du nuage des normales), puis le moindre
        carré linéaire p·n = c·n + r dans le plan orthogonal."""
        if (p, mini, tol) in s._cyl:
            return s._cyl[(p, mini, tol)]
        ids = s.triangles(p); f = np.asarray(s.F[ids]); v, n, a = s.normales(p)
        m = len(ids); res = []
        if m >= mini:
            e = np.concatenate([f[:, [0, 1]], f[:, [1, 2]], f[:, [2, 0]]]).astype(np.int64); e.sort(1)
            cle = e[:, 0] * (int(f.max()) + 1) + e[:, 1]
            o = np.argsort(cle, kind='stable'); ct = cle[o]; ti = np.tile(np.arange(m), 3)[o]
            eq = ct[1:] == ct[:-1]; t1, t2 = ti[:-1][eq], ti[1:][eq]
            lisse = np.einsum('ij,ij->i', n[t1], n[t2]) > np.cos(np.radians(25))
            lab = connected_components(coo_matrix((np.ones(lisse.sum(), np.int8), (t1[lisse], t2[lisse])), shape=(m, m)),
                                       directed=False)[1]
            c = v.mean(1)
            for l in np.unique(lab):
                k = np.flatnonzero(lab == l)
                if len(k) < mini:
                    continue
                q, pc, w = n[k], c[k], a[k] / 2
                ev, evec = np.linalg.eigh((q * w[:, None]).T @ q / w.sum())
                if ev[0] > 2e-3 or ev[1] < 0.08:
                    continue
                d, e1, e2 = evec[:, 0], evec[:, 1], evec[:, 2]
                P2 = np.column_stack([pc @ e1, pc @ e2]); Q2 = np.column_stack([q @ e1, q @ e2])
                c1, c2, r = np.linalg.lstsq(np.column_stack([Q2, np.ones(len(k))]), (P2 * Q2).sum(1), rcond=None)[0]
                if abs(r) < 1.0 or np.abs(np.hypot(P2[:, 0] - c1, P2[:, 1] - c2) - abs(r)).max() > tol:
                    continue
                res.append(dict(d=d * np.sign(d[np.argmax(np.abs(d))]), p=c1 * e1 + c2 * e2, r=abs(r), aire=w.sum()))
        s._cyl[(p, mini, tol)] = res
        return res

    def ligne(s, pieces, dref, approx, tol=1.0, rmin=0.0, mini=16):
        """Axe moyen (pondéré par l'aire) des cylindres coaxiaux passant près d'un point approché."""
        dref = np.asarray(dref, float); dref /= np.linalg.norm(dref); approx = np.asarray(approx, float)
        Q, D, W = [], [], []
        for p in pieces:
            for c in s.cylindres(p, mini=mini):
                if abs(abs(c['d'] @ dref) - 1) > 1e-3 or c['r'] < rmin:
                    continue
                d = c['d'] * np.sign(c['d'] @ dref); q = c['p'] + ((approx - c['p']) @ d) * d
                if np.linalg.norm(q - approx) < tol:
                    Q.append(q); D.append(d); W.append(c['aire'])
        if not W:
            raise ValueError('aucun axe près de %s dans %s' % (approx, list(pieces)[:6]))
        W = np.array(W); d = (np.array(D) * W[:, None]).sum(0); d /= np.linalg.norm(d)
        return (np.array(Q) * W[:, None]).sum(0) / W.sum(), d

    def orientation(s, p, axe, e1, e2, angles):
        """Part de l'aire latérale (normales ⟂ axe) dont l'angle, modulo 90°, vaut chacun des angles
        demandés : une pièce tournée avec un corps en porte l'angle sur toutes ses faces planes."""
        _, n, a = s.normales(p)
        k = np.abs(n @ axe) < 0.08; ang = np.degrees(np.arctan2(n[k] @ e2, n[k] @ e1)) % 90; a = a[k]
        tot = max(a.sum(), 1e-9)
        return [a[np.abs(((ang - x + 45) % 90) - 45) < 0.3].sum() / tot for x in angles]


X, Y, Z = np.eye(3)


def relever(R):
    g = GRAINES; r = {}
    r['arbre'], ey = R.ligne(*g['arbre'])
    r['inclinaison'], ex = R.ligne(*g['inclinaison'])
    ex = ex - (ex @ ey) * ey; ex /= np.linalg.norm(ex)
    r['ex'], r['ey'], r['ez'] = ex, ey, np.cross(ex, ey)
    r['panoramique'], _ = R.ligne(*g['panoramique'])
    r['pignon'], _ = R.ligne(*g['pignon'])
    def tete(q): v = q - r['arbre']; return np.array([v @ ex, v @ r['ez']])
    r['yeux'] = {c: {k: tete(R.ligne(pc, ey, pa)[0]) for k, (pc, pa) in v.items()} for c, v in g['yeux'].items()}
    yz = lambda pcs, pa, **kw: R.ligne(pcs, X, pa, **kw)[0][1:]
    r['O2'] = yz(*g['O2'], rmin=3)
    r['R0'] = yz(*g['R0'])
    pcs, z, ys = g['bas']; r['bas'] = [yz(pcs, (18, y, z)) for y in ys]
    pcs, z, ys = g['haut']; r['haut'] = [yz(pcs, (18, y, z)) for y in ys]
    r['epaules'] = {n: yz(pcs, pa, rmin=6) for n, (pcs, pa) in g['epaules'].items()}
    return r


# ── 5. le monter / descendre ─────────────────────────────────────────────────

class Monter:
    """Servo → bras → tige → levier de la barre du milieu → parallélogramme, dans le plan (Y arrière,
    Z haut). Géométrie du CAD pour le servo, les pivots et le levier ; bras et tige du robot ; calage
    tiré de la courbe de hauteur mesurée."""
    def __init__(s, r):
        s.O2, s.R0, s.bas, s.haut = r['O2'], r['R0'], r['bas'], r['haut']
        s.B = s.bas[1]
        s.barre = float(np.mean([np.linalg.norm(h - b) for h, b in zip(s.haut, s.bas)]))
        s.levier = float(np.linalg.norm(s.R0 - s.B))
        s.theta0 = math.degrees(math.atan2(*(s.haut[1] - s.B)[::-1]))       # barres dans l'export
        s.bras, s.tige, s.K = MONTER_REEL['bras'], MONTER_REEL['tige'], MONTER_REEL['deg_par_unite']
        s.caler()

    def barre_(s, v, calage=None, sens=None, branche=None):
        """Angle des barres (degrés) pour une valeur d'organe v (l'unité du HUD)."""
        calage = s.calage if calage is None else calage; sens = s.sens if sens is None else sens
        branche = s.branche if branche is None else branche
        phi = np.radians(calage + sens * s.K * np.asarray(v, float))
        M = s.O2[:, None] + s.bras * np.vstack([np.cos(phi), np.sin(phi)])
        d = M - s.B[:, None]; q = np.hypot(*d)
        k = (q * q + s.levier ** 2 - s.tige ** 2) / (2 * q * s.levier)
        th = np.arctan2(d[1], d[0]) + branche * np.arccos(np.clip(k, -1, 1)) + math.pi
        return np.degrees(th), np.abs(k).max()

    def caler(s):
        v = np.array(sorted(MESURE_HAUTEUR), float)
        h = (np.array([MESURE_HAUTEUR[x] for x in sorted(MESURE_HAUTEUR)]) - ORIGINE_REGLET_CM) * 10
        u = (v >= PLAGE_CALAGE[0]) & (v <= PLAGE_CALAGE[1])
        best = None
        for sens in (1, -1):
            for branche in (1, -1):
                for c0 in range(0, 360, 10):
                    def ecart(p):
                        th, _ = s.barre_(v[u], p[0], sens, branche)
                        return p[1] + s.barre * np.sin(np.radians(th)) - h[u]
                    sol = least_squares(ecart, [c0, 0])
                    th, kmax = s.barre_(v, sol.x[0], sens, branche)
                    if kmax <= 1 and th.min() > 80 and th.max() < 180 and (best is None or sol.cost < best[0].cost):
                        best = (sol, sens, branche)
        sol, s.sens, s.branche = best
        s.calage, s.origine = float(sol.x[0] % 360), float(sol.x[1])
        th, _ = s.barre_(v)
        s.ecarts = dict(zip(v.astype(int).tolist(), (h - s.origine - s.barre * np.sin(np.radians(th))).round(2).tolist()))
        e = np.array([s.ecarts[int(x)] for x in v[u]])
        s.rms, s.max = float(np.sqrt(np.mean(e * e))), float(np.abs(e).max())
        # valeur d'organe qui redonne la pose de l'export
        vv = np.linspace(-60, 120, 18001); tt, _ = s.barre_(vv)
        s.v_export = float(vv[np.argmin(np.abs(tt - s.theta0))])

    def chariot(s, v):
        """Déplacement du chariot (recul, montée) par rapport à l'export, en mm."""
        t, t0 = math.radians(float(s.barre_([v])[0][0])), math.radians(s.theta0)
        return s.barre * (math.cos(t) - math.cos(t0)), s.barre * (math.sin(t) - math.sin(t0))


# ── 6. corps rigides ─────────────────────────────────────────────────────────

MECA = dict(S=(86.132, -16.082), D=(13.0, -26.001), BRAS=32.0, BIELLE=84.728, PSI0=-73.652)   # tringlerie.ts


def dist_seg(p, a, b):
    t = np.clip((p - a) @ (b - a) / ((b - a) @ (b - a)), 0, 1); return np.linalg.norm(p - a - t * (b - a))


def affecter(R, fi, r):
    ar, cg = fi['ar'], fi['cg']; k = len(ar)
    nd = np.array(['caisse'] * k, dtype=object)
    for nom, ids in PIECES.items():
        nd[ids] = nom
    nd[EXCLUS] = 'exclu'
    for p in COU:
        if nd[p] == 'caisse' and 430 < cg[p, 2] < 506 and abs(cg[p, 0] - 19) < 60:
            nd[p] = 'chariot'
    for p in BRAS:                    # les servos d'épaule, dans la caisse, restent à la caisse
        if cg[p, 0] > 165: nd[p] = 'bras_gauche'
        elif cg[p, 0] < -116: nd[p] = 'bras_droit'
    # les chenilles : tout ce qui reste hors du cadre et bas ; statiques, seul le pas de décimation change
    for p in range(k):
        if nd[p] == 'caisse' and (cg[p, 0] < -150 or cg[p, 0] > 200) and cg[p, 2] < 185:
            nd[p] = 'chenilles'

    O, ex, ey, ez = r['arbre'], r['ex'], r['ey'], r['ez']
    H = {p: np.array([(cg[p] - O) @ ex, (cg[p] - O) @ ey, (cg[p] - O) @ ez]) for p in TETE}
    prof_bielle = np.mean([H[p][1] for p in BIELLES.values()])
    # Le palonnier est dans le plan du servo, derrière la coque : sans ce critère, une vis de la coque qui
    # passe par hasard sur la ligne S–P (œil droit exporté à −30°) lui était attribuée.
    prof_palonnier = {c: H[p][1] for c, p in PALONNIERS.items()}
    rot = {c: math.degrees(math.atan2(*r['yeux'][c]['S'][::-1])) - math.degrees(math.atan2(MECA['S'][1], s * MECA['S'][0]))
           for c, s in (('droit', -1), ('gauche', 1))}
    for p in H:
        x, y, z = H[p]; xz = np.array([x, z]); cote = 'droit' if x < 0 else 'gauche'; q = r['yeux'][cote]
        if abs(x) < 3 and abs(z) < 3 or np.linalg.norm(xz - q['D']) < 3:
            nd[p] = 'tete'                                        # arbre, paliers, axes d'ancrage
        elif ar[p] > 250 and dist_seg(xz, q['S'], q['P']) < 6 and abs(y - prof_palonnier[cote]) < 12:
            nd[p] = 'palonnier_oeil_' + cote
        elif ar[p] > 250 and dist_seg(xz, q['P'], q['D']) < 5 and abs(y - prof_bielle) < 12:
            nd[p] = 'bielle_oeil_' + cote
        else:
            a0, ad, ag = R.orientation(p, ey, ex, ez, [0, rot['droit'], rot['gauche']])
            a1 = max(ad, ag)
            nd[p] = 'coque_' + cote if a1 > 0.2 and a1 > 2 * a0 else 'tete' if a0 > 0.2 and a0 > 2 * a1 else None
    cible = [p for p in H if nd[p] in ('tete', 'coque_droit', 'coque_gauche')]
    for p in H:                                    # visserie ronde : au corps fixe le plus proche
        if nd[p] is None:
            nd[p] = nd[cible[int(np.argmin([np.linalg.norm(cg[p] - cg[c]) for c in cible]))]]
    return nd.astype(str)


# ── 7. modèle ────────────────────────────────────────────────────────────────

def rotation(axe, deg):
    a = np.asarray(axe, float); a = a / np.linalg.norm(a); t = math.radians(deg)
    K = np.array([[0, -a[2], a[1]], [a[2], 0, -a[0]], [-a[1], a[0], 0]])
    return np.eye(3) + math.sin(t) * K + (1 - math.cos(t)) * K @ K


class Tr:
    def __init__(s, Rm=np.eye(3), t=np.zeros(3)): s.R, s.t = Rm, np.asarray(t, float)
    def __call__(s, x): return np.asarray(x) @ s.R.T + s.t
    def apres(s, o): return Tr(s.R @ o.R, s.R @ o.t + s.t)


def autour(point, axe, deg):
    Rm = rotation(axe, deg); p = np.asarray(point, float); return Tr(Rm, p - Rm @ p)


angle = lambda v: math.degrees(math.atan2(v[1], v[0]))


def t3(p):
    """STL → three : x = droite du robot, y = haut, z = arrière ; origine au sol dans le plan de symétrie."""
    p = np.asarray(p, float) - CENTRE; return np.stack([-p[..., 0], p[..., 2], p[..., 1]], -1)


def decimer(V, tris, pas):
    """Regroupement de sommets sur une grille : une cellule, un sommet ; les triangles écrasés et les doublons partent."""
    v = np.asarray(V[tris], np.float64).reshape(-1, 3)
    q = np.floor((v - v.min(0)) / pas).astype(np.int64)
    u, inv = np.unique((q[:, 0] << 42) | (q[:, 1] << 21) | q[:, 2], return_inverse=True)
    pos = np.zeros((len(u), 3)); np.add.at(pos, inv, v); pos /= np.bincount(inv)[:, None]
    f = inv.reshape(-1, 3); f = f[(f[:, 0] != f[:, 1]) & (f[:, 1] != f[:, 2]) & (f[:, 0] != f[:, 2])]
    s = np.sort(f, 1); _, k = np.unique(s[:, 0] * len(u) ** 2 + s[:, 1] * len(u) + s[:, 2], return_index=True)
    used, f2 = np.unique(f[np.sort(k)], return_inverse=True)
    return pos[used], f2.reshape(-1, 3)


PARENT = {'caisse': None, 'chenilles': 'caisse', 'palonnier_monter': 'caisse',
          'barre_avant': 'caisse', 'barre_milieu': 'caisse', 'barre_arriere': 'caisse',
          'bras_gauche': 'caisse', 'bras_droit': 'caisse', 'chariot': 'caisse', 'panoramique': 'chariot',
          'tete': 'panoramique', 'pignon_inclinaison': 'tete', 'coque_droit': 'tete', 'coque_gauche': 'tete',
          'palonnier_oeil_droit': 'coque_droit', 'palonnier_oeil_gauche': 'coque_gauche',
          'bielle_oeil_droit': 'tete', 'bielle_oeil_gauche': 'tete'}
# Pas de la grille : fin là où l'animation se regarde, grossier sur les chenilles (9,8 M triangles)
PAS = lambda n: 6.0 if n == 'chenilles' else 3.0 if n in ('caisse', 'bras_gauche', 'bras_droit') else \
    1.2 if n.startswith(('coque', 'tete')) else 0.8 if n.startswith(('palonnier', 'bielle', 'barre', 'pignon')) else 1.5
MATERIAUX = [('caisse', [0.62, 0.55, 0.40], 0.25, 0.6), ('chenilles', [0.20, 0.20, 0.21], 0.1, 0.9),
             ('bras', [0.55, 0.52, 0.46], 0.3, 0.55), ('mecanique', [0.72, 0.74, 0.78], 0.7, 0.35),
             ('coque', [0.55, 0.53, 0.46], 0.12, 0.55), ('palonnier', [0.96, 0.70, 0.14], 0.4, 0.35),
             ('bielle', [0.85, 0.87, 0.89], 0.6, 0.35)]
MAT = lambda n: 0 if n == 'caisse' else 1 if n == 'chenilles' else 2 if n.startswith('bras') else \
    4 if n.startswith('coque') else 5 if n.startswith('palonnier') else 6 if n.startswith('bielle') else 3


def construire(R, fi, r, mo, nd, sortie):
    V, L, ar, cg = R.V, R.L, fi['ar'], fi['cg']
    O, ex, ey, ez, PT = r['arbre'], r['ex'], r['ey'], r['ez'], r['inclinaison']
    lacet = angle(ex[:2])
    U_pan = autour([PT[0], PT[1], 0], Z, -lacet)
    ey1 = U_pan.R @ ey; tangage = math.degrees(math.atan2(ey1[2], ey1[1]))
    U_tete = autour(PT, X, -tangage).apres(U_pan)
    o = U_tete(O); O_ref = np.array([o[0], PT[1], o[2]])           # l'arbre, au droit de l'inclinaison
    # Le pignon roule autour de la roue fixe : quand la tête s'incline de α, il tourne de α·80/40 par rapport
    # à elle, dans le même sens. Ramener la tête au neutre le fait donc tourner d'autant sur son axe.
    rapport = DENTS_INCLINAISON[0] / DENTS_INCLINAISON[1]
    P_pignon = U_tete(r['pignon'])
    U = {'panoramique': U_pan, 'tete': U_tete,
         'pignon_inclinaison': autour(P_pignon, X, -rapport * tangage).apres(U_tete)}
    exp = lambda xz: O + xz[0] * ex + xz[1] * ez
    oeil = {}
    for cote, s in (('droit', -1), ('gauche', 1)):
        q = r['yeux'][cote]
        S = O_ref + [s * MECA['S'][0], 0, MECA['S'][1]]
        pa = math.radians(MECA['PSI0'])
        P = S + [s * MECA['BRAS'] * math.cos(pa), 0, MECA['BRAS'] * math.sin(pa)]
        D = O_ref + [s * MECA['D'][0], 0, MECA['D'][1]]
        beta = angle((U_tete(exp(q['S'])) - O_ref)[[0, 2]]) - angle((S - O_ref)[[0, 2]])
        U_c = autour(O_ref, Y, beta).apres(U_tete)
        U_h = autour(S, Y, angle((U_c(exp(q['P'])) - S)[[0, 2]]) - angle((P - S)[[0, 2]])).apres(U_c)
        D2 = U_tete(exp(q['D']))
        U_b = autour(D2, Y, angle((U_tete(exp(q['P'])) - D2)[[0, 2]]) - angle((P - D)[[0, 2]])).apres(U_tete)
        U['coque_' + cote], U['palonnier_oeil_' + cote], U['bielle_oeil_' + cote] = U_c, U_h, U_b
        oeil[cote] = dict(S=S, P=P, D=D, export=s * beta,
                          ecart=max(np.linalg.norm((U_c(exp(q['S'])) - S)[[0, 2]]), np.linalg.norm((U_h(exp(q['P'])) - P)[[0, 2]]),
                                    np.linalg.norm((U_b(exp(q['P'])) - P)[[0, 2]])))
    for cote in oeil:
        oeil[cote]['yh'] = U['palonnier_oeil_' + cote](cg[PALONNIERS[cote]])[1] - O_ref[1]
        oeil[cote]['yb'] = U['bielle_oeil_' + cote](cg[BIELLES[cote]])[1] - O_ref[1]
    o2 = np.array([44.0, *mo.O2])
    pivot = {'caisse': CENTRE, 'chenilles': CENTRE, 'palonnier_monter': o2, 'pignon_inclinaison': P_pignon,
             'chariot': np.array([19.1, *mo.haut[1]]), 'panoramique': PT, 'tete': PT,
             **{n: np.array([19.1, *b]) for n, b in zip(('barre_avant', 'barre_milieu', 'barre_arriere'), mo.bas)},
             **{n: np.array([225.0 if n == 'bras_gauche' else -175.6, *yz]) for n, yz in r['epaules'].items()}}
    for cote in oeil:
        pivot['coque_' + cote] = O_ref
        pivot['palonnier_oeil_' + cote] = oeil[cote]['S'] + [0, oeil[cote]['yh'], 0]
        pivot['bielle_oeil_' + cote] = oeil[cote]['P'] + [0, oeil[cote]['yb'], 0]

    geo = {}
    for n in PARENT:
        tris = np.flatnonzero((nd[L] == n) & (ar[L] >= 40))      # la petite visserie ne se voit pas
        p, f = decimer(V, tris, PAS(n))
        for piece, decalage in AJOUTS.get(n, []):
            p2, f2 = decimer(V, np.flatnonzero(L == piece), PAS(n))
            f = np.vstack([f, f2 + len(p)]); p = np.vstack([p, p2 + decalage])
        geo[n] = ((t3(U.get(n, Tr())(p)) - t3(pivot[n])).astype(np.float32), f, len(tris))

    v3 = lambda p: [round(float(x), 3) for x in t3(p)]
    cine = dict(
        unite='mm', repere='x droite du robot, y haut, z arriere ; origine au sol dans le plan de symetrie',
        export=dict(panoramique=round(-lacet, 3), inclinaison=round(-tangage, 3),
                    oeilDroit=round(oeil['droit']['export'], 3), oeilGauche=round(oeil['gauche']['export'], 3),
                    monterDescendre=round(mo.v_export, 2)),
        pivots={k: v3(v) for k, v in pivot.items()}, parents=PARENT,
        monter=dict(O2=v3(o2), B=v3([19.1, *mo.B]), R0=v3([19.1, *mo.R0]), barre=round(mo.barre, 3),
                    theta0=round(mo.theta0, 3), levier=round(mo.levier, 3), bras=mo.bras, tige=mo.tige,
                    degParUnite=mo.K, calage=round(mo.calage, 3), sens=mo.sens, branche=mo.branche,
                    xBras=round(float(t3([X_BRAS_MONTER, 0, 0])[0]), 3), xLevier=round(float(t3([X_LEVIER, 0, 0])[0]), 3)),
        oeil={c: dict(S=v3(oeil[c]['S'] + [0, oeil[c]['yh'], 0]), P=v3(oeil[c]['P'] + [0, oeil[c]['yb'], 0]),
                      D=v3(oeil[c]['D'] + [0, oeil[c]['yb'], 0])) for c in oeil},
        pignon=dict(rapport=rapport, dents=list(DENTS_INCLINAISON)),
        meca=MECA, arbre=v3(O_ref), inclinaison=v3(PT))
    ecrire_glb(os.path.join(sortie, 'robot-walle.glb'), geo, pivot, cine)
    return dict(lacet=lacet, tangage=tangage, O_ref=O_ref, oeil=oeil, geo=geo, cine=cine)


def ecrire_glb(chemin, geo, pivot, cine):
    ordre = list(PARENT); num = {n: i for i, n in enumerate(ordre)}
    binaire, vues, accs, meshes, nodes = bytearray(), [], [], [], []

    def ajoute(data, cible):
        while len(binaire) % 4: binaire.append(0)
        vues.append(dict(buffer=0, byteOffset=len(binaire), byteLength=len(data), target=cible)); binaire.extend(data)
        return len(vues) - 1
    for n in ordre:
        pos, f, _ = geo[n]
        accs.append(dict(bufferView=ajoute(pos.tobytes(), 34962), componentType=5126, count=len(pos), type='VEC3',
                         min=pos.min(0).tolist(), max=pos.max(0).tolist()))
        ind, ct = (f.astype(np.uint16), 5123) if len(pos) < 65536 else (f.astype(np.uint32), 5125)
        accs.append(dict(bufferView=ajoute(ind.tobytes(), 34963), componentType=ct, count=int(f.size), type='SCALAR'))
        meshes.append(dict(name=n, primitives=[dict(attributes=dict(POSITION=len(accs) - 2), indices=len(accs) - 1, material=MAT(n))]))
        loc = t3(pivot[n]) - (t3(pivot[PARENT[n]]) if PARENT[n] else 0)
        enf = [num[m] for m in ordre if PARENT[m] == n]
        nodes.append(dict(name=n, mesh=num[n], translation=[float(x) for x in loc], **({'children': enf} if enf else {})))
    gl = dict(asset=dict(version='2.0', generator='releve_robot.py'), scene=0,
              scenes=[dict(name='Wall-E', nodes=[num['caisse']], extras=cine)], nodes=nodes, meshes=meshes,
              materials=[dict(name=m, doubleSided=True, pbrMetallicRoughness=dict(baseColorFactor=[*c, 1.0], metallicFactor=me, roughnessFactor=ro))
                         for m, c, me, ro in MATERIAUX],
              accessors=accs, bufferViews=vues, buffers=[dict(byteLength=len(binaire))])
    js = json.dumps(gl, separators=(',', ':')).encode(); js += b' ' * ((4 - len(js) % 4) % 4)
    while len(binaire) % 4: binaire.append(0)
    with open(chemin, 'wb') as h:
        h.write(struct.pack('<III', 0x46546C67, 2, 28 + len(js) + len(binaire)))
        h.write(struct.pack('<II', len(js), 0x4E4F534A)); h.write(js)
        h.write(struct.pack('<II', len(binaire), 0x004E4942)); h.write(binaire)


# ── fiche ────────────────────────────────────────────────────────────────────

def main():
    ap = argparse.ArgumentParser(description=__doc__.split('\n\n')[0])
    ap.add_argument('stl', nargs='?', default=STL_DEFAUT)
    ap.add_argument('--travail', default=os.path.join(tempfile.gettempdir(), 'releve-walle'))
    ap.add_argument('--sortie', default=os.path.dirname(ICI))
    a = ap.parse_args()
    os.makedirs(a.travail, exist_ok=True)
    V, A = extraire(a.stl, a.travail)
    F, L = segmenter(V, a.travail)
    fi = ficher(V, A, L, a.travail)
    print('%d triangles, %d pièces' % (len(L), L.max() + 1))
    R = Releve(V, F, A, L)
    r = relever(R)
    mo = Monter(r)
    nd = affecter(R, fi, r)
    m = construire(R, fi, r, mo, nd, a.sortie)
    # L'éditeur d'animation garde sa propre copie : Vite ne sert rien hors de robot-webapp-v2, et la
    # maquette y importe le GLB comme une ressource.
    editeur = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(ICI))),
                           'robot-webapp-v2', 'src', 'features', 'animation', 'maquette')
    if os.path.isdir(editeur):
        shutil.copyfile(os.path.join(a.sortie, 'robot-walle.glb'), os.path.join(editeur, 'robot-walle.glb'))

    print('\nTÊTE')
    print('  arbre des yeux %.3f mm au-dessus de l inclinaison, à %.3f mm de l axe du panoramique' % (
        m['O_ref'][2] - r['inclinaison'][2], abs(m['O_ref'][0] - r['panoramique'][0])))
    print('  panoramique à %.2f mm à droite du plan de symétrie' % (CENTRE[0] - r['panoramique'][0]))
    e = r['pignon'] - r['inclinaison']
    print('  inclinaison : roue %d dents, pignon %d, entraxe %.3f mm' % (*DENTS_INCLINAISON, np.linalg.norm(e - (e @ r['ex']) * r['ex'])))
    for c in ('droit', 'gauche'):
        q = r['yeux'][c]; s = -1 if c == 'droit' else 1
        print('  œil %-6s |OS| %.3f  bras %.3f  bielle %.3f  D (%.3f ; %.3f)  retour au neutre %.3f mm' % (
            c, np.linalg.norm(q['S']), np.linalg.norm(q['P'] - q['S']), np.linalg.norm(q['P'] - q['D']), s * q['D'][0], q['D'][1], m['oeil'][c]['ecart']))
    print('\nMONTER / DESCENDRE')
    print('  pivots bas Z %.3f (Y %s), hauts Z %.3f ; barres %.3f ; levier %.3f' % (
        mo.bas[0][1], ' / '.join('%.3f' % b[0] for b in mo.bas), mo.haut[0][1], mo.barre, mo.levier))
    print('  servo à %.3f mm devant et %.3f sous le pivot du levier ; bras %.1f, tige %.1f (robot)' % (
        mo.B[0] - mo.O2[0], mo.B[1] - mo.O2[1], mo.bras, mo.tige))
    print('  calage du bras au repos (0) %.1f°, sens %+d, %.3f °/unité ; écart à la mesure sur %d..%d : %.2f mm rms, %.2f max' % (
        mo.calage, mo.sens, mo.K, *PLAGE_CALAGE, mo.rms, mo.max))
    print('  bouts de course (butées) : %s' % {k: mo.ecarts[k] for k in (-10, -8, 52, 56, 60)})
    print('  export à la valeur %.1f (barres %.2f°)' % (mo.v_export, mo.theta0))
    print('  valeur    barre     montée du chariot / recul (par rapport au repos)')
    t0 = mo.chariot(0)
    for v in (-10, 0, 10, 20, 30, 40, 50, 60):
        du, dv = mo.chariot(v)
        print('  %+5d    %7.2f°    %+6.1f / %+6.1f mm' % (v, float(mo.barre_([v])[0][0]), dv - t0[1], du - t0[0]))
    print('\nÉPAULES  gauche Z %.3f   droite Z %.3f   (Y %.3f)' % (r['epaules']['bras_gauche'][1], r['epaules']['bras_droit'][1], r['epaules']['bras_gauche'][0]))
    print('\nEXPORT', m['cine']['export'])
    print('\nMODÈLE')
    tot = 0
    for n, (pos, f, n0) in m['geo'].items():
        tot += len(f); print('  %-22s %9d → %7d triangles (pas %.1f)' % (n, n0, len(f), PAS(n)))
    print('  total %d triangles, %s' % (tot, os.path.join(a.sortie, 'robot-walle.glb')))

    cotes = dict(source=os.path.basename(a.stl), cinematique=m['cine'],
                 stl=dict(arbre=r['arbre'].round(3).tolist(), dir_arbre=r['ey'].round(5).tolist(),
                          inclinaison=r['inclinaison'].round(3).tolist(), dir_inclinaison=r['ex'].round(5).tolist(),
                          panoramique_xy=r['panoramique'][:2].round(3).tolist(),
                          yeux={c: {k: v.round(3).tolist() for k, v in q.items()} for c, q in r['yeux'].items()},
                          monter=dict(O2=mo.O2.round(3).tolist(), R0=mo.R0.round(3).tolist(),
                                      bas=[b.round(3).tolist() for b in mo.bas], haut=[h.round(3).tolist() for h in mo.haut]),
                          epaules={k: v.round(3).tolist() for k, v in r['epaules'].items()}),
                 monter=dict(bras=mo.bras, tige=mo.tige, levier=round(mo.levier, 3), barre=round(mo.barre, 3),
                             deg_par_unite=mo.K, calage=round(mo.calage, 2), sens=mo.sens, origine_mesure=round(mo.origine, 2),
                             ecart_rms=round(mo.rms, 2), ecarts=mo.ecarts,
                             table=[dict(valeur=v, barre=round(float(mo.barre_([v])[0][0]), 2),
                                         montee=round(mo.chariot(v)[1] - t0[1], 2), recul=round(mo.chariot(v)[0] - t0[0], 2))
                                    for v in range(-10, 61, 5)]))
    with open(os.path.join(ICI, 'cotes-robot.json'), 'w') as h:
        json.dump(cotes, h, indent=1, ensure_ascii=False)


if __name__ == '__main__':
    main()
