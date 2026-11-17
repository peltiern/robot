# Ranger l'électronique dans la caisse

`implantation-caisse.html` compare trois façons de ranger les modules dans la caisse : **A · étagères**,
**B · parois**, **C · tiroir**. Les modules sont les modèles des fabricants, posés dans le robot. La page se sert
par un serveur, pas en `file://` :

```
cd robot-core/3d/implantation && python3 -m http.server     # puis http://localhost:8000/implantation-caisse.html
```

Tout ce qui se mesure est dans `calculs.js` : les cotes et la pose de chaque module, et les contrôles, que node
peut lancer sans la page.

## La place

L'intérieur de la caisse est le cube entre les goRAIL : **x ±144, y 134 → 422, z ±144** (288 mm de côté). La caisse
n'a pas de plancher : les rails du bas reposent sur les deux plaques 1107 des chenilles. Seul le haut est pris, relevé
pièce par pièce dans `../bras/assemblage/robot-anime.glb`, avec 2 mm de marge :

| zone | x | y | z |
|---|---|---|---|
| servo du monter et parallélogramme | ±36 | 350 → 446 | −62 → 76 |
| servo de flexion de chaque épaule | ±72 → ±144 | 340 → 422 | 26 → 72 |

Les moteurs 5203 sont dans les chenilles, hors de la caisse.

## Ce que la page contrôle

- **Chevauchements** : entre modules, avec les plaques ajoutées et avec les zones prises. Aucun dans les trois
  implantations.
- **Longueur de câble** : trajet en équerre entre centres. Ça sert à comparer, pas à couper des câbles.
- **Centrale inertielle** : écart au perturbateur le plus proche (batterie, puissance, DCC1000, moteurs,
  haut-parleur). La première version la mettait à 25–35 mm de l'aimant du haut-parleur ; elle est maintenant à
  50 mm au moins partout. Ça compte pour le choix IMU/AHRS du chantier centrale inertielle.
- **Centre de gravité de l'électronique** : vers 240 mm du sol dans les trois cas. La batterie est au plancher
  partout, il ne départage rien.

## Les modèles

`composants.glb` vient des STEP publiés par les fabricants (≈ 110 Mo décompressés, **hors dépôt**) :

| composant | archive |
|---|---|
| Smart Dock | https://www.gobilda.com/content/step_files/3104-0020-0001.zip |
| PDB XT30 | https://www.gobilda.com/content/step_files/3108-2833-0802.zip |
| BEC 6 A | https://www.gobilda.com/content/step_files/3119-2829-0106.zip |
| HUB0000 | https://www.phidgets.com/productfiles/HUB0000/HUB0000_0/Images/HUB0000_0_3D.zip |
| RCC1000 | https://www.phidgets.com/productfiles/RCC1000/RCC1000_1/Images/RCC1000_1_3D.zip |
| DCC1000 | https://www.phidgets.com/productfiles/DCC1000/DCC1000_0/Images/DCC1000_0_3D.zip |
| MOT1102 | https://www.phidgets.com/productfiles/MOT1102/MOT1102_1/Images/MOT1102_1_3D.zip |
| ReSpeaker v2.0 | https://files.seeedstudio.com/wiki/ReSpeaker_Mic_Array_V2/res/RESPEAKER%20MIC-3D%20v2.0.stp.zip |
| Jetson Nano | https://www.waveshare.com/w/upload/b/bb/P3450_P3449_A02_P3448_A02_3D_CAD_STEP_model.zip |

Chez goBILDA, l'adresse suit la référence : `content/step_files/<référence>.zip`. Le miroir Waveshare coupe souvent
le transfert du Jetson : reprendre avec `curl -C -`. Ce STEP est la révision **A02** ; celui de la B01 est chez
NVIDIA, derrière un compte. Le contour est le même, seuls les connecteurs caméra changent.

**Sans modèle** : la batterie DCB184 (celui de GrabCAD demande un compte ; c'est un bloc de 110 × 76 × 65 posé sur le
vrai dock), le haut-parleur et l'arrêt d'urgence, dont les références manquent encore. Le Jetson est dessiné sans
ventilateur : 15 mm sont réservés au-dessus du radiateur.

**Le sens des cartes** se déduit du modèle : la normale est l'axe le plus mince, et le côté composants celui qui dépasse
le plus du plan où s'accumule la surface à plat. Ça se trompe sur le PDB, dont le socle 3117 de 8 mm dépasse plus que
le boîtier : il est retourné à la main (`RETOURNER`). À revérifier pour tout nouveau composant.

## Refaire les modèles

```
python3 robot_leger.py                       # robot-leger.glb, depuis ../robot-walle.glb et robot-anime.glb (bras)
python3 composants.py /chemin/des/STEP       # composants.glb ; il faut cascadio et trimesh
```

`robot-walle.glb` a encore les anciens bras ; `robot_leger.py` prend donc les bras dans `robot-anime.glb`.

## Publiée en artifact

La page est aussi publiée en artifact, qui ne sert pas les `.glb` : elle prend alors `robot-leger.glb.txt` et
`composants.glb.txt`, les mêmes fichiers en base64 (`base64 -w0 x.glb > x.glb.txt`). Ces copies ne sont pas dans le
dépôt.
