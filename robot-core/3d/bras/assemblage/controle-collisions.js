// Contrôle des collisions de robot-complet.html : à charger dans la page ouverte (console), puis balayerDemo(0, 60, 0.2).
// (0, eval)(await (await fetch("controle-collisions.js")).text())
const VOX = 3;
window.cleVox = (x, y, z) => (Math.floor(x / VOX) + 4000) * 67108864 + (Math.floor(y / VOX) + 4000) * 8192 + (Math.floor(z / VOX) + 4000);
function echantillonner(o, pas) {
  const g = o.geometry, pos = g.attributes.position, idx = g.index, n = idx ? idx.count / 3 : pos.count / 3, out = [];
  const P = i => [pos.getX(i), pos.getY(i), pos.getZ(i)];
  let graine = 1; const alea = () => (graine = (graine * 16807) % 2147483647) / 2147483647;
  for (let t = 0; t < n; t++) {
    const [a, b, c] = idx ? [idx.getX(3*t), idx.getX(3*t+1), idx.getX(3*t+2)] : [3*t, 3*t+1, 3*t+2];
    const A = P(a), B = P(b), C = P(c);
    const u = [B[0]-A[0], B[1]-A[1], B[2]-A[2]], v = [C[0]-A[0], C[1]-A[1], C[2]-A[2]];
    const cr = [u[1]*v[2]-u[2]*v[1], u[2]*v[0]-u[0]*v[2], u[0]*v[1]-u[1]*v[0]], aire = Math.hypot(...cr) / 2;
    const k = aire / (pas * pas), m = Math.floor(k) + (alea() < k % 1 ? 1 : 0);
    for (let q = 0; q < m; q++) { let r1 = alea(), r2 = alea(); if (r1 + r2 > 1) { r1 = 1 - r1; r2 = 1 - r2; }
      out.push(A[0] + u[0]*r1 + v[0]*r2, A[1] + u[1]*r1 + v[1]*r2, A[2] + u[2]*r1 + v[2]*r2); }
  }
  return new Float32Array(out);
}
const famille = g => g.startsWith('bras_') ? g : /oeil_droit$/.test(g) ? 'oeil_droit' : /oeil_gauche$/.test(g) ? 'oeil_gauche'
  : /^(panoramique|tete|pignon_inclinaison)$/.test(g) ? 'tete' : (g === 'caisse' || g === 'chenilles') ? 'fixe' : null;
window.ECH = [];
for (const p of pieces) { const f = famille(p.n.groupe); if (!f || p.n.articulation === 'cable') continue; ECH.push({ p, f, pts: echantillonner(p.o, 4) }); }
window.FAM = ['fixe', 'tete', 'oeil_droit', 'oeil_gauche', 'bras_droit', 'bras_gauche'];
window.mondePts = f => {
  const parts = ECH.filter(e => e.f === f), out = []; const qui = [];
  for (const e of parts) { const m = e.p.o.matrix.elements, p = e.pts;
    for (let i = 0; i < p.length; i += 3) { const x = p[i], y = p[i+1], z = p[i+2];
      out.push(m[0]*x + m[4]*y + m[8]*z + m[12], m[1]*x + m[5]*y + m[9]*z + m[13], m[2]*x + m[6]*y + m[10]*z + m[14]); qui.push(e.p.n.piece); } }
  return { a: out, qui };
};
window.voxMap = P => { const s = new Map(); for (let i = 0; i < P.a.length; i += 3) s.set(cleVox(P.a[i], P.a[i+1], P.a[i+2]), P.qui[i / 3]); return s; };
window.PAIRES = [['bras_droit','fixe'],['bras_droit','tete'],['bras_droit','oeil_droit'],['bras_droit','oeil_gauche'],['bras_droit','bras_gauche'],
  ['bras_gauche','fixe'],['bras_gauche','tete'],['bras_gauche','oeil_droit'],['bras_gauche','oeil_gauche'],
  ['tete','fixe'],['oeil_droit','fixe'],['oeil_gauche','fixe'],['oeil_droit','oeil_gauche']];
function contacts(P, S, a, b, base) {
  const A = P[a], set = S[b]; const qui = {}; let n = 0;
  for (let i = 0; i < A.a.length; i += 3) { if (base && base.has(i / 3)) continue; const h = set.get(cleVox(A.a[i], A.a[i+1], A.a[i+2]));
    if (h !== undefined) { n++; const k = A.qui[i / 3] + ' ↔ ' + h; qui[k] = (qui[k] || 0) + 1; } }
  return { n, qui };
}
window.ensembles = () => { const P = {}, S = {}; for (const f of FAM) { P[f] = mondePts(f); S[f] = voxMap(P[f]); } return { P, S }; };
// référence : ce qui se touche déjà au repos et dans la pose de l'export (pièces en appui par construction)
window.BASE = {};
for (const v of [Object.fromEntries(Object.keys(pose).map(k => [k, 0])), { panoramique: -1.985, inclinaison: -4.309, monterDescendre: 45.83, oeilDroit: -29.8, oeilGauche: -16.2 }]) {
  poser(v); const { P, S } = ensembles();
  for (const [a, b] of PAIRES) { const A = P[a], set = S[b], s = BASE[a + '|' + b] ?? new Set();
    for (let i = 0; i < A.a.length; i += 3) if (set.has(cleVox(A.a[i], A.a[i+1], A.a[i+2]))) s.add(i / 3); BASE[a + '|' + b] = s; }
}
window.SFIXE = voxMap(mondePts('fixe'));
window.balayerDemo = (s0, s1, ds, fonction = poseDemo) => {
  const ev = [];
  for (let s = s0; s < s1; s += ds) {
    poser(fonction(s));
    const P = {}, S = { fixe: SFIXE };
    for (const f of FAM) if (f !== 'fixe') { P[f] = mondePts(f); S[f] = voxMap(P[f]); }
    for (const [a, b] of PAIRES) { const c = contacts(P, S, a, b, BASE[a + '|' + b]); if (c.n > 3) ev.push({ s: +s.toFixed(2), paire: a + '|' + b, n: c.n, qui: Object.entries(c.qui).sort((x, y) => y[1] - x[1]).slice(0, 2).map(x => x[0]).join(' ; ') }); }
  }
  return ev;
};
'prêt : ' + Object.entries(BASE).map(([k, s]) => k + ' ' + s.size).filter(x => !x.endsWith(' 0')).join(', ');
