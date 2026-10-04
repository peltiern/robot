"""Pièce de liaison servo rose -> goTUBE, porte-micro-servo rotatif.
Repère local : X = axe du tube (vers le bout du bras = +X), origine sur l'axe.
Toutes cotes en mm."""
import cadquery as cq, math

# --- interfaces relevées dans bras.stl ---
X0, X1   = -166.35, -129.35   # face du moyeu servo / extrémité du goTUBE
R_ENV    = 19.5               # rayon max tournant (plaques latérales à 21.5)
R_FL     = 17.0               # rayon des brides
T_FL     = 5.0                # épaisseur des brides
PCD_HUB  = [(11.27,0),(-11.27,0),(0,11.27),(0,-11.27)]            # taraudés M4 du moyeu
PCD_TUBE = [(7.83,-8.02),(-7.82,8.03),(-8.02,-7.82),(8.03,7.83)]  # taraudés M4 du goTUBE
D_M4, D_HEAD = 4.4, 9.0
# --- micro-servo (cotes du modèle) ---
SW, SL   = 15.6, 24.9          # épaisseur (selon X), longueur (selon Y)
Z_BOT    = -13.5               # dessous du servo
EAR_Z    = Z_BOT + 14.0        # dessous des oreilles
EAR_Y    = 14.6                # entraxe/2 des vis d'oreilles (à vérifier sur ton servo)
CLR      = 0.4
XC       = (X0 + T_FL + X1 - T_FL) / 2
FLOOR_T  = 2.6
Z_FLOOR  = Z_BOT - CLR - FLOOR_T

def box(x0,x1,y0,y1,z0,z1):
    return cq.Workplane("XY").box(x1-x0,y1-y0,z1-z0,centered=False).translate((x0,y0,z0))
def xcyl(r,x0,x1,y=0,z=0):
    return cq.Workplane("YZ").workplane(offset=x0).center(y,z).circle(r).extrude(x1-x0)

rear  = xcyl(R_FL, X0, X0+T_FL)
front = xcyl(R_FL, X1-T_FL, X1)
floor = box(X0+T_FL-0.1, X1-T_FL+0.1, -R_ENV, R_ENV, Z_FLOOR, Z_BOT-CLR)
wy0   = SL/2 + CLR
walls = (box(X0+T_FL-0.1, X1-T_FL+0.1,  wy0, R_ENV, Z_FLOOR, EAR_Z)
        .union(box(X0+T_FL-0.1, X1-T_FL+0.1, -R_ENV, -wy0, Z_FLOOR, EAR_Z)))
body = rear.union(front).union(floor).union(walls)

# enveloppe tournante + méplat d'impression (fond plat)
body = body.intersect(xcyl(R_ENV, X0-1, X1+1))
body = body.cut(box(X0-1, X1+1, -30, 30, -30, Z_FLOOR))

# perçages brides
for (y,z) in PCD_HUB:
    body = body.cut(xcyl(D_M4/2, X0-1, X0+T_FL+1, y, z))
    body = body.cut(xcyl(D_HEAD/2, X0+T_FL, X0+T_FL+6, y, z))   # dégagement têtes + clé
for (y,z) in PCD_TUBE:
    body = body.cut(xcyl(D_M4/2, X1-T_FL-1, X1+1, y, z))
body = body.cut(xcyl(7.0, X0-1, X0+T_FL+1))          # Ø14 : accès vis centrale du moyeu
body = body.cut(xcyl(4.0, X1-T_FL-1, X1+1))          # Ø8 : passage optionnel alésage central
# lumière câble -> canal +Z du goTUBE (y ±4, z 9.5..14)
slot = (cq.Workplane("YZ").workplane(offset=X1-T_FL-1).center(0,11.75)
        .slot2D(8,4.5,0).extrude(T_FL+2))
body = body.cut(slot)
# chanfrein d'entrée de câble (évasement côté servo)
body = body.cut(cq.Workplane("YZ").workplane(offset=X1-T_FL-0.01).center(0,11.75)
        .slot2D(10,6.5,0).workplane(offset=1.5).slot2D(8,4.5,0).loft().translate((0,0,0)))
# avant-trous M2 pour les oreilles du servo
for s in (1,-1):
    body = body.cut(cq.Workplane("XY").workplane(offset=EAR_Z-9).center(XC, s*EAR_Y).circle(0.9).extrude(9.1))
# encoches pour le câble du servo (les deux côtés)
for s in (1,-1):
    y0 = wy0-0.2 if s>0 else -R_ENV-1
    body = body.cut(box(XC-4, XC+4, y0, y0+R_ENV-wy0+1.2, Z_BOT, Z_BOT+6))

if __name__ == "__main__":
    import sys
    cq.exporters.export(body, "porte_servo_tube.step")
    cq.exporters.export(body, "porte_servo_tube_local.stl", tolerance=0.02, angularTolerance=0.1)
    # version orientée pour l'impression : fond sur le plateau
    cq.exporters.export(body.translate((-X0, 0, -Z_FLOOR)), "porte_servo_tube_IMPRESSION.stl", tolerance=0.02, angularTolerance=0.1)
    bb = body.val().BoundingBox()
    print("bbox", round(bb.xlen,2), round(bb.ylen,2), round(bb.zlen,2), "vol cm3", round(body.val().Volume()/1000,2))
