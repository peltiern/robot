#!/usr/bin/env python3
"""
Les mouvements du bras : à quel ensemble mobile appartient chaque pièce, autour de quel axe il tourne,
et le contrôle « rien ne frotte » sur toute la course de chaque mouvement.

Ensembles, du corps vers la pince :
  épaule   — tourne en flexion autour de l'axe REX (Y) : blocs 1201, moyeux 1311, couronnes, entretoises 1526,
             roulements d'abduction, goTUBE de flexion et leur visserie ;
  caisson  — tourne en abduction autour de Z (par l'origine) par rapport à l'épaule : tout le reste du bras ;
  pignon   — pignon laiton 48 dents, tourne sur l'axe du servo d'abduction (roule sur la couronne fixe, 72/48) ;
  engr_d85 — engrenage laiton 20 dents du D85MG, tourne sur l'axe du D85MG (1:1 avec le tube, sens inverse) ;
  tube     — tourne autour de X : liaison, goTUBE du bras, pignon Slip-Fit, HS-65MG et ce qui le suit ;
  palonnier — bras du HS-65MG, tourne sur l'axe du servo (Z dans le repère du tube) et tire le câble de pince.

Écrit animation.json (pour la visionneuse) sur le disque externe.
"""
import json, math, os, sys
import numpy as np, trimesh
from scipy.spatial import cKDTree

ICI = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, ICI)
from recaler import SORTIE

AXE_SERVO_ABDUCTION = (47.85, 0.0)                 # x, y de la cannelure du servo 2000 (axe Z)
AXE_D85MG = None                                   # relevé sur le modèle (axe X)
HS65_AXE = (154.5, -5.7)                           # x, y de la cannelure du HS-65MG (axe Z), dessus à z = 14,6
# (recalés le 2026-10-04 sur le servo redressé ; avant, (154,0 ; −5,6) et 15,5, relevés sur le servo de travers)
SOMMET_CANNELURE_HS65 = 14.6
PALONNIER = dict(longueur=8.0, z=SOMMET_CANNELURE_HS65 + 1.0)   # bras unique de 8 mm (rester dans Ø39 de la liaison)
FENTE_LIAISON = (166.5, 0.0, 12.0)                 # lumière de la bride avant vers le canal +z du goTUBE
BOUT_TUBE = 265.6
# Abduction bornée à −12° et non −16° : au-delà, la plaque 1123 du caisson vient sur le faisceau de câbles
# dans l'épaule (balayer_cables, 2026-10-03). Et à +22° et non +23° : à +23°, la plaque 1123 touche le moyeu
# 1311 côté y+, c'est la butée mécanique (0,45 mm de jeu à +22° ; vu le 2026-10-04, le balayage au pas de 5°
# s'arrêtait à +19°). Butées à reporter dans l'organe du bras quand il existera.
COURSES = dict(flexion=(-45, 180), abduction=(-12, 22), tube=(-135, 135), pince=(-60, 60))
RAPPORT_ABDUCTION = 72 / 48

EPAULE = ('1201-', '1311-', '2312-0414-0072', '1526-', '1601-0014', '1116-', '4103-0032-0048',
          '2800-0004-0014', '1502-0006-0040', '2102-')
TUBE = ('liaison', 'HS-65MG', 'pignon Slip-Fit', '4103-0032-0096', 'bague ', 'vis M2 du HS-65MG',
        'palonnier', 'câble de pince', 'main')


def groupe(nom, ref, centre):
    """Ensemble mobile d'une pièce, d'après sa référence et, pour la visserie, sa position."""
    # les cordons se déforment : la visionneuse les redessine point par point (voir « cables »)
    if ref.startswith('CABLE:câble'):
        return 'cable'
    # les prises attendent côté corps, au bout du goTUBE de flexion
    if ref.startswith('CABLE:prise'):
        return 'epaule'
    # la soudure du fil du HS-65MG est dans le couloir au-dessus du cadre : elle reste dans le caisson
    # (son nom contient « HS-65MG » et la faisait tourner avec le tube)
    if ref.startswith('CABLE:soudure'):
        return 'caisson'
    if 'palonnier' in nom:
        return 'palonnier'
    if any(t in nom for t in TUBE) or ref in ('4103-0032-0096', '2303-4008-0020', 'HS-65MG') \
            or 'liaison' in ref:
        return 'tube'
    if ref == '2305-0025-0048':
        return 'pignon'
    if ref == '2305-0025-0020':
        return 'engr_d85'
    if ref.startswith(EPAULE) or 'retournée' in nom:
        return 'epaule'
    x, y, z = centre
    # visserie de l'épaule : tout ce qui est dans l'épaule (x < 22) entre les chapes (|z| < 33)
    if ref.startswith(('2802-0004-0018', '2802-0004-0016', '2802-0004-0014', '2812-', '2801-', '2807-0407')) \
            and x < 22 and abs(z) < 33:
        return 'epaule'
    if ref.startswith('2802-') and 'plaque 1116' in nom:
        return 'epaule'
    return 'caisson'


AXE_D85MG = (10.6, -12.0)                          # y, z de la cannelure du D85MG (axe X), centre du 2305


def bout_palonnier(angle_deg=0.0):
    """Bout du palonnier : au repos il pointe vers +y (tirer = tourner vers −x)."""
    a = math.radians(angle_deg)
    x0, y0 = HS65_AXE
    return np.array([x0 - PALONNIER['longueur'] * math.sin(a), y0 + PALONNIER['longueur'] * math.cos(a), PALONNIER['z']])


def cable_de_pince(angle_deg=0.0):
    """Du bout du palonnier, par la lumière de la bride avant, puis le canal +z du goTUBE jusqu'à la main."""
    return [bout_palonnier(angle_deg), np.array(FENTE_LIAISON), np.array([175.0, 0.0, 11.2]),
            np.array([BOUT_TUBE, 0.0, 11.2]), np.array([BOUT_TUBE + 6.0, 0.0, 11.2])]


def pieces_pince():
    """Palonnier, câble de pince et repère de la main, au repos (pour le calque v5)."""
    x0, y0 = HS65_AXE
    z0 = SOMMET_CANNELURE_HS65
    moyeu = trimesh.creation.cylinder(radius=3.5, segment=[(x0, y0, z0), (x0, y0, z0 + 2.0)], sections=24)
    bras = trimesh.creation.box(extents=(3.0, PALONNIER['longueur'] + 1.5, 1.6))
    bras.apply_translation((x0, y0 + PALONNIER['longueur'] / 2, PALONNIER['z']))
    pal = trimesh.util.concatenate([moyeu, bras])
    from cables import cordon
    cab = cordon(cable_de_pince(0.0), r=0.5)
    main = trimesh.creation.cylinder(radius=14.0, segment=[(BOUT_TUBE + 6, 0, 0), (BOUT_TUBE + 9, 0, 0)], sections=48)
    return [('palonnier du HS-65MG (bras de 8 mm)', 'achetée', pal, 'FOURNI:palonnier du HS-65MG'),
            ('câble de pince (Ø1, du palonnier à la main)', 'câblage', cab, 'CABLE:câble de pince'),
            ('main : emplacement des doigts (non dessinés)', 'câblage', main, 'CABLE:main (doigts non dessinés)')]


def ecrire():
    """animation.json : ensembles, axes, appartenance de chaque nœud, et câbles déformables."""
    from audit import pieces_nouveau_bras
    from cables import CABLES, FIL, fils
    P = pieces_nouveau_bras()
    v5 = json.load(open(os.path.join(SORTIE, 'v5.json')))
    noeuds = {}
    for n, (m, ref, role) in P.items():
        g = groupe(n, ref, m.bounds.mean(0))
        if n.startswith('v5 '):
            k = int(n.split()[1]); noeuds['v5-%d' % k] = g
        else:
            noeuds[n] = g
    def grp_point(p, cable):
        x, y, z = p
        if cable == 'HS-65MG' and x > 124.5 and abs(y) < 16 and abs(z) < 16:
            return 'tube'
        if x < 23 and abs(z) < 33:
            return 'epaule'
        if y < -32:
            return 'epaule'
        return 'caisson'
    cables = []
    # un tracé par fil : chaque nappe compte 3 fils
    for nom, c in CABLES.items():
        groupes_points = [grp_point(p, nom) for p in c['points']]
        for k, f in enumerate(fils(nom)[0]):
            cables.append(dict(nom='câble du %s, fil %d' % (nom, k + 1), points=f.round(3).tolist(),
                               groupes=groupes_points, rayon=FIL[nom] / 2))
    pince = cable_de_pince(0.0)
    cables.append(dict(nom='câble de pince', points=[list(map(float, p)) for p in pince],
                       groupes=['palonnier'] + ['tube'] * (len(pince) - 1), rayon=0.5))
    groupes = {
        'epaule': dict(parent=None, point=[0, 0, 0], axe=[0, 1, 0], commande='flexion', rapport=1),
        'caisson': dict(parent='epaule', point=[0, 0, 0], axe=[0, 0, 1], commande='abduction', rapport=1),
        'pignon': dict(parent='caisson', point=[AXE_SERVO_ABDUCTION[0], AXE_SERVO_ABDUCTION[1], 0], axe=[0, 0, 1],
                       commande='abduction', rapport=RAPPORT_ABDUCTION),
        'tube': dict(parent='caisson', point=[0, 0, 0], axe=[1, 0, 0], commande='tube', rapport=1),
        'engr_d85': dict(parent='caisson', point=[0, AXE_D85MG[0], AXE_D85MG[1]], axe=[1, 0, 0], commande='tube',
                         rapport=-1),
        'palonnier': dict(parent='tube', point=[HS65_AXE[0], HS65_AXE[1], 0], axe=[0, 0, 1], commande='pince',
                          rapport=1),
    }
    json.dump(dict(groupes=groupes, noeuds=noeuds, cables=cables, courses=COURSES),
              open(os.path.join(SORTIE, 'animation.json'), 'w'), ensure_ascii=False, indent=1)
    from collections import Counter
    print('pièces par ensemble :', dict(Counter(noeuds.values())))
    return P, noeuds


def rotation(point, axe, angle_deg):
    T = trimesh.transformations.rotation_matrix(math.radians(angle_deg), axe, point)
    return T


def poses(groupes, commandes):
    """Matrice de chaque ensemble pour des angles de commande donnés (en degrés)."""
    out = {}
    def pose(g):
        if g in out:
            return out[g]
        d = groupes[g]
        R = rotation(d['point'], d['axe'], commandes.get(d['commande'], 0.0) * d['rapport'])
        out[g] = (pose(d['parent']) @ R) if d['parent'] else R
        return out[g]
    for g in groupes:
        pose(g)
    return out


# Contacts voulus : ce qui roule, engrène ou porte, et qui garde le même jeu quel que soit l'angle.
ENGRENEMENTS = [('2305-0025-0048', '2312-0414-0072'), ('2305-0025-0020', '2303-4008-0020')]


def balayer(pas=5.0):
    """Pour chaque mouvement, jeu minimal entre ce qui bouge et ce qui reste, sur toute la course.

    On compare au jeu au repos : un couple déjà au contact au repos (roulement sur son tube, pignon dans
    sa couronne) et qui ne se rapproche pas est un contact voulu ; un couple qui se rapproche à moins de
    0,3 mm en cours de course frotte."""
    P, noeuds = ecrire()
    A = json.load(open(os.path.join(SORTIE, 'animation.json')))
    G = A['groupes']
    cle = {k: ('v5-%d' % int(k.split()[1]) if k.startswith('v5 ') else k) for k in P}
    nuages = {}
    for k, (m, ref, role) in P.items():
        if noeuds[cle[k]] == 'cable':
            continue
        n = int(min(20000, max(800, m.area * 2)))
        nuages[k] = trimesh.sample.sample_surface(m, n, seed=3)[0]
    descend = {g: set() for g in G}
    for g in G:
        h = g
        while h:
            descend[h].add(g); h = G[h]['parent']
    mouvements = dict(abduction='caisson', tube='tube', pince='palonnier')
    rapport = {}
    for cmd, racine in mouvements.items():
        mobiles = [k for k in nuages if noeuds[cle[k]] in descend[racine]]
        fixes = [k for k in nuages if noeuds[cle[k]] not in descend[racine]]
        # on ne garde que les pièces fixes proches de la zone balayée (rayon de l'ensemble mobile)
        lo = np.min([P[k][0].bounds[0] for k in mobiles], 0); hi = np.max([P[k][0].bounds[1] for k in mobiles], 0)
        idx = np.concatenate([np.full(len(nuages[k]), i) for i, k in enumerate(fixes)])
        # une pièce « fixe » peut tourner elle aussi avec la commande (engrenage du D85MG avec le tube)
        entraines = {noeuds[cle[k]] for k in fixes if G[noeuds[cle[k]]]['commande'] == cmd}
        def arbre_fixe(T):
            return cKDTree(np.concatenate([trimesh.transformations.transform_points(nuages[k], T[noeuds[cle[k]]])
                                           if noeuds[cle[k]] in entraines else nuages[k] for k in fixes]))
        arbre = None if entraines else arbre_fixe(poses(G, {}))
        a0, a1 = COURSES[cmd]
        angles = np.unique(np.r_[np.arange(a0, a1 + 1e-6, pas), 0.0, a1])   # le bout de course, toujours
        jeu = {}
        for ang in angles:
            T = poses(G, {cmd: ang})
            arb = arbre or arbre_fixe(T)
            for k in mobiles:
                M = T[noeuds[cle[k]]]
                q = trimesh.transformations.transform_points(nuages[k], M)
                d, j = arb.query(q, distance_upper_bound=3.0)
                ok = np.isfinite(d)
                if not ok.any():
                    continue
                for f in np.unique(idx[j[ok]]):
                    sel = ok.copy(); sel[ok] = idx[j[ok]] == f
                    dm = float(d[sel].min())
                    c = (k, fixes[f])
                    if c not in jeu:
                        jeu[c] = dict(repos=None, mini=9.0, a=None, ou=None)
                    if ang == 0.0:
                        jeu[c]['repos'] = dm
                    if dm < jeu[c]['mini']:
                        jeu[c]['mini'], jeu[c]['a'] = dm, float(ang)
                        jeu[c]['ou'] = np.round(q[sel][d[sel].argmin()], 1).tolist()
        frotte = []
        for (k, f), v in jeu.items():
            repos = v['repos'] if v['repos'] is not None else 3.0
            refs = (P[k][1], P[f][1])
            if any(set(e) == set(refs) for e in ENGRENEMENTS):
                continue
            if v['mini'] < 0.3 and v['mini'] < repos - 0.15:
                frotte.append((v['mini'], v['a'], repos, k, f, v['ou']))
        frotte.sort()
        rapport[cmd] = [dict(jeu=round(a, 2), angle=b, repos=round(c, 2), mobile=k, fixe=f, point=o) for a, b, c, k, f, o in frotte]
        print('=== %s de %g à %g° : %d couple(s) qui se rapprochent à moins de 0,3 mm' % (cmd, a0, a1, len(frotte)))
        for a, b, c, k, f, o in frotte[:15]:
            print('   %5.2f mm à %+5.0f° (repos %.2f)  %-36s ↔ %-36s en %s' % (a, b, c, k[:36], f[:36], o))
    json.dump(rapport, open(os.path.join(SORTIE, 'balayage.json'), 'w'), ensure_ascii=False, indent=1)
    return rapport


def balayer_cables():
    """Les câbles suivent leurs ensembles point par point : jeu et longueur aux bouts de chaque course."""
    from audit import pieces_nouveau_bras
    from cables import CABLES
    P = pieces_nouveau_bras()
    A = json.load(open(os.path.join(SORTIE, 'animation.json')))
    G, noeuds = A['groupes'], A['noeuds']
    cle = {k: ('v5-%d' % int(k.split()[1]) if k.startswith('v5 ') else k) for k in P}
    nuages = {k: trimesh.sample.sample_surface(m, int(min(20000, max(800, m.area * 2))), seed=3)[0]
              for k, (m, ref, role) in P.items() if noeuds[cle[k]] != 'cable'}
    propres = {'câble du %s' % n: (c['servo'],) for n, c in CABLES.items()}
    propres['câble de pince'] = ('palonnier', 'main', 'HS-65MG')
    propre_a = lambda nom: next(v for k, v in propres.items() if nom.startswith(k))
    cas = [dict(abduction=a) for a in COURSES['abduction']] + [dict(tube=t) for t in COURSES['tube']] + \
          [dict(pince=p) for p in COURSES['pince']] + [{}]
    for c in A['cables']:
        print('--- %s' % c['nom'])
        for cmd in cas:
            T = poses(G, cmd)
            pts = np.array([trimesh.transformations.transform_points([p], T[g])[0] for p, g in zip(c['points'], c['groupes'])])
            if c['nom'] == 'câble de pince':
                pts[0] = trimesh.transformations.transform_points([bout_palonnier(cmd.get('pince', 0.0))], T['tube'])[0]
            L = float(np.linalg.norm(np.diff(pts, axis=0), axis=1).sum())
            ech = np.concatenate([a + np.outer(np.linspace(0, 1, max(2, int(np.linalg.norm(b - a) / 0.5))), b - a)
                                  for a, b in zip(pts[:-1], pts[1:])])
            pire = (9.0, None, None)
            for k, q in nuages.items():
                if any(e in k for e in propre_a(c['nom'])) or P[k][1].startswith('CABLE:') \
                        or (c['nom'].startswith('câble du HS-65MG') and noeuds[cle[k]] == 'tube'):
                    continue
                qq = trimesh.transformations.transform_points(q, T[noeuds[cle[k]]])
                lo, hi = qq.min(0) - 3, qq.max(0) + 3
                sel = np.all((ech > lo) & (ech < hi), axis=1)
                if not sel.any():
                    continue
                d, j = cKDTree(qq).query(ech[sel])
                i = int(d.argmin())
                if d[i] - c['rayon'] < pire[0]:
                    pire = (float(d[i]) - c['rayon'], k, np.round(ech[sel][i], 1).tolist())
            print('   %-16s longueur %6.1f mm ; jeu mini %5.2f mm avec %s en %s' % (
                ', '.join('%s %+g°' % kv for kv in cmd.items()) or 'repos', L, pire[0], (pire[1] or '')[:40], pire[2]))


if __name__ == '__main__':
    if 'cables' in sys.argv:
        balayer_cables()
    elif 'balayer' in sys.argv:
        balayer()
    else:
        ecrire()
