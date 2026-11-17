"""Lecture d'un GLB sans dépendance autre que numpy : {nom du nœud: (sommets en repère monde, faces)}."""
import json
import struct

import numpy as np


def charger(chemin):
    b = open(chemin, 'rb').read()
    l = struct.unpack('<I', b[12:16])[0]
    j = json.loads(b[20:20 + l])
    debut = 20 + l
    lb = struct.unpack('<I', b[debut:debut + 4])[0]
    binaire = b[debut + 8:debut + 8 + lb]

    def accesseur(i):
        a = j['accessors'][i]
        v = j['bufferViews'][a['bufferView']]
        n = {'SCALAR': 1, 'VEC3': 3, 'VEC4': 4}[a['type']]
        dt = {5126: np.float32, 5125: np.uint32, 5123: np.uint16}[a['componentType']]
        o = v.get('byteOffset', 0) + a.get('byteOffset', 0)
        x = np.frombuffer(binaire, dt, a['count'] * n, o)
        return x.reshape(-1, n) if n > 1 else x

    def matrice(nd):
        if 'matrix' in nd:
            return np.array(nd['matrix']).reshape(4, 4).T
        M = np.eye(4)
        if 'scale' in nd:
            M = np.diag(list(nd['scale']) + [1]) @ M
        if 'rotation' in nd:
            x, y, z, w = nd['rotation']
            R = np.eye(4)
            R[:3, :3] = [[1 - 2 * (y * y + z * z), 2 * (x * y - z * w), 2 * (x * z + y * w)],
                         [2 * (x * y + z * w), 1 - 2 * (x * x + z * z), 2 * (y * z - x * w)],
                         [2 * (x * z - y * w), 2 * (y * z + x * w), 1 - 2 * (x * x + y * y)]]
            M = R @ M
        if 'translation' in nd:
            T = np.eye(4)
            T[:3, 3] = nd['translation']
            M = T @ M
        return M

    sortie = {}

    def visiter(i, parent):
        nd = j['nodes'][i]
        M = parent @ matrice(nd)
        if 'mesh' in nd:
            V, F = [], []
            for p in j['meshes'][nd['mesh']]['primitives']:
                v = accesseur(p['attributes']['POSITION'])
                f = accesseur(p['indices']).reshape(-1, 3)
                F.append(f + sum(len(x) for x in V))
                V.append(v)
            V = np.vstack(V) @ M[:3, :3].T + M[:3, 3]
            sortie[nd.get('name', str(i))] = (V, np.vstack(F))
        for c in nd.get('children', []):
            visiter(c, M)

    for r in j['scenes'][0]['nodes']:
        visiter(r, np.eye(4))
    return sortie


def grappe(V, F, pas):
    """Allège un maillage en fusionnant les sommets d'une même maille de `pas` mm."""
    q = np.round(V / pas).astype(np.int64)
    u, inv = np.unique(q, axis=0, return_inverse=True)
    inv = inv.ravel()
    Vn = np.zeros((len(u), 3))
    np.add.at(Vn, inv, V)
    Vn /= np.bincount(inv)[:, None]
    Fn = inv[F]
    Fn = Fn[(Fn[:, 0] != Fn[:, 1]) & (Fn[:, 1] != Fn[:, 2]) & (Fn[:, 0] != Fn[:, 2])]
    return Vn.astype(np.float32), Fn.astype(np.uint32)


def ecrire(chemin, noeuds):
    """noeuds : {nom: [(couleur rgb ou None, sommets float32, faces uint32), ...]} -> GLB, un nœud par nom."""
    binaire = b''
    vues, accs, maillages, nds, mats, idx_mat = [], [], [], [], [], {}

    def ajoute(data, cible):
        nonlocal binaire
        while len(binaire) % 4:
            binaire += b'\0'
        vues.append(dict(buffer=0, byteOffset=len(binaire), byteLength=len(data), target=cible))
        binaire += data
        return len(vues) - 1

    for nom, prims in noeuds.items():
        P = []
        for c, V, F in prims:
            accs.append(dict(bufferView=ajoute(V.tobytes(), 34962), componentType=5126, count=len(V), type='VEC3',
                             min=V.min(0).tolist(), max=V.max(0).tolist()))
            accs.append(dict(bufferView=ajoute(F.ravel().tobytes(), 34963), componentType=5125, count=F.size, type='SCALAR'))
            prim = dict(attributes=dict(POSITION=len(accs) - 2), indices=len(accs) - 1)
            if c is not None:
                if c not in idx_mat:
                    idx_mat[c] = len(mats)
                    mats.append(dict(pbrMetallicRoughness=dict(baseColorFactor=[*map(float, c), 1.0],
                                                               metallicFactor=0.2, roughnessFactor=0.6), doubleSided=True))
                prim['material'] = idx_mat[c]
            P.append(prim)
        maillages.append(dict(name=nom, primitives=P))
        nds.append(dict(name=nom, mesh=len(maillages) - 1))
    while len(binaire) % 4:
        binaire += b'\0'
    j = dict(asset=dict(version='2.0'), scene=0, scenes=[dict(nodes=list(range(len(nds))))], nodes=nds, meshes=maillages,
             accessors=accs, bufferViews=vues, buffers=[dict(byteLength=len(binaire))])
    if mats:
        j['materials'] = mats
    js = json.dumps(j).encode()
    js += b' ' * ((4 - len(js) % 4) % 4)
    out = struct.pack('<III', 0x46546C67, 2, 12 + 8 + len(js) + 8 + len(binaire)) + \
        struct.pack('<II', len(js), 0x4E4F534A) + js + struct.pack('<II', len(binaire), 0x004E4942) + binaire
    open(chemin, 'wb').write(out)
    return len(out)
