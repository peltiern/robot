import cadquery as cq, numpy as np, math, sys
sys.path.insert(0,'/home/claude')
from roll2 import *
from gears import gear_solid
def xcyl(r,x0,x1,y=0,z=0):
    return cq.Workplane("YZ").workplane(offset=x0).center(y,z).circle(r).extrude(x1-x0)
def rex(x0,x1,af=7.0,dia=7.9):   # profil REX approché : hexagone (sur plats) ∩ cercle
    hexa=cq.Workplane("YZ").workplane(offset=x0).polygon(6,af/math.cos(math.pi/6)).extrude(x1-x0)
    return hexa.intersect(xcyl(dia/2,x0,x1))
GX0=GEAR_X1-GW
BOSS0=GX0                         # embout affleurant : pignon emmanché serré
# --- moyeu creux imprimé (tourne avec le tube) ---
rotor=(rex(BOSS0,GEAR_X1+0.01,7.15,8.0)
       .union(xcyl(7.4,GEAR_X1,133.0))                  # épaulement d'appui du pignon
       .union(xcyl(17.3,133.0,134.3))                   # collerette contre la bague intérieure du roulement
       .union(xcyl(15.9,134.3,140.4)))          # portée dans le roulement Ø32 (134,5..139,5)
rotor=rotor.cut(xcyl(2.25,BOSS0-1,141))          # passage du fil Ø4,5
for (y,z) in [(11.27,0),(-11.27,0),(0,11.27),(0,-11.27)]:
    rotor=rotor.cut(xcyl(2.0,140.4-6.0,140.5,y,z))   # inserts M3
# --- clip d'arrêt imprimé (anneau fendu) ---
clip=xcyl(4.6,BOSS0+0.65,BOSS0+1.35).cut(xcyl(3.4,BOSS0+0.6,BOSS0+1.4)).cut(
     cq.Workplane("XY").box(3,2.6,12).translate((BOSS0+1,0,-4)))
# --- engrenages goBILDA (visualisation / vérification) ---
TH_C=math.degrees(math.atan2(Z_S,Y_S))
g_rotor=gear_solid(ZG,M,GW,offset=GX0).rotate((0,0,0),(1,0,0),TH_C).cut(rex(GX0-1,GEAR_X1+1,7.1,8.0))
g_servo=gear_solid(ZG,M,GW,offset=GX0).rotate((0,0,0),(1,0,0),TH_C+180+180/ZG).translate((0,Y_S,Z_S)).cut(xcyl(2.9,GX0-1,GEAR_X1+1,Y_S,Z_S))
if __name__=='__main__':
    for name,b in (('moyeu_creux_REX',rotor),('gobilda_slipfit_20T',g_rotor),('gobilda_2305_20T',g_servo)):
        cq.exporters.export(b,f'piece/{name}_S.stl',tolerance=0.01,angularTolerance=0.1)
        bb=b.val().BoundingBox(); print(name,'vol',round(b.val().Volume()/1000,3),'x',round(bb.xmin,2),round(bb.xmax,2))
