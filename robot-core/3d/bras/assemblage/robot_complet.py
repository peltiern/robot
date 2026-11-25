#!/usr/bin/env python3
"""
Le robot complet (« Wall-E GoBilda Full.step », export Onshape du 2026-10-05) avec le nouveau bras à la place
des deux « Long Right Arm » de Nicolas.

Dans l'export, les deux bras sont la même pièce (aucun n'est en miroir), symétriques par rapport au plan
x = 24,7 mm du robot. On garde de chacun ce qui est dans le corps (servo de flexion, son cadre, ses entretoises,
l'accouplement sur l'arbre) : c'est le côté corps de la flexion, que le nouveau bras n'a pas. Le reste (bras,
main, arbre REX et moyeux) est remplacé.

Le nouveau bras est posé sur l'épaule de l'ancien :
  - origine sur l'axe de l'arbre de flexion (REX 2102-0008-0110), dans le plan médian des deux plaques 1108 ;
  - X (le long du bras) dans la direction des plaques 1108, vers la main : même pose que le bras de Nicolas ;
  - Y (axe de flexion) le long de l'arbre, côté corps en −Y comme dans le bras modélisé.
Le même bras sert des deux côtés : à droite (x < 24,7 dans l'export, x > 0 dans le repère de la maquette) il est
retourné d'un demi-tour autour de son axe, comme Nicolas l'a fait avec le sien.

Écrit robot_complet/robot-complet.glb (mm) et robot_complet/robot-complet.json (groupes) sur le disque externe.
"""
import json, os, re, sys
import numpy as np, trimesh, cascadio

ICI = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, ICI)
from recaler import SORTIE

DISQUE = '/media/npeltier/disque_usb/Robot'
STEP = DISQUE + '/Wall-E GoBilda Full.step'
DOSSIER = DISQUE + '/robot_complet'
GLB_COMPLET = DOSSIER + '/full.glb'
PLAN_SYMETRIE = 24.7                     # x du plan de symétrie du robot, mm
GARDE_CORPS = 144.7                      # ce qui est à moins de cette distance du plan reste (côté corps)
ANCIENS_BRAS = ('Long Right Arm <1>', 'Long Right Arm <2>')
# L'épaule ramenée aux proportions des plans de Nicolas (2026-11-25, échelle ≈ 0,80 du cadre de 336 mm) : elle est à
# 56 % de la profondeur du corps depuis l'avant (243,5 / 434 mm), soit 30 mm plus en avant que sur l'arbre de l'export,
# Dans l'export, −y est l'avant du robot. La hauteur et l'écart au flanc viennent de la jonction (jonction.py) : axe de
# flexion à 40 mm sous le goRAIL du haut du flanc (64 mm sous le haut du cadre, comme sur les plans), épaule écartée
# jusqu'à ce que le bout du goTUBE de flexion de 72 mm vienne contre la roue (10,6 mm de plus que l'arbre de l'export ;
# les plans en demandaient environ 8).
EPAULE_AVANT = 30.0


def charger_complet():
    if not os.path.exists(GLB_COMPLET):
        os.makedirs(DOSSIER, exist_ok=True)
        # 0,1 mm de flèche : 3 min et 2,8 Go de mémoire pour les 233 Mo du STEP
        cascadio.step_to_glb(STEP, GLB_COMPLET, tol_linear=0.1, tol_angular=0.5, use_parallel=False)
    s = trimesh.load(GLB_COMPLET)
    parent = {b: a for a, b in s.graph.transforms.edge_data.keys()}
    pieces = []
    for n in s.graph.nodes_geometry:
        T, g = s.graph[n]
        m = s.geometry[g].copy(); m.apply_transform(T); m.apply_scale(1000.0)      # le GLB est en mètres
        chaine, x = [], n
        while x in parent:
            chaine.append(x); x = parent[x]
        pieces.append(dict(nom=n, chaine=chaine[::-1], maillage=m))
    return pieces


def axe(m):
    v = m.vertices - m.vertices.mean(0)
    return np.linalg.eigh(v.T @ v)[1][:, -1]


def epaule(pieces, bras):
    """Repère du nouveau bras posé sur l'épaule de l'ancien : matrice 4×4, du repère du bras au robot."""
    du_bras = [p for p in pieces if bras in p['chaine']]
    arbre = next(p['maillage'] for p in du_bras if p['nom'].startswith('2102-0008-0110'))
    plaques = [p['maillage'] for p in du_bras if p['nom'].startswith('1108-0001-0002')]
    assert len(plaques) == 2, bras
    a = axe(arbre); c_arbre = arbre.vertices.mean(0)
    milieu = np.mean([pl.bounds.mean(0) for pl in plaques], axis=0)
    # origine : le point de l'arbre dans le plan médian des plaques
    O = c_arbre + a * np.dot(milieu - c_arbre, a)
    X = axe(plaques[0]); X -= a * np.dot(X, a); X /= np.linalg.norm(X)
    main = np.mean([p['maillage'].bounds.mean(0) for p in du_bras if any('Hand' in c for c in p['chaine'])], axis=0)
    if np.dot(main - O, X) < 0:
        X = -X
    vers_corps = np.array([np.sign(PLAN_SYMETRIE - O[0]), 0.0, 0.0])
    import jonction
    cible = jonction.epaule('droit' if O[0] < PLAN_SYMETRIE else 'gauche')
    assert abs(O[1] - EPAULE_AVANT - cible[1]) < 0.01, (O, cible)    # jonction.Y_AXE suit l'arbre de l'export
    O = cible
    Y = -a if np.dot(a, vers_corps) > 0 else a                       # −Y vers le corps
    Z = np.cross(X, Y)
    T = np.eye(4); T[:3, 0], T[:3, 1], T[:3, 2], T[:3, 3] = X, Y, Z, O
    return T


def nouveau_bras():
    """Les pièces du bras modélisé, dans son repère (mm) : (clé de la visionneuse, nom, référence, maillage)."""
    v5 = json.load(open(os.path.join(SORTIE, 'v5.json')))
    masquees = {n for l in v5['masquer'].values() for n in l}
    out = []
    for f, origine in (('bras-assemble-leger.glb', True), ('bras-v5-leger.glb', False)):
        s = trimesh.load(os.path.join(SORTIE, f))
        for n in s.graph.nodes_geometry:
            if origine and n in masquees:
                continue
            T, g = s.graph[n]
            m = s.geometry[g].copy(); m.apply_transform(T)
            if origine:
                out.append((n, n, re.sub(r'#\d+$', '', n), m))
            else:
                p = v5['pieces'][int(n.split('-')[1])]
                out.append((n, p['nom'], p['ref'], m))
    return out


def main():
    os.makedirs(DOSSIER, exist_ok=True)
    pieces = charger_complet()
    scene = trimesh.Scene()
    groupes = {}
    retirees = 0
    def unique(nom):
        # Deux nœuds du même nom s'écrasent dans la scène : les vis identiques d'une même liaison disparaissaient
        # toutes sauf une (14 par jonction, 44 par bras, vu le 2026-11-25).
        k, n = 2, nom
        while n in groupes:
            n = '%s (%d)' % (nom, k); k += 1
        return n
    for p in pieces:
        if any(b in p['chaine'] for b in ANCIENS_BRAS) and \
                abs(p['maillage'].bounds.mean(0)[0] - PLAN_SYMETRIE) > GARDE_CORPS:
            retirees += 1
            continue
        # Le côté corps de l'ancien bras (servo dans l'axe, cadre, accouplement 4001) ne tenait à rien : la jonction le
        # remplace.
        if any(b in p['chaine'] for b in ANCIENS_BRAS):
            retirees += 1
            continue
        haut = p['chaine'][1] if len(p['chaine']) > 1 else p['nom']
        nom = unique(p['nom'])
        nom = scene.add_geometry(p['maillage'], node_name=nom, geom_name=nom)
        groupes[nom] = haut
    # La jonction, posée dans le corps : à droite telle quelle, à gauche en miroir par le plan médian des rails. Elle
    # n'est pas reprise du repère du bras : le bras gauche est le droit retourné d'un demi-tour, pas son miroir.
    import jonction
    for nom_j, ref, m in jonction.pieces_robot():
        for cote in ('droit', 'gauche'):
            mm = m.copy()
            if cote == 'gauche':
                mm.apply_transform(np.diag([-1.0, 1.0, 1.0, 1.0])); mm.apply_translation([2 * jonction.PLAN, 0, 0]); mm.invert()
            n = unique('%s %s' % (nom_j, cote))
            n = scene.add_geometry(mm, node_name=n, geom_name=n)
            groupes[n] = 'jonction ' + cote
    # La traverse sous la jonction (jonction.TRAVERSE_HAUT) : le rail du haut du flanc et ses deux angles (gousset 1137,
    # barrettes d'écrous 2805-0004-0038 dans les fentes extérieures, vis M4 × 6), recopiés plus bas. Les vis M4 × 14 des
    # angles restent : elles entrent dans le bout des traverses avant et arrière, que la traverse n'a pas.
    dz = jonction.TRAVERSE_HAUT - (jonction.RAIL_BAS + 24.0)
    def de_l_angle(p):
        lo, hi = p['maillage'].bounds
        return (p['nom'].startswith(('1137-0001-0002', '2805-0004-0038', '2802-0004-0006')) and lo[0] > -150 and hi[0] < -118
                and lo[2] > 270 and hi[2] < 342 and (hi[1] < -70 or lo[1] > 128))
    traverse = [p for p in pieces if p['nom'] == '1109-0024-0288_2' or de_l_angle(p)]
    assert len(traverse) == 15, [p['nom'] for p in traverse]
    for p in traverse:
        for cote in ('droit', 'gauche'):
            mm = p['maillage'].copy(); mm.apply_translation([0, 0, dz])
            if cote == 'gauche':
                mm.apply_transform(np.diag([-1.0, 1.0, 1.0, 1.0])); mm.apply_translation([2 * jonction.PLAN, 0, 0]); mm.invert()
            n = unique('traverse, %s %s' % (re.sub(r'_\d+$', '', p['nom']), cote))
            n = scene.add_geometry(mm, node_name=n, geom_name=n)
            groupes[n] = 'traverse ' + cote
    bras = [b for b in nouveau_bras() if not b[1].startswith('jonction')]
    # l'export est vu de face : son x négatif est la droite du robot
    for cote, ancien in (('droit', ANCIENS_BRAS[0]), ('gauche', ANCIENS_BRAS[1])):
        T = epaule(pieces, ancien)
        print('bras %s : épaule en %s, axe du bras %s, axe de flexion %s' % (
            cote, T[:3, 3].round(1).tolist(), T[:3, 0].round(3).tolist(), T[:3, 1].round(3).tolist()))
        for cle, nom, ref, m in bras:
            m = m.copy(); m.apply_transform(T)
            n = unique('bras %s : %s' % (cote, nom))
            n = scene.add_geometry(m, node_name=n, geom_name=n)
            groupes[n] = 'bras ' + cote
    scene.export(os.path.join(DOSSIER, 'robot-complet.glb'))
    json.dump(dict(groupes=groupes), open(os.path.join(DOSSIER, 'robot-complet.json'), 'w'), ensure_ascii=False, indent=0)
    print('%d pièces de l\'ancien bras retirées, %d pièces dans le robot, %d triangles' % (
        retirees, len(groupes), sum(len(g.faces) for g in scene.geometry.values())))
    # 11 millions de triangles : trop pour un navigateur. Copie allégée, comme pour la visionneuse du bras.
    from allegement import alleger
    alleger(os.path.join(DOSSIER, 'robot-complet.glb'), os.path.join(DOSSIER, 'robot-complet-leger.glb'))


if __name__ == '__main__':
    main()
