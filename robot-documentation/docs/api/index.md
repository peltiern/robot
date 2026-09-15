# API

Documentation technique, pour qui veut écrire un client (le HUD en est un).

## Principe : découverte de capacités

Le client ne code rien en dur. `GET /api/organes` et `GET /api/axes-animables`
décrivent les organes, leurs degrés de liberté, leurs bornes et leurs unités — le HUD
construit ses curseurs et ses jauges à partir de cette réponse. Un organe ajouté côté
robot apparaît côté client sans qu'une ligne y soit écrite.

CORS ouvert (`@CrossOrigin(origins = "*")`) sur tous les contrôleurs : rien ne
restreint aujourd'hui l'origine des clients.

## Référence REST

Générée depuis une spec [OpenAPI](openapi.yaml) écrite à la main (le projet n'a pas
encore de génération automatique type springdoc) et affichée avec
[Redoc](https://github.com/Redocly/redoc).

<div class="redoc-embed">
<div id="redoc-container">Chargement de la référence API…</div>
<script src="https://cdn.jsdelivr.net/npm/redoc@2.1.3/bundles/redoc.standalone.js"></script>
<script>
  Redoc.init('openapi.yaml', {
    expandResponses: '200,204',
    hideDownloadButton: false,
    theme: {
      colors: { primary: { main: '#c9853f' } },
      typography: {
        fontFamily: 'Nunito, sans-serif',
        code: { fontFamily: '"Roboto Mono", monospace' }
      }
    }
  }, document.getElementById('redoc-container'));
</script>
</div>

## WebSocket

Pas de spec formelle ici (OpenAPI ne décrit pas le WebSocket — l'équivalent serait
AsyncAPI, pas encore en place). STOMP sur WebSocket, endpoint `/wsendpoint`.

- **Client → serveur**, préfixe `/app` :
    - `/app/robotevents` — envoie un `RobotEvent` en JSON (un champ `eventType` indique
      la sous-classe concrète : `MouvementCouEvent`, `ArretUrgenceEvent`...). Republié
      tel quel sur le bus d'évènements interne — voir
      [Architecture logicielle](../architecture/index.md#les-couches-autour-du-bus) :
      c'est le point d'entrée qui fait du HUD un participant du bus, au même titre
      qu'un organe.
    - `/app/animation/curseur` — déplace le curseur de l'éditeur d'animation
      (`{animation, instant}`). En WebSocket et non en REST volontairement : un geste
      de souris envoie plusieurs dizaines de positions par seconde, sans réponse
      attendue, et seule la dernière compte.

- **Serveur → clients**, topics diffusés :
    - `/video` — trames vidéo de la caméra ;
    - `/audio` — flux audio ;
    - `/events/{eventType}` — un sous-topic par type d'évènement (par exemple
      `/events/VisagePercuEvent`), pour qu'un client ne s'abonne qu'à ce qui
      l'intéresse plutôt que de filtrer un flux unique.

Tout évènement qui circule sur le bus interne du robot (`systemenerveux/`) peut se
retrouver diffusé ici : le WebSocket est le prolongement du bus jusqu'au navigateur.
