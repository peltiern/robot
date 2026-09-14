# Architecture logicielle

Le code du robot est organisé comme un corps : des **organes** (caméra, cou, yeux,
roues...), un **cerveau** (mémoire, décision, activités), et un point commun à tous :
personne ne se parle jamais directement. Tout passe par un seul canal, le **bus
d'évènements** — c'est la règle la plus stricte du projet, et elle rend chaque organe
remplaçable, testable et surveillable indépendamment des autres.

## Les couches, autour du bus

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
.archi-bus .etiquette{font-size:12px;font-weight:600;fill:var(--md-accent-fg-color);opacity:0}

@keyframes archi-organes-pulse{
  0%,3%,100%{stroke:currentColor;stroke-width:1.5}
  1.5%{stroke:var(--md-accent-fg-color);stroke-width:3}
  80%,86%{stroke:currentColor;stroke-width:1.5}
  83%{stroke:var(--md-accent-fg-color);stroke-width:3}
}
@keyframes archi-decisionnel-pulse{
  0%,33%,49%,100%{stroke:currentColor;stroke-width:1.5}
  40%{stroke:var(--md-accent-fg-color);stroke-width:3}
}
@keyframes archi-paquet-a{
  0%,100%{opacity:0}
  1%{opacity:1;cy:340px}
  14%{opacity:1;cy:255px}
  16%{opacity:0}
}
@keyframes archi-paquet-b{
  0%,17%,100%{opacity:0}
  19%{opacity:1;cy:255px}
  32%{opacity:1;cy:340px}
  34%{opacity:0}
}
@keyframes archi-paquet-c{
  0%,48%,100%{opacity:0}
  50%{opacity:1;cy:340px}
  63%{opacity:1;cy:255px}
  65%{opacity:0}
}
@keyframes archi-paquet-d{
  0%,66%,100%{opacity:0}
  68%{opacity:1;cy:255px}
  80%{opacity:1;cy:340px}
  82%{opacity:0}
}
@keyframes archi-etiquette-1{
  0%,2%,33%,100%{opacity:0}
  4%{opacity:1}
  31%{opacity:1}
}
@keyframes archi-etiquette-2{
  0%,48%,79%,100%{opacity:0}
  50%{opacity:1}
  77%{opacity:1}
}
.archi-bus #p-organes{animation:archi-organes-pulse 9s ease-in-out infinite}
.archi-bus #p-decisionnel{animation:archi-decisionnel-pulse 9s ease-in-out infinite}
.archi-bus #q-a{animation:archi-paquet-a 9s linear infinite}
.archi-bus #q-b{animation:archi-paquet-b 9s linear infinite}
.archi-bus #q-c{animation:archi-paquet-c 9s linear infinite}
.archi-bus #q-d{animation:archi-paquet-d 9s linear infinite}
.archi-bus #t-1{animation:archi-etiquette-1 9s linear infinite}
.archi-bus #t-2{animation:archi-etiquette-2 9s linear infinite}
</style>
<svg viewBox="0 0 900 460" role="img" aria-label="Toutes les couches parlent uniquement au bus d'évènements, jamais entre elles directement. Exemple animé : la caméra détecte un visage, le décisionnel décide où regarder, le cou reçoit l'ordre — chaque étape passe par le bus.">
  <defs>
    <marker id="archi-fleche" viewBox="0 0 8 8" refX="4" refY="4" markerWidth="6" markerHeight="6" orient="auto-start-reverse">
      <path d="M0,0 L8,4 L0,8 Z" fill="currentColor"/>
    </marker>
  </defs>

  <!-- connecteurs -->
  <line class="lien" x1="175" y1="114" x2="175" y2="205"/>
  <line class="lien" x1="725" y1="114" x2="725" y2="205"/>
  <line class="lien" x1="145" y1="340" x2="145" y2="255"/>
  <line class="lien" x1="345" y1="340" x2="345" y2="255"/>
  <line class="lien" x1="530" y1="340" x2="530" y2="255"/>
  <line class="lien" x1="720" y1="340" x2="720" y2="255"/>

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
  <rect id="p-organes" class="boite" x="50" y="340" width="190" height="64" rx="8"/>
  <text x="145" y="366" text-anchor="middle" class="titre-boite">organes/</text>
  <text x="145" y="384" text-anchor="middle" class="sous-titre">capteurs, actionneurs</text>

  <rect class="boite" x="270" y="340" width="150" height="64" rx="8"/>
  <text x="345" y="366" text-anchor="middle" class="titre-boite">mémoire/</text>
  <text x="345" y="384" text-anchor="middle" class="sous-titre">courte, longue</text>

  <rect id="p-decisionnel" class="boite" x="450" y="340" width="160" height="64" rx="8"/>
  <text x="530" y="366" text-anchor="middle" class="titre-boite">décisionnel/</text>
  <text x="530" y="384" text-anchor="middle" class="sous-titre">arbitrage, regard</text>

  <rect class="boite" x="640" y="340" width="160" height="64" rx="8"/>
  <text x="720" y="366" text-anchor="middle" class="titre-boite">activités/</text>
  <text x="720" y="384" text-anchor="middle" class="sous-titre">conversation...</text>

  <!-- étiquettes de l'évènement en cours -->
  <text id="t-1" x="450" y="195" text-anchor="middle" class="etiquette">VisagePercuEvent</text>
  <text id="t-2" x="450" y="195" text-anchor="middle" class="etiquette">MouvementCouEvent</text>

  <!-- paquets animés -->
  <circle id="q-a" class="accent" cx="145" cy="340" r="6"/>
  <circle id="q-b" class="accent" cx="530" cy="255" r="6"/>
  <circle id="q-c" class="accent" cx="530" cy="340" r="6"/>
  <circle id="q-d" class="accent" cx="145" cy="255" r="6"/>
</svg>
</div>
<figcaption>
Exemple réel et complet : la caméra détecte un visage (<code>VisagePercuEvent</code>),
le <code>décisionnel</code> (<code>Regard</code>) décide où tourner la tête et publie
<code>MouvementCouEvent</code>, que l'organe <code>Cou</code> exécute. Quatre couches,
zéro appel direct entre elles — seulement le bus.
</figcaption>
</figure>

Aucune flèche ne relie deux boîtes entre elles : c'est voulu. Un organe qui appellerait
une autre couche directement (un `@Scheduled` qui pousse lui-même au websocket, par
exemple) est invisible pour tout ce qui surveille le bus — ça a été fait deux fois par
erreur, et corrigé les deux fois.

`services/` n'apparaît pas dans le schéma : c'est une boîte à outils utilisée *par* les
couches ci-dessus (reconnaissance faciale, synthèse vocale...), pas un participant du
bus.

## Mémoire courte, mémoire longue

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

## Et ensuite

Les pages **Fonctionnalités** détaillent, pour chaque activité, quels évènements elle
écoute et lesquels elle publie — la même grammaire que ci-dessus, appliquée à des cas
concrets (conversation, accueil, retrouvailles...).
