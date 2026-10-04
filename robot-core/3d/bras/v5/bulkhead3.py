from layout5 import *
from roll2 import *
import trimesh
orig=trimesh.Trimesh(S(parts[217].vertices),parts[217].faces)
def box(x0,x1,y0,y1,z0,z1):
    b=trimesh.creation.box(extents=(x1-x0,y1-y0,z1-z0)); b.apply_translation(((x0+x1)/2,(y0+y1)/2,(z0+z1)/2)); return b
def xcyl(r,x0,x1,y,z):
    c=trimesh.creation.cylinder(radius=r,height=x1-x0,sections=40); c.apply_transform(trimesh.transformations.rotation_matrix(np.pi/2,[0,1,0])); c.apply_translation(((x0+x1)/2,y,z)); return c
X0,X1=97.2,113.2
EARX0=112.9+DX                     # dos des oreilles du servo déplacé
XF=EARX0-0.4                       # nouvelle face avant (appui des oreilles)
CL=0.5
nb=orig.union(box(X0+0.01,X1-0.01,-13.6,13.8,-27.4,13.6))
# prolongement avant jusqu'à l'appui des oreilles (même section que la partie centrale)
ext=box(X1-0.5,XF,-21.35,21.4,-21.4,21.4); nb=nb.union(ext)
yb0,yb1=Y_S-6.4-CL,Y_S+6.4+CL
nb=nb.difference(box(X0-1,XF+1,yb0,yb1,Z_S-6.5-CL,Z_S+22.4+CL))                 # corps du servo
nb=nb.difference(box(X0-1,XF+1,Y_S-5.9,Y_S+5.9,-31.8,Z_S-6.5))                    # embout de sortie + fil vers le fond
for z in (Z_S-10.2,Z_S+25.5): nb=nb.difference(xcyl(1.25,XF-9,XF+0.1,Y_S,z))     # avant-trous des vis d'oreilles
nb=nb.difference(xcyl(3.5,X0-1,XF+1,-2.5,0))                                     # fil du micro-servo
print('front face',round(XF,2),'watertight',nb.is_watertight,'vol',round(nb.volume/1000,2))
nb.export('piece/cloison_v5_S.stl')
sv=trimesh.Trimesh(servo_new(S(parts[39].vertices)),parts[39].faces)
print('servo∩cloison',round(sv.intersection(nb).volume,3))
