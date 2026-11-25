#!/usr/bin/env python3
"""
Construit le bras à partir de assemblage.json : un nœud par pièce, nommé par sa référence, avec le modèle
du catalogue posé par sa matrice. Écrit bras-assemble.glb sur le disque externe et une planche de contrôle
qui superpose le résultat à bras.stl.
"""
import json, os, sqlite3, sys
import numpy as np, trimesh

ICI = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, ICI)
from recaler import DISQUE, SORTIE, modele, pieces_du_bras

COULEURS = {'structure': (170, 175, 185), 'transmission': (90, 90, 95), 'palier': (200, 200, 205),
            'servo': (40, 40, 45), 'imprime': (70, 120, 220), 'vis': (60, 60, 60), 'ecrou': (110, 110, 110),
            'rondelle': (190, 190, 190), 'entretoise': (150, 150, 160)}


def maillage(u, pieces):
    if u['sku'].startswith(('IMPRIME:', 'BRAS:')):
        return trimesh.util.concatenate([pieces[i] for i in u['pieces']])
    m = modele(u['source'], u['filtre'])
    m.apply_transform(np.array(u['matrice']))
    return m


def ecrire_legende(unites):
    """Référence -> désignation, rôle et nombre, pour la légende de la visionneuse."""
    base = sqlite3.connect(DISQUE + '/catalogue_gobilda/gobilda.db')
    autres = {'D85MG': 'Hitec D85MG (servo du tube)', 'HS-65MG': 'Hitec HS-65MG (servo de pince)'}
    legende = {}
    for u in unites:
        e = legende.setdefault(u['sku'], dict(role=u['role'], nombre=0))
        e['nombre'] += 1
        if 'designation' not in e:
            r = base.execute('select name from parts where sku in (?, ?)', (u['sku'], 'parent-' + u['sku'])).fetchone()
            e['designation'] = r[0] if r else autres.get(u['sku'], u['sku'].split(':', 1)[-1])
    json.dump(legende, open(os.path.join(SORTIE, 'legende.json'), 'w'), ensure_ascii=False, indent=1)


def main():
    a = json.load(open(os.path.join(SORTIE, 'assemblage.json')))
    pieces = pieces_du_bras()
    scene = trimesh.Scene()
    for u in a['unites']:
        m = maillage(u, pieces)
        m.visual = trimesh.visual.ColorVisuals(m, face_colors=(*COULEURS[u['role']], 255))
        scene.add_geometry(m, node_name=u['nom'], geom_name=u['nom'])
    scene.export(os.path.join(SORTIE, 'bras-assemble.glb'))
    ecrire_legende(a['unites'])
    print(len(a['unites']), 'pièces ->', os.path.join(SORTIE, 'bras-assemble.glb'))


if __name__ == '__main__':
    main()
