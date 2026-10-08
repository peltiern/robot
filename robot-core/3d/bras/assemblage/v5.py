#!/usr/bin/env python3
"""
Superpose au bras reconstruit les modifications de la v5 (conversation claude.ai du 2026-10-01) : pièces
imprimées, pièces achetées ajoutées, pièces goBILDA déplacées. Écrit bras-v5.glb et v5.json (les pièces
du bras qu'elles remplacent, à masquer) sur le disque externe.

Les fichiers de la v5 sont dans le repère de bras.stl : on leur applique le même redressement.
"""
import json, os, sqlite3, sys
import numpy as np, trimesh, cascadio

ICI = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, ICI)
from recaler import SORTIE, repere_bras, modele
from liaisons import axe_de_vis

CLOISON = os.path.join(SORTIE, 'cloison')

V5 = os.path.join(ICI, '..', 'v5')

# (fichier, nom affiché, nature, référence) — nature : imprimée, achetée (nouvelle référence),
# déplacée (pièce existante)
PIECES = [
    ('liaison_29mm_position_assemblage.step', 'liaison 29 mm', 'imprimée', 'IMPRIME:liaison'),
    ('cloison_position_assemblage.stl', 'cloison refaite', 'imprimée', 'IMPRIME:cloison'),
    ('moyeu_creux_REX_position_assemblage.stl', 'moyeu creux à embout REX', 'imprimée', 'IMPRIME:moyeu creux'),
    # Refusées par Nicolas le 2026-10-01 : la bride du goTUBE de flexion, et la barrette 1147 (ni
    # raccourcie ni d'origine).
    # Le pignon Slip-Fit et l'engrenage du D85MG étaient deux disques approximatifs de la v5 : ils sont pris au
    # catalogue (voir PIGNON_TUBE, ENGRENAGE_D85).
    ('REF_gotube_flexion_position_assemblage.stl', 'goTUBE 48 mm de flexion', 'achetée', '4103-0032-0048'),
    ('REF_servo_rose_position_assemblage.stl', 'D85MG déplacé', 'déplacée', 'D85MG'),
    ('REF_micro_servo_position_assemblage.stl', 'HS-65MG déplacé', 'déplacée', 'HS-65MG'),
]
# Pour la visionneuse : l'état d'une pièce du nouveau bras
ETATS = {'imprimée': 'imprimé', 'achetée': 'ajouté', 'déplacée': 'déplacé', 'visserie ajoutée': 'ajouté',
         'câblage': 'câble'}

# Le palier 1604 de la cloison avance de 8 mm (trous existants des plaques de 72). On le reconstruit à
# partir du catalogue plutôt que du fichier de la v5. Son roulement reste collerette vers l'avant : le bloc
# a un rebord à l'arrière et un lamage à l'avant où la collerette affleure (essai de retournement abandonné
# le 2026-10-03, il venait d'un roulement compté deux fois).
AVANCE = 8.0
PALIER_AVANCE = ['1604-0043-0032#3', '2802-0004-0008#1', '2802-0004-0008#3']
# Côté y−, les deux vis de la plaque 72 dans le palier croisaient les vis M4 × 16 qui traversent le palier vers
# la cloison (même coin, y = −16) : M4 × 5 au lieu de M4 × 8, 1 mm de jeu, 2,5 mm de filet (2026-10-07).
# (x, z) ; elles remplacent 2802-0004-0008#5 et #10, qui restent masquées avec le palier d'origine.
VIS_PALIER_Y_MOINS = [(135.96, -16.0), (135.96, 16.0)]
ROULEMENT = '1601-0039-0032#2'

# Numéros de pièce de bras.stl remplacés par la v5 (layout5.py de la conversation, plus le micro-servo
# et la barrette, déplacés eux aussi).
REMPLACEES = {
    'palier, roulement et vis, avancés de 8 mm': [189, 163, 75, 89, 98, 171],
    'barrette 1147 et sa visserie, supprimées': [140, 133, 200, 271, 83, 88, 194],
    'D85MG, moyeu 1908, cloison et vis de palonnier, remplacés': [39, 130, 217, 42],
    'HS-65MG, déplacé': [121, 34, 190, 87],
    # Cylindre de Ø6 qui occupe le même volume que la cannelure du servo d'abduction : un doublon du
    # modèle de Nicolas (« pas certain que ça serve »), retiré.
    'entretoise 1501 en doublon de la cannelure du servo, retirée': [102],
}


def charger(f):
    chemin = os.path.join(V5, f)
    if f.endswith('.step'):
        glb = os.path.join(SORTIE, 'tmp-v5.glb')
        cascadio.step_to_glb(chemin, glb, tol_linear=0.02, tol_angular=0.2)
        m = trimesh.load(glb, force='scene').to_geometry()
        if m.extents.max() < 2:
            m.apply_scale(1000)
        os.remove(glb)
        return m
    return trimesh.load(chemin)


# Le cadre 1802 du servo d'abduction se visse dans les trous oblongs (5,4 × 4) de la ligne médiane des
# plaques de 96 mm, 3 vis par côté (relevé de Nicolas sur le vrai bras, confirmé par fixations.py une fois
# les oblongs pris en compte). Vis bombée M4 × 8 dans les taraudages des tranches du cadre.
FIXATIONS_CADRE = [(x, s) for x in (35.95, 59.95, 84.0) for s in (1, -1)]      # (x, côté y)


# Retenue du tube vers l'avant : entre la face avant de la liaison (x = 169,55) et la bague intérieure du
# roulement du palier à x = 172 (face arrière à 172,48), rien n'arrêtait le tube s'il était tiré (2,9 mm
# de glissement). Cale inox côté liaison (Ø36, ne touche que ce qui tourne), puis deux bagues plastique
# Ø34 qui n'appuient que sur la bague intérieure. Reste ≈ 0,4 mm de jeu, à régler au montage par cales.
BAGUES_TUBE = [('2807-3236-0500', 169.60, 0.5), ('1500-0010-0032', 170.10, 1.0), ('1500-0010-0032', 171.10, 1.0)]


# La plaque 1116-0024-0040 ferme le bras côté épaule (x = −24) : elle s'appuie sur les deux blocs 1201,
# taraudés en face de ses trous (y = ±16, z = ±8). Dans bras.stl elle n'était tenue par rien.
# M4 × 5 et non M4 × 8 : plus longues, elles croisaient dans le coin des blocs les vis M4 × 16 des couronnes,
# qui descendent jusqu'à z = ±6,4 ; 1 mm de jeu, 2,5 mm de filet. Les trous du milieu (z = 0), essayés le
# 2026-10-07, ne sont pas taraudés et le câble du servo d'abduction passe derrière.
FIXATIONS_PLAQUE_EPAULE = [(y, z) for y in (16.0, -16.0) for z in (8.0, -8.0)]


# Vis d'oreille des servos Hitec, fournies avec eux (M2 autotaraudeuses), dans les avant-trous des
# pièces imprimées. (tête : centre du dessous de la tête, direction de la tige, longueur, nom)
# La vis du bas du D85MG se visse dans le bossage ajouté à la cloison (le fil sort par le bas, à l'arrière).
VIS_OREILLES = [
    ((116.8, 10.63, 13.56), (-1, 0, 0), 8.0, 'FOURNI:vis M2 du D85MG'),
    ((116.8, 10.63, -22.14), (-1, 0, 0), 8.0, 'FOURNI:vis M2 du D85MG'),
    ((154.5, 14.0, 4.1), (0, 0, -1), 6.0, 'FOURNI:vis M2 du HS-65MG'),
    ((154.5, -14.0, 4.1), (0, 0, -1), 6.0, 'FOURNI:vis M2 du HS-65MG'),
]


def vis_m2(dessous_tete, direction, longueur):
    """Vis de servo Hitec, dessinée à la main (pas de STEP pour la visserie des servos) : autotaraudeuse à tête
    bombée cruciforme, Ø3,8 × 1,5 (0,5 de collet + 1,0 de dôme). Jusqu'au 2026-10-04, une tête plate, qui
    ne ressemblait pas aux vraies."""
    p, d = np.asarray(dessous_tete, float), np.asarray(direction, float)
    d = d / np.linalg.norm(d)
    tige = trimesh.creation.cylinder(radius=1.0, segment=[p, p + d * longueur], sections=24)
    R = (1.9 ** 2 + 1.0 ** 2) / 2.0                           # sphère du dôme : 1,0 de flèche sur Ø3,8
    dome = trimesh.creation.icosphere(subdivisions=3, radius=R).apply_translation(p - d * 1.5 + d * R)
    tete = trimesh.creation.cylinder(radius=1.9, segment=[p - d * 1.5, p], sections=32)
    tete = trimesh.boolean.intersection([tete, dome], engine='manifold')
    # empreinte cruciforme : deux fentes de 2,2 × 0,5, 0,8 de profondeur
    e1 = np.cross(d, [1, 0, 0] if abs(d[0]) < 0.9 else [0, 1, 0]); e1 /= np.linalg.norm(e1); e2 = np.cross(d, e1)
    fentes = []
    for e in (e1, e2):
        f = trimesh.creation.box(extents=(2.2, 0.5, 1.6))
        f.apply_transform(np.column_stack([np.r_[e, 0], np.r_[np.cross(d, e), 0], np.r_[d, 0], [0, 0, 0, 1]]))
        f.apply_translation(p - d * 1.5)
        fentes.append(f)
    tete = trimesh.boolean.difference([tete] + fentes, engine='manifold')
    return trimesh.util.concatenate([tige, tete])


VIS_LIAISON_GOTUBE = [(7.83, -8.02), (-7.82, 8.03), (-8.02, -7.82), (8.03, 7.83)]   # motif taraudé du goTUBE
FOND_LAMAGE_LIAISON = 167.0


# goTUBE de flexion sans bride (refusée) : il se pose contre la face extérieure du moyeu 1311 côté corps
# (y = −32,1), dont les 4 trous sont au motif taraudé du bout du goTUBE. Les 4 vis M4 × 18 qui tenaient le
# moyeu au bloc d'épaule sont retournées : tête à la place de l'écrou dans le bloc, elles traversent bloc
# et moyeu et se vissent dans le goTUBE (≈ 5,8 mm de filet). Les écrous disparaissent.
GOTUBE_FLEXION_RECUL = 4.9                     # v5 : y −37 → −85 (place de la bride) ; maintenant −32,1 → −80,1
VIS_RETOURNEES = {'2802-0004-0018#1': '2812-0004-0007#3', '2802-0004-0018#3': '2812-0004-0007#12',
                  '2802-0004-0018#5': '2812-0004-0007#14', '2802-0004-0018#6': '2812-0004-0007#1'}
# Retirées par leur nom (pas de numéro de pièce simple) : vis du moyeu côté corps retournées, écrous supprimés
RETIREES_PAR_NOM = {'vis du moyeu 1311 côté corps, retournées pour tenir le goTUBE ; écrous supprimés':
                    list(VIS_RETOURNEES) + list(VIS_RETOURNEES.values())}


# Engrènement d'abduction : dans bras.stl, le pignon acétal de 48 dents, monté sur un moyeu 1906, est 2 mm
# plus bas que la couronne de 72 dents (2,1 mm de denture en prise sur 4,1). On le remplace par le pignon
# laiton 2305-0025-0048 posé directement sur la cannelure du servo — le montage que goBILDA décrit pour
# entraîner la couronne de 72 dents à 48 mm. Sa cannelure femelle fait 3,2 mm de profondeur, la cannelure du
# servo dépasse de 4,1 mm : il bute au bout de la cannelure, denture de z = −22,0 à −16,0, et couvre toute
# la couronne (−22,1 à −18,0) sans rien déplacer (2026-10-03 ; l'abaissement de la couronne, essayé avant,
# est abandonné).
PIGNON_ACETAL = ['2312-0414-0048#1', '1906-0025-0032#1', '2802-0004-0008#2', '2802-0004-0008#9',
                 '2802-0004-0008#13', '2802-0004-0008#15']
PIGNON_LAITON = ('2305-0025-0048', (47.85, 0.0), -15.99)       # référence, axe de la cannelure, face côté servo
# Engrenage laiton du D85MG et pignon Slip-Fit de la liaison, modèles du catalogue (2026-10-08). La v5 dessinait deux
# disques de 5 mm à x 126–131, et nommait le pignon 2303-4008-0020 (14 mm avec moyeu : pas la place). L'engrenage
# (6 mm, cannelure côté servo sur 3 mm, lamage de vis de l'autre côté) se pose sur le bossage de sortie du D85MG
# (x 125,75 dans le modèle de Nicolas, où la cannelure ne dépasse que de 0,15 mm : à revoir sur le vrai servo) ; la
# denture du pignon (7 mm : 6 mm de denture entre deux collets de 0,5) lui fait face, à 0,1 mm du palier avancé.
# (référence, centre (y, z), face arrière en x, rotation sur son axe en degrés : alésage REX sur l'embout, dents en prise)
ENGRENAGE_D85 = ('2305-0025-0020', (10.63, -12.0), 125.8, 8.1)
PIGNON_TUBE = ('2322-4008-0020', (0.0, 0.0), 125.3, 30.0)


def poser_vis(sku, tete, direction):
    """Le modèle du catalogue, dessus de tête en « tete », tige dans « direction »."""
    m = modele(sku, None)
    t0, d0, *_ = axe_de_vis(m)
    R = trimesh.geometry.align_vectors(d0, direction)
    m.apply_translation(-t0); m.apply_transform(R); m.apply_translation(tete)
    return m


def poser_ecrou(sku, centre, direction):
    m = modele(sku, None)
    m.apply_translation(-m.bounds.mean(0))
    axe = np.eye(3)[int(np.argmin(m.extents))]
    m.apply_transform(trimesh.geometry.align_vectors(axe, direction)); m.apply_translation(centre)
    return m


def poser_entretoise(sku, centre, direction):
    m = modele(sku, None)
    m.apply_translation(-m.bounds.mean(0))
    axe = np.eye(3)[int(np.argmax(m.extents))]
    m.apply_transform(trimesh.geometry.align_vectors(axe, direction)); m.apply_translation(centre)
    return m


# Le HS-65MG vient du fichier de la v5 (REF_micro_servo), posé de travers : 5,1° de tangage (l'inclinaison de
# bras.stl, que ce fichier n'a jamais perdue) et 10° de lacet dans sa poche ; une de ses oreilles tombait à côté
# de sa vis (vu par Nicolas le 2026-10-04). On le redresse sur ses grandes faces, sans le déformer, et on le
# pose dans la poche de la liaison : fond à z = −11,5 (Z_BOT), centré entre les parois (y = 0) et sur l'axe
# de la poche (x = 154,5). Ses oreilles tombent alors à z = 2,5 sur le haut des parois, et la cannelure en
# (154,5 ; −5,7), sommet à z = 14,6.
POCHE_HS65 = dict(x=154.5, y=0.0, fond=-11.5)

# Arbre REX de flexion (2026-10-04) : de la face extérieure du moyeu 1311 côté y+ (y = +32) à 2 mm du bout du
# goTUBE de flexion (y = −78), à travers les deux moyeux et l'alésage Ø14,7 du goTUBE. Ses plats tournés de
# −15° pour entrer dans l'hexagone du moyeu côté y+ (relevé par rayons).
ARBRE_FLEXION = dict(sku='2102-0008-0110', y=(-78.0, 32.0), rotation=-15.0)
# Les deux moyeux étaient montés en miroir l'un de l'autre (chacun son bossage de centrage dans son bloc) :
# leurs hexagones tombaient à 30° l'un de l'autre, et un même arbre n'entrait pas dans les deux. Le moyeu
# côté corps est retourné (choix de Nicolas, 2026-10-04) : demi-tour autour de z, par le milieu de son corps
# (y −32,1 → −24) ; son hexagone rejoint celui du moyeu y+, son contour (±12,9 en x) ne change pas, et son
# bossage de Ø14 × 2 entre dans l'alésage Ø14,7 du goTUBE de flexion, qu'il centre. Tourné d'un quart de
# tour, un moyeu débordait en x (câbles bouchés côté corps, plaque 1123 en butée dès +16,5° côté y+).
MOYEU_RETOURNE = dict(pieces=['1311-0016-4008#2', '2800-0004-0014#3', '2800-0004-0014#4'], y_milieu=-28.05)


def redresser_hs65(m):
    m = m.copy(); m.merge_vertices()
    a = m.facets_area; o = np.argsort(a)[::-1]
    nz = m.facets_normal[o[0]]; nz = nz if nz[2] > 0 else -nz
    nx = next(m.facets_normal[i] for i in o if abs(np.dot(m.facets_normal[i], nz)) < 0.2)
    nx = nx - np.dot(nx, nz) * nz; nx /= np.linalg.norm(nx); nx = nx if nx[0] > 0 else -nx
    R = np.eye(4); R[:3, :3] = np.vstack([nx, np.cross(nz, nx), nz])
    m.apply_translation(-m.bounds.mean(0)); m.apply_transform(R)
    b = m.bounds
    m.apply_translation([POCHE_HS65['x'] - b[:, 0].mean(), POCHE_HS65['y'] - b[:, 1].mean(), POCHE_HS65['fond'] - b[0, 2]])
    return m


def pieces_v5():
    """(nom, nature, maillage dans le repère du bras, référence) : la v5, avec la cloison structurelle et la
    liaison fusionnée quand elles existent."""
    T = repere_bras()
    out = []
    fusion = os.path.join(SORTIE, 'liaison', 'liaison_moyeu_position.stl')
    for f, nom, nature, ref in PIECES:
        if nom == 'moyeu creux à embout REX' and os.path.exists(fusion):
            continue                                   # fondu dans la liaison (liaison_moyeu.py)
        if nom == 'liaison 29 mm' and os.path.exists(fusion):
            out.append(('liaison et moyeu en une pièce', 'imprimée', trimesh.load(fusion), 'IMPRIME:liaison et moyeu'))
            continue
        if nom == 'cloison refaite' and os.path.exists(os.path.join(CLOISON, 'cloison_structurelle_position.stl')):
            out.append(('cloison structurelle', 'imprimée',
                        trimesh.load(os.path.join(CLOISON, 'cloison_structurelle_position.stl')), 'IMPRIME:cloison structurelle'))
            continue
        m = charger(f); m.apply_transform(T)
        if ref == 'HS-65MG':
            m = redresser_hs65(m)
        if ref == '4103-0032-0048':
            m.apply_translation([0, GOTUBE_FLEXION_RECUL, 0])       # contre le moyeu, sans bride
            nom = 'goTUBE 48 mm de flexion, vissé sur le moyeu'
        out.append((nom, nature, m, ref))
    m = modele(ARBRE_FLEXION['sku'], None)
    m.apply_translation(-m.bounds.mean(0))
    m.apply_transform(trimesh.transformations.rotation_matrix(np.radians(ARBRE_FLEXION['rotation']), [0, 1, 0]))
    m.apply_translation([0, sum(ARBRE_FLEXION['y']) / 2, 0])
    out.append(('arbre REX 8 mm de flexion, 110 mm', 'achetée', m, ARBRE_FLEXION['sku']))
    a = json.load(open(os.path.join(SORTIE, 'assemblage.json')))
    U = {u['nom']: u for u in a['unites']}
    for n in MOYEU_RETOURNE['pieces']:
        u = U[n]; m = modele(u['source'], u['filtre']); m.apply_transform(np.array(u['matrice']))
        m.apply_transform(trimesh.transformations.rotation_matrix(np.pi, [0, 0, 1], [0, MOYEU_RETOURNE['y_milieu'], 0]))
        out.append(('%s retourné pour l\'arbre REX' % n, 'déplacée', m, n.split('#')[0]))
    for n in PALIER_AVANCE + [ROULEMENT]:
        u = U[n]; m = modele(u['source'], u['filtre']); m.apply_transform(np.array(u['matrice']))
        m.apply_translation([AVANCE, 0, 0])
        out.append(('%s avancé de 8 mm' % n, 'déplacée', m, n.split('#')[0]))
    for sku, x0, ep in BAGUES_TUBE:
        out.append(('bague %s — retenue du tube vers l\'avant' % sku, 'visserie ajoutée',
                    poser_ecrou(sku, [x0 + ep / 2, 0.0, 0.0], [1, 0, 0]), sku))
    for (sku, (y, z), face, tour), nom in ((ENGRENAGE_D85, 'engrenage laiton 2305, 20 dents'),
                                     (PIGNON_TUBE, 'pignon Slip-Fit 20 dents, alésage REX')):
        m = modele(sku, None)
        b = m.bounds; c = (b[0] + b[1]) / 2
        m.apply_translation([-c[0], -c[1], -b[0][2]])                                  # axe du modèle : z
        m.apply_transform(trimesh.transformations.rotation_matrix(np.pi / 2, [0, 1, 0]))   # z → x, alésage vers l'arrière
        m.apply_transform(trimesh.transformations.rotation_matrix(np.radians(tour), [1, 0, 0]))
        m.apply_translation([face, y, z])
        out.append((nom, 'achetée', m, sku))
    sku, (xa, ya), face = PIGNON_LAITON
    m = modele(sku, None)
    m.apply_translation(-m.bounds.mean(0))
    m.apply_transform(trimesh.transformations.rotation_matrix(np.pi, [1, 0, 0]))   # cannelure vers le servo (+z)
    m.apply_translation([xa, ya, face - m.extents[2] / 2])
    out.append(('pignon laiton 48 dents sur la cannelure du servo d\'abduction', 'achetée', m, sku))
    for nv, ne in VIS_RETOURNEES.items():
        ecrou = modele(U[ne]['source'], U[ne]['filtre']); ecrou.apply_transform(np.array(U[ne]['matrice']))
        appui = ecrou.bounds[0][1]                       # face de l'écrou contre le bloc (côté moyeu)
        x, z = ecrou.bounds.mean(0)[[0, 2]]
        out.append(('vis 2802-0004-0018 retournée — bloc 1201 → moyeu 1311 → goTUBE de flexion', 'déplacée',
                    poser_vis('2802-0004-0018', [x, appui + 2.2, z], [0, -1, 0]), '2802-0004-0018'))
    for p, d, L, ref in VIS_OREILLES:
        out.append(('vis ' + ref.split(':', 1)[1], 'visserie ajoutée', vis_m2(p, d, L), ref))
    for y, z in FIXATIONS_PLAQUE_EPAULE:
        out.append(('vis 2802-0004-0005 — plaque 1116 ↔ bloc d\'épaule 1201', 'visserie ajoutée',
                    poser_vis('2802-0004-0005', [-24.0 - 2.2, y, z], [1, 0, 0]), '2802-0004-0005'))
    for x, z in VIS_PALIER_Y_MOINS:
        out.append(('vis 2802-0004-0005 — plaque 72 y− ↔ palier avancé', 'visserie ajoutée',
                    poser_vis('2802-0004-0005', [x, -(24.05 + 2.2), z], [0, 1, 0]), '2802-0004-0005'))
    for x, cote in FIXATIONS_CADRE:
        tete = [x, cote * (24.05 + 2.2), 0.0]                # dessus de tête, sur la face extérieure de la plaque
        out.append(('vis 2802-0004-0008 — cadre 1802 ↔ plaque 96', 'visserie ajoutée',
                    poser_vis('2802-0004-0008', tete, [0, -cote, 0]), '2802-0004-0008'))
    if os.path.exists(os.path.join(CLOISON, 'visserie.json')):
        for v in json.load(open(os.path.join(CLOISON, 'visserie.json'))):
            if v.get('entretoise'):
                out.append(('entretoise %s — %s' % (v['entretoise'], v['paroi']), 'visserie ajoutée',
                            poser_entretoise(v['entretoise'], v['centre_entretoise'], v['direction']), v['entretoise']))
                continue
            out.append(('vis %s — %s' % (v['sku'], v['paroi']), 'visserie ajoutée', poser_vis(v['sku'], v['tete'], v['direction']), v['sku']))
            if v.get('ecrou'):
                out.append(('écrou %s — %s' % (v['ecrou'], v['paroi']), 'visserie ajoutée',
                            poser_ecrou(v['ecrou'], v['centre_ecrou'], v['direction']), v['ecrou']))
    # Plaque imprimée du bout du caisson (plaque_bout.py), et sa visserie
    PB = os.path.join(SORTIE, 'plaque_bout')
    if os.path.exists(os.path.join(PB, 'plaque_bout_position.stl')):
        out.append(('plaque de bout du caisson', 'imprimée', trimesh.load(os.path.join(PB, 'plaque_bout_position.stl')),
                    'IMPRIME:plaque de bout'))
        for v in json.load(open(os.path.join(PB, 'visserie.json'))):
            out.append(('vis %s — %s' % (v['sku'], v['paroi']), 'visserie ajoutée', poser_vis(v['sku'], v['tete'], v['direction']), v['sku']))
            out.append(('écrou %s — %s' % (v['ecrou'], v['paroi']), 'visserie ajoutée',
                        poser_ecrou(v['ecrou'], v['centre_ecrou'], v['direction']), v['ecrou']))
    # La liaison tient au goTUBE du bras par 4 vis M4, têtes noyées dans le lamage de la bride avant (fond à
    # x = 167,0, côté servo) ; elles manquaient au modèle jusqu'au 2026-10-04. 2,5 mm de bride, puis le
    # taraudage du bout du goTUBE (Ø3,6, plus de 12 mm de profondeur) : 10 mm de vis, 7,5 mm de filet en prise.
    for y, z in VIS_LIAISON_GOTUBE:
        out.append(('vis 2802-0004-0010 — liaison ↔ goTUBE du bras', 'visserie ajoutée',
                    poser_vis('2802-0004-0010', [FOND_LAMAGE_LIAISON - 2.2, y, z], [1, 0, 0]), '2802-0004-0010'))
    from cables import pieces_cables
    for nom, m, ref in pieces_cables():
        out.append((nom, 'câblage', m, ref))
    from animation import pieces_pince
    out.extend(pieces_pince())
    return out


def main():
    scene = trimesh.Scene()
    liste = pieces_v5()
    for k, (nom, nature, m, ref) in enumerate(liste):
        # le chargeur three.js retire « : », « . » et « / » des noms : un simple numéro, détaillé dans v5.json
        scene.add_geometry(m, node_name='v5-%d' % k, geom_name='v5-%d' % k)
        print('%-55s %-17s %s' % (nom, nature, np.round(m.bounds, 1).tolist()))
    scene.export(os.path.join(SORTIE, 'bras-v5.glb'))
    a = json.load(open(os.path.join(SORTIE, 'assemblage.json')))
    masquer = {}
    for raison, ids in REMPLACEES.items():
        masquer[raison] = [u['nom'] for u in a['unites'] if set(u['pieces']) & set(ids)]
    masquer.update(RETIREES_PAR_NOM)
    masquer['moyeu 1311 côté corps et ses vis de serrage, retournés pour entrer sur l\'arbre REX'] = list(MOYEU_RETOURNE['pieces'])
    masquer['pignon acétal, moyeu 1906 et ses 4 vis, remplacés par le pignon laiton sur la cannelure'] = list(PIGNON_ACETAL)
    base = sqlite3.connect(os.path.join(SORTIE, '..', 'catalogue_gobilda', 'gobilda.db'))
    autres = {'D85MG': 'Hitec D85MG (servo du tube)', 'HS-65MG': 'Hitec HS-65MG (servo de pince)'}
    def designation(ref):
        if ref.startswith('IMPRIME:'):
            return 'pièce imprimée : ' + ref.split(':', 1)[1]
        if ref.startswith('FOURNI:'):
            return ref.split(':', 1)[1] + ', fournie avec le servo'
        if ref.startswith('CABLE:câble de pince'):
            return 'câble de traction Ø1 (tresse ou acier gainé), tiré par le palonnier du HS-65MG'
        if ref.startswith('CABLE:soudure'):
            return 'raccord soudé des 3 fils du HS-65MG à une nappe 28 AWG plus longue, sous gaine thermorétractable'
        if ref.startswith('CABLE:main'):
            return 'repère de la main : les doigts ne sont pas encore dessinés'
        if ref.startswith('CABLE:'):
            return ref.split(':', 1)[1] + ' (3 fils ; prise JR/TJC8 de 7,85 × 2,42 × 15 mm)'
        r = base.execute('select name from parts where sku in (?, ?)', (ref, 'parent-' + ref)).fetchone()
        return r[0] if r else autres.get(ref, ref)
    json.dump(dict(pieces=[dict(nom=n, nature=k, etat=ETATS[k], ref=r, designation=designation(r)) for n, k, _, r in liste],
                   masquer=masquer),
              open(os.path.join(SORTIE, 'v5.json'), 'w'), ensure_ascii=False, indent=1)
    for r, noms in masquer.items():
        print(r, ':', ', '.join(noms))


if __name__ == '__main__':
    main()
