#!/usr/bin/env python3
"""
Cherche toutes les façons de relier deux pièces du caisson avec de la visserie goBILDA, sans rien percer :

  - taraudage d'un bloc en face d'un trou de passage d'une tôle (4 mm, ou 14 mm avec un réducteur
    2904-0004-0014) : une vis, éventuellement une entretoise 1501 si les deux ne se touchent pas ;
  - deux trous de passage en face l'un de l'autre : une vis et un écrou, ou une entretoise taraudée
    1501 et deux vis quand il y a du vide entre les deux (le cas des deux profilés, à 43 mm).

On suit l'axe de chaque trou : on cherche la première pièce rencontrée, on vérifie que l'axe y entre par
un trou et non dans la matière, et que la colonne d'une entretoise (Ø6) traverse le vide sans rien
toucher, pièces de la v5 comprises, et l'enveloppe de ce qui tourne autour de l'axe du tube.
Écrit fixations.json sur le disque externe.
"""
import json, os, sys
import numpy as np, trimesh
from scipy.spatial import cKDTree

ICI = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, ICI)
from recaler import SORTIE, pieces_du_bras
from construire import maillage
from coincidences import trous, faces

TARAUDE = (3.0, 3.75)
PASSAGE = (3.9, 4.9)
GRAND = (13.6, 14.6)                     # trou de 14 mm, avec le réducteur 2904-0004-0014
ENTRETOISES = [4, 5, 6, 7, 8, 9, 10, 11, 12, 14, 16, 18, 19, 20, 22, 24, 27, 30, 32, 36, 40, 42, 43, 44, 46, 48]
R_COLONNE = 3.3                          # entretoise Ø6 et un peu de jeu
# Ce qui tourne autour de l'axe du tube (X) : liaison, micro-servo et moyeu de la v5 balaient ce rayon.
ENVELOPPE_TUBE = (120.0, 172.0, 19.5)    # de X, à X, rayon
STRUCTURE = ('1121-', '1123-', '1108-', '1604-', '1802-', '1201-')


def echantillons(m, densite=3.0):
    n = int(min(400_000, max(2_000, m.area * densite)))
    p, _ = trimesh.sample.sample_surface(m, n, seed=2)
    return cKDTree(p)


def main():
    a = json.load(open(os.path.join(SORTIE, 'assemblage.json')))
    v5 = json.load(open(os.path.join(SORTIE, 'v5.json')))
    masquees = {n for l in v5['masquer'].values() for n in l}
    pieces = pieces_du_bras()
    U = {u['nom']: u for u in a['unites'] if u['nom'] not in masquees}
    M = {n: maillage(u, pieces) for n, u in U.items()}
    s5 = trimesh.load(os.path.join(SORTIE, 'bras-v5.glb'), force='scene')
    for k, p in enumerate(v5['pieces']):
        T, g = s5.graph['v5-%d' % k]; m = s5.geometry[g].copy(); m.apply_transform(T); M['v5:' + p['nom']] = m
    arbres = {n: echantillons(m) for n, m in M.items()}
    structure = [n for n in M if n.startswith(STRUCTURE)]
    print(len(M), 'pièces, dont', len(structure), 'de structure', flush=True)

    def obstacle(p0, d, L, sauf, r):
        """Première pièce (hors « sauf ») qui passe à moins de r de l'axe sur [0, L], ou None."""
        t = np.arange(0.3, L - 0.3, 0.4)
        if len(t) == 0:
            return None
        pts = p0 + np.outer(t, d)
        x0, x1, re = ENVELOPPE_TUBE
        dans = (pts[:, 0] > x0) & (pts[:, 0] < x1) & (np.hypot(pts[:, 1], pts[:, 2]) < re + r)
        if dans.any():
            return 'enveloppe tournante du tube'
        for n, arbre in arbres.items():
            if n in sauf:
                continue
            lo, hi = M[n].bounds
            if np.any(pts.min(0) > hi + r) or np.any(pts.max(0) < lo - r):
                continue
            if (arbre.query(pts)[0] < r).any():
                return n
        return None

    # tous les trous des pièces de structure, par face de boîte englobante
    trous_par_piece = {}
    for n in structure:
        m = M[n]; liste = []
        for k, s, cote in faces(m):
            nrm = np.zeros(3); nrm[k] = 1
            ep = m.extents[k]
            prof = min(1.5, ep / 2)
            for plage, sorte in ((TARAUDE, 'taraudé'), (PASSAGE, 'passage'), (GRAND, '14 mm')):
                for c in trous(m, nrm * (cote - s * prof), nrm, plage):
                    q = c.copy(); q[k] = cote
                    liste.append(dict(point=q, sens=s * nrm, sorte=sorte, epaisseur=ep))
        trous_par_piece[n] = liste
        print('%-22s %s' % (n, {s: sum(1 for t in liste if t['sorte'] == s) for s in ('taraudé', 'passage', '14 mm')}), flush=True)

    propositions, vues = [], set()
    for n1 in structure:
        for t1 in trous_par_piece[n1]:
            p0, d = t1['point'], t1['sens']
            for n2 in structure:
                if n2 == n1:
                    continue
                for t2 in trous_par_piece[n2]:
                    if t1['sorte'] != 'taraudé' and t2['sorte'] == 'taraudé':
                        continue                         # on part toujours du taraudage
                    v = t2['point'] - p0
                    L = float(v @ d)
                    if L < -0.6 or L > 48.5 or np.linalg.norm(v - L * d) > 1.2 or float(t2['sens'] @ d) > -0.9:
                        continue
                    cle = tuple(sorted((n1, n2))) + (tuple(np.round((p0 + t2['point']) / 2, 0)),)
                    if cle in vues:
                        continue
                    if t1['sorte'] == 'passage' and t2['sorte'] == 'passage' and n1 > n2:
                        continue
                    L = max(L, 0.0)
                    if L < 0.6:
                        entretoise = None
                    else:
                        entretoise = min(ENTRETOISES, key=lambda e: abs(e - L))
                        if abs(entretoise - L) > 0.5:
                            continue
                        gene = obstacle(p0, d, L, {n1, n2}, R_COLONNE)
                        if gene:
                            continue
                    vues.add(cle)
                    propositions.append(dict(de=n1, vers=n2, trou_de=t1['sorte'], trou_vers=t2['sorte'],
                                             point=np.round(p0, 2).tolist(), direction=np.round(d, 3).tolist(),
                                             vide=round(L, 2), entretoise=entretoise))
    json.dump(propositions, open(os.path.join(SORTIE, 'fixations.json'), 'w'), ensure_ascii=False, indent=1)
    from collections import defaultdict
    g = defaultdict(list)
    for p in propositions:
        g[tuple(sorted((p['de'], p['vers'])))].append(p)
    for (n1, n2), l in sorted(g.items()):
        details = ', '.join(sorted({'%s→%s%s' % (p['trou_de'], p['trou_vers'], ' + entretoise %d' % p['entretoise'] if p['entretoise'] else '') for p in l}))
        print('%-22s ↔ %-22s %2d possibilité(s) : %s' % (n1, n2, len(l), details))


if __name__ == '__main__':
    main()
