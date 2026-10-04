"""
Ce que chaque pièce de bras.stl est réellement : la référence goBILDA (ou Hitec, ou imprimée) et le
corps du STEP qui lui correspond.

Les numéros sont ceux de trimesh.split sur bras.stl (export de Nicolas du 2026-09-29) : un autre export
les renumérote, et c'est cette table qu'il faut revoir. Quand plusieurs références restent possibles, le
recalage les essaie toutes et garde la plus proche.
"""

# La rondelle 2801 n'a pas de STEP chez goBILDA : on prend celle que contient le cadre 1802.
# (pièces du bras, [(référence, expression régulière sur le nom du corps dans le GLB, ou None pour tout le modèle
#   [, référence affichée si le corps est pris dans le STEP d'une autre pièce])], rôle)
# « IMPRIME:nom » et « BRAS:nom » gardent la pièce telle que bras.stl la dessine (pièce imprimée, ou pièce
# fournie sans STEP).
# Une entrée à plusieurs pièces du bras se recale d'un bloc (servos découpés en morceaux dans l'export).
UNITES = [
    # ── structure ──
    ([235], [('1121-0006-0168', None)], 'structure'),
    ([275], [('1121-0006-0168', None)], 'structure'),
    ([109], [('1123-0043-0096', None)], 'structure'),
    ([152], [('1123-0043-0096', None)], 'structure'),
    ([223], [('1123-0043-0072', None)], 'structure'),
    ([256], [('1123-0043-0072', None)], 'structure'),
    ([3], [('1108-0001-0002', None)], 'structure'),
    ([52], [('1108-0001-0002', None)], 'structure'),
    ([46], [('1802-0043-0001', '^1802-0043-0001$')], 'structure'),
    ([136], [('1201-0027-0001', None)], 'structure'),
    ([219], [('1201-0027-0001', None)], 'structure'),
    ([140], [('1147-0001-0005', None), ('1103-0005-0040', None)], 'structure'),
    ([169], [('1116-0024-0040', None)], 'structure'),
    ([182], [('4103-0032-0096', None)], 'structure'),
    # ── transmission ──
    ([92], [('2312-0414-0072', None)], 'transmission'),
    ([195], [('2312-0414-0072', None)], 'transmission'),
    ([122], [('2312-0414-0048', None)], 'transmission'),
    ([25], [('1906-0025-0032', None), ('1504-0032-0040', None)], 'transmission'),
    ([130], [('1908-0025-0032', None)], 'transmission'),
    ([147], [('1311-0016-4008', None)], 'transmission'),
    ([183], [('1311-0016-4008', None)], 'transmission'),
    ([36], [('1526-0032-0100', None)], 'transmission'),
    ([165], [('1526-0032-0100', None)], 'transmission'),
    # Le STEP du 1604 contient aussi son roulement (NAUO2) : on ne prend que le bloc, le roulement est
    # la pièce 1601-0039-0032 recalée à part (sinon chaque palier aurait deux roulements).
    *[([i], [('1604-0043-0032', 'NAUO1')], 'palier') for i in (178, 185, 189)],
    *[([i], [('1601-0039-0032', None)], 'palier') for i in (103, 163, 207)],
    *[([i], [('1601-0014-0004', None), ('1601-0014-0005', None)], 'palier') for i in (99, 149, 184, 192)],
    *[([i], [('2904-0004-0014', None), ('1515-0015-0020', None)], 'palier') for i in (90, 170, 188, 205)],
    # ── servos ──
    ([80, 76, 77, 78, 79], [('2000-0025-0002', 'case|Screw|Seal|Spline')], 'servo'),
    ([39, 40, 41, 43], [('D85MG', None)], 'servo'),
    ([121, 34, 190, 87], [('HS-65MG', None)], 'servo'),
    # ── imprimé ──
    ([217], [('IMPRIME:cloison', None)], 'imprime'),
    # ── visserie ──
    *[([i], [('2800-0004-0020', None)], 'vis') for i in (73, 145)],
    *[([i], [('2802-0004-0018', None)], 'vis') for i in (33, 35, 95, 104, 139, 141, 168, 210)],
    *[([i], [('2802-0004-0016', None), ('2800-0004-0014', None)], 'vis') for i in (0, 44, 48, 94, 191, 197, 202, 272)],
    *[([i], [('2800-0004-0014', None)], 'vis') for i in (84, 100, 173, 193)],
    *[([i], [('2802-0004-0014', None)], 'vis') for i in (106, 108, 179, 206, 222, 233, 234, 269)],
    *[([i], [('2800-0004-0010', None)], 'vis') for i in (101, 120, 148, 150)],
    *[([i], [('2802-0004-0012', None)], 'vis') for i in (133, 194)],
    *[([i], [('2802-0004-0010', None)], 'vis') for i in (49, 50, 51, 74, 91, 96, 131, 201)],
    *[([i], [('2802-0004-0008', None)], 'vis') for i in (75, 86, 89, 93, 98, 119, 132, 146, 164, 171, 180, 181, 187,
                                                         198, 215, 298)],
    *[([i], [('2802-0004-0005', None)], 'vis') for i in (88, 200)],
    ([42], [('BRAS:vis de palonnier du D85MG', None)], 'vis'),
    *[([i], [('2812-0004-0007', None)], 'ecrou') for i in (2, 47, 81, 105, 134, 135, 138, 142, 143, 144, 172, 174, 176,
                                                           199, 203, 208, 209, 274)],
    *[([i], [('1802-0043-0001', '2801-0004-0008_91166A230$', '2801-0004-0008')], 'rondelle') for i in (45, 82, 85, 107, 151, 177, 196, 212, 213, 268, 270, 273)],
    *[([i], [('2807-0407-0500', None)], 'rondelle') for i in (1, 24, 37, 38, 137, 166, 167, 186, 211, 214)],
    ([102], [('1501-0006-0080', None), ('1502-0006-0080', None)], 'entretoise'),
    *[([i], [('1502-0006-0070', None)], 'entretoise') for i in (83, 271)],
    *[([i], [('1502-0006-0040', None)], 'entretoise') for i in (72, 97, 175, 204, 216, 218, 220, 221)],
]

# Ce qui reste n'a pas de volume : étiquettes de servo, filetages et facettes isolées de l'export.
