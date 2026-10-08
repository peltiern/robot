#!/usr/bin/env python3
"""
Audit de la visserie du nouveau bras (pièces conservées + v5 + cloison + liaison) :

  1. chaque vis sert-elle ? Elle doit serrer au moins deux pièces et finir dans un écrou ou un vrai
     taraudage (trou de Ø3,0 à 3,8 à sa pointe), pas dans un trou de passage ni dans le vide ;
  2. chaque pièce est-elle tenue ? Graphe « qui tient à quoi » : vis, roulements dans leur bloc, et
     pièces de la liaison qui tournent ensemble ; toute pièce doit rejoindre le reste du bras ;
  3. est-elle tenue de manière équilibrée ? Au moins deux fixations, écartées d'au moins un quart de
     la taille de la pièce (sinon elle pivote ou porte-à-faux).

À lancer sous plafond mémoire (ulimit -v 5000000) : le 2026-10-03, un contrôle trop gourmand a fait
tomber la session.
"""
import json, os, sys
from collections import defaultdict
import numpy as np, trimesh

ICI = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, ICI)
from recaler import SORTIE, modele
from liaisons import axe_de_vis

JEU = 0.6
TARAUDAGE = (2.9, 3.85)


def pieces_nouveau_bras():
    """nom -> (maillage, référence, rôle) pour toutes les pièces du nouveau bras."""
    a = json.load(open(os.path.join(SORTIE, 'assemblage.json')))
    v5 = json.load(open(os.path.join(SORTIE, 'v5.json')))
    retirees = {n for l in v5['masquer'].values() for n in l}
    out = {}
    for u in a['unites']:
        if u['nom'] in retirees:
            continue
        m = modele(u['source'], u['filtre']); m.apply_transform(np.array(u['matrice']))
        out[u['nom']] = (m, u['sku'], u['role'])
    s5 = trimesh.load(os.path.join(SORTIE, 'bras-v5.glb'), force='scene')
    for k, p in enumerate(v5['pieces']):
        T, g = s5.graph['v5-%d' % k]; m = s5.geometry[g].copy(); m.apply_transform(T)
        ref = p['ref']
        # une vis déplacée (palier avancé) reste une vis : on lit la référence, pas seulement le nom
        role = ('vis' if p['nom'].startswith('vis ') or ref.startswith(('2800-', '2802-')) else
                'ecrou' if p['nom'].startswith('écrou ') else
                'rondelle' if p['nom'].startswith('bague ') else 'entretoise' if p['nom'].startswith('entretoise ') else
                'imprime' if ref.startswith('IMPRIME') else 'piece')
        out['v5 %d %s' % (k, p['nom'])] = (m, ref, role)
    return out


def diametre_a(m, point, axe):
    """Diamètre du trou de m centré sur l'axe, dans la coupe perpendiculaire passant par point (None si plein)."""
    s = m.section(plane_origin=point, plane_normal=axe)
    if s is None:
        return None
    meilleur = None
    for b in s.discrete:
        c = b.mean(0)
        if np.linalg.norm((c - point) - np.dot(c - point, axe) * axe) > 1.5:
            continue
        d = 2 * np.linalg.norm(b - c, axis=1).mean()
        if d < 20 and (meilleur is None or d < meilleur):
            meilleur = d
    return meilleur


def main():
    P = pieces_nouveau_bras()
    vis = [n for n, (_, _, r) in P.items() if r == 'vis' and not n.startswith('BRAS')]
    structure = {n for n, (_, _, r) in P.items() if r not in ('vis', 'ecrou', 'rondelle')}
    print(len(P), 'pièces,', len(vis), 'vis', flush=True)

    bilan, aretes = [], defaultdict(list)
    for n in vis:
        m = P[n][0]; ref_vis = P[n][1]
        tete, d, L, rt, rT, hT = axe_de_vis(m)
        s = np.linspace(hT + 0.3, L - 0.2, 30)
        pts = tete + np.outer(s, d)
        trav = []
        for k, (mk, ref, role) in P.items():
            if k == n:
                continue
            lo, hi = mk.bounds
            if np.any(pts.min(0) > hi + 3) or np.any(pts.max(0) < lo - 3):
                continue
            _, dist, _ = trimesh.proximity.closest_point(mk, pts)
            # une pièce frôlée seulement par la pointe (dernier millimètre) n'est pas traversée
            pres = (dist < rt + JEU) & ((s < L - 1.2) | (dist < rt))
            if pres.any():
                trav.append((float(s[pres].min()), float(s[pres].max()), k, role))
        # La pièce sur laquelle s'appuie la tête compte aussi, même si son trou est plus large que la tige
        # (oreille de servo, lumière) : on regarde un anneau juste sous la tête.
        ang = np.linspace(0, 2 * np.pi, 16, endpoint=False)
        e1 = np.cross(d, [1, 0, 0] if abs(d[0]) < 0.9 else [0, 1, 0]); e1 /= np.linalg.norm(e1); e2 = np.cross(d, e1)
        anneau = tete + d * (hT + 0.25) + (rt + rT) / 2 * (np.outer(np.cos(ang), e1) + np.outer(np.sin(ang), e2))
        for k, (mk, ref, role) in P.items():
            if k == n or role in ('ecrou',) or any(t[2] == k for t in trav):
                continue
            lo, hi = mk.bounds
            if np.any(anneau.min(0) > hi + 1) or np.any(anneau.max(0) < lo - 1):
                continue
            _, dist, _ = trimesh.proximity.closest_point(mk, anneau)
            if (dist < 0.45).sum() >= 4:
                trav.append((hT, hT, k, role))
        trav.sort()
        serrees = [k for _, _, k, r in trav if r not in ('ecrou', 'rondelle', 'vis')]
        ecrou = [k for _, _, k, r in trav if r == 'ecrou']
        probleme = []
        if ecrou:
            fin = 'écrou'
        elif serrees:
            der = [t for t in trav if t[2] == serrees[-1]][0]
            # on sonde le trou à plusieurs profondeurs dans la dernière pièce : les maillages des STEP
            # ne donnent pas toujours un contour fermé à une profondeur donnée
            dias = [diametre_a(P[serrees[-1]][0], tete + d * t, d)
                    for t in np.linspace(der[0] + 0.3, min(L - 0.3, der[1]), 6)]
            dias = [x for x in dias if x]
            dia = min(dias) if dias else None
            if dia is not None and TARAUDAGE[0] <= dia <= TARAUDAGE[1]:
                fin = 'taraudage (Ø%.1f) de %s' % (dia, serrees[-1])
            elif P[serrees[-1]][2] == 'imprime' and ref_vis.startswith('FOURNI:'):
                fin = 'avant-trou de %s (vis autotaraudeuse)' % serrees[-1]
            elif P[serrees[-1]][2] == 'imprime':
                fin = 'pièce imprimée %s, sans écrou' % serrees[-1]
                probleme.append('finit dans une pièce imprimée sans écrou')
            else:
                fin = 'trou de Ø%s dans %s' % ('%.1f' % dia if dia else '?', serrees[-1])
                if dia is None or dia > TARAUDAGE[1]:
                    probleme.append('ne finit ni dans un écrou ni dans un taraudage')
        else:
            fin = 'rien'
            probleme.append('ne traverse aucune pièce')
        if len(set(serrees)) < 2:
            probleme.append('ne serre qu\'une pièce' if serrees else 'ne serre rien')
        for i, a in enumerate(sorted(set(serrees))):
            for b in sorted(set(serrees))[i + 1:]:
                aretes[(a, b)].append((n, tete + d * L / 2))
        bilan.append(dict(vis=n, serre=serrees, fin=fin, problemes=probleme))
        print('%-48s %-60s %s %s' % (n[:48], ' + '.join(x[:22] for x in serrees), fin[:40],
                                     ('⚠ ' + ' ; '.join(probleme)) if probleme else ''), flush=True)

    # Liaisons qui ne sont pas des vis : roulement dans son bloc (bague extérieure), ensemble qui tourne
    # (bague intérieure, tube, liaison, pignon), couronnes vissées... on les déduit du contact.
    def contact(a, b, tol=0.15):
        ma, mb = P[a][0], P[b][0]
        if np.any(ma.bounds[0] > mb.bounds[1] + tol) or np.any(mb.bounds[0] > ma.bounds[1] + tol):
            return False
        pts, _ = trimesh.sample.sample_surface(ma, 3000, seed=4)
        _, dist, _ = trimesh.proximity.closest_point(mb, pts)
        return (dist < tol).sum() > 20
    # Contacts qui tiennent une pièce sans vis : roulement dans son bloc, pignon sur son embout, engrenage
    # sur la cannelure, servo dans sa poche, tube dans ses roulements.
    roulements = [n for n in structure if P[n][1].startswith(('1601-', '2904-', '2303-', '2322-', '2305-', '4103-', '1906-', '1908-', 'D85MG', 'HS-65MG', '2102-'))]
    for r in roulements:
        for k in structure:
            if k != r and contact(r, k):
                aretes[tuple(sorted((r, k)))].append(('appui ou emboîtement', None))

    # Graphe : composantes connexes de la structure
    voisins = defaultdict(set)
    for (a, b) in aretes:
        voisins[a].add(b); voisins[b].add(a)
    vues, composantes = set(), []
    for n in sorted(structure):
        if n in vues:
            continue
        pile, comp = [n], set()
        while pile:
            x = pile.pop()
            if x in comp:
                continue
            comp.add(x); pile.extend(voisins[x] - comp)
        vues |= comp; composantes.append(comp)
    composantes.sort(key=len, reverse=True)
    print('\n=== pièces reliées entre elles : %d groupe(s)' % len(composantes))
    for c in composantes:
        print('  %3d pièces : %s' % (len(c), ', '.join(sorted(x[:30] for x in c)) if len(c) < 12 else '%d pièces, dont %s…' % (len(c), ', '.join(sorted(x[:22] for x in c)[:6]))))

    # Équilibre : nombre de fixations et écartement, par pièce
    print('\n=== pièces tenues par une seule fixation, ou par des fixations trop rapprochées')
    for n in sorted(structure):
        pos = [p for (a, b), l in aretes.items() if n in (a, b) for (v, p) in l if p is not None]
        autres = [x for (a, b) in aretes if n in (a, b) for x in (a, b) if x != n]
        if not pos:
            continue
        taille = float(P[n][0].extents.max())
        ecart = max((np.linalg.norm(p - q) for p in pos for q in pos), default=0.0)
        vis_distinctes = {v for (a, b), l in aretes.items() if n in (a, b) for (v, p) in l if p is not None}
        if len(vis_distinctes) < 2 and P[n][2] not in ('rondelle', 'entretoise') and P[n][0].extents.max() > 20 \
                or len(vis_distinctes) >= 2 and ecart < taille / 4 and taille > 20:
            print('  %-40s %d vis, écartées de %.1f mm (pièce de %.0f mm) — reliée à %s' % (
                n[:40], len(pos), ecart, taille, ', '.join(sorted(set(x[:22] for x in autres)))))
    json.dump(dict(vis=bilan, groupes=[sorted(c) for c in composantes]),
              open(os.path.join(SORTIE, 'audit.json'), 'w'), ensure_ascii=False, indent=1)


if __name__ == '__main__':
    main()
