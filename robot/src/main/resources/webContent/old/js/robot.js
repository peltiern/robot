$(document).ready(chargerListeAnimations);

// Websocket pour la vidéo
var wsVideo = new WebSocket("ws://" + location.hostname + ":" + location.port + "/ws/liveVideo/");
wsVideo.onmessage = function(msg) {
	var target = document.getElementById("liveVideo");
	url = window.URL.createObjectURL(msg.data);
	target.onload = function() {
		window.URL.revokeObjectURL(url);
	};
	target.src = url;
}

// Websocket bidirectionnel pour les évènements envoyés par le robot ou à envoyer au robot
var wsRobotEvent = new WebSocket("ws://" + location.hostname + ":" + location.port + "/ws/robotEvents/");
wsRobotEvent.onmessage = function(message) {
//	if (event.eventType == "Conversation") {
//		traiterConversationEvent(event);
//	}
	traiterConversationMessage(message);
}
 
Mousetrap.bind("left", tournerAGauche, "keydown");
Mousetrap.bind("left", stopperTeteGaucheDroite, "keyup");
Mousetrap.bind("right", tournerADroite, "keydown");
Mousetrap.bind("right", stopperTeteGaucheDroite, "keyup");
Mousetrap.bind("up", tournerEnHaut, "keydown");
Mousetrap.bind("up", stopperTeteHautBas, "keyup");
Mousetrap.bind("down", tournerEnBas, "keydown");
Mousetrap.bind("down", stopperTeteHautBas, "keyup");
Mousetrap.bind("enter", parler);
Mousetrap.bind("a", function(){jouerExpression("01_allumer");playbackAnimation("01_allumer");});
Mousetrap.bind("q", function(){jouerExpression("02_eteindre");playbackAnimation("02_eteindre");});
Mousetrap.bind("z", function(){jouerExpression("03_clignement");playbackAnimation("03_clignement");});
Mousetrap.bind("s", function(){jouerExpression("04_clin_oeil");playbackAnimation("04_clin_oeil");});
Mousetrap.bind("w", function(){jouerExpression("05_amour");playbackAnimation("05_amour");});
Mousetrap.bind("e", function(){jouerExpression("06_triste");playbackAnimation("06_triste");});
Mousetrap.bind("d", function(){jouerExpression("07_triste_inverse");playbackAnimation("07_triste_inverse");});
 
function bougerTete(e) {
	var mouvementTeteEvent = {};
	mouvementTeteEvent.eventType = "MouvementTete";
	mouvementTeteEvent.positionGaucheDroite = (e.clientX * 180) / 640;
	mouvementTeteEvent.positionHautBas = ((480 - e.clientY) * 180) / 480;
	wsRobotEvent.send(JSON.stringify(mouvementTeteEvent));
}

function tournerAGauche() {
	var mouvementTeteEvent = {};
	mouvementTeteEvent.eventType = "MouvementTete";
	mouvementTeteEvent.mouvementGaucheDroite = "TOURNER_GAUCHE";
	wsRobotEvent.send(JSON.stringify(mouvementTeteEvent));
}
 
function tournerADroite() {
	var mouvementTeteEvent = {};
	mouvementTeteEvent.eventType = "MouvementTete";
	mouvementTeteEvent.mouvementGaucheDroite = "TOURNER_DROITE";
	wsRobotEvent.send(JSON.stringify(mouvementTeteEvent));
}

function stopperTeteGaucheDroite() {
	var mouvementTeteEvent = {};
	mouvementTeteEvent.eventType = "MouvementTete";
	mouvementTeteEvent.mouvementGaucheDroite = "STOPPER";
	wsRobotEvent.send(JSON.stringify(mouvementTeteEvent));
}
 
function tournerEnHaut() {
	var mouvementTeteEvent = {};
	mouvementTeteEvent.eventType = "MouvementTete";
	mouvementTeteEvent.mouvementHauBas = "TOURNER_HAUT";
	wsRobotEvent.send(JSON.stringify(mouvementTeteEvent));
}
 
function tournerEnBas() {
	var mouvementTeteEvent = {};
	mouvementTeteEvent.eventType = "MouvementTete";
	mouvementTeteEvent.mouvementHauBas = "TOURNER_BAS";
	wsRobotEvent.send(JSON.stringify(mouvementTeteEvent));
}

function stopperTeteHautBas() {
	var mouvementTeteEvent = {};
	mouvementTeteEvent.eventType = "MouvementTete";
	mouvementTeteEvent.mouvementHauBas = "STOPPER";
	wsRobotEvent.send(JSON.stringify(mouvementTeteEvent));
}

function parler() {
	var message = $("#inputDiscussion").val();
	if (message && message != "") {
		var paroleEvent = {};
		paroleEvent.eventType = "Parole";
		paroleEvent.texte = message;
		wsRobotEvent.send(JSON.stringify(paroleEvent));
	}
	$("#inputMessage").val("");
	$("#inputMessage").focus();
}

function traiterConversationEvent(conversationEvent) {
	if (event.texte && event.texte != "") {
		var classCoteImage = "right";
		var classCoteNom = "left";
		var nomLocuteur = "Robot";
		if (conversationEvent.idLocuteur != -1) {
			classCoteImage = "left";
			classCoteNom = "right";
			nomLocuteur = "Utilisateur";
		}
		$("#conteneurConversations").append(
			"<li class='" + classCoteImage + " clearfix'><span class='chat-img pull-" + classCoteImage + "'>"
            	+ "<img src='http://placehold.it/50/FA6F57/fff&text=ME' alt='User Avatar' class='img-circle' />"
                	+ "</span>"
                    + "<div class='chat-body clearfix'>"
                    	+ "<div class='header'>"
                        	+ "<small class='text-muted'><span class='glyphicon glyphicon-time'></span>15 mins ago</small>"
                            	+ "<strong class='pull-" + classCoteNom + " primary-font'>" + nomLocuteur + "</strong>"
                        + "</div>"
                        + "<p>"
                            + conversation.texte
                        + "</p>"
                    + "</div>"
            + "</li>"
		);
	}
}

function traiterConversationMessage(message) {
	if (message && message != "") {
		var classCoteImage = "right";
		var classCoteNom = "left";
		var nomLocuteur = "Robot";
//		if (conversationEvent.idLocuteur != -1) {
//			classCoteImage = "left";
//			classCoteNom = "right";
//			nomLocuteur = "Utilisateur";
//		}
		$("#conteneurConversations").append(
			"<li class='" + classCoteImage + " clearfix'><span class='chat-img pull-" + classCoteImage + "'>"
            	+ "<img src='http://placehold.it/50/FA6F57/fff&text=ME' alt='User Avatar' class='img-circle' />"
                	+ "</span>"
                    + "<div class='chat-body clearfix'>"
                    	+ "<div class='header'>"
                        	+ "<small class='text-muted'><span class='glyphicon glyphicon-time'></span>15 mins ago</small>"
                            	+ "<strong class='pull-" + classCoteNom + " primary-font'>" + nomLocuteur + "</strong>"
                        + "</div>"
                        + "<p>"
                            + conversation.texte
                        + "</p>"
                    + "</div>"
            + "</li>"
		);
	}
}

function jouerExpression(expression) {
	if (expression && expression != "") {
		var expressionVisageEvent = {};
		expressionVisageEvent.eventType = "ExpressionVisage";
		expressionVisageEvent.expression = expression;
		wsRobotEvent.send(JSON.stringify(expressionVisageEvent));
	}
}


// Gestion des animations du visage
var mapAnimations = {};

/**
 * Charge la liste des animations disponibles
 */
function chargerListeAnimations() {
	$.ajax({
        url: "http://" + location.hostname + ":" + location.port + "/rest/animations"
    }).then(function(data) {
    	$.each(data, function( index, animation ) {
    		mapAnimations[animation.idAnimation] = animation;
    		// Ajout du bouton de l'animation
    		$(".listeAnimations").append("<button onclick=\"jouerExpression('" + animation.idAnimation + "');playbackAnimation('" + animation.idAnimation + "');\">" + animation.libelleAnimation + "</button>");
    	});
    	
    });
}

/**
 * Joue une animation.
 * @param idAnimation
 */
function playbackAnimation(idAnimation) {
	var animation = mapAnimations[idAnimation];
	var pause = 0
	$.each(animation.listeItems, function( index, item ){
		if (item.type == "I") {
			setTimeout(function() {
				$(".previewAnimation").attr("src","http://" + location.hostname + ":" + location.port + "/rest/images/image-" + item.idImage);
			}, pause);
		} else if (item.type == "P") {
			pause = pause + item.tempsPause * 1;
		}
	});
}