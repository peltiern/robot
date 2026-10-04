import numpy as np, cadquery as cq
def involute_gear_pts(N,m,pa=20,backlash=0.06,npts=8):
    rp=N*m/2; rb=rp*np.cos(np.radians(pa)); ra=rp+m; rf=rp-1.25*m
    inv=lambda a: np.tan(a)-a
    a_p=np.arccos(rb/rp); half=np.pi/(2*N)-backlash/(2*rp)+inv(a_p)   # half tooth angle at base
    out=[]
    for k in range(N):
        c=2*np.pi*k/N
        rs=np.linspace(max(rb,rf),ra,npts)
        L=[];R=[]
        for r in rs:
            a=np.arccos(rb/r); th=half-inv(a)
            L.append((r,c-th)); R.append((r,c+th))
        # root
        t0=c-half-(np.pi/N-half)*0.0
        pts=[(rf,c-half-0.0)] if rf<rb else []
        pts+=L+R[::-1]
        if rf<rb: pts+=[(rf,c+half)]
        # root arc to next tooth
        nxt=c+2*np.pi/N-half
        for t in np.linspace(c+half,nxt,4)[1:-1]: pts.append((rf,t))
        out+= [(r*np.cos(t),r*np.sin(t)) for r,t in pts]
    return out
def gear_solid(N,m,width,plane="YZ",offset=0.0,center=(0,0)):
    pts=involute_gear_pts(N,m)
    return (cq.Workplane(plane).workplane(offset=offset).center(*center).polyline(pts).close().extrude(width))
