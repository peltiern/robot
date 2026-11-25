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
from pince import COTE_POUCE, PRECHARGE, SPIRES

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
            if mvt.get('joindre'):                       # un sous-ensemble monté à part rejoint le bras (ou un autre)
                bouge = poses.pop(mvt['joindre'])
                obstacles = poses.setdefault(e.get('atelier'), [])
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
            obstacles.extend(bouge)
    oubliees = [k for k in P if k not in vues]
    return heurts, oubliees


# ── les étapes ───────────────────────────────────────────────────────────────────────────────────────

def etapes(P):
    def cles(motif, ou=lambda c: True):
        """Les pièces dont le nom ou la référence répond au motif, filtrées par la position de leur centre."""
        # La jonction (jonction.py) reprend des références du bras (1604, 1802, vis) : ses pièces ne répondent qu'aux
        # motifs qui la nomment, sinon les étapes du bras les poseraient aussi.
        r = [k for k, p in P.items() if (re.search(motif, p['nom']) or re.search(motif, p['ref']))
             and p['nom'].startswith('jonction') == ('jonction' in motif)
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
          'Côté corps, le moyeu 1311 est retourné : son bossage vers l\'extérieur entre dans l\'alésage du goTUBE de flexion (72 mm). '
          'Les quatre vis M4 × 18 se posent tête dans le bloc, depuis l\'intérieur de l\'épaule, traversent bloc '
          'et moyeu et se vissent dans le goTUBE. Clé Allen coudée, petit bras : l\'autre bloc est à 24 mm.',
          glisse(cles(r'1311-0016-4008#2 retourné'), mY, 20), glisse(cles(r'^goTUBE de flexion'), mY, 30),
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
    etape('Le goTUBE sur le berceau de la liaison, à l\'établi',
          'Le berceau de la liaison (la pièce imprimée qui portera le HS-65MG) se visse sur le bout du goTUBE de 72 mm : '
          'quatre vis M4 × 12 par l\'intérieur, clé coudée par l\'arrière du berceau, encore ouvert. La bague fendue '
          'imprimée se clipse de côté sur le goTUBE, contre le berceau, lèvre vers l\'avant : c\'est elle qui empêche '
          'le tube de glisser vers la main. Elle se pose aussi plus tard, bras monté, sans rien démonter.',
          poser(cles(r'^liaison, berceau')), glisse(cles(r'^bague '), X, 100), glisse(cles(r'^4103-0032-0072 goTUBE du bras'), X, 100),
          [dict(glisse([v], mX, 16, (Z, 30)), tol=5.0) for v in cles(r'liaison ↔ goTUBE')], atelier='liaison')
    etape('Le servo de pince (HS-65MG), à l\'établi',
          'Le servo entre dans le berceau par l\'arrière, comme un tiroir, oreilles sur le haut des parois : le passe-fil '
          'et le bout du câble glissent dans la fente basse de la paroi y−. Deux vis M2 dans les oreilles. De '
          'l\'extérieur, dans la fenêtre : le câble plat remonte contre le bout du servo, un pli à 45° le renvoie vers '
          'l\'arrière, debout ; on le pousse dans le couloir de la paroi par la fente basse, et il ressort à l\'arrière '
          'du berceau. Le palonnier sur la cannelure.',
          glisse(cles(r'^HS-65MG'), mX, 30), visser(cles(r'vis M2 du HS-65MG')),
          glisse(cles(r'^palonnier'), Z, 15), atelier='liaison')
    etape('La partie arrière de la liaison, à l\'établi',
          'Le fil du HS-65MG passe dans l\'alésage de la partie arrière, par la gorge de sa face, sans sa prise (elle se '
          'sertit à la fin). Le câble se couche dans la gorge, tourné vers le centre ; un second pli à 45° le mène à '
          'l\'alésage. On pousse la partie arrière à plat contre le berceau, en tirant le fil : sa dent glisse dans la '
          'rainure du fond, ses pattes dans les encoches du haut des parois. Une vis M2 × 6 '
          'dans chaque patte. Le pignon Slip-Fit de 20 dents (2322) se clipse sur l\'embout REX : on le pousse jusqu\'à '
          'l\'épaulement, les trois doigts fendus de l\'embout plient et la lèvre revient derrière lui (tenir le fil au '
          'centre).',
          glisse(cles(r'^liaison, partie arrière'), mX, 30), visser(cles(r'partie arrière ↔ berceau')),
          glisse(cles(r'^pignon Slip-Fit'), mX, 20), atelier='liaison')
    etape('La liaison du tube',
          'L\'ensemble (liaison, HS-65MG, goTUBE) entre dans le roulement par l\'avant, le pignon passe dans l\'alésage '
          'et vient engrener avec celui du D85MG ; la collerette de la liaison arrête l\'ensemble contre le roulement.',
          dict(joindre='liaison', chemin=[[[1, 0, 0], 60]]), atelier='module')
    etape('Les paliers avant',
          'Chaque roulement dans son palier 1604, collerette à l\'avant ; les deux paliers s\'enfilent sur le tube '
          'par l\'avant.',
          # Roulement Ø32 sur goTUBE Ø32 : le volume commun des deux maillages facettés monte jusqu'à 8,1 mm³ selon la
          # position (même courbe avec le goTUBE de 96 et de 72, décalée de 24 mm). Avec le 72, la pose de repos n'en
          # garde que 1,2 mm³ au lieu de 4,0 : l'écart dépasse 5 sans qu'aucune pièce ne gêne (2026-11-25).
          dict(glisse(k('1604-0043-0032#2', '1601-0039-0032#1'), X, 100), tol=7.5),
          dict(glisse(k('1604-0043-0032#1', '1601-0039-0032#3'), X, 100), tol=7.5), atelier='module')

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
          # Les prises ne sont posées qu'à l'étape de la roue de flexion : jusque-là elles pendent et passent au milieu
          # des roulements de la jonction (Ø32) ; leur place dessinée est dans les fenêtres de la roue.
          poser(cles(r'^câble du (D85MG|HS-65MG|servo d)') + cles(r'^bout du câble du HS-65MG')
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
    # ── la main (pince.py) ──
    # aucune colle (Nicolas, 2026-11-09) : chaque nœud est retenu par une poche ou une perle, qu'il ne peut pas passer
    # la « perle » est un bout de gaine PTFE 1 × 2 mm (Nicolas l'avait déjà) : Ø 2 bloque dans les trous du fléau, qui
    # sortent vers Ø 1,4, et tient sous les bouts du fléau, où une perle de Ø 3 frôlait le fond de la chambre en fin de course
    NOEUD = ('nœud en huit fait trois fois, derrière une « perle » : un bout de 2 mm coupé au cutter dans la gaine PTFE '
             '1 × 2 mm, enfilé avant le nœud (une perle dure de Ø 2 à 2,5 mm convient aussi, pas plus grosse). Tirer fort '
             'pour vérifier que le nœud ne traverse pas la perle')
    etape('Le câble de pince',
          'La tresse s\'attache au bout du bras du palonnier du HS-65MG : elle monte par-dessous dans le trou du bout, '
          'et se bloque dessus : ' + NOEUD + '. Le nœud reste au-dessus du bras : dessous, le boîtier du servo n\'est '
          'qu\'à 3 mm, alors qu\'au-dessus il reste plus de 2 mm sur toute la course. Puis la tresse passe la lumière '
          'du berceau, file tout droit dans le canal du haut du goTUBE et sort au bout du tube ; on la laisse longue.',
          poser(cles(r'^câble de pince \(')))
    etape('Le poignet',
          'Le poignet se pose sur le bout du goTUBE, son téton dans l\'alésage central, la tresse passée dans le petit '
          'trou du fond, face au canal du haut. Quatre vis M4 × 8 par l\'intérieur, dans les taraudages du bout du tube.',
          glisse(cles(r'^main, poignet'), X, 40), visser(cles(r'— main, poignet ↔ goTUBE')))
    # Les brins se nouent au fléau AVANT la paume : une fois vissée, elle ferme la chambre et plus rien n'y passe
    # (l'ordre « brins à la fin » de la première notice, 2026-11-08, ne pouvait pas se monter)
    etape('Le pouce, à l\'établi',
          'Le pouce est imprimé d\'une pièce avec son moyeu et sa poulie (base et bout figés ; des supports sous le '
          'pouce, qui déborde du moyeu des deux côtés). Deux roulements 1600-0410-0004 entrent par le côté A du moyeu : le premier '
          'jusqu\'à la poulie, le second affleurant. Le brin du pouce (une tresse d\'environ 15 cm) entre dans le trou '
          'au fond de la gorge, ressort dans la poche du bord de la poulie et s\'y bloque d\'un nœud en huit fait trois '
          'fois, plus gros que le trou ; on le tire pour rentrer le nœud dans la poche. Enfin le ressort du pouce. Les deux '
          'ressorts s\'enroulent à la main, en fil inox 1.4310 Ø 0,6 sur la queue d\'un foret Ø 3,5 : %d spires '
          'jointives, l\'un dans un sens, l\'autre dans l\'autre (ils sont le miroir l\'un de l\'autre). À la pince '
          'plate, chaque bout part droit des spires puis se coude à angle droit, le long de l\'axe : une patte courte '
          '(2 mm puis 2 mm, dans la joue) et une longue (4,5 mm puis 3 mm, dans la pièce qui tourne). La longue se '
          'pique dans le petit trou de la face A du moyeu. Côté poulie, la bague d\'appui (imprimée, Ø6,4, un nez de '
          'Ø5,1) entre dans l\'alésage de la poulie, nez en premier, contre la bague intérieure du roulement : une '
          'pointe de graisse la retient le temps que l\'axe la traverse. C\'est elle qui porte la poussée du brin, '
          'au lieu de la poulie contre la joue B.' % SPIRES,
          poser(cles(r'^main, pouce : pouce')),
          dict(glisse(cles(r'^main, pouce : roulement côté B'), mZ, 20), tol=3.0),
          dict(glisse(cles(r'^main, pouce : roulement côté A'), mZ, 15), tol=3.0),
          dict(glisse(cles(r'^main : ressort de rappel du pouce'), mZ, 10), tol=3.0),
          dict(glisse(cles(r"^main : bague d'appui"), Z, 10), tol=3.0), atelier='pouce')
    etape('Le doigt A, à l\'établi',
          'Le doigt A est imprimé d\'une pièce (jointure et phalange figée). Un roulement dans la jointure. '
          'Les doigts A et B sont les deux moitiés d\'un même doigt, coupé en deux tranches le long de l\'axe : '
          'écartées près de l\'axe, où passent la paume et le pouce, elles se touchent à plat plus loin, au milieu du '
          'doigt. Sur cette face plate du doigt A (pas sur la jointure), deux trous Ø1,7, l\'un vers la base du doigt, '
          'l\'autre 28,5 mm plus loin vers le bout : deux chevilles de filament PLA 1,75 mm, coupées droites à 11 mm, '
          's\'y enfoncent au maillet de moitié, où elles serrent : pas de colle. Un trou bouché à l\'impression se '
          'rouvre au foret de 1,5 mm tourné à la main. Le ressort des doigts : sa patte la plus longue se pique dans le '
          'petit trou Ø1 de la face intérieure de la jointure, près du roulement (pas dans un trou de cheville). '
          'Les deux trous Ø4,8 qui traversent le doigt de part en part, lamés sur sa face extérieure, sont ceux des vis '
          'qui le serreront au doigt B.',
          poser(cles(r'^main, doigts : doigt A')),
          dict(glisse(cles(r'^main, doigts : roulement de la jointure A'), Z, 15), tol=3.0),
          dict(glisse(cles(r'^main : ressort de rappel des doigts'), Z, 10), tol=3.0), atelier='doigt A')
    etape('Le doigt B, à l\'établi',
          'Le doigt B, d\'une pièce lui aussi, porte la poulie des doigts ; son roulement entre à travers elle. '
          'Ses faces plates tournées vers le doigt A sont prolongées en cales, qui viennent contre la face du doigt A ; '
          'les deux trous Ø1,7 des chevilles y sont, en face de ceux du doigt A (même conseil s\'ils sont bouchés), '
          'ainsi que deux avant-trous Ø3,5 pour les vis. '
          'Le brin des doigts se noue dans l\'ancrage de cette poulie, comme celui du pouce.',
          poser(cles(r'^main, doigts : doigt B')),
          dict(glisse(cles(r'^main, doigts : roulement de la jointure B'), mZ, 15), tol=3.0), atelier='doigt B')
    etape('Le fléau et les brins',
          'Une marque au feutre sur chaque brin, à 66,5 mm de son nœud de poulie. Chaque brin passe par le dessus dans '
          'sa fente de la paume (pas encore vissée), puis dans le trou du bout du fléau, et se bloque dessous à la '
          'marque : ' + NOEUD + '. Le brin du pouce prend la fente et le trou les plus proches du milieu, celui des '
          'doigts ceux du bord. Puis la tresse du servo monte par le trou du milieu du fléau, par dessous, et se bloque '
          'au-dessus de la même façon, sa perle de gaine logée dans le chambrage : palonnier à 0°, tresse tendue, le dessus du fléau à 1,5 mm sous le bord du '
          'poignet. Une erreur d\'un millimètre se rattrape en réglant la course du servo.',
          dict(glisse(cles(r'^main, fléau'), X, 25), tol=3.0))
    etape('La paume',
          'La paume descend le long des brins et ferme la chambre du fléau : ses deux joues debout, côté canal pour la '
          'joue mince. Deux vis M4 × 10 entre les joues, dans les bossages du poignet. Le petit tube qui prolonge la '
          'joue B vers l\'extérieur autour du trou de l\'axe est l\'appui du roulement du doigt B : il entrera dans '
          'l\'alésage de sa poulie.',
          glisse(cles(r'^main, paume'), X, 40), visser(cles(r'— main, paume ↔ poignet')))
    etape('Le pouce entre les joues',
          'Le pouce glisse entre les joues depuis son côté, roulements dans l\'axe des trous des joues, son brin '
          'enroulé par-dessus la poulie, ouvert d\'environ %d° de plus qu\'au repos : la patte libre du ressort entre '
          'dans la rainure de la joue A par son bout ouvert et va jusqu\'au fond. On ramène le pouce au repos : le '
          'ressort s\'arme et tient sa patte au fond de la rainure.' % PRECHARGE['pouce'],
          dict(joindre='pouce', chemin=[[[0, COTE_POUCE, 0], 45]]))
    etape('Les doigts sur la paume',
          'Le doigt A vient de son côté, ouvert d\'environ %d° de plus qu\'au repos : la patte libre du ressort se '
          'pique dans le trou de la face extérieure de la joue A. On le ramène au repos, ce qui arme le ressort, et on '
          'l\'y tient. Puis le doigt B vient du sien, son brin enroulé par-dessus la poulie : ses trous s\'enfilent sur '
          'les chevilles du doigt A, à force, jusqu\'à ce que ses cales soient à plat contre la face du doigt A. C\'est '
          'par les chevilles que le doigt A suit : seule la jointure B porte une poulie. Deux vis M4 × 50 (2802-0004-0050) '
          'traversent le doigt A depuis sa face extérieure et se taraudent dans B, jusqu\'à ce que leur tête soit au '
          'fond du lamage, sans forcer au-delà. Ce sont elles qui empêchent A de quitter B quand la main se retourne ; '
          'les chevilles seules ne le tenaient que par frottement.' % PRECHARGE['doigts'],
          dict(joindre='doigt A', chemin=[[[0, 0, -1], 50]]), dict(joindre='doigt B', chemin=[[[0, 0, 1], 50]]),
          visser(cles(r'^main, doigts : vis 2802-0004-0050')),
          # les brins, noués depuis l'étape du fléau, ne se dessinent qu'ici, main en place : posés plus tôt dans leur
          # position finale, ils ne suivraient pas leur poulie quand pouce et doigt B entrent, et le contrôle les heurte
          poser(cles(r'^câble de pince, brin')))
    etape('L\'axe',
          'La tige Ø4 × 78 entre par le siège du cache B et traverse tout : le doigt B, son roulement, la joue B, les '
          'roulements du pouce, la joue A, le roulement et le doigt A. Elle force dans les joues : si elle ne passe '
          'pas à la main, repasser les trous des joues au foret de 4 tourné à la main. On la centre, chaque bout à '
          '2 mm sous le fond de son siège : la rondelle de l\'étape suivante doit entrer à plat dans sa poche. Ce '
          'sont les caches qui l\'empêchent de sortir. Le doigt tourne autour d\'elle sur toute sa longueur : s\'il '
          'frotte, repasser son trou au foret de 4,5.',
          dict(glisse(cles(r'^main : axe'), Z, 80), tol=3.0))
    etape('Les caches',
          'Les caches tiennent par aimant, sans colle : on peut les ôter pour atteindre l\'axe. Une rondelle '
          '2801-0004-0008 se serre dans la poche au fond du siège de chaque jointure, un aimant 2912-0005-0001 dans '
          'la poche de chaque cache (il entre à force ; son sens est indifférent). Pour '
          'ôter un cache, un ongle dans l\'encoche du bord du siège.',
          dict(glisse(cles(r'^main, doigts : rondelle du cache, côté A'), mZ, 10), tol=3.0),
          dict(glisse(cles(r'^main, doigts : rondelle du cache, côté B'), Z, 10), tol=3.0),
          dict(glisse(cles(r'^main, doigts : (cache de l\'axe|aimant du cache), côté A'), mZ, 10), tol=3.0),
          dict(glisse(cles(r'^main, doigts : (cache de l\'axe|aimant du cache), côté B'), Z, 10), tol=3.0))

    # ── la jonction bras ↔ corps (jonction.py), montée à part puis enfilée sur le goTUBE de flexion ──
    # Elle est posée dans le corps, horizontale : dans le repère du bras, le haut du robot est penché comme le bras.
    from jonction import R_BRAS
    HAUT = tuple(float(v) for v in R_BRAS.T @ [0, 0, 1])
    BAS = tuple(-v for v in HAUT)
    etape('Le servo de flexion dans son cadre, à l\'établi',
          'Le servo 25-2 cinq tours entre dans le cadre 1802 par le côté de la cannelure, corps en premier, et s\'y '
          'visse par ses oreilles. Le pignon laiton de 40 dents se pousse sur la cannelure jusqu\'au bout.',
          poser(cles(r'^jonction, cadre du servo')), glisse(cles(r'^jonction, servo de flexion'), mY, 45),
          glisse(cles(r'^jonction, pignon laiton'), mY, 20), atelier='jonction')
    etape('La jonction, à l\'établi',
          'Chaque roulement dans son palier 1604. Sur la plaque 1123 du bas : les deux paliers (rangées de trous à ±16 mm '
          'du milieu de la plaque) et le cadre du servo (rangée +16, trois trous de l\'avant), chacun par deux ou trois vis '
          'M4 × 8 dans ses chants taraudés. La plaque du haut se pose par-dessus et se visse de même.',
          glisse(cles(r'^jonction, plaque du bas'), BAS, 30),
          glisse(cles(r'^jonction, (palier 1604|roulement du palier) extérieur'), HAUT, 60),
          glisse(cles(r'^jonction, (palier 1604|roulement du palier) intérieur'), HAUT, 60),
          visser(cles(r'^jonction, vis .*plaque du bas ↔')),
          glisse(cles(r'^jonction, plaque du haut'), HAUT, 30),
          visser(cles(r'^jonction, vis .*plaque du haut ↔')), atelier='jonction')
    etape('La jonction sous le goRAIL du haut du flanc',
          'Trois écrous Hurricane entrent dans la fente du dessous du rail et se tournent d\'un quart de tour. Trois '
          'entretoises de 16 mm entre le rail et la plaque du haut ; les vis M4 × 25 viennent par-dessous la plaque du '
          'haut, entre les deux plaques (clé coudée par les trous de la plaque du bas), traversent plaque et entretoise et '
          'se vissent dans les écrous.',
          [dict(glisse([e_], BAS, 12), tol=3.0) for e_ in cles(r'^jonction, écrou Hurricane .*du rail')],
          glisse(cles(r'^jonction, entretoise .*rail'), (0, 1, 0), 20),
          visser(cles(r'^jonction, vis .*écrou du rail')), atelier='jonction')
    etape('La traverse sous la jonction',
          'Un second goRAIL de 288 mm se monte entre les montants avant et arrière du flanc, juste sous la jonction : '
          'trois écrous Hurricane dans la fente de son dessus, un gousset 1137 sur chaque angle, côté extérieur, comme '
          'aux coins du cadre. Trois entretoises de 8 mm entre la traverse et la plaque du bas ; les vis M4 × 16 se '
          'glissent par le côté entre les deux plaques, tête sur la plaque du bas, et se serrent à la clé coudée par les '
          'trous libres de la plaque du haut. Le haut et le bas tenus, la jonction ne peut plus pivoter autour du rail.',
          [dict(glisse([e_], BAS, 12), tol=3.0) for e_ in cles(r'^jonction, écrou Hurricane .*traverse')],
          glisse(cles(r'^jonction, entretoise .*traverse'), (0, 1, 0), 20),
          visser(cles(r'^jonction, vis .*traverse')), atelier='jonction')
    etape('Le bras dans la jonction',
          'Les prises des câbles d\'abord : elles passent dans les roulements. Le goTUBE de flexion entre par le flanc '
          'du corps, traverse le palier extérieur puis le palier intérieur, jusqu\'à ce que son bout arrive au droit du '
          'pignon du servo.',
          dict(joindre='jonction', chemin=[[[0, -1, 0], 90]]))
    etape('Le collier de butée',
          'Le collier imprimé, enfilé sur le goTUBE avant qu\'il entre dans la jonction, se pousse contre le palier '
          'extérieur : son col de Ø34 entre dans le bloc et n\'appuie que sur la bague intérieure du roulement. Une vis '
          'M3 × 12 dans son oreille le serre sur le tube (elle se visse dans le plastique). Bras poussé vers le corps, '
          'c\'est lui qui bute.',
          glisse(cles(r'^collier de butée'), Y, 10), visser(cles(r'^vis M3 × 12 du collier')))
    etape('La roue de flexion',
          'L\'entretoise imprimée s\'enfile sur le bout du goTUBE, lèvre de Ø34 contre la bague intérieure du roulement '
          'du palier intérieur : bras tiré, la roue la pousse contre le roulement. Avant de poser la roue, chaque prise passe par une des fenêtres de la roue (8,7 × 4 mm), en face du canal du goTUBE '
          'par où sort son câble : roue vissée, il ne reste que 5,9 × 4 mm, assez pour les nappes mais pas pour les prises. '
          'La roue alu de 90 dents se visse au bout du goTUBE par quatre vis M4 × 12 dans ses taraudages, et engrène '
          'avec le pignon du servo.',
          glisse(cles(r'^entretoise de la roue de flexion'), mY, 30),
          poser(cles(r'^prise du câble')),
          # La prise (7,85 × 2,42) passe dans l'oblong (8,7 × 4, bouts ronds de r 2) avec 0,4 mm de jeu en longueur, 0,8
          # par côté en largeur : les facettes des deux maillages s'y recouvrent d'environ 1 mm³.
          dict(glisse(cles(r'^roue alu 90 dents de flexion'), mY, 15), tol=3.0), visser(cles(r'roue de flexion ↔ goTUBE')))
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
