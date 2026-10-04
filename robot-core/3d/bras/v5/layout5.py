# Implantation v5 : plaque-palier avancée de 8 mm, barrette 140 retirée
from kin import *
SHIFT=8.0
MOVED={189,163,75,89,98,171}            # plaque-palier, roulement, vis de fixation
DROPPED={140,133,200,271,83,88,194}       # barrette + visserie
REPLACED={39,130,217,42}                 # servo rose (déplacé), moyeu d'origine, cloison, vis de cannelure
def SV(i):
    v=S(parts[i].vertices)
    if i in MOVED: v=v.copy(); v[:,0]+=SHIFT
    return v
