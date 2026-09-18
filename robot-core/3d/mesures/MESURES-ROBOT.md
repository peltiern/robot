# Le robot complet, relevé dans `Wall-E GoBilda Full.stl`

Relevé du **2026-09-13**, sur l'export Onshape de 19 h 22 (27 004 034 triangles, 1 391 pièces, 1,35 Go,
**hors dépôt**, sur le disque USB). `releve_robot.py` le refait en une quarantaine de secondes une fois les
sommets extraits. Il produit `../robot-walle.glb` (528 k triangles), que lisent `../robot-walle.html` et
la maquette de l'éditeur d'animation (`robot-webapp-v2/.../maquette/`, qui en garde une copie), et
`cotes-robot.json`, les mêmes chiffres pour un programme.

Le monter/descendre du CAD n'est pas celui du robot : le modèle le dessine d'après les cotes relevées
sur le robot, et le cale sur une courbe de hauteur mesurée (voir plus bas).

## Les repères

- **STL** : Z en haut, l'avant vers −Y. Donc **la droite du robot est en −X** : la coque 24, en X≈−45,
  est l'œil droit.
- **Modèle** (three) : x vers la droite du robot, y en haut, z vers l'arrière, origine au sol dans le
  plan de symétrie (X = 24,7 du STL). C'est le repère de `maquette/scene.ts`.

## La chaîne

```
caisse ─┬─ chenilles (statiques)
        ├─ bras droit, bras gauche ............ épaules, axes ∥X
        ├─ moyeu du servo du monter ─ bras 28 ─ tige 87 ─┐   (bras et tige dessinés à part)
        ├─ barres avant, milieu, arrière ◄───────────────┘   levier de 48 sur la barre du milieu
        └─ chariot (translation, sans rotation)
             └─ panoramique ................... axe vertical
                  └─ tête ..................... axe d'inclinaison, horizontal
                       ├─ coque droite ─ palonnier ─┐
                       ├─ coque gauche ─ palonnier ─┤
                       └─ bielles des yeux ◄────────┘
```

Tous les pivots sortent d'ajustements de cylindres sur les alésages, au centième de millimètre.

## Le monter / descendre

**Le parallélogramme** : barres de **128,000 mm** ; axes bas à Z 322,378 (Y 51,425 / 67,425 / 83,425),
axes hauts à Z 442,187. Le chariot décrit un arc **sans tourner**, ce qui explique que la caméra ne
pivotait pas à l'étalonnage du 2026-09-06. La barre du milieu se prolonge sous son pivot bas en un
**levier de 48,000 mm**, exactement dans son alignement.

**Le servo** est à **72,000 mm** devant et **32,000 mm** sous le pivot du levier.

**Ce que le CAD ne dit pas** : il accroche la tige sur un trou du moyeu, à 11,3 mm, avec une tige de
92,65 mm déportée de 36 mm sur le côté. Le robot a un **bras goBILDA de 6 trous** (1102-0006-0048) vissé
dans deux logements d'écrou opposés du moyeu, rotule au trou 6 : **28 mm** de l'axe. Et une **tige de
87 mm**, rotule à rotule. Le servo est un « 300° Torque » : **1,583 °/unité**.

**Le calage** vient d'une mesure sur le robot : la hauteur au réglet pour chaque valeur du HUD, de −10
à 60 par pas de 2, du dessus du U des axes bas au dessous du bloc au-dessus des axes hauts (6 mm
d'origine du réglet). Elle est inscrite dans le script (`MESURE_HAUTEUR`). Avec la géométrie ci-dessus,
la loi la reproduit à **0,91 mm près en moyenne** (1,78 au pire) entre −2 et 50, avec pour seules
inconnues le calage du bras (349,1° au repos, décroissant quand la valeur augmente) et l'origine de la
mesure.

| valeur du HUD | −10 | 0 | 10 | 20 | 30 | 40 | 50 | 60 |
|---|---|---|---|---|---|---|---|---|
| barres | 152,5° | 149,8° | 143,4° | 135,0° | 125,7° | 116,1° | 106,7° | 97,9° |
| chariot, montée / repos | −5,3 | 0 | +12,0 | +26,2 | +39,6 | +50,5 | +58,2 | +62,4 mm |
| chariot, recul / repos | −2,9 | 0 | +7,9 | +20,1 | +35,9 | +54,3 | +73,8 | +93,1 mm |

**Le haut de la course bute** : à partir de 50, le robot monte moins que la loi (−2,3 mm à 52, −4,4 à
60). Les points morts du mécanisme sont hors de la course, c'est donc une butée mécanique. Toute la course
est d'un seul côté des points morts : une valeur du HUD donne une seule hauteur.

Conséquence pour le logiciel : `Cou.RAPPORT_MONTER_DESCENDRE = -1` fait compter l'axe en unités de servo
brutes ; la hauteur, elle, suit cette loi (de 0,9 à 1,4 mm par unité au milieu, presque rien en haut).

**Une plaque manque dans le modèle Onshape** : à l'arrière du parallélogramme, il n'y en a qu'une (pièce
176, X 2,1). Les entretoises réservent la place d'une seconde en X 34,1, en face de la plaque avant 126, et
rien ne l'occupe. Le modèle la reconstitue comme la paire avant : copie de la 176 décalée de 32 mm. Si le
robot a bien cette plaque, c'est à compléter aussi dans Onshape (et à retirer alors d'`AJOUTS`).

## Le panoramique et l'inclinaison

- Panoramique : axe **vertical** en X 18,13 ; Y 38,38, soit **6,6 mm à droite** du plan de symétrie.
- Inclinaison : axe horizontal à Z 556,66, qui **coupe l'axe du panoramique**.
- L'arbre des yeux passe **21,70 mm au-dessus** de l'axe d'inclinaison (53,70 dans l'export du matin :
  la tête a été reposée plus bas sur son axe).
- Le moteur d'inclinaison (Stingray-2, pièce 105) **tourne avec la tête** : ses trous partagent la
  direction inclinée de la platine. Son **pignon de 40 dents** (107) roule autour d'une **roue de 80 dents**
  (104) fixée à l'équerre du panoramique (114) : module 0,8 (rayons extérieurs 32,8 et 16,8 mm, entraxe
  48,00), **réduction 2:1**. Quand la tête s'incline de α, le pignon tourne de 2α par rapport à elle, dans le
  même sens ; le modèle l'anime. Côté logiciel, la réduction est déjà dans `RAPPORT_INCLINAISON`
  (4,97 °/unité), mesuré sur la tête elle-même.

## Les yeux

Le quadrilatère est celui d'août, au centième : |OS| 87,622 / 87,623, bras 32,000, bielle 84,740 /
84,722, D (±13,000 ; −26,000). Le zéro de chaque coque est défini comme en août, par la direction de S,
et la loi de `tringlerie.ts` s'applique telle quelle. La coque, elle, a changé depuis août : une pièce
unique, coque et caisson arrière ; le contour de `cotes.json` n'est plus la coque réelle.

## Les épaules

Axes ∥X en Y 79,375 ; Z **275,777** (bras gauche, +X) et **276,977** (bras droit) : 1,2 mm d'écart.
Chaque servo d'épaule est dans la caisse et mène l'épaule par un arbre de 110 mm.

**Non articulés** : l'avant-bras porte deux corps de servo (l'un à sortie le long du bras, l'autre avec
un pignon Ø32) dont la géométrie seule ne dit pas s'ils font tourner le tube ou l'allongent ; la pince a
un servo et deux doigts en parallélogramme. Ils restent rigides dans le modèle.

## La pose de l'export

| axe | valeur |
|---|---|
| panoramique | −1,985° |
| inclinaison | −4,309° (tête baissée) |
| œil droit / gauche | −29,725° / −15,978° |
| monter/descendre | 45,8 (valeur du HUD) |

Le modèle est ramené au neutre corps par corps, avec un écart de retour sur les broches inférieur à
0,01 mm. **Les zéros de la tête et des yeux sont géométriques** (tête de face, arbre horizontal, coques au
neutre d'août) ; leur écart avec les zéros du robot n'est pas mesuré. Le monter, lui, compte en valeur du
HUD, grâce au calage.

## Refaire le relevé

```
python3 releve_robot.py                     # relevé + robot-walle.glb (deux copies) + cotes-robot.json
python3 releve_robot.py AUTRE.stl --travail /chemin/avec/2Go/libres
```

**Après un nouvel export, changer de dossier de travail** (ou le vider) : sinon l'ancien est relu sans
rien dire. Puis revoir le bloc « Cet export » en tête du script : Onshape renumérote les pièces à chaque
export (le 2026-09-13, aucune des pièces de contrôle n'a gardé son numéro), et les points de départ des
ajustements suivent la géométrie. Trois outils du script valent d'être réutilisés :

- **les cylindres de direction quelconque** : on découpe chaque pièce en nappes lisses ; une nappe dont
  les normales tiennent dans un plan est un cylindre, d'axe le plus petit vecteur propre du nuage des
  normales. Ça trouve l'arbre des yeux incliné de la tête sans rien supposer ;
- **le test d'orientation** : une pièce tournée avec un corps en porte l'angle sur toutes ses faces planes.
  L'histogramme des normales modulo 90° sépare ce que porte une coque de ce qui est fixe, et le chariot
  de ce qui est au-dessus du panoramique ;
- **l'ordre des pièces suit l'arbre d'assemblage** : les sous-ensembles forment des blocs de numéros
  contigus, ce qui dégrossit l'affectation avant toute géométrie.
