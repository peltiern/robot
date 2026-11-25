#!/usr/bin/env python3
"""
Pour chaque vis de l'assemblage : son axe, le côté de sa tête, les pièces que traverse sa tige, dans
l'ordre, et ce qui la retient (écrou ou taraudage). Écrit liaisons.json sur le disque externe.

Une vis passe dans un trou avec du jeu sans en toucher les parois : on ne cherche donc pas le contact,
mais les pièces qui passent à moins d'un rayon de vis (plus le jeu) de son axe.
"""
import json, os, sys
import numpy as np, trimesh

ICI = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, ICI)
from recaler import SORTIE, pieces_du_bras
from construire import maillage

SERRAGE = ('ecrou', 'rondelle', 'entretoise')
JEU = 0.6


def axe_de_vis(m):
    """Axe principal, et sens tête -> pointe : la tête est le bout le plus large."""
    v = m.vertices; c = v.mean(0)
    _, _, vt = np.linalg.svd(v - c, full_matrices=False); a = vt[0]
    t = (v - c) @ a; r = np.linalg.norm((v - c) - np.outer(t, a), axis=1)
    L = t.max() - t.min(); bout0 = r[t < t.min() + L * 0.15].max(); bout1 = r[t > t.max() - L * 0.15].max()
    if bout1 > bout0:
        a, t = -a, -t
    tete = c + a * t.min()
    r_tige = np.percentile(r[t > t.min() + L * 0.4], 90)
    r_tete = r[t < t.min() + L * 0.15].max()
    h_tete = float(np.ptp(t[r > r_tige + 0.5])) if (r > r_tige + 0.5).any() else 0.0
    return tete, a, float(L), float(r_tige), float(r_tete), h_tete


def main():
    a = json.load(open(os.path.join(SORTIE, 'assemblage.json')))
    pieces = pieces_du_bras()
    U = {u['nom']: u for u in a['unites']}
    M = {n: maillage(u, pieces) for n, u in U.items()}
    sortie = []
    for n, u in U.items():
        if u['role'] != 'vis' or n.startswith('BRAS'):
            continue
        tete, d, L, rt, rT, hT = axe_de_vis(M[n])
        # points de la tige, de dessous la tête à la pointe ; la tête elle-même sert à trouver la pièce d'appui
        s = np.linspace(hT + 0.3, L - 0.2, 24)
        pts = tete + np.outer(s, d)
        trav = []
        for k, m in M.items():
            if k == n:
                continue
            lo, hi = m.bounds
            if np.any(pts.min(0) > hi + 3) or np.any(pts.max(0) < lo - 3):
                continue
            _, dist, _ = trimesh.proximity.closest_point(m, pts)
            pres = dist < rt + JEU
            if pres.any():
                trav.append((float(s[pres].min()), float(s[pres].max()), k, U[k]['role']))
        trav.sort()
        tient = [k for _, _, k, r in trav if r == 'ecrou']
        sortie.append(dict(vis=n, sku=u['sku'], tete=np.round(tete, 3).tolist(), direction=np.round(d, 4).tolist(),
                           longueur=round(L, 2), rayon_tige=round(rt, 2), rayon_tete=round(rT, 2), hauteur_tete=round(hT, 2),
                           traverse=[dict(piece=k, role=r, de=round(a0, 1), a=round(a1, 1)) for a0, a1, k, r in trav],
                           retenue='écrou ' + tient[0] if tient else 'taraudage de ' + (trav[-1][2] if trav else '?')))
        print('%-22s %s -> %s' % (n, ' / '.join(k for _, _, k, _ in trav), sortie[-1]['retenue']), flush=True)
    json.dump(sortie, open(os.path.join(SORTIE, 'liaisons.json'), 'w'), ensure_ascii=False, indent=1)


if __name__ == '__main__':
    main()
