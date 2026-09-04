#!/usr/bin/env python3
"""Relevé des cotes des yeux de Wall-E dans « Wall-E Eye.stl ».

Le STL fait 130 Mo et 2 609 248 triangles : il n'est jamais chargé en entier
ailleurs qu'ici, et jamais recopié. Tout se calcule en local, en mémoire mappée.

    python3 releve_stl.py                  # relevé complet, imprime la fiche de cotes
    python3 releve_stl.py --vues           # en plus, écrit les rendus PNG de contrôle

Les résultats sont figés dans MESURES.md et dans ../yeux-walle.html ; ce script
sert à les refaire si le modèle Onshape bouge, ou à relever autre chose.

Repère du STL : X = gauche/droite, Y = profondeur (l'avant est vers -Y),
Z = haut/bas. Tous les axes d'articulation sont donc parallèles à Y.
"""

import argparse
import json
import os
import sys

import numpy as np
from scipy.sparse import coo_matrix
from scipy.sparse.csgraph import connected_components
from scipy.ndimage import binary_closing, binary_fill_holes

R2D = 180 / np.pi
D2R = np.pi / 180
ICI = os.path.dirname(os.path.abspath(__file__))
STL = os.path.join(ICI, '..', 'Wall-E Eye.stl')
CACHE = os.path.join(ICI, '.cache.npz')


# --------------------------------------------------------------------------
# Lecture du STL
# --------------------------------------------------------------------------

def charger(chemin=STL):
    """(normales, sommets) en float32, sans recopier le fichier en mémoire."""
    n = (os.path.getsize(chemin) - 84) // 50
    brut = np.memmap(chemin, dtype=np.uint8, mode='r', offset=84, shape=(n, 50))
    f = brut[:, :48].copy().view(np.float32).reshape(n, 4, 3)
    return f[:, 0, :], f[:, 1:, :]


def sommets_soudes(tri):
    """Indice de sommet unique par point, au micron près."""
    v = tri.reshape(-1, 3).astype(np.float64)
    q = np.rint(v * 1000.0).astype(np.int64)
    q -= q.min(0)
    cle = (q[:, 0] << 42) | (q[:, 1] << 21) | q[:, 2]
    _, idx = np.unique(cle, return_inverse=True)
    return idx.reshape(-1, 3)


def pieces(tri):
    """Segmente en pièces : deux triangles sont liés s'ils partagent une ARÊTE.

    Le critère « sommet commun » ne suffit pas : l'export soude les pièces qui
    se touchent à plat, et tout l'assemblage retombe en deux gros blocs.
    """
    idx = sommets_soudes(tri)
    n = len(tri)
    a = np.concatenate([idx[:, [0, 1]], idx[:, [1, 2]], idx[:, [2, 0]]])
    a.sort(axis=1)
    cle = a[:, 0].astype(np.int64) * (idx.max() + 1) + a[:, 1]
    o = np.argsort(cle, kind='stable')
    ct, ti = cle[o], np.tile(np.arange(n), 3)[o]
    eq = ct[1:] == ct[:-1]
    g = coo_matrix((np.ones(eq.sum(), np.int8), (ti[:-1][eq], ti[1:][eq])), shape=(n, n))
    _, lab = connected_components(g, directed=False)
    return lab


# --------------------------------------------------------------------------
# Les axes : tous les cylindres parallèles à Y
# --------------------------------------------------------------------------

def aires(tri):
    a, b = tri[:, 1] - tri[:, 0], tri[:, 2] - tri[:, 0]
    return 0.5 * np.linalg.norm(np.cross(a, b), axis=1)


def cylindres_y(tri, nrm, seuil_n=0.05, mini_tri=12, tolerance=0.35):
    """Trouve les cylindres d'axe parallèle à Y.

    Astuce qui rend le calcul linéaire : sur un cercle, la projection du point
    sur sa propre normale vaut `c·n + r`, donc p·n = c·n + r est linéaire en
    (cx, cz, r) — un moindre carré à trois inconnues par groupe de triangles.
    Les groupes sortent de la connexité par arête à l'intérieur de la sélection.
    """
    n = nrm / (np.linalg.norm(nrm, axis=1, keepdims=True) + 1e-12)
    ids = np.flatnonzero(np.abs(n[:, 1]) < seuil_n)
    if len(ids) == 0:
        return []
    idx = sommets_soudes(tri)[ids]
    m = len(ids)
    a = np.concatenate([idx[:, [0, 1]], idx[:, [1, 2]], idx[:, [2, 0]]])
    a.sort(axis=1)
    cle = a[:, 0].astype(np.int64) * (idx.max() + 1) + a[:, 1]
    o = np.argsort(cle, kind='stable')
    ct, ti = cle[o], np.tile(np.arange(m), 3)[o]
    eq = ct[1:] == ct[:-1]
    g = coo_matrix((np.ones(eq.sum(), np.int8), (ti[:-1][eq], ti[1:][eq])), shape=(m, m))
    nb, lab = connected_components(g, directed=False)

    cen, nn, ar = tri[ids].mean(1), n[ids], aires(tri)[ids]
    res = []
    for l in range(nb):
        k = lab == l
        if k.sum() < mini_tri:
            continue
        p, q = cen[k], nn[k]
        A = np.column_stack([q[:, 0], q[:, 2], np.ones(k.sum())])
        b = p[:, 0] * q[:, 0] + p[:, 2] * q[:, 2]
        cx, cz, r = np.linalg.lstsq(A, b, rcond=None)[0]
        err = np.abs(np.hypot(p[:, 0] - cx, p[:, 2] - cz) - abs(r)).max()
        if err > tolerance or abs(r) < 0.5:
            continue
        v = tri[ids[k]].reshape(-1, 3)
        dedans = np.mean((p[:, 0] - cx) * q[:, 0] + (p[:, 2] - cz) * q[:, 2])
        res.append(dict(cx=cx, cz=cz, r=abs(r), ymin=v[:, 1].min(), ymax=v[:, 1].max(),
                        aire=ar[k].sum(), ntri=int(k.sum()), err=err,
                        genre='arbre' if dedans > 0 else 'trou'))
    return res


def axe_le_plus_net(res, x, z, rayon=2.5):
    """Le cylindre le mieux ajusté autour d'une position approchée."""
    proches = [d for d in res if abs(d['cx'] - x) < rayon and abs(d['cz'] - z) < rayon]
    if not proches:
        return None
    return max(proches, key=lambda d: d['aire'])


# --------------------------------------------------------------------------
# Nuage de points surfacique (pour les silhouettes et les rendus)
# --------------------------------------------------------------------------

def semer(tri, n=8_000_000, graine=11):
    """Points tirés uniformément sur la surface, proportionnellement à l'aire.

    Le centre des triangles ne marche pas : une vis compte 30 000 triangles
    minuscules et une coque en compte quelques centaines d'énormes, le nuage
    ne montre alors que la visserie.
    """
    rng = np.random.default_rng(graine)
    a = aires(tri).astype(np.float64)
    idx = rng.choice(len(tri), size=n, p=a / a.sum())
    u, v = rng.random((n, 1)), rng.random((n, 1))
    deborde = (u + v) > 1
    u[deborde], v[deborde] = 1 - u[deborde], 1 - v[deborde]
    t = tri[idx]
    return t[:, 0] + u * (t[:, 1] - t[:, 0]) + v * (t[:, 2] - t[:, 0]), idx


# --------------------------------------------------------------------------
# Contour de la coque
# --------------------------------------------------------------------------

def silhouette(pts_xz, pas=0.2):
    x0, z0 = pts_xz[:, 0].min() - 2, pts_xz[:, 1].min() - 2
    w = int((pts_xz[:, 0].max() + 2 - x0) / pas) + 1
    h = int((pts_xz[:, 1].max() + 2 - z0) / pas) + 1
    g = np.zeros((h, w), bool)
    g[((pts_xz[:, 1] - z0) / pas).astype(int), ((pts_xz[:, 0] - x0) / pas).astype(int)] = True
    return binary_fill_holes(binary_closing(g, np.ones((5, 5)))), x0, z0, pas


def contour_radial(g, x0, z0, pas, cx, cz, n=720):
    """Contour vu depuis (cx, cz). La coque est étoilée depuis le centre de la
    lentille — vérifié : l'aire du polygone colle à 0,2 % près à celle du raster."""
    h, w = g.shape
    sortie = []
    for ang in np.linspace(0, 2 * np.pi, n, endpoint=False):
        ca, sa = np.cos(ang), np.sin(ang)
        r = np.arange(0, 200, 0.1)
        ix = ((cx + r * ca - x0) / pas).astype(int)
        iz = ((cz + r * sa - z0) / pas).astype(int)
        ok = (ix >= 0) & (ix < w) & (iz >= 0) & (iz < h)
        v = np.zeros(len(r), bool)
        v[ok] = g[iz[ok], ix[ok]]
        trou, arret = 0, len(r) - 1
        for i in range(len(v)):
            if v[i]:
                trou = 0
            else:
                trou += 1
                if trou * 0.1 > 1.0:       # 1 mm de vide : on est sorti
                    arret = i - trou
                    break
        sortie.append((cx + r[arret] * ca, cz + r[arret] * sa))
    return np.array(sortie)


def simplifier(pts, tol=0.15):
    """Douglas-Peucker, pour passer de 720 points à ~86 sans perdre 0,15 mm."""
    def rec(a, b):
        if b <= a + 1:
            return []
        p0, v = pts[a], pts[b] - pts[a]
        L = np.hypot(*v)
        if L < 1e-9:
            d = np.hypot(*(pts[a + 1:b] - p0).T)
        else:
            d = np.abs(np.cross(np.tile(v, (b - a - 1, 1)), pts[a + 1:b] - p0)) / L
        i = int(np.argmax(d))
        if d[i] < tol:
            return []
        k = a + 1 + i
        return rec(a, k) + [k] + rec(k, b)
    return pts[[0] + rec(0, len(pts) - 1) + [len(pts) - 1]]


# --------------------------------------------------------------------------
# Le quadrilatère servo → œil
# --------------------------------------------------------------------------

class Transmission:
    """Fermeture du quadrilatère O–S–P–D.

    O = axe de rotation des coques (origine), D = ancrage de bielle sur la
    platine FIXE, S = axe du servo PORTÉ PAR LA COQUE, P = rotule bras/bielle.
    Le servo impose l'angle du bras dans le repère de la coque ; l'angle de la
    coque est ce qui reste pour que la bielle garde sa longueur.
    """

    def __init__(self, S, D, bras, bielle, psi0):
        self.S, self.D = np.asarray(S, float), np.asarray(D, float)
        self.bras, self.bielle, self.psi0 = bras, bielle, psi0
        self.d = float(np.hypot(*self.D))
        self.delta = float(np.arctan2(self.D[1], self.D[0]))

    def oeil(self, servo_deg):
        a = (self.psi0 + servo_deg) * D2R
        Q = self.S + self.bras * np.array([np.cos(a), np.sin(a)])
        q, beta = np.hypot(*Q), np.arctan2(Q[1], Q[0])
        k = (q * q + self.d * self.d - self.bielle ** 2) / (2 * q * self.d)
        if abs(k) > 1:
            return float('nan')          # point mort dépassé : le bras ne peut plus fermer
        return (self.delta - beta + np.arccos(k)) * R2D

    def gain(self, servo_deg, h=1e-4):
        return (self.oeil(servo_deg + h) - self.oeil(servo_deg - h)) / (2 * h)

    def servo(self, oeil_deg, lo=-90.0, hi=22.0):
        """Réciproque, par dichotomie : la loi est strictement décroissante."""
        for _ in range(80):
            m = (lo + hi) / 2
            if self.oeil(m) > oeil_deg:
                lo = m
            else:
                hi = m
        return (lo + hi) / 2

    def point_mort(self, lo=0.0, hi=40.0):
        for _ in range(80):
            m = (lo + hi) / 2
            if np.isnan(self.oeil(m)):
                hi = m
            else:
                lo = m
        return lo


def butee(contour, sens, un_seul=False):
    """Angle où les deux coques se touchent (sens=+1 : bord extérieur en haut)."""
    from matplotlib.path import Path

    def tourne(Q, t):
        ct, st = np.cos(t), np.sin(t)
        return np.column_stack([ct * Q[:, 0] - st * Q[:, 1], st * Q[:, 0] + ct * Q[:, 1]])

    def touche(a, b):
        A = tourne(contour, a * D2R)
        B = tourne(contour, b * D2R)
        B[:, 0] *= -1
        return Path(A).contains_points(B).any() or Path(B).contains_points(A).any()

    lo, hi = 0.0, 60.0
    for _ in range(50):
        m = (lo + hi) / 2
        if touche(sens * m, 0 if un_seul else sens * m):
            hi = m
        else:
            lo = m
    return sens * lo


# --------------------------------------------------------------------------
# Relevé complet
# --------------------------------------------------------------------------

def relever(chemin=STL, cache=True):
    if cache and os.path.exists(CACHE):
        z = np.load(CACHE)
        lab, pts = z['lab'], z['pts']
        nrm, tri = charger(chemin)
    else:
        nrm, tri = charger(chemin)
        print(f'{len(tri)} triangles', file=sys.stderr)
        lab = pieces(tri)
        pts, _ = semer(tri)
        if cache:
            np.savez_compressed(CACHE, lab=lab, pts=pts.astype(np.float32))
    return nrm, tri, lab, pts


def fiche(chemin=STL, vues=False):
    nrm, tri, lab, pts = relever(chemin)
    cyl = cylindres_y(tri, nrm)
    m = {}

    # --- les axes, repérés par leur position approchée puis réajustés ---------
    reperes = {
        'arbre':      (-32.2, 30.4),   # l'arbre Ø8 commun aux deux coques
        'lentilleG':  (-97.8, 23.6), 'lentilleD':  (33.5, 25.1),
        'ancrageG':   (-45.1,  4.3), 'ancrageD':  (-19.1,  4.4),
        'servoG':    (-118.2, 13.3), 'servoD':     (54.1, 15.3),
        'rotuleG':   (-127.0,-17.4), 'rotuleD':    (63.3,-15.4),
    }
    axes = {}
    for nom, (x, z) in reperes.items():
        d = axe_le_plus_net(cyl, x, z)
        if d is None:
            raise SystemExit(f'axe « {nom} » introuvable — le modèle a bougé ?')
        axes[nom] = np.array([d['cx'], d['cz']])

    O = axes['arbre']
    # Le STL est exporté incliné : le plan de symétrie passe par l'arbre et par
    # le milieu des deux ancrages de bielle (les seuls points sûrement fixes).
    milieu = (axes['ancrageG'] + axes['ancrageD']) / 2
    u = O - milieu
    alpha = float(np.arctan2(-u[0], u[1]))
    c, s = np.cos(-alpha), np.sin(-alpha)
    R = np.array([[c, -s], [s, c]])
    loc = lambda p: R @ (np.asarray(p) - O)     # noqa: E731
    mir = lambda p: np.array([-p[0], p[1]])     # noqa: E731
    m['inclinaison_stl_deg'] = alpha * R2D

    # --- les deux yeux ne sont pas au même angle dans l'export ---------------
    ang = lambda p: np.arctan2(p[1], p[0]) * R2D      # noqa: E731
    ecart = ang(mir(loc(axes['lentilleG']))) - ang(loc(axes['lentilleD']))
    m['ecart_pose_entre_yeux_deg'] = -ecart
    demi = -ecart / 2

    def canon(g, d):
        """Moyenne des deux côtés, ramenée sur l'œil droit et remise au neutre."""
        a = tourne_pt(mir(loc(g)), +demi * D2R)
        b = tourne_pt(loc(d), -demi * D2R)
        return (a + b) / 2

    m['lentille'] = canon(axes['lentilleG'], axes['lentilleD'])
    m['ancrage']  = canon(axes['ancrageG'],  axes['ancrageD'])
    m['servo']    = canon(axes['servoG'],    axes['servoD'])
    m['rotule']   = canon(axes['rotuleG'],   axes['rotuleD'])
    for nom in ('lentille', 'ancrage', 'servo', 'rotule'):
        m[nom + '_rayon'] = float(np.hypot(*m[nom]))

    m['bras']   = float(np.hypot(*(m['rotule'] - m['servo'])))
    m['bielle'] = float(np.hypot(*(m['rotule'] - m['ancrage'])))
    m['bati']   = m['ancrage_rayon']
    m['psi0']   = float(np.arctan2(*(m['rotule'] - m['servo'])[::-1]) * R2D)

    # --- rayons du bloc optique ---------------------------------------------
    m['cercles_lentille'] = sorted({round(2 * d['r'], 3) for d in cyl
                                    if abs(d['cx'] - axes['lentilleD'][0]) < 1.5
                                    and abs(d['cz'] - axes['lentilleD'][1]) < 1.5
                                    and d['aire'] > 200}, reverse=True)

    # --- le contour de la coque ---------------------------------------------
    xz = np.column_stack([pts[:, 0], pts[:, 2]])
    xz = (R @ (xz - O).T).T
    avant = pts[:, 1] < -137                       # au-delà, c'est le bâti
    dr, ga = avant & (pts[:, 0] > O[0]), avant & (pts[:, 0] < O[0])
    A = tourne_pts(xz[dr], -demi * D2R)
    B = tourne_pts(np.column_stack([-xz[ga][:, 0], xz[ga][:, 1]]), +demi * D2R)
    nuage = np.vstack([A, B])
    g, x0, z0, pas = silhouette(nuage)
    contour = contour_radial(g, x0, z0, pas, *m['lentille'])
    m['contour'] = contour
    m['contour_simplifie'] = simplifier(contour)
    m['coque_largeur'] = float(contour[:, 0].max() - contour[:, 0].min())
    m['coque_hauteur'] = float(contour[:, 1].max() - contour[:, 1].min())
    m['bord_interne']  = float(contour[:, 0].min())
    m['jeu_entre_coques'] = 2 * m['bord_interne']
    m['dessus'] = float(contour[:, 1].max())
    ycoque = pts[(pts[:, 0] > O[0]) & (pts[:, 1] < -100), 1]
    m['coque_avant'] = float(ycoque.min())
    m['coque_profondeur'] = -137.0 - m['coque_avant']

    # --- la loi de transmission ---------------------------------------------
    t = Transmission(m['servo'], m['ancrage'], m['bras'], m['bielle'], m['psi0'])
    m['transmission'] = t
    m['gain_neutre'] = t.gain(0)
    m['point_mort_servo'] = t.point_mort()
    m['butee_haut'] = butee(contour, +1)
    m['butee_bas']  = butee(contour, -1)
    m['butee_haut_un_oeil'] = butee(contour, +1, un_seul=True)
    m['servo_min'] = t.servo(m['butee_haut'])
    m['servo_max'] = t.servo(m['butee_bas'])

    if vues:
        rendre(pts, m)
    return m


def tourne_pt(p, t):
    ct, st = np.cos(t), np.sin(t)
    return np.array([ct * p[0] - st * p[1], st * p[0] + ct * p[1]])


def tourne_pts(P, t):
    ct, st = np.cos(t), np.sin(t)
    return np.column_stack([ct * P[:, 0] - st * P[:, 1], st * P[:, 0] + ct * P[:, 1]])


def rendre(pts, m):
    import matplotlib
    matplotlib.use('Agg')
    import matplotlib.pyplot as plt
    fig, axs = plt.subplots(1, 3, figsize=(26, 8))
    for ax, (i, j, nom) in zip(axs, [(0, 2, 'face X-Z'), (0, 1, 'dessus X-Y'), (1, 2, 'côté Y-Z')]):
        ax.scatter(pts[:, i], pts[:, j], s=0.03, alpha=.2, linewidths=0)
        ax.set_aspect('equal'); ax.grid(alpha=.3); ax.set_title(nom)
    plt.tight_layout(); plt.savefig(os.path.join(ICI, 'vues.png')); plt.close()

    C = m['contour']
    plt.figure(figsize=(12, 6))
    for sens, style in ((1, '-'), (-1, '--')):
        plt.plot(sens * C[:, 0], C[:, 1], style, lw=1)
    plt.plot(*m['lentille'], 'ko'); plt.plot(0, 0, 'r+', ms=16, mew=2)
    plt.gca().set_aspect('equal'); plt.grid(alpha=.3)
    plt.title('contour canonique, origine = axe de rotation')
    plt.savefig(os.path.join(ICI, 'contour.png'), bbox_inches='tight'); plt.close()
    print('vues.png et contour.png écrits', file=sys.stderr)


def imprimer(m):
    t = m['transmission']
    print(f"""
=== COQUE ===
  contour                {m['coque_largeur']:.2f} x {m['coque_hauteur']:.2f} mm,
                         profondeur {m['coque_profondeur']:.2f} mm (section constante)
  bord interne à         {m['bord_interne']:.2f} mm de l'axe → jeu {m['jeu_entre_coques']:.2f} mm
  dessus à               {m['dessus']:.2f} mm au-dessus de l'axe
  lentille               ({m['lentille'][0]:.3f} ; {m['lentille'][1]:.3f}), à {m['lentille_rayon']:.3f} mm de l'axe
  cercles concentriques  Ø{' Ø'.join(f'{d:.3f}' for d in m['cercles_lentille'])}

=== TRINGLERIE (repère œil, origine = axe de rotation) ===
  D ancrage bielle, FIXE      ({m['ancrage'][0]:8.3f} ; {m['ancrage'][1]:8.3f})   |OD| = {m['bati']:.3f}
  S axe servo, SUR LA COQUE   ({m['servo'][0]:8.3f} ; {m['servo'][1]:8.3f})   |OS| = {m['servo_rayon']:.3f}
  P rotule bras/bielle        ({m['rotule'][0]:8.3f} ; {m['rotule'][1]:8.3f})
  bras de servo {m['bras']:.3f}   bielle {m['bielle']:.3f}   psi0 {m['psi0']:.3f}°

=== TRANSMISSION ===""")
    print(f"  {'servo':>8} {'oeil':>9} {'gain':>8}")
    for s in (-5, 0, 5, 10, 15, 20, 21.3):
        print(f'  {s:8.1f} {t.oeil(s):9.3f} {t.gain(s):8.3f}')
    print(f"""  point mort du quadrilatère à {m['point_mort_servo']:.2f}° de servo

=== BUTÉES (contact entre les deux coques) ===
  bord extérieur en haut   {m['butee_haut']:+.2f}° d'oeil  (servo {m['servo_min']:+.2f}°)
  bord extérieur en bas    {m['butee_bas']:+.2f}° d'oeil  (servo {m['servo_max']:+.2f}°)
  un seul oeil, vers le haut {m['butee_haut_un_oeil']:+.2f}°

=== PIÈGES DE L'EXPORT ===
  modèle incliné de {m['inclinaison_stl_deg']:+.4f}° autour de l'axe de profondeur
  les deux yeux sont à {m['ecart_pose_entre_yeux_deg']:+.3f}° l'un de l'autre (redressé et moyenné ci-dessus)
""")


def ecrire_json(m):
    chemin = os.path.join(ICI, 'cotes.json')
    t = m['transmission']
    json.dump({
        'repere': "origine sur l'axe de rotation des coques, x vers l'exterieur, "
                  "z vers le haut, y en profondeur (avant vers -y)",
        'coque': {'largeur': m['coque_largeur'], 'hauteur': m['coque_hauteur'],
                  'profondeur': m['coque_profondeur'], 'avant_y': m['coque_avant'],
                  'bord_interne': m['bord_interne'], 'jeu': m['jeu_entre_coques'],
                  'dessus': m['dessus']},
        'lentille': {'centre': list(map(float, m['lentille'])),
                     'rayon_depuis_axe': m['lentille_rayon'],
                     'cercles': m['cercles_lentille']},
        'tringlerie': {'ancrage': list(map(float, m['ancrage'])),
                       'servo': list(map(float, m['servo'])),
                       'rotule': list(map(float, m['rotule'])),
                       'bras': m['bras'], 'bielle': m['bielle'], 'bati': m['bati'],
                       'psi0_deg': m['psi0']},
        'transmission': {'gain_neutre': m['gain_neutre'],
                         'point_mort_servo': m['point_mort_servo'],
                         'servo_min': m['servo_min'], 'servo_max': m['servo_max'],
                         'oeil_max': m['butee_haut'], 'oeil_min': m['butee_bas'],
                         'table': [[float(s), t.oeil(s), t.gain(s)]
                                   for s in np.arange(-6, 21.5, 1.0)]},
        'export': {'inclinaison_deg': m['inclinaison_stl_deg'],
                   'ecart_entre_yeux_deg': m['ecart_pose_entre_yeux_deg']},
        'contour': [[round(float(x), 2), round(float(z), 2)] for x, z in m['contour_simplifie']],
    }, open(chemin, 'w'), indent=1, ensure_ascii=False)
    print(f'{chemin} écrit ({len(m["contour_simplifie"])} points de contour)', file=sys.stderr)


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('stl', nargs='?', default=STL)
    p.add_argument('--vues', action='store_true', help='écrit les rendus PNG de contrôle')
    p.add_argument('--sans-cache', action='store_true')
    args = p.parse_args()
    if args.sans_cache and os.path.exists(CACHE):
        os.remove(CACHE)
    if not os.path.exists(args.stl):
        raise SystemExit(f'{args.stl} introuvable — le STL est hors dépôt (130 Mo).')
    mesures = fiche(args.stl, vues=args.vues)
    imprimer(mesures)
    ecrire_json(mesures)
