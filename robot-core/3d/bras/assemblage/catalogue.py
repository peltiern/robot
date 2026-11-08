#!/usr/bin/env python3
"""
Ce que la visionneuse doit savoir des pièces du bras pour préparer une commande : désignation, prix, conditionnement
et lien goBILDA, tirés du catalogue local (catalogue_gobilda/gobilda.db). Les quantités, elles, restent comptées par
la visionneuse sur les pièces visibles, comme sa nomenclature.

goBILDA vend la visserie par paquets (25 vis, 12 cales…) : on commande des paquets, pas des pièces. Les références
déclinées en plusieurs conditionnements n'ont dans le catalogue qu'une fiche « parent- » sans prix ni paquet ; pour
la visserie 28xx on suppose alors le paquet de 25, le conditionnement habituel, et la visionneuse le signale.

Nicolas commande chez steplab.net (revendeur italien, WooCommerce) : on y reprend, par la référence goBILDA, le prix TTC
en euros, la disponibilité et le lien. L'API publique de la boutique répond par référence ; quelques fiches portent
la référence suivie de « _ » (2801-0004-0008_), on les demande aussi.

Écrit catalogue.json à côté de legende.json et v5.json.
"""
import json, os, re, sqlite3, sys, urllib.request

ICI = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, ICI)
from recaler import SORTIE

BASE = os.path.join(os.path.dirname(SORTIE), 'catalogue_gobilda', 'gobilda.db')
# Ce qui ne se commande pas chez goBILDA : imprimé, fourni avec un servo, câblage, servos Hitec.
AILLEURS = {'IMPRIME': 'à imprimer', 'FOURNI': 'fourni avec le servo', 'BRAS': 'fourni avec le servo',
            'CABLE': 'câblage', 'D85MG': 'Hitec', 'HS-65MG': 'Hitec'}


def fiche(base, ref):
    for sku in (ref, 'parent-' + ref):
        ligne = base.execute('select name, price, url from parts where sku = ?', (sku,)).fetchone()
        if ligne:
            return ligne
    return None


STEPLAB = 'https://www.steplab.net/wp-json/wc/store/v1/products?per_page=100&sku='


def steplab(refs):
    """Fiche steplab par référence goBILDA ; pour une référence en double, la moins chère des disponibles."""
    skus = ','.join(refs + [r + '_' for r in refs])
    requete = urllib.request.Request(STEPLAB + skus, headers={'User-Agent': 'Mozilla/5.0'})
    fiches = {}
    for p in json.load(urllib.request.urlopen(requete, timeout=60)):
        ref, prix = p['sku'].rstrip('_'), p['prices']
        f = dict(prix=int(prix['price']) / 10 ** prix['currency_minor_unit'], dispo=p['is_in_stock'],
                 url=p['permalink'], nom=p['name'])
        if ref not in fiches or (f['dispo'], -f['prix']) > (fiches[ref]['dispo'], -fiches[ref]['prix']):
            fiches[ref] = f
    return fiches


def main():
    legende = json.load(open(os.path.join(SORTIE, 'legende.json')))
    v5 = json.load(open(os.path.join(SORTIE, 'v5.json')))
    refs = sorted(set(legende) | {p['ref'] for p in v5['pieces']})
    base = sqlite3.connect(BASE)
    catalogue = {}
    for ref in refs:
        ailleurs = AILLEURS.get(ref.split(':')[0])
        if ailleurs:
            catalogue[ref] = dict(fournisseur=ailleurs)
            continue
        f = fiche(base, ref)
        if not f:
            catalogue[ref] = dict(fournisseur='inconnu')
            print('absente du catalogue :', ref)
            continue
        nom, prix, url = f
        paquet = re.search(r'(\d+) Pack', (nom or '') + ' ' + (prix or ''))
        prix = re.search(r'\$([\d.]+)', prix or '')
        e = dict(fournisseur='goBILDA', nom=nom, url=url, prix=float(prix[1]) if prix else None,
                 paquet=int(paquet[1]) if paquet else 1)
        if not paquet and ref.startswith('28'):
            e.update(paquet=25, paquet_suppose=True)
        catalogue[ref] = e
    chez_steplab = steplab([r for r, e in catalogue.items() if e['fournisseur'] == 'goBILDA'])
    for ref, f in chez_steplab.items():
        catalogue[ref]['steplab'] = f
    absentes = [r for r, e in catalogue.items() if e['fournisseur'] == 'goBILDA' and r not in chez_steplab]
    print('absentes de steplab :', ', '.join(absentes))
    print('en rupture chez steplab :', ', '.join(r for r, f in chez_steplab.items() if not f['dispo']))
    chemin = os.path.join(SORTIE, 'catalogue.json')
    json.dump(catalogue, open(chemin, 'w'), ensure_ascii=False, indent=1)
    print('%d références, %d goBILDA, %d sans prix → %s' % (
        len(catalogue), sum(e['fournisseur'] == 'goBILDA' for e in catalogue.values()),
        sum(e['fournisseur'] == 'goBILDA' and e['prix'] is None for e in catalogue.values()), chemin))


if __name__ == '__main__':
    main()
