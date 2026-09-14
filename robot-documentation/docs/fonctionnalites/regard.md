# Le regard

## Quand ça se déclenche

À chaque image où la caméra voit un visage — dix fois par seconde environ — sauf si
quelqu'un conduit le robot à la manette (le regard se tait tant que la manette est
utilisée, et pendant les 3 secondes qui suivent le dernier ordre reçu d'elle) ou si une
animation a déjà la main sur le cou.

## Ce que fait le robot

Il ne suit pas le visage en continu comme le ferait une caméra de surveillance. Il
mesure l'écart **une fois**, tourne le cou de cet angle exact, puis attend. Comme la
caméra est fixée sur la tête, tourner la tête recentre l'image — le visage se retrouve
au milieu tout seul, sans qu'il faille le re-mesurer sans arrêt.

<div class="diagram-zoom">
<input type="checkbox" id="zoom-regard" class="diagram-zoom-toggle">
<label for="zoom-regard" class="diagram-zoom-label">
<figure>
<div class="regard-diag">
<style>
.regard-diag{max-width:100%}
.regard-diag svg{width:100%;height:auto;display:block}
.regard-diag text{fill:currentColor;font-family:inherit}
.regard-diag .legende{font-size:12px;opacity:.75;text-anchor:middle}
.regard-diag .corps{fill:var(--md-default-fg-color--lighter);stroke:currentColor;stroke-width:1.5}
.regard-diag .cone{fill:var(--md-accent-fg-color);opacity:.15}
.regard-diag .visee{stroke:var(--md-accent-fg-color);stroke-width:2.5}
.regard-diag .point-vise{fill:var(--md-accent-fg-color)}
.regard-diag .tete{fill:var(--md-default-bg-color);stroke:currentColor;stroke-width:1.5}
.regard-diag .oeil-tete{fill:currentColor}
.regard-diag .personne-tete{fill:var(--md-accent-fg-color)}
.regard-diag .oeil{fill:var(--md-default-bg-color)}
.regard-diag .trait-visage{stroke:var(--md-default-bg-color);stroke-width:2;fill:none;stroke-linecap:round}
.regard-diag .personne-corps{fill:var(--md-accent-fg-color);opacity:.85}
.regard-diag .confirme{fill:none;stroke:var(--md-accent-fg-color);stroke-width:2.5}

.regard-diag #tete-mobile{transform-origin:280px 350px;animation:regard-rotation 6s ease-in-out infinite}
.regard-diag #confirme{animation:regard-confirme 6s ease-in-out infinite}

@keyframes regard-rotation{
  0%{transform:rotate(-18deg)}
  25%{transform:rotate(-18deg)}
  42%{transform:rotate(20deg)}
  90%{transform:rotate(20deg)}
  91%{transform:rotate(-18deg)}
  100%{transform:rotate(-18deg)}
}
@keyframes regard-confirme{
  0%,38%,58%,100%{opacity:0}
  44%,52%{opacity:1}
}
</style>
<svg viewBox="0 0 560 380" role="img" aria-label="Vue de dessus. La tête du robot porte un champ de vision en forme de cône, avec un point qui indique la direction exacte du regard. La personne ne bouge pas : la tête tourne jusqu'à ce que le point de visée arrive pile sur elle.">
  <text x="280" y="22" class="legende">vu de dessus — le cône est ce que la caméra voit</text>

  <!-- la personne : fixe, ne bouge jamais dans cette histoire -->
  <circle class="personne-corps" cx="362" cy="150" r="17"/>
  <circle class="personne-tete" cx="362" cy="112" r="17"/>
  <circle class="oeil" cx="355" cy="107" r="2.2"/>
  <circle class="oeil" cx="369" cy="107" r="2.2"/>
  <path class="trait-visage" d="M 353 120 Q 362 127 371 120"/>
  <circle id="confirme" cx="362" cy="112" r="25"/>

  <!-- le corps du robot : fixe, seule la tête tourne -->
  <path class="corps" d="M 250,350 L 310,350 L 330,378 L 230,378 Z"/>

  <!-- la tête du robot : tourne pour amener le point de visée sur la personne -->
  <g id="tete-mobile">
    <path class="cone" d="M 280,350 L 113,151 L 447,151 Z"/>
    <line class="visee" x1="280" y1="350" x2="280" y2="90"/>
    <circle class="tete" cx="280" cy="350" r="24"/>
    <circle class="oeil-tete" cx="272" cy="345" r="2.5"/>
    <circle class="oeil-tete" cx="288" cy="345" r="2.5"/>
    <circle class="point-vise" cx="280" cy="90" r="5"/>
  </g>
</svg>
</div>
<figcaption>
Vu de dessus : le cône est tout ce que la caméra peut voir, le point au bout de la
ligne est la direction exacte du regard. La personne ne bouge jamais — c'est la tête
qui tourne, d'un seul coup, jusqu'à ce que le point de visée arrive sur elle
(<code>MouvementCouEvent</code>). Pas de nouvelle mesure en cours de route.
</figcaption>
</figure>
</label>
</div>

??? note "Comment ça marche, en détail ?"
    L'écart en pixels entre le visage et l'axe optique de la caméra est converti en
    degrés grâce au champ de vision de la caméra (69,36° à l'horizontale) : plus la
    caméra voit large, moins un même déplacement en pixels représente d'angle.

    Quelques réglages, tous rechargeables à chaud (`robot.properties`) :

    - **zone morte** : 4° — en dessous, la tête ne bouge pas (évite le tic nerveux) ;
    - **gain** : 0,9° de tête par degré vu — la correction n'est pas amortie, elle
      corrige l'écart en entier en un coup, pas en plusieurs petits pas ;
    - **déport caméra** : 3,8° — la caméra est logée dans un œil, pas au centre de la
      tête ; sans cette correction, viser le centre de l'image laisserait la tête tournée
      légèrement à côté ;
    - **temporisation** : 1 s entre deux corrections, le temps que le servo arrive
      réellement — les servos du cou ne renvoient pas leur position, il n'y a pas moyen
      de savoir plus tôt qu'ils sont arrivés.

    Ce n'est **pas** un asservissement continu (pas de vitesse recalculée sans arrêt sur
    l'erreur) : un seul ordre, une seule fois, puis on attend que la géométrie fasse le
    reste.
