#!/usr/bin/env python3
"""
Les modèles des fabricants -> composants.glb : un nœud par composant, tous posés de la même façon —
face de montage en y = 0, côté composants vers +y, grande longueur selon x, centrés en x et z, en mm.
La page tourne ensuite chaque nœud selon sa pose (face d'appui, axe de la longueur) décrite dans calculs.js.

    python3 composants.py DOSSIER_DES_STEP

Les STEP vivent hors dépôt (≈ 110 Mo décompressés) ; IMPLANTATION.md dit où les reprendre. Les conversions
STEP -> GLB sont gardées dans DOSSIER_DES_STEP/glb : les refaire prend plusieurs minutes pour le Jetson.
Il faut cascadio et trimesh (pip install --target ... cascadio trimesh numpy scipy).
"""
import json
import os
import sys

import numpy as np
import trimesh
import cascadio

from lire_glb import grappe, ecrire

ICI = os.path.dirname(os.path.abspath(__file__))
FICHIERS = {
    'batterie': '3104-0020-0001.step', 'pdb': '3108-2833-0802.step', 'bec': '3119-2829-0106.step',
    'dcc': 'DCC1000_0_3D.stp', 'hub': 'HUB0000_0_3D.step', 'imu': 'MOT1102_1_3D.stp', 'rcc': 'RCC1000_1_3D.stp',
    'respeaker': 'RESPEAKER MIC-3D v2.0.stp', 'jetson': 'P3450_P3449_A02_P3448_A02.stp',
}
PAS = {'jetson': 0.5, 'batterie': 0.5}       # maille de fusion des sommets, mm ; 0,3 ailleurs
# L'heuristique du côté composants se trompe sur le PDB : son socle 3117 (8 mm) dépasse plus que le boîtier.
RETOURNER = {'pdb'}


def couleur(m):
    try:
        c = np.array(m.visual.material.main_color, float) / 255
    except Exception:
        c = np.array([.6, .6, .6, 1.])
    return tuple(np.round(c[:3], 2))


def repere(maillages):
    """Rotation qui met la normale de la carte en +y et la grande longueur en x.
    Le côté composants est celui qui dépasse le plus du plan où s'accumule la surface à plat (la carte, la semelle)."""
    V = np.vstack([m.vertices for m in maillages])
    lo, hi = V.min(0), V.max(0)
    d = hi - lo
    mince = int(np.argmin(d))
    longueur = max((i for i in range(3) if i != mince), key=lambda i: d[i])
    aires, pos = [], []
    for m in maillages:
        a_plat = np.abs(m.face_normals[:, mince]) > .95
        aires.append(m.area_faces[a_plat])
        pos.append(m.triangles_center[a_plat, mince])
    h, b = np.histogram(np.concatenate(pos), bins=200, range=(lo[mince], hi[mince]), weights=np.concatenate(aires))
    plan = b[np.argmax(h)]
    Y = np.zeros(3)
    Y[mince] = 1 if (hi[mince] - plan) >= (plan - lo[mince]) else -1
    X = np.zeros(3)
    X[longueur] = 1
    return np.vstack([X, Y, np.cross(X, Y)])


def main():
    step = sys.argv[1]
    cache = os.path.join(step, 'glb')
    os.makedirs(cache, exist_ok=True)
    noeuds, bilan = {}, {}
    for id_, f in FICHIERS.items():
        glb = os.path.join(cache, os.path.splitext(f)[0].replace(' ', '_') + '.glb')
        if not os.path.exists(glb):
            cascadio.step_to_glb(os.path.join(step, f), glb, tol_linear=0.05, tol_angular=0.5)
        s = trimesh.load(glb)
        maillages = []
        for n in s.graph.nodes_geometry:
            T, g = s.graph[n]
            m = s.geometry[g].copy()
            m.apply_transform(T)
            m.apply_scale(1000.0)                       # cascadio écrit en mètres
            maillages.append(m)
        R = repere(maillages)
        if id_ in RETOURNER:
            R = R * np.array([[1], [-1], [-1]])        # demi-tour autour de x
        par_couleur = {}
        for m in maillages:
            par_couleur.setdefault(couleur(m), []).append((m.vertices @ R.T, m.faces))
        tout = np.vstack([v for l in par_couleur.values() for v, _ in l])
        lo, hi = tout.min(0), tout.max(0)
        decalage = np.array([-(lo[0] + hi[0]) / 2, -lo[1], -(lo[2] + hi[2]) / 2])
        prims = []
        for c, l in par_couleur.items():
            VV, FF, n = [], [], 0
            for V, F in l:
                v, fa = grappe(V + decalage, F, PAS.get(id_, 0.3))
                VV.append(v)
                FF.append(fa + n)
                n += len(v)
            V, F = np.vstack(VV), np.vstack(FF).astype(np.uint32)
            if len(F):
                prims.append((c, V, F))
        if id_ == 'batterie':
            # aucun modèle libre de la DCB184 : un bloc à ses cotes (110 × 76 × 65, fiche DeWalt), posé sur le dock
            dessus = hi[1] - lo[1]
            for taille, y, c in (([110, 65, 76], dessus + 32.5, (0.16, 0.16, 0.17)), ([111, 10, 77], dessus + 58, (0.96, 0.75, 0.05))):
                b = trimesh.creation.box(extents=taille)
                b.apply_translation([0, y, 0])
                prims.append((c, b.vertices.astype(np.float32), b.faces.astype(np.uint32)))
        noeuds[id_] = prims
        V = np.vstack([p[1] for p in prims])
        bilan[id_] = dict(taille=(V.max(0) - V.min(0)).round(1).tolist(), triangles=int(sum(len(p[2]) for p in prims)))
        print('%-10s %s mm, %d triangles' % (id_, ' × '.join('%.1f' % x for x in bilan[id_]['taille']), bilan[id_]['triangles']))
    taille = ecrire(os.path.join(ICI, 'composants.glb'), noeuds)
    print('composants.glb : %.1f Mo' % (taille / 1e6))


if __name__ == '__main__':
    main()
