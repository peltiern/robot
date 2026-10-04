#!/usr/bin/env python3
"""
Recale chaque modèle du catalogue (STEP goBILDA ou Hitec converti en GLB) sur la pièce de bras.stl qu'il
représente, et écrit l'assemblage : une entrée par pièce, avec sa référence et sa matrice de pose.

    python recaler.py            (avec le venv du disque externe : /media/npeltier/disque_usb/Robot/venv-cao)

Repère du bras : origine au centre de l'épaule (là où se croisent flexion, abduction et axe du tube),
X le long du bras vers le goTUBE, Y = axe de flexion, en mm. L'export de Nicolas penche de 5,1° autour
de X : on le redresse.

Les fichiers lourds vont sur le disque externe : le poste n'a que quelques Go libres.
"""
import json, math, os, pickle, re, sys
import numpy as np, trimesh
from scipy.spatial import cKDTree

ICI = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, ICI)
from nomenclature import UNITES

STL = os.path.join(ICI, '..', 'bras.stl')
DISQUE = '/media/npeltier/disque_usb/Robot'
GLB = DISQUE + '/catalogue_gobilda/modeles_glb'
SORTIE = DISQUE + '/bras'
CACHE = SORTIE + '/pieces_bras.pkl'

INCLINAISON = math.atan2(0.089, 0.996)
CENTRE_EPAULE = np.array([-298.85, 20.82, 8.02])
N_ECH = 3000


def repere_bras():
    R = np.array([[1, 0, 0],
                  [0, math.cos(-INCLINAISON), -math.sin(-INCLINAISON)],
                  [0, math.sin(-INCLINAISON), math.cos(-INCLINAISON)]])
    T = np.eye(4); T[:3, :3] = R; T[:3, 3] = -R @ CENTRE_EPAULE
    return T


def pieces_du_bras():
    if os.path.exists(CACHE):
        return pickle.load(open(CACHE, 'rb'))
    pieces = trimesh.load(STL).split(only_watertight=False)
    T = repere_bras()
    for p in pieces:
        p.apply_transform(T)
    pickle.dump(pieces, open(CACHE, 'wb'))
    return pieces


def modele(sku, filtre):
    """Le GLB du catalogue, en mm, réduit aux corps dont le nom de nœud répond au filtre."""
    s = trimesh.load(os.path.join(GLB, sku + '.glb'), force='scene')
    morceaux = []
    for nom in s.graph.nodes_geometry:
        T, g = s.graph[nom]
        if filtre and not re.search(filtre, nom) and not re.search(filtre, g):
            continue
        m = s.geometry[g].copy(); m.apply_transform(T); morceaux.append(m)
        if filtre and filtre.endswith('$'):
            break                      # un seul corps demandé, même s'il y en a plusieurs semblables
    m = trimesh.util.concatenate(morceaux)
    if m.extents.max() < 2:            # cascadio écrit en mètres
        m.apply_scale(1000)
    return m


def echantillon(m, n=N_ECH):
    p, _ = trimesh.sample.sample_surface_even(m, n, seed=1)
    if len(p) < n // 2:
        p, _ = trimesh.sample.sample_surface(m, n, seed=1)
    return np.asarray(p)


def rotations_de_base():
    """Les 24 rotations qui permutent les axes principaux (signes compris)."""
    out = []
    for perm in ([0, 1, 2], [0, 2, 1], [1, 0, 2], [1, 2, 0], [2, 0, 1], [2, 1, 0]):
        for sx in (1, -1):
            for sy in (1, -1):
                for sz in (1, -1):
                    M = np.zeros((3, 3)); M[0, perm[0]] = sx; M[1, perm[1]] = sy; M[2, perm[2]] = sz
                    if np.linalg.det(M) > 0:
                        out.append(M)
    return out


ROTS = rotations_de_base()


def axes(p):
    c = p.mean(0)
    _, _, vt = np.linalg.svd(p - c, full_matrices=False)
    if np.linalg.det(vt) < 0:
        vt[2] *= -1
    return c, vt


def icp(src, arbre, cible, R, t, iterations=40):
    for _ in range(iterations):
        q = src @ R.T + t
        _, idx = arbre.query(q)
        m = cible[idx]
        cs, cm = q.mean(0), m.mean(0)
        U, _, Vt = np.linalg.svd((q - cs).T @ (m - cm))
        D = np.diag([1, 1, np.sign(np.linalg.det(Vt.T @ U.T))])
        dR = Vt.T @ D @ U.T
        R, t = dR @ R, dR @ (t - cs) + cm
    return R, t


def recaler(src, cible):
    """Pose qui amène le modèle sur la pièce : écart moyen dans les deux sens (mm)."""
    arbre_c = cKDTree(cible)
    cs, As = axes(src); cc, Ac = axes(cible)
    meilleur = None
    for M in ROTS:
        R0 = Ac.T @ M @ As
        R, t = icp(src, arbre_c, cible, R0, cc - R0 @ cs, iterations=15)
        aller = arbre_c.query(src @ R.T + t)[0].mean()
        if meilleur is None or aller < meilleur[0]:
            meilleur = (aller, R, t)
    _, R, t = meilleur
    R, t = icp(src, arbre_c, cible, R, t, iterations=40)
    posee = src @ R.T + t
    aller = arbre_c.query(posee)[0]
    retour = cKDTree(posee).query(cible)[0]
    T = np.eye(4); T[:3, :3] = R; T[:3, 3] = t
    return T, float(aller.mean()), float(retour.mean()), float(max(np.percentile(aller, 99), np.percentile(retour, 99)))


# Pièces presque de révolution : l'ICP les pose bien en position mais pas forcément en angle autour de leur
# axe, les petits trous pesant trop peu. Le 2026-10-03, l'entretoise 1526 du bas et le moyeu 1906 étaient
# tournés d'une trentaine de degrés : les vis tombaient dans les trous lisses au lieu des taraudages.
DE_REVOLUTION = ('1526-', '1504-', '1906-', '1908-', '2312-', '1601-', '2904-', '1311-', '1515-')


def caler_angle(src, cible, T):
    """Cherche, autour de l'axe de la pièce posée (normale du disque), l'angle qui colle le mieux."""
    arbre = cKDTree(cible)
    posee = src @ T[:3, :3].T + T[:3, 3]
    c, A = axes(posee); axe = A[2]                       # plus petite dispersion : l'axe d'un disque
    def tourne(deg):
        R = trimesh.transformations.rotation_matrix(np.radians(deg), axe, c)
        return R
    def score(deg):
        R = tourne(deg)
        return arbre.query(posee @ R[:3, :3].T + R[:3, 3])[0].mean()
    grossier = min(np.arange(0, 360, 1.0), key=score)
    fin = min(np.arange(grossier - 1, grossier + 1, 0.1), key=score)
    return tourne(fin) @ T, fin


def main():
    os.makedirs(SORTIE, exist_ok=True)
    pieces = pieces_du_bras()
    noms = {}
    sortie, vues = [], set()
    for ids, candidats, role in UNITES:
        cible_m = trimesh.util.concatenate([pieces[i] for i in ids])
        cible = echantillon(cible_m, N_ECH * 2)
        essais = []
        for cand in candidats:
            sku, filtre = cand[0], cand[1]
            if sku.startswith(('IMPRIME:', 'BRAS:')):
                essais.append(dict(sku=sku, T=np.eye(4), aller=0.0, retour=0.0, p99=0.0))
                continue
            src = echantillon(modele(sku, filtre))
            T, a, r, p = recaler(src, cible)
            if sku.startswith(DE_REVOLUTION):
                T, angle = caler_angle(src, cible, T)
                arbre_c = cKDTree(cible); posee = src @ T[:3, :3].T + T[:3, 3]
                aller = arbre_c.query(posee)[0]; retour = cKDTree(posee).query(cible)[0]
                a, r = float(aller.mean()), float(retour.mean())
                p = float(max(np.percentile(aller, 99), np.percentile(retour, 99)))
            essais.append(dict(sku=cand[2] if len(cand) > 2 else sku, source=sku, filtre=filtre, T=T, aller=a, retour=r, p99=p))
        bon = min(essais, key=lambda e: e['aller'] + e['retour'])
        noms[bon['sku']] = noms.get(bon['sku'], 0) + 1
        nom = '%s#%d' % (bon['sku'], noms[bon['sku']])
        vues.update(ids)
        sortie.append(dict(nom=nom, sku=bon['sku'], source=bon.get('source', bon['sku']), filtre=bon.get('filtre'), role=role, pieces=ids,
                           matrice=np.round(bon['T'], 6).tolist(), ecart_moyen=round((bon['aller'] + bon['retour']) / 2, 3),
                           ecart_p99=round(bon['p99'], 3),
                           autres=[dict(sku=e['sku'], ecart=round((e['aller'] + e['retour']) / 2, 3)) for e in essais if e is not bon]))
        print('%-26s %-12s écart moyen %.3f mm  p99 %.2f mm %s' % (
            nom, role, sortie[-1]['ecart_moyen'], bon['p99'],
            ' | '.join('%s %.3f' % (e['sku'], (e['aller'] + e['retour']) / 2) for e in essais if e is not bon)), flush=True)
    reste = [i for i in range(len(pieces)) if i not in vues]
    aire_reste = sum(pieces[i].area for i in reste)
    print('pièces non attribuées : %d, aire totale %.0f mm²' % (len(reste), aire_reste))
    json.dump(dict(repere='épaule : X le long du bras, Y axe de flexion, mm', unites=sortie, non_attribuees=reste),
              open(os.path.join(SORTIE, 'assemblage.json'), 'w'), ensure_ascii=False, indent=1)


if __name__ == '__main__':
    main()
