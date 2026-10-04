from kin import *
# Nouvel entraînement : servo rose retourné (180° autour de X), axe de sortie décalé à (Y_S, Z_S), engrenages 18/18 m0,8
M=0.8; ZG=20; C=M*ZG          # entraxe 16.0 (goBILDA 20T + 20T)
Z_S=-12.0; Y_S=float(np.sqrt(C**2-Z_S**2))
FACE0=123.5                    # face d'appui de la sortie du servo d'origine (repère épaule)
GEAR_X1=131.0                  # face avant des engrenages (1,5 mm devant la plaque-palier à 132,5)
GW=5.0                         # largeur des engrenages (Slip-Fit ~5 mm, à confirmer)
FACE=GEAR_X1-GW-1.0             # face du boîtier du servo : 1 mm derrière les engrenages
DX=FACE-FACE0
Rx180=np.diag([1,-1,-1.0])
def servo_new(v):  # v en repère épaule
    w=(v-np.array([0,0,0]))@Rx180.T; w[:,0]+=DX; w[:,1]+=Y_S; w[:,2]+=Z_S; return w
if __name__=='__main__':
    print('Y_S',round(Y_S,2),'Z_S',Z_S,'DX',round(DX,2))
    v=servo_new(S(parts[39].vertices)); print('servo bounds',np.round(v.min(0),2),np.round(v.max(0),2))
    stub=S(parts[39].vertices); s=(stub[:,0]<101)&(stub[:,2]>6.4); print('stub new',np.round(servo_new(stub[s]).min(0),1),np.round(servo_new(stub[s]).max(0),1))
    import trimesh
    from scipy.spatial import cKDTree
    sm=trimesh.Trimesh(v,parts[39].faces)
    p,_=trimesh.sample.sample_surface(sm,30000)
    for d in info:
        i=d['i']
        if i in (39,130,217) or G[i] not in ('arm','skin','pinion'): continue
        m=trimesh.Trimesh(S(parts[i].vertices),parts[i].faces)
        q,_=trimesh.sample.sample_surface(m,max(500,int(m.area*0.5)))
        dd=cKDTree(q).query(p)[0].min()
        if dd<1.5: print('  near part',i,'dist',round(dd,2),'bounds',np.round(m.bounds,1).tolist())
