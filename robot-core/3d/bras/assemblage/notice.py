#!/usr/bin/env python3
"""
Notice de montage du bras : l'ordre des étapes, et pour chacune les pièces qui arrivent, d'où elles
arrivent et de combien.

On vérifie que chaque pièce peut glisser jusqu'à sa place sans traverser ce qui est déjà monté : on la
fait avancer millimètre par millimètre le long de son chemin, et on mesure à chaque pas le volume qu'elle
partage avec les pièces déjà posées. Les pièces qui se touchent au repos (une vis dans son taraudage,
deux faces en appui, les maillages allégés à 0,15 mm près) partagent déjà un peu de volume à leur place :
c'est cette part qui sert de référence, seul ce qui la dépasse compte comme un heurt.

La vérification ne voit qu'un trajet en ligne droite ; une pièce qu'il faut incliner ou tourner pour
l'engager est signalée dans le texte de l'étape.

Écrit notice.json sur le disque externe, que lit notice.html.
    notice.py           vérifie les trajets et écrit notice.json
    notice.py liste     liste les pièces (clé, référence, désignation, boîte)
"""
import json, os, re, sys
import numpy as np, trimesh, manifold3d as mf

ICI = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, ICI)
from recaler import SORTIE

TOLERANCE = 1.0          # mm³ partagés au-delà de ce qu'ils partagent à leur place
PAS = 1.0                # mm


def pieces():
    """{clé: dict(ref, nom, maillage)} : les pièces visibles du bras, avec les clés de la visionneuse
    (nom du nœud pour les pièces de bras.stl, « v5-k » pour celles de la v5)."""
    v5 = json.load(open(os.path.join(SORTIE, 'v5.json')))
    masquees = {n for l in v5['masquer'].values() for n in l}
    P = {}
    for f, origine in (('bras-assemble-leger.glb', True), ('bras-v5-leger.glb', False)):
        s = trimesh.load(os.path.join(SORTIE, f))
        for noeud in s.graph.nodes_geometry:
            if origine and noeud in masquees:
                continue
            T, g = s.graph[noeud]
            m = s.geometry[g].copy(); m.apply_transform(T); m.merge_vertices()
            if origine:
                ref, nom = re.sub(r'#\d+$', '', noeud), noeud
            else:
                p = v5['pieces'][int(noeud.split('-')[1])]
                ref, nom = p['ref'], p['nom']
            P[noeud] = dict(ref=ref, nom=nom, maillage=m)
    return P


def solide(m):
    s = mf.Manifold(mf.Mesh(vert_properties=np.asarray(m.vertices, np.float32),
                            tri_verts=np.asarray(m.faces, np.uint32)))
    if s.status() != mf.Error.NoError or s.volume() <= 0:
        # le servo goBILDA n'est pas fermé dans son STEP : son enveloppe convexe suffit pour les heurts
        h = m.convex_hull
        s = mf.Manifold(mf.Mesh(vert_properties=np.asarray(h.vertices, np.float32),
                                tri_verts=np.asarray(h.faces, np.uint32)))
    return s


def commun(a, b):
    return (a ^ b).volume()


def recouvrent(b1, b2, marge=0.5):
    return np.all(b1[0] <= b2[1] + marge) and np.all(b2[0] <= b1[1] + marge)


def axe_de_vis(m):
    """(direction de sortie, longueur) d'une vis : le long de son axe (axe principal de ses sommets, les vis de
    serrage des moyeux sont en biais), du côté de la tête, reconnue à ce qu'elle est plus large que la tige."""
    v = m.vertices - m.vertices.mean(0)
    a = np.linalg.eigh(v.T @ v)[1][:, -1]
    a[np.abs(a) < 1e-3] = 0; a /= np.linalg.norm(a)
    u = v @ a; L = np.ptp(u); u = (u - u.min()) / L
    rayon = lambda sel: np.linalg.norm(v[sel] - np.outer(v[sel] @ a, a), axis=1).max() if sel.any() else 0
    return (a if rayon(u > 0.9) > rayon(u < 0.1) else -a), L


def verifier(etapes, P):
    """Fait glisser chaque groupe de pièces le long de son chemin ; renvoie les heurts et les pièces
    qu'aucune étape ne pose."""
    S = {k: solide(p['maillage']) for k, p in P.items()}
    B = {k: p['maillage'].bounds for k, p in P.items()}
    poses = {None: []}                       # atelier -> clés posées ; None : le bras
    heurts, vues = [], set()
    for n, e in enumerate(etapes, 1):
        for mvt in e['mouvements']:
            if mvt.get('joindre'):                       # un sous-ensemble monté à part rejoint le bras
                bouge = poses.pop(mvt['joindre'])
                obstacles = poses[None]
            else:
                bouge = mvt['cles']
                for k in bouge:
                    assert k in P, k
                    assert k not in vues, f'{k} posée deux fois (étape {n})'
                    vues.add(k)
                obstacles = poses.setdefault(e.get('atelier'), [])
            # le chemin part de la place de la pièce et s'en éloigne, tronçon par tronçon
            points = [np.zeros(3)]
            for d, L in mvt['chemin']:
                d = np.asarray(d, float) / np.linalg.norm(d)
                points += [points[-1] + d * t for t in np.arange(PAS, L + 1e-6, PAS)]
            points = np.array(points[1:])
            if len(points):
                for k in bouge:
                    balaye = np.array([B[k][0] + points.min(0), B[k][1] + points.max(0)])
                    for o in obstacles:
                        if not recouvrent(balaye, B[o]):
                            continue
                        base, pire, ou = commun(S[k], S[o]), 0.0, 0
                        for p in points:
                            v = commun(S[k].translate(tuple(p)), S[o]) - base
                            if v > pire: pire, ou = v, np.linalg.norm(p)
                        if pire > mvt.get('tol', TOLERANCE):
                            heurts.append((n, e['titre'], k, P[k]['nom'], o, P[o]['nom'], round(pire, 1), ou))
            (poses[None] if mvt.get('joindre') else obstacles).extend(bouge)
    oubliees = [k for k in P if k not in vues]
    return heurts, oubliees


# ── les étapes ───────────────────────────────────────────────────────────────────────────────────────

def etapes(P):
    def cles(motif, ou=lambda c: True):
        """Les pièces dont le nom ou la référence répond au motif, filtrées par la position de leur centre."""
        r = [k for k, p in P.items() if (re.search(motif, p['nom']) or re.search(motif, p['ref']))
             and ou(p['maillage'].bounds.mean(0))]
        assert r, motif
        return r

    def glisse(c, d, dist, *suite):
        """Chemin de la pièce, depuis sa place : dist mm dans la direction d, puis les tronçons suivants
        ((direction, distance), …)."""
        return dict(cles=list(c), chemin=[[list(map(float, d)), float(dist)]] + [[list(map(float, a)), float(b)] for a, b in suite])

    def visser(c, marge=4.0):
        # une vis dans un taraudage partage avec lui un peu plus de volume selon la position du filet
        mvts = []
        for k in c:
            d, L = axe_de_vis(P[k]['maillage'])
            mvts.append(dict(glisse([k], d, L + marge), tol=5.0))
        return mvts

    def ecrou(c, d, dist=10):
        return dict(glisse(c, d, dist), tol=3.0)

    def poser(c):
        return dict(cles=list(c), chemin=[])

    E = []

    def etape(titre, texte, *mvts, atelier=None, outils=None):
        plat = []
        def aplatir(m):
            if isinstance(m, list):
                for x in m: aplatir(x)
            else:
                plat.append(m)
        aplatir(list(mvts))
        E.append(dict(titre=titre, texte=texte, mouvements=plat, atelier=atelier, outils=outils))

    X, Y, Z = (1, 0, 0), (0, 1, 0), (0, 0, 1)
    mX, mY, mZ = (-1, 0, 0), (0, -1, 0), (0, 0, -1)
    k = lambda *n: list(n)

    # ── l'épaule ──
    etape('Les blocs d\'épaule',
          'Les deux blocs 1201 se posent face à face ; la plaque 1116 les relie à l\'arrière par quatre vis '
          'M4 × 5 dans leurs taraudages (plus longues, elles buteraient sur les vis des couronnes).',
          poser(k('1201-0027-0001#1')), glisse(k('1201-0027-0001#2'), mY, 25),
          glisse(cles(r'^1116-'), mX, 20), visser(cles(r'plaque 1116 ↔ bloc')))
    etape('Le moyeu côté bras',
          'Le moyeu 1311 se pose contre la face extérieure du bloc côté bras, vis de serrage vers l\'avant. Les quatre '
          'écrous Nylstop entrent dans les poches du bloc par l\'intérieur de l\'épaule ; les vis M4 × 18 '
          'viennent de l\'extérieur.',
          glisse(k('1311-0016-4008#1'), Y, 20),
          glisse(k('2812-0004-0007#9', '2812-0004-0007#10', '2812-0004-0007#17', '2812-0004-0007#18'), mY, 10),
          visser(k('2802-0004-0018#2', '2802-0004-0018#4', '2802-0004-0018#7', '2802-0004-0018#8')))
    etape('Le moyeu retourné et le goTUBE de flexion',
          'Côté corps, le moyeu 1311 est retourné : son bossage vers l\'extérieur entre dans l\'alésage du goTUBE 48. '
          'Les quatre vis M4 × 18 se posent tête dans le bloc, depuis l\'intérieur de l\'épaule, traversent bloc '
          'et moyeu et se vissent dans le goTUBE. Clé Allen coudée, petit bras : l\'autre bloc est à 24 mm.',
          glisse(cles(r'1311-0016-4008#2 retourné'), mY, 20), glisse(cles(r'^goTUBE 48'), mY, 30),
          visser(cles(r'retournée — bloc 1201')))
    for haut, nom, sens in ((True, 'haute', 'au-dessus'), (False, 'basse', 'au-dessous')):
        s_ = 1 if haut else -1
        cote = lambda c, s_=s_: c[2] * s_ > 0
        vis_c = cles(r'^2802-0004-0014#', cote)
        rond_c = cles(r'^2801-0004-0008#', lambda c, s_=s_: c[2] * s_ > 10)
        paire = []
        for v in vis_c:                                   # chaque vis avec sa rondelle
            cv = P[v]['maillage'].bounds.mean(0)
            r_ = min(rond_c, key=lambda r: np.linalg.norm(P[r]['maillage'].bounds.mean(0)[:2] - cv[:2]))
            d, L = axe_de_vis(P[v]['maillage'])
            paire.append(glisse([v, r_], d, L + 4))
        at = 'couronne ' + nom
        etape(f'La couronne {nom}, à l\'établi',
              'Le moyeu 1526 reçoit ses deux roulements à collerette 1601 (un de chaque côté). La couronne de '
              '72 dents se pose dessous ; quatre vis M4 × 14 avec rondelles la serrent par l\'intérieur.',
              poser(cles(r'^1526-', cote)),
              [glisse([p], (0, 0, -s_), 15) for p in cles(r'^1601-0014-0004#', lambda c, s_=s_: 0 < c[2] * s_ < 26)],
              [glisse([p], (0, 0, s_), 15) for p in cles(r'^1601-0014-0004#', lambda c, s_=s_: c[2] * s_ > 26)],
              glisse(cles(r'^2312-0414-0072#', cote), (0, 0, -s_), 15),
              paire, atelier=at)
        etape(f'La couronne {nom} sur l\'épaule',
              f'Une rondelle et une entretoise de 4 mm sur chacun des quatre taraudages {sens} des blocs ; la '
              f'couronne par-dessus, quatre vis M4 × 16.',
              [glisse([r_], (0, 0, s_), 15) for r_ in cles(r'^2807-0407-0500#', lambda c, s_=s_: 10 < c[2] * s_ < 16)],
              [glisse([e_], (0, 0, s_), 15) for e_ in cles(r'^1502-0006-0040#', cote)],
              dict(joindre=at, chemin=[[[0, 0, s_], 30]]),
              visser(cles(r'^2802-0004-0016#', cote)))
    etape('Les plaques du caisson sur le pivot',
          'Sur chaque moyeu : une rondelle de 0,5 mm sur le roulement, un réducteur 14 → 4 mm, la plaque 1108, '
          'le second réducteur dans son grand trou. La vis épaulée M4 × 20 se glisse entre les deux blocs, depuis '
          'l\'avant, puis remonte dans les roulements ; écrou Nylstop dessus. L\'arbre de flexion n\'est pas encore '
          'là : c\'est ce qui laisse la place de la vis.',
          glisse(k('2807-0407-0500#2'), Z, 20), glisse(k('2904-0004-0014#3'), Z, 20),
          glisse(k('1108-0001-0002#2'), Z, 20), glisse(k('2904-0004-0014#2'), Z, 20),
          glisse(k('2807-0407-0500#9'), mZ, 20), glisse(k('2904-0004-0014#4'), mZ, 20),
          glisse(k('1108-0001-0002#1'), mZ, 20), glisse(k('2904-0004-0014#1'), mZ, 20),
          glisse(k('2800-0004-0020#1'), mZ, 29, (X, 40)), glisse(k('2812-0004-0007#15'), Z, 12),
          glisse(k('2800-0004-0020#2'), Z, 29, (X, 40)), glisse(k('2812-0004-0007#13'), mZ, 12))

    # ── le servo d'abduction, à l'établi ──
    etape('Le servo d\'abduction dans son cadre',
          'Le servo goBILDA entre dans le cadre 1802 par le dessous, cannelure vers le bas. Quatre vis M4 × 10 '
          'avec rondelles, par-dessous. Le pignon laiton de 48 dents s\'enfonce sur la cannelure.',
          poser(cles(r'^1802-')), glisse(cles(r'^2000-'), mZ, 45),
          [dict(glisse([v, r_], mZ, 14), tol=5.0) for v, r_ in (('2800-0004-0010#1', '2801-0004-0008#6'), ('2800-0004-0010#2', '2801-0004-0008#4'),
                                                 ('2800-0004-0010#3', '2801-0004-0008#7'), ('2800-0004-0010#4', '2801-0004-0008#12'))],
          glisse(cles(r'pignon laiton 48'), mZ, 15), atelier='module')

    # ── la cloison et le servo du tube ──
    etape('Les écrous de la cloison',
          'Cinq écrous Nylstop glissent dans les fentes de la cloison, par le côté où chaque fente débouche : '
          'ils recevront les vis des profilés et de la plaque latérale y+. Les deux de la plaque y− viennent plus '
          'tard : leurs fentes servent d\'abord de passage aux vis du palier.',
          poser(cles(r'^cloison structurelle')),
          [ecrou([e_], (1 if P[e_]['maillage'].bounds.mean(0)[0] > 108 else -1, 0, 0))
           for e_ in cles(r'^écrou .*— (profilé|plaque 96 y\+)')], atelier='module')
    etape('Le servo du tube (D85MG)',
          'Le D85MG entre dans sa poche par le côté y+, oreilles contre la face avant, la sortie du câble en bas '
          'vient se loger dans le passage, à l\'arrière. Deux vis M2 du servo dans les oreilles. L\'engrenage laiton '
          'de 20 dents sur sa cannelure.',
          glisse(cles(r'^D85MG'), Y, 30), visser(cles(r'vis M2 du D85MG')),
          glisse(cles(r'engrenage laiton 2305'), X, 12), atelier='module')
    etape('Le palier du tube',
          'Le palier 1604 se pose sur les deux bras de la cloison. Ses deux vis M4 × 16 entrent par l\'arrière de la '
          'cloison, dans l\'alésage qui prolonge la fente de l\'écrou de la plaque latérale y− (cet écrou-là se pose '
          'ensuite). Le roulement entre dans le palier, collerette à l\'avant, puis les écrous Nylstop sur les vis. '
          'Les deux écrous de la plaque y− glissent alors dans leurs fentes.',
          glisse(cles(r'^1604-0043-0032#3'), X, 25),
          [dict(glisse([v], mX, 50), tol=5.0) for v in cles(r'^vis 2802-0004-0016 — palier')],
          glisse(cles(r'^1601-0039-0032#2'), X, 15), ecrou(cles(r'^écrou .*— palier'), X),
          [ecrou([e_], mX) for e_ in cles(r'^écrou .*— plaque 96 y−')], atelier='module')
    etape('La liaison du tube',
          'Le pignon Slip-Fit de 20 dents (2322) se clipse sur l\'embout REX de la liaison imprimée : on le pousse '
          'jusqu\'à l\'épaulement, les trois doigts fendus de l\'embout plient et la lèvre revient derrière lui (tenir le '
          'fil du HS-65MG au centre). L\'ensemble entre dans le roulement par l\'avant, le pignon passe dans l\'alésage et '
          'vient engrener avec celui du D85MG ; la collerette de la liaison arrête l\'ensemble contre le roulement.',
          glisse(cles(r'^liaison et moyeu') + cles(r'^pignon Slip-Fit'), X, 60), atelier='module')
    etape('Le goTUBE du bras',
          'Une cale inox et deux bagues sur le bout du goTUBE de 96 mm, qui se visse sur la face avant de la '
          'liaison : quatre vis M4 × 10 par l\'intérieur de la liaison, clé coudée par la fenêtre du dessus.',
          glisse(cles(r'^bague '), X, 100), glisse(cles(r'^4103-0032-0096'), X, 100),
          [dict(glisse([v], mX, 16, (Z, 30)), tol=5.0) for v in cles(r'liaison ↔ goTUBE')], atelier='module')
    etape('Le servo de pince (HS-65MG)',
          'Le fil du HS-65MG passe d\'abord dans l\'alésage de la liaison, sans sa prise (elle se sertit à la fin). '
          'Le servo descend dans la liaison, deux vis M2 dans les oreilles ; le palonnier sur la cannelure.',
          glisse(cles(r'^HS-65MG'), Z, 40), visser(cles(r'vis M2 du HS-65MG')),
          glisse(cles(r'^palonnier'), Z, 15), atelier='module')
    etape('Les paliers avant',
          'Chaque roulement dans son palier 1604, collerette à l\'avant ; les deux paliers s\'enfilent sur le tube '
          'par l\'avant.',
          dict(glisse(k('1604-0043-0032#2', '1601-0039-0032#1'), X, 100), tol=5.0),
          dict(glisse(k('1604-0043-0032#1', '1601-0039-0032#3'), X, 100), tol=5.0), atelier='module')

    # ── le caisson ──
    etape('La plaque latérale y−',
          'Les deux plaques 1123 du côté y− (96 et 72 mm) ferment le module d\'un côté : trois vis M4 × 8 dans le '
          'cadre du servo, deux M4 × 12 dans les écrous de la cloison, deux M4 × 5 dans le palier du tube (plus '
          'longues, elles buteraient sur les vis qui le traversent) et quatre M4 × 8 dans les paliers avant.',
          glisse(k('1123-0043-0096#1', '1123-0043-0072#1'), mY, 20),
          visser(cles(r'cadre 1802 ↔ plaque 96', lambda c: c[1] < 0)),
          visser(cles(r'^vis 2802-0004-0012 — plaque 96 y−')),
          visser(cles(r'plaque 72 y− ↔ palier avancé') + k('2802-0004-0008#4', '2802-0004-0008#7', '2802-0004-0008#8', '2802-0004-0008#16')),
          atelier='module')
    etape('La plaque latérale y+',
          'Deux entretoises de 43 mm vissées sur la plaque y− ; les deux plaques du côté y+ par-dessus, avec la même '
          'visserie, plus une vis dans la cloison et deux dans les entretoises.',
          glisse(cles(r'^entretoise 1501'), Y, 30),
          visser(cles(r'entretoise 43 mm', lambda c: c[1] < 0)),
          glisse(k('1123-0043-0096#2', '1123-0043-0072#2'), Y, 20),
          visser(cles(r'cadre 1802 ↔ plaque 96', lambda c: c[1] > 0)),
          visser(cles(r'^vis 2802-0004-0012 — plaque 96 y\+')),
          visser(cles(r'entretoise 43 mm', lambda c: c[1] > 0)),
          visser(cles(r'2802-0004-0008#[13] avancé') + k('2802-0004-0008#6', '2802-0004-0008#11', '2802-0004-0008#12', '2802-0004-0008#14')),
          atelier='module')
    etape('Le module rejoint l\'épaule',
          'Le module glisse vers l\'arrière entre les deux plaques 1108, jusqu\'à ce que le pignon de 48 dents engrène '
          'avec la couronne du bas (tourner légèrement le pignon pour engager les dents).',
          dict(joindre='module', chemin=[[[1, 0, 0], 60]]))
    etape('Les profilés',
          'Les deux profilés 1121 glissent depuis l\'avant, sous les plaques 1108 et par-dessus le module, jusqu\'à '
          'l\'épaule.',
          # les ailes frottent sur la tranche des plaques latérales (le recalage laisse 0,1 mm de roulis) : 6 mm³
          dict(glisse(k('1121-0006-0168#2'), X, 80), tol=6.0), dict(glisse(k('1121-0006-0168#1'), X, 80), tol=6.0))
    ecr = {'2802-0004-0010#1': '2812-0004-0007#5', '2802-0004-0010#2': '2812-0004-0007#4', '2802-0004-0010#3': '2812-0004-0007#2',
           '2802-0004-0010#4': '2812-0004-0007#7', '2802-0004-0010#5': '2812-0004-0007#6', '2802-0004-0010#6': '2812-0004-0007#11',
           '2802-0004-0010#7': '2812-0004-0007#16', '2802-0004-0010#8': '2812-0004-0007#8'}
    etape('Les profilés sur les plaques 1108',
          'Huit vis M4 × 10 traversent plaques 1108 et fonds des profilés ; les écrous Nylstop se tiennent par '
          'l\'intérieur du caisson, de part et d\'autre du servo d\'abduction. Puis quatre vis M4 × 12 relient les '
          'profilés aux écrous de la cloison.',
          [[ecrou([e_], (0, 0, -np.sign(P[e_]['maillage'].bounds.mean(0)[2])), 6), visser([v])] for v, e_ in ecr.items()],
          visser(cles(r'^vis 2802-0004-0012 — profilé')))
    etape('Les câbles',
          'Les trois câbles de servo partent vers l\'épaule. Le fil du HS-65MG sort de la liaison par l\'arrière, '
          'traverse la cloison et reçoit sa rallonge, soudée sous gaine dans le couloir au-dessus du cadre. Les câbles '
          'des deux Hitec, empilés, passent côté +x de l\'épaule ; celui du servo d\'abduction côté −x. Tous '
          'passent au-dessus de l\'emplacement de l\'arbre et ressortent par le goTUBE de flexion, prises comprises : '
          'l\'arbre n\'est pas encore là. La prise du HS-65MG se sertit à la sortie.',
          poser(cles(r'^câble du (D85MG|HS-65MG|servo d)') + cles(r'^prise du câble') + cles(r'^bout du câble du HS-65MG')
                + cles(r'^soudure du fil')))
    etape('L\'arbre de flexion',
          'L\'arbre REX de 8 mm entre par le goTUBE de flexion, traverse les deux moyeux 1311 sous les câbles, profil '
          'aligné sur celui des moyeux. Les deux vis de serrage de chaque moyeu le bloquent.',
          glisse(cles(r'^arbre REX'), mY, 120),
          visser(k('2800-0004-0014#1', '2800-0004-0014#2')), visser(cles(r'2800-0004-0014#[34] retourné')))
    etape('La plaque de bout, à l\'établi',
          'Un écrou Nylstop dans chacune des quatre poches des pattes, par la face libre (un morceau de ruban '
          'adhésif les retient le temps de la pose).',
          poser(cles(r'^plaque de bout')),
          [ecrou([e_], (0, 0, -np.sign(P[e_]['maillage'].bounds.mean(0)[2]))) for e_ in cles(r'^écrou .*— plaque de bout')],
          atelier='plaque de bout')
    etape('La plaque de bout',
          'La plaque s\'enfile sur le tube et ferme le bout du caisson, pattes dans les profilés. Quatre vis M4 × 10 par '
          'les trous des profilés.',
          dict(joindre='plaque de bout', chemin=[[[1, 0, 0], 90]]),
          visser(cles(r'^vis 2802-0004-0010 — plaque de bout')))
    etape('Le câble de pince',
          'Le câble de pince part du palonnier du HS-65MG et file dans le goTUBE jusqu\'à la main (pas encore dessinée).',
          poser(cles(r'^câble de pince') + cles(r'^main')))
    return E


def main():
    P = pieces()
    E = etapes(P)
    heurts, oubliees = verifier(E, P)
    for h in heurts:
        print('HEURT étape %d (%s) : %s [%s] contre %s [%s] : %.1f mm³ à %.0f mm' % h)
    print('%d étapes, %d heurts, %d pièces sans étape' % (len(E), len(heurts), len(oubliees)))
    if len(sys.argv) > 1 and sys.argv[1] == 'oubliees':
        for k in oubliees: print('  ', k, P[k]['nom'])
    # la notice montre aussi ce qui ne passe pas encore : une étape fausse ne doit pas avoir l'air juste
    for n, e in enumerate(E, 1):
        e['heurts'] = [dict(piece=h[3], contre=h[5], volume=float(h[6]), a=float(h[7])) for h in heurts if h[0] == n]
    json.dump(dict(etapes=E), open(os.path.join(SORTIE, 'notice.json'), 'w'), ensure_ascii=False, indent=1)


if __name__ == '__main__':
    main()
