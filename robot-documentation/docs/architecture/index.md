# Architecture logicielle

Le code du robot est organisé comme un corps : des **organes** (caméra, cou, yeux,
roues...), un **cerveau** (mémoire, décision, activités), et un point commun à tous :
personne ne se parle jamais directement. Tout passe par un seul canal, le **bus
d'évènements** — c'est la règle la plus stricte du projet, et elle rend chaque organe
remplaçable, testable et surveillable indépendamment des autres.

## Les couches, autour du bus

<div class="diagram-zoom">
<input type="checkbox" id="zoom-archi-bus" class="diagram-zoom-toggle">
<label for="zoom-archi-bus" class="diagram-zoom-label">
<figure>
<div class="archi-bus" aria-hidden="false">
<style>
.archi-bus{max-width:100%}
.archi-bus svg{width:100%;height:auto;display:block}
.archi-bus .boite{fill:var(--md-default-bg-color);stroke:currentColor;stroke-width:1.5}
.archi-bus .bus{fill:none;stroke:currentColor;stroke-width:1.5}
.archi-bus text{fill:currentColor;font-family:inherit}
.archi-bus .titre-boite{font-size:14px;font-weight:600}
.archi-bus .sous-titre{font-size:11px;opacity:.75}
.archi-bus .lien{stroke:currentColor;stroke-width:1.25;opacity:.45;marker-start:url(#archi-fleche);marker-end:url(#archi-fleche)}
.archi-bus .accent{fill:var(--md-accent-fg-color)}
.archi-bus .etiquette-vol{font-size:12px;font-weight:600;fill:var(--md-accent-fg-color)}

@keyframes archi-capteurs-pulse{
  0%,100%{stroke:currentColor;stroke-width:1.5}
  1.5%{stroke:var(--md-accent-fg-color);stroke-width:3}
  6%{stroke:currentColor;stroke-width:1.5}
}
@keyframes archi-actionneurs-pulse{
  0%,76%,100%{stroke:currentColor;stroke-width:1.5}
  80%{stroke:var(--md-accent-fg-color);stroke-width:3}
  86%{stroke:currentColor;stroke-width:1.5}
}
@keyframes archi-decisionnel-pulse{
  0%,31%,49%,100%{stroke:currentColor;stroke-width:1.5}
  40%{stroke:var(--md-accent-fg-color);stroke-width:3}
}
/* Le paquet grimpe de la couche vers le bus, glisse À L'INTÉRIEUR du bus
   (cy=230, à mi-hauteur), puis redescend dans la couche d'arrivée — un
   coude, pas un saut, pour que le trajet dans le bus se voie vraiment. */
@keyframes archi-paquet-aller{
  0%,100%{transform:translate(120px,340px);opacity:0}
  1%{transform:translate(120px,340px);opacity:1}
  9%{transform:translate(120px,255px);opacity:1}
  13%{transform:translate(120px,230px);opacity:1}
  27%{transform:translate(600px,230px);opacity:1}
  31%{transform:translate(600px,340px);opacity:1}
  32%{opacity:0}
}
@keyframes archi-paquet-retour{
  0%,48%{transform:translate(600px,340px);opacity:0}
  49%{transform:translate(600px,340px);opacity:1}
  57%{transform:translate(600px,255px);opacity:1}
  61%{transform:translate(600px,230px);opacity:1}
  75%{transform:translate(280px,230px);opacity:1}
  79%{transform:translate(280px,340px);opacity:1}
  80%{opacity:0}
  100%{transform:translate(600px,340px);opacity:0}
}
.archi-bus #p-capteurs{animation:archi-capteurs-pulse 9s ease-in-out infinite}
.archi-bus #p-actionneurs{animation:archi-actionneurs-pulse 9s ease-in-out infinite}
.archi-bus #p-decisionnel{animation:archi-decisionnel-pulse 9s ease-in-out infinite}
.archi-bus #g-aller{animation:archi-paquet-aller 9s linear infinite}
.archi-bus #g-retour{animation:archi-paquet-retour 9s linear infinite}
</style>
<svg viewBox="0 0 900 460" role="img" aria-label="Toutes les couches parlent uniquement au bus d'évènements, jamais entre elles directement. Exemple animé : l'évènement VisagePercuEvent grimpe des capteurs jusqu'au bus, s'y déplace horizontalement avec son nom visible, puis redescend vers le décisionnel ; l'évènement MouvementCouEvent fait le même trajet du décisionnel vers les actionneurs.">
  <defs>
    <marker id="archi-fleche" viewBox="0 0 8 8" refX="4" refY="4" markerWidth="6" markerHeight="6" orient="auto-start-reverse">
      <path d="M0,0 L8,4 L0,8 Z" fill="currentColor"/>
    </marker>
  </defs>

  <!-- connecteurs -->
  <line class="lien" x1="175" y1="114" x2="175" y2="205"/>
  <line class="lien" x1="725" y1="114" x2="725" y2="205"/>
  <line class="lien" x1="120" y1="340" x2="120" y2="255"/>
  <line class="lien" x1="280" y1="340" x2="280" y2="255"/>
  <line class="lien" x1="440" y1="340" x2="440" y2="255"/>
  <line class="lien" x1="600" y1="340" x2="600" y2="255"/>
  <line class="lien" x1="760" y1="340" x2="760" y2="255"/>

  <!-- bus -->
  <rect class="bus" x="50" y="205" width="800" height="50" rx="6"/>
  <text x="450" y="234" text-anchor="middle" class="titre-boite">systemenerveux</text>
  <text x="450" y="249" text-anchor="middle" class="sous-titre">le bus d'évènements — tout passe par ici</text>

  <!-- couches du dessus -->
  <rect class="boite" x="90" y="50" width="170" height="64" rx="8"/>
  <text x="175" y="76" text-anchor="middle" class="titre-boite">web/</text>
  <text x="175" y="94" text-anchor="middle" class="sous-titre">HUD, API</text>

  <rect class="boite" x="640" y="50" width="170" height="64" rx="8"/>
  <text x="725" y="76" text-anchor="middle" class="titre-boite">sécurité/</text>
  <text x="725" y="94" text-anchor="middle" class="sous-titre">surveille tout</text>

  <!-- couches du dessous -->
  <text x="200" y="330" text-anchor="middle" class="sous-titre">— organes/ —</text>
  <rect id="p-capteurs" class="boite" x="50" y="340" width="140" height="64" rx="8"/>
  <text x="120" y="366" text-anchor="middle" class="titre-boite">capteurs/</text>
  <text x="120" y="384" text-anchor="middle" class="sous-titre">caméra, micro...</text>

  <rect id="p-actionneurs" class="boite" x="210" y="340" width="140" height="64" rx="8"/>
  <text x="280" y="366" text-anchor="middle" class="titre-boite">actionneurs/</text>
  <text x="280" y="384" text-anchor="middle" class="sous-titre">cou, yeux, roues...</text>

  <rect class="boite" x="370" y="340" width="140" height="64" rx="8"/>
  <text x="440" y="366" text-anchor="middle" class="titre-boite">mémoire/</text>
  <text x="440" y="384" text-anchor="middle" class="sous-titre">courte, longue</text>

  <rect id="p-decisionnel" class="boite" x="530" y="340" width="140" height="64" rx="8"/>
  <text x="600" y="366" text-anchor="middle" class="titre-boite">décisionnel/</text>
  <text x="600" y="384" text-anchor="middle" class="sous-titre">arbitrage, regard</text>

  <rect class="boite" x="690" y="340" width="140" height="64" rx="8"/>
  <text x="760" y="366" text-anchor="middle" class="titre-boite">activités/</text>
  <text x="760" y="384" text-anchor="middle" class="sous-titre">conversation...</text>

  <!-- évènements animés : chacun grimpe jusqu'au bus, s'y déplace à l'horizontale
       avec son nom, puis redescend dans la couche d'arrivée -->
  <g id="g-aller" class="paquet">
    <circle class="accent" r="6"/>
    <text class="etiquette-vol" y="-14" text-anchor="middle">VisagePercuEvent</text>
  </g>
  <g id="g-retour" class="paquet">
    <circle class="accent" r="6"/>
    <text class="etiquette-vol" y="-14" text-anchor="middle">MouvementCouEvent</text>
  </g>
</svg>
</div>
<figcaption>
Exemple réel et complet : la caméra (<code>capteurs/</code>) détecte un visage
(<code>VisagePercuEvent</code>), le <code>décisionnel</code> (<code>Regard</code>)
décide où tourner la tête et publie <code>MouvementCouEvent</code>, que l'organe
<code>Cou</code> (<code>actionneurs/</code>) exécute. Cinq couches, zéro appel direct
entre elles — seulement le bus.
</figcaption>
</figure>
</label>
</div>

Aucune flèche ne relie deux boîtes entre elles : c'est voulu. Un organe qui appellerait
une autre couche directement (un `@Scheduled` qui pousse lui-même au websocket, par
exemple) est invisible pour tout ce qui surveille le bus — ça a été fait deux fois par
erreur, et corrigé les deux fois.

`services/` n'apparaît pas dans le schéma : c'est une boîte à outils utilisée *par* les
couches ci-dessus (reconnaissance faciale, synthèse vocale...), pas un participant du
bus.

## Mémoire courte, mémoire longue

<div class="diagram-zoom">
<input type="checkbox" id="zoom-archi-mem" class="diagram-zoom-toggle">
<label for="zoom-archi-mem" class="diagram-zoom-label">
<figure>
<div class="archi-mem">
<style>
.archi-mem{max-width:100%}
.archi-mem svg{width:100%;height:auto;display:block}
.archi-mem .boite{fill:var(--md-default-bg-color);stroke:currentColor;stroke-width:1.5}
.archi-mem text{fill:currentColor;font-family:inherit}
.archi-mem .titre-boite{font-size:15px;font-weight:600}
.archi-mem .sous-titre{font-size:11.5px;opacity:.75}
.archi-mem .puce{fill:var(--md-default-fg-color--lighter);stroke:currentColor;stroke-width:1}
.archi-mem .puce-texte{font-size:11.5px}
.archi-mem .eclair{font-size:22px;text-anchor:middle;opacity:0}

@keyframes archi-mem-clear{
  0%,38%,58%,100%{opacity:1}
  45%,52%{opacity:0}
}
@keyframes archi-mem-eclair{
  0%,40%,56%,100%{opacity:0}
  46%,51%{opacity:1}
}
.archi-mem .courtterme-contenu{animation:archi-mem-clear 7s ease-in-out infinite}
.archi-mem #eclair{animation:archi-mem-eclair 7s ease-in-out infinite}
</style>
<svg viewBox="0 0 700 300" role="img" aria-label="La mémoire courte perd tout à chaque redémarrage du robot, la mémoire longue survit — animation qui vide périodiquement les puces de gauche pendant que celles de droite restent inchangées.">
  <rect class="boite" x="40" y="50" width="280" height="220" rx="10"/>
  <text x="180" y="80" text-anchor="middle" class="titre-boite">memoire/courtterme</text>
  <text x="180" y="98" text-anchor="middle" class="sous-titre">perdu à chaque redémarrage</text>

  <g class="courtterme-contenu">
    <rect class="puce" x="65" y="115" width="230" height="30" rx="6"/>
    <text x="180" y="134" text-anchor="middle" class="puce-texte">qui est devant moi</text>
    <rect class="puce" x="65" y="155" width="230" height="30" rx="6"/>
    <text x="180" y="174" text-anchor="middle" class="puce-texte">visage suivi</text>
    <rect class="puce" x="65" y="195" width="230" height="30" rx="6"/>
    <text x="180" y="214" text-anchor="middle" class="puce-texte">apprentissage en cours</text>
  </g>
  <text id="eclair" x="180" y="185" class="eclair">⚡ redémarrage</text>

  <rect class="boite" x="380" y="50" width="280" height="220" rx="10"/>
  <text x="520" y="80" text-anchor="middle" class="titre-boite">memoire/longterme</text>
  <text x="520" y="98" text-anchor="middle" class="sous-titre">survit — SQLite sur disque</text>

  <rect class="puce" x="405" y="115" width="230" height="30" rx="6"/>
  <text x="520" y="134" text-anchor="middle" class="puce-texte">personnes</text>
  <rect class="puce" x="405" y="155" width="230" height="30" rx="6"/>
  <text x="520" y="174" text-anchor="middle" class="puce-texte">visages, rencontres</text>
  <rect class="puce" x="405" y="195" width="230" height="30" rx="6"/>
  <text x="520" y="214" text-anchor="middle" class="puce-texte">conversations</text>
</svg>
</div>
<figcaption>
À gauche, tout disparaît au redémarrage (voulu : qui est actuellement suivi n'a pas de
sens après coupure). À droite, rien ne bouge : c'est le seul fichier SQLite du robot,
rejoué au démarrage via <code>schema.sql</code>.
</figcaption>
</figure>
</label>
</div>

## Et ensuite

Les pages **Fonctionnalités** détaillent, pour chaque activité, quels évènements elle
écoute et lesquels elle publie — la même grammaire que ci-dessus, appliquée à des cas
concrets (conversation, accueil, retrouvailles...).
