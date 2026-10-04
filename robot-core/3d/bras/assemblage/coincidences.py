#!/usr/bin/env python3
"""
Cherche où une vis M4 peut relier deux pièces sans rien percer : un trou de passage d'une tôle (profilé,
plaque) en face d'un trou taraudé d'un bloc (plaque-palier, cadre de servo), ou deux trous de passage
en face l'un de l'autre (vis + écrou).

Les trous se lisent sur des coupes : une tôle coupée à mi-épaisseur montre ses trous de passage (Ø4,1 à
Ø4,8) ; un bloc coupé à 1,5 mm sous une face montre ses taraudages (Ø3,0 à Ø3,8).
"""
import json, os, sys
import numpy as np, trimesh
import shapely.geometry as sg

ICI = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, ICI)
from recaler import SORTIE, pieces_du_bras
from construire import maillage

PASSAGE = (3.9, 4.9)          # goBILDA dessine ses trous de passage à 4,0 tout rond
TARAUDE = (3.0, 3.75)
ALIGNE = 1.2                  # recalage à 0,5 mm près ; la trame goBILDA est au pas de 8 mm


def trous(m, origine, normale, plage, oblongs=True):
    """Centres (3D) des trous de diamètre dans la plage, sur la coupe par ce plan.

    Les plaques goBILDA ont aussi des trous oblongs (5,4 × 4 mm sur la ligne médiane des 1123) : écartés
    par erreur jusqu'au 2026-10-03, ils sont justement ceux où se visse le cadre 1802. Un oblong compte
    comme un trou de passage si sa petite largeur tombe dans la plage et la grande ne dépasse pas 6 mm."""
    s = m.section(plane_origin=origine, plane_normal=normale)
    if s is None:
        return []
    out = []
    for boucle in s.discrete:
        if len(boucle) < 8:
            continue
        c = boucle.mean(0)
        r = np.linalg.norm(boucle - c, axis=1)
        if r.std() <= 0.15:
            if plage[0] <= 2 * r.mean() <= plage[1]:
                out.append(c)
        elif oblongs:
            # dimensions dans le plan de coupe
            e = np.ptp(boucle, axis=0); n = np.asarray(normale, float)
            dims = sorted(e[np.abs(n) < 0.5]) if np.count_nonzero(np.abs(n) < 0.5) == 2 else []
            if len(dims) == 2 and plage[0] <= dims[0] <= plage[1] and dims[1] <= 6.0:
                c = (boucle.min(0) + boucle.max(0)) / 2
                out.append(c)
    return out


def faces(m):
    """Les six faces de la boîte englobante : (axe, signe, cote)."""
    lo, hi = m.bounds
    return [(k, s, (hi if s > 0 else lo)[k]) for k in range(3) for s in (1, -1)]


def main():
    a = json.load(open(os.path.join(SORTIE, 'assemblage.json')))
    pieces = pieces_du_bras()
    U = {u['nom']: u for u in a['unites']}
    TOLES = [n for n in U if n.startswith(('1121-', '1123-', '1108-'))]
    BLOCS = [n for n in U if n.startswith(('1604-', '1802-', '1201-'))]
    M = {n: maillage(U[n], pieces) for n in TOLES + BLOCS}
    resultat = []
    for b in BLOCS:
        for k, s, cote in faces(M[b]):
            n = np.zeros(3); n[k] = 1
            taraudes = trous(M[b], n * (cote - s * 1.5), n, TARAUDE)
            if not taraudes:
                continue
            for t in TOLES:
                lo, hi = M[t].bounds
                # la tôle doit être plaquée contre cette face du bloc
                plaquee = abs((lo if s > 0 else hi)[k] - cote) < 0.6
                if not plaquee:
                    continue
                mi = (lo[k] + hi[k]) / 2
                passages = trous(M[t], n * mi, n, PASSAGE)
                for c in taraudes:
                    for p in passages:
                        d = np.delete(c - p, k)
                        if np.linalg.norm(d) < ALIGNE:
                            resultat.append(dict(bloc=b, tole=t, axe='xyz'[k], sens=s,
                                                 point=np.round(c, 2).tolist()))
    # tôles l'une contre l'autre, trous en face : vis + écrou
    for i, t1 in enumerate(TOLES):
        for t2 in TOLES[i + 1:]:
            for k, s, cote in faces(M[t1]):
                lo2, hi2 = M[t2].bounds
                if abs((lo2 if s > 0 else hi2)[k] - cote) > 0.6:
                    continue
                n = np.zeros(3); n[k] = 1
                lo1, hi1 = M[t1].bounds
                p1 = trous(M[t1], n * ((lo1[k] + hi1[k]) / 2), n, PASSAGE)
                p2 = trous(M[t2], n * ((lo2[k] + hi2[k]) / 2), n, PASSAGE)
                for c in p1:
                    for p in p2:
                        if np.linalg.norm(np.delete(c - p, k)) < ALIGNE:
                            resultat.append(dict(bloc=t2, tole=t1, axe='xyz'[k], sens=s, point=np.round(c, 2).tolist(),
                                                 ecrou=True))
    json.dump(resultat, open(os.path.join(SORTIE, 'coincidences.json'), 'w'), ensure_ascii=False, indent=1)
    from collections import Counter
    for (b, t), nb in sorted(Counter((r['bloc'], r['tole']) for r in resultat).items()):
        print('%-20s <- %-20s %d trou(s) en face' % (b, t, nb))


if __name__ == '__main__':
    main()
