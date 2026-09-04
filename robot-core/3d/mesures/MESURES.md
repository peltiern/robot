# Cotes des yeux, relevées dans `Wall-E Eye.stl`

Relevé du **2026-08-31**, refait en 40 s par `releve_stl.py`. Le STL (130 Mo, 2 609 248
triangles, binaire) est l'assemblage réel, moteurs et visserie compris ; il vit **hors dépôt**,
dans le dossier parent. Les chiffres ci-dessous sont ceux qui alimentent `../yeux-walle.html`,
et `cotes.json` les porte sous forme lisible par un programme.

## Le repère

Origine sur **l'axe de rotation des coques** (l'arbre Ø8 commun), `x` vers l'extérieur de la
tête, `z` vers le haut, `y` en profondeur — l'avant de l'œil est à `y = -125,30`.

Deux pièges de l'export, corrigés dans tout ce qui suit :

- le modèle est **incliné de 0,2660°** autour de l'axe de profondeur. Le plan de symétrie ne
  passe pas par `x = 0` du STL : il passe par l'arbre et par le milieu des deux ancrages de
  bielle, les seuls points sûrement solidaires du bâti ;
- les **deux yeux n'y sont pas au même angle** — 0,783° d'écart, mesuré identiquement sur
  quatre repères de chaque coque (lentille, axe de servo, deux goujons à 32 et 112 mm de
  l'axe). L'export a été fait avec les deux servos à des positions légèrement différentes.
  Les cotes sont redressées puis **moyennées entre les deux côtés**.

## La coque

| | |
|---|---|
| contour | **149,80 × 113,60** mm |
| profondeur | **125,30** mm, section constante d'un bout à l'autre |
| bord interne | à **4,76** mm de l'axe → **9,52** mm de jeu entre les deux coques |
| dessus | **43,48** mm au-dessus de l'axe |
| lentille | centre à (65,644 ; −6,033), soit **65,921** mm de l'axe |
| cercles concentriques | Ø83,178 · Ø69,897 · Ø35,548 · Ø23,316 · Ø14,978 |
| ouverture percée dans la coque | Ø93,96, sur 13,6 mm de profondeur |

L'axe de rotation est donc **au coin haut-interne**, dans le jeu entre les deux coques — pas
au milieu du bord interne comme le supposait la première maquette.

Le contour complet (90 points, simplifié à 0,15 mm près depuis 720 points relevés) est dans
`cotes.json`. Il est étoilé depuis le centre de la lentille : l'aire du polygone colle à 0,2 %
près à celle du raster, c'est ce qui autorise le relevé radial.

## La tringlerie

**Le servo n'est pas sur l'axe de l'œil.** Il est vissé *dans la coque* et pousse une bielle
ancrée sur la platine fixe. Quadrilatère articulé **O–S–P–D** :

| point | position | rôle |
|---|---|---|
| `O` | (0 ; 0) | axe de rotation des coques, sur le bâti |
| `D` | (13,000 ; −25,999) | ancrage de la bielle, sur la platine **fixe** |
| `S` | (86,134 ; −16,082) | axe de sortie du servo, **porté par la coque** |
| `P` | (95,141 ; −46,788) | rotule bras/bielle, au neutre |

Longueurs : bras de servo **31,999**, bielle **84,731**, bâti `|OD|` **29,068**,
`|OS|` **87,623**. Orientation du bras au neutre `ψ₀ = −73,653°`.

Le bras porte **quatre trous à 8, 16, 24 et 32 mm** de l'axe du servo ; c'est celui de 32 qui
est monté. Le déplacer change tout le rapport — c'est le réglage matériel disponible.

### La loi

Le servo impose l'angle du bras **dans le repère de la coque** ; l'angle de la coque est ce
qui reste pour que la bielle garde sa longueur :

```
Q = S + 32·(cos(ψ₀+Δψ), sin(ψ₀+Δψ))      q = |Q|   β = atan2(Qz, Qx)
φ = δ − β + acos((q² + d² − c²) / (2·q·d))     d = |OD| = 29,068   δ = atan2(Dz, Dx)
```

| servo | −5° | 0° | +5° | +10° | +15° | +20° | +21,3° |
|---|---|---|---|---|---|---|---|
| œil | +6,14° | 0° | −6,42° | −13,30° | −21,03° | −31,07° | −35,01° |
| gain | 1,205 | **1,252** | 1,322 | 1,441 | 1,686 | 2,595 | **3,695** |

Le signe est inversé (servo positif = bord extérieur vers le bas). **Point mort du
quadrilatère à +22,33° de servo** (l'œil y serait à −42,28°) : au-delà la fermeture n'a plus
de solution, et juste avant le mécanisme n'a plus aucune autorité.

### Les butées

Ce ne sont pas des butées moteur : ce sont **les deux coques qui se touchent**.

| | œil | servo |
|---|---|---|
| bord extérieur en haut, les deux yeux | **+6,86°** | −5,60° |
| bord extérieur en bas, les deux yeux | **−35,01°** | +21,30° |
| un seul œil vers le haut | +13,71° | — |

La course est très dissymétrique parce que l'axe est au coin haut-interne : les deux angles
supérieurs internes se rejoignent presque tout de suite. Et la butée basse tombe **juste avant**
le point mort du quadrilatère — la géométrie protège le mécanisme, ce n'est sans doute pas un
hasard.

## Refaire le relevé

```
python3 releve_stl.py                # fiche complète + cotes.json      (~40 s)
python3 releve_stl.py --vues         # + vues.png et contour.png
python3 releve_stl.py --sans-cache   # ignore .cache.npz
```

Trois outils du script valent d'être réutilisés pour relever autre chose dans un STL :

- `pieces()` segmente par **arête partagée** et non par sommet. Le critère « sommet commun »
  ne marche pas : l'export soude les pièces qui se touchent à plat, et les 93 pièces
  retombent en deux gros blocs inexploitables ;
- `cylindres_y()` trouve tous les axes d'articulation d'un coup. Sur un cercle,
  `p·n = c·n + r` est **linéaire** en (cx, cz, r) : un moindre carré à trois inconnues par
  groupe de triangles, avec une erreur résiduelle de l'ordre du micron ;
- `semer()` tire les points **proportionnellement à l'aire**. Prendre le centre des triangles
  ne montre que la visserie — une vis pèse 30 000 triangles minuscules, une coque quelques
  centaines d'énormes.
