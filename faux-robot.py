"""Faux robot : /api/organes + STOMP sur /wsendpoint, juste de quoi regarder le HUD."""
import asyncio, base64, io, json, math, random, time
from aiohttp import web, WSMsgType
from PIL import Image, ImageDraw

# ── Images ────────────────────────────────────────────────────────────────────
L, H = 640, 480
TRAMES = []
for i in range(48):
    t = i / 48
    img = Image.new("RGB", (L, H), (58, 49, 40))
    d = ImageDraw.Draw(img)
    d.rectangle([0, int(H * .68), L, H], fill=(33, 26, 21))
    d.rounded_rectangle([int(L * .06), int(H * .5), int(L * .23), int(H * .8)], 12, fill=(61, 50, 39))
    d.rounded_rectangle([int(L * .72), int(H * .55), int(L * .94), int(H * .71)], 10, fill=(70, 58, 45))
    cx = (.5 + math.sin(t * 2 * math.pi) * .16) * L
    cy = .46 * H
    d.rounded_rectangle([cx - H * .13, cy + H * .09, cx + H * .13, cy + H * .47], int(H * .06), fill=(74, 61, 49))
    d.ellipse([cx - H * .1, cy - H * .1, cx + H * .1, cy + H * .1], fill=(92, 76, 61))
    buf = io.BytesIO()
    img.save(buf, "JPEG", quality=70)
    TRAMES.append((base64.b64encode(buf.getvalue()).decode(), cx, cy))

ORGANES = [
    {"id": "yeux", "libelle": "Yeux", "type": "ACTIONNEUR", "articulations": [
        {"id": "oeilGauche", "libelle": "Œil gauche", "unite": "deg", "min": -5, "max": 20, "orientation": "VERTICAL", "position": 2},
        {"id": "oeilDroit", "libelle": "Œil droit", "unite": "deg", "min": -5, "max": 20, "orientation": "VERTICAL", "position": 2}], "mesures": []},
    {"id": "cou", "libelle": "Cou", "type": "ACTIONNEUR", "articulations": [
        {"id": "pan", "libelle": "Panoramique (gauche / droite)", "unite": "deg", "min": -60, "max": 60, "orientation": "HORIZONTAL", "position": 0},
        {"id": "tilt", "libelle": "Inclinaison (haut / bas)", "unite": "deg", "min": -8, "max": 7, "orientation": "VERTICAL", "position": 1},
        {"id": "upDown", "libelle": "Monter / descendre", "unite": "deg", "min": -10, "max": 60, "orientation": "VERTICAL", "position": 12}], "mesures": []},
    {"id": "materiel", "libelle": "Matériel", "type": "CAPTEUR", "articulations": [], "mesures": [
        {"id": "cpuCharge", "libelle": "Charge CPU", "unite": "%", "min": 0, "max": 100, "valeur": 34},
        {"id": "memoire", "libelle": "Mémoire", "unite": "%", "min": 0, "max": 100, "valeur": 71},
        {"id": "temperatureCpu", "libelle": "Température CPU", "unite": "°C", "min": 0, "max": 100, "valeur": 42},
        {"id": "disque", "libelle": "Disque", "unite": "%", "min": 0, "max": 100, "valeur": 58}]},
]

abonnes = []  # (ws, destination, id)
compteur = 0


async def envoyer(destination, corps):
    global compteur
    for ws, dest, sid in list(abonnes):
        if dest != destination or ws.closed:
            continue
        compteur += 1
        trame = (f"MESSAGE\ndestination:{destination}\nsubscription:{sid}\n"
                 f"message-id:{compteur}\ncontent-type:application/json\n\n{corps}\x00")
        try:
            await ws.send_str(trame)
        except Exception:
            pass


# Arrêt d'urgence : le vrai robot en est le seul maître, on reproduit ce comportement
# (le front demande, le robot confirme sur /events/arret-urgence).
arret_urgence = {"actif": False, "origine": None}


async def traiter_ordre(corps):
    """Rejoue le peu de logique robot dont le HUD a besoin en retour."""
    try:
        ordre = json.loads(corps)
    except (ValueError, TypeError):
        return
    if ordre.get("eventType") == "arret-urgence":
        arret_urgence["actif"] = bool(ordre.get("actif"))
        arret_urgence["origine"] = ordre.get("origine")
        await envoyer("/events/arret-urgence", json.dumps({
            "eventType": "arret-urgence",
            "actif": arret_urgence["actif"],
            "origine": arret_urgence["origine"],
        }))


async def ws_handler(requete):
    ws = web.WebSocketResponse(protocols=("v12.stomp", "v11.stomp", "v10.stomp"))
    await ws.prepare(requete)
    async for msg in ws:
        if msg.type is not WSMsgType.TEXT:
            continue
        for brute in msg.data.split("\x00"):
            brute = brute.strip("\n\r ")
            if not brute:
                continue
            lignes = brute.split("\n")
            commande = lignes[0].strip()
            entetes = {}
            for ligne in lignes[1:]:
                if not ligne.strip():
                    break
                k, _, v = ligne.partition(":")
                entetes[k.strip()] = v.strip()
            if commande in ("CONNECT", "STOMP"):
                await ws.send_str("CONNECTED\nversion:1.2\nheart-beat:0,0\n\n\x00")
            elif commande == "SUBSCRIBE":
                abonnes.append((ws, entetes.get("destination"), entetes.get("id")))
                print("abonnement", entetes.get("destination"))
            elif commande == "SEND":
                corps = brute.split("\n\n", 1)[-1]
                print("reçu du front →", entetes.get("destination"), corps[:120])
                await traiter_ordre(corps)
            elif commande == "DISCONNECT":
                await ws.close()
    abonnes[:] = [a for a in abonnes if a[0] is not ws]
    return ws


async def boucle_video(_app):
    i = 0
    while True:
        b64, cx, cy = TRAMES[i % len(TRAMES)]
        i += 1
        r = H * .11
        await envoyer("/video", json.dumps({
            "imageBase64": b64, "faceFound": True,
            "faces": [{"x": cx - r, "y": cy - r * 1.3, "width": r * 2, "height": r * 2.4, "name": "Nicolas · 94 %"}],
            "objects": [{"x": L * .72, "y": H * .55, "width": L * .22, "height": H * .16, "name": "chaise · 71 %"}],
        }))
        await asyncio.sleep(1 / 12)


async def boucle_telemetrie(_app):
    v = {"cpuCharge": 34, "memoire": 71, "temperatureCpu": 42, "disque": 58}
    pos = {"pan": 0, "tilt": 1, "upDown": 12, "oeilGauche": 2, "oeilDroit": 2}
    bornes = {"pan": (-60, 60), "tilt": (-8, 7), "upDown": (-10, 60), "oeilGauche": (-5, 20), "oeilDroit": (-5, 20)}
    while True:
        for k in v:
            v[k] = max(0, min(100, v[k] + random.uniform(-3, 3)))
        await envoyer("/events/telemetrie-organe", json.dumps({"eventType": "telemetrie-organe", "idOrgane": "materiel", "valeurs": v}))
        for k in pos:
            lo, hi = bornes[k]
            pos[k] = max(lo, min(hi, pos[k] + random.uniform(-4, 4)))
        await envoyer("/events/telemetrie-organe", json.dumps({"eventType": "telemetrie-organe", "idOrgane": "cou", "valeurs": pos}))
        await asyncio.sleep(1.2)


ECHANGES = [
    (0, "salut wall-e"), (-1, "Salut Nicolas. Je t'écoute."),
    (0, "qu'est-ce que tu vois là"), (-1, "Je vois Nicolas, et une chaise derrière lui."),
    (0, "tourne la tête à gauche"), (-1, "Voilà. Je regarde à gauche."),
    (0, "raconte-moi une blague"), (-1, "Pourquoi le robot a traversé la route ? Il suivait son programme."),
]


async def boucle_conversation(_app):
    i = 0
    while True:
        locuteur, texte = ECHANGES[i % len(ECHANGES)]
        i += 1
        if locuteur == 0:
            await envoyer("/events/reconnaissance-vocale", json.dumps({"eventType": "reconnaissance-vocale", "texteReconnu": texte}))
        await envoyer("/events/conversation", json.dumps({
            "eventType": "conversation", "texte": texte, "idLocuteur": locuteur,
            "dateTime": time.strftime("%Y-%m-%dT%H:%M:%S")}))
        await asyncio.sleep(3.5)


async def demarrer(app):
    for boucle in (boucle_video, boucle_telemetrie, boucle_conversation):
        app[boucle.__name__] = asyncio.create_task(boucle(app))


app = web.Application()
app.router.add_get("/api/organes", lambda r: web.json_response(ORGANES))
app.router.add_get("/api/arret-urgence", lambda r: web.json_response({"actif": arret_urgence["actif"]}))
app.router.add_get("/wsendpoint", ws_handler)
app.on_startup.append(demarrer)
web.run_app(app, host=["127.0.0.1", "::1"], port=8080)
