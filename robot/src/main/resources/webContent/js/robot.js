$(document).ready(function() {
	moment.locale("fr");
	chargerListeAnimations();
	chargerListeImagesVisage();
	initialiserJoystick();
	initialiserSlidersYeux();
	initialiserDropzone();
});

// Création du contexte audio
window.AudioContext = window.AudioContext||window.webkitAudioContext;
contexteAudio = new AudioContext();

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

// Websocket pour l'audio
var wsAudio = new WebSocket("ws://" + location.hostname + ":" + location.port + "/ws/liveAudio/");
wsAudio.binaryType = "arraybuffer"
wsAudio.onmessage = function(msg) {
	traiterAudio(msg.data);
}

//Websocket bidirectionnel pour les évènements envoyés par le robot ou à envoyer au robot
var wsRobotEvent = new WebSocket("ws://" + location.hostname + ":" + location.port + "/ws/robotEvents/");
wsRobotEvent.onmessage = function(msg) {
	if (msg.data && msg.data != "") {
		var event = JSON.parse(msg.data);
		if (event.eventType == "Conversation") {
			traiterConversationEvent(event);
		}
	}
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
	/*var mouvementTeteEvent = {};
	mouvementTeteEvent.eventType = "MouvementTete";
	mouvementTeteEvent.positionGaucheDroite = (e.clientX * 180) / 640;
	mouvementTeteEvent.positionHautBas = ((480 - e.clientY) * 180) / 480;
	wsRobotEvent.send(JSON.stringify(mouvementTeteEvent));*/
}

function tournerAGauche() {
	var mouvementTeteEvent = {};
	mouvementTeteEvent.eventType = "MouvementCou";
	mouvementTeteEvent.mouvementGaucheDroite = "TOURNER_GAUCHE";
	wsRobotEvent.send(JSON.stringify(mouvementTeteEvent));
}
 
function tournerADroite() {
	var mouvementTeteEvent = {};
	mouvementTeteEvent.eventType = "MouvementCou";
	mouvementTeteEvent.mouvementGaucheDroite = "TOURNER_DROITE";
	wsRobotEvent.send(JSON.stringify(mouvementTeteEvent));
}

function stopperTeteGaucheDroite() {
	var mouvementTeteEvent = {};
	mouvementTeteEvent.eventType = "MouvementCou";
	mouvementTeteEvent.mouvementGaucheDroite = "STOPPER";
	wsRobotEvent.send(JSON.stringify(mouvementTeteEvent));
}
 
function tournerEnHaut() {
	var mouvementTeteEvent = {};
	mouvementTeteEvent.eventType = "MouvementCou";
	mouvementTeteEvent.mouvementHauBas = "TOURNER_HAUT";
	wsRobotEvent.send(JSON.stringify(mouvementTeteEvent));
}
 
function tournerEnBas() {
	var mouvementTeteEvent = {};
	mouvementTeteEvent.eventType = "MouvementCou";
	mouvementTeteEvent.mouvementHauBas = "TOURNER_BAS";
	wsRobotEvent.send(JSON.stringify(mouvementTeteEvent));
}

function stopperTeteHautBas() {
	var mouvementTeteEvent = {};
	mouvementTeteEvent.eventType = "MouvementCou";
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
	$("#inputDiscussion").val("");
	$("#inputDiscussion").focus();
}

function traiterConversationEvent(conversationEvent) {
	if (conversationEvent.texte && conversationEvent.texte != "") {
		var jour = moment(conversationEvent.timestamp).format("L");
		var heure = moment(conversationEvent.timestamp).format("LT");
		var classCoteImage = "right";
		var classCoteNom = "left";
		var nomLocuteur = "Robot";
		if (conversationEvent.idLocuteur != -1) {
			classCoteImage = "left";
			classCoteNom = "right";
			nomLocuteur = "Utilisateur";
		}
		var conteneurConversation = $("#conteneurConversations");
		conteneurConversation.append(
			"<li class='" + classCoteImage + " clearfix'><span class='chat-img pull-" + classCoteImage + "'>"
            	+ "<img src='http://placehold.it/50/FA6F57/fff&text=ME' alt='User Avatar' class='img-circle' />"
                	+ "</span>"
                    + "<div class='chat-body clearfix'>"
                    	+ "<div class='header'>"
                        	+ "<small class='text-muted'><span class='glyphicon glyphicon-time'></span>" + jour + " " + heure + "</small>"
                            	+ "<strong class='pull-" + classCoteNom + " primary-font'>" + nomLocuteur + "</strong>"
                        + "</div>"
                        + "<p>"
                            + conversationEvent.texte
                        + "</p>"
                    + "</div>"
            + "</li>"
		);
	    var height = conteneurConversation[0].scrollHeight;
	    conteneurConversation.scrollTop(height);
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

// Gestion de la fenêtre modale de modification des animations
$("#modalAnimation").on("show.bs.modal", function (event) {
	  var button = $(event.relatedTarget) // Button that triggered the modal
	  var idAnimation = button.data("idanimation"); // Extract info from data-* attributes
	  // Chargement de l'animation dans la popup
	  chargerAnimation(idAnimation);
})

/**
 * Charge la liste des animations disponibles
 */
function chargerListeAnimations() {
	$.ajax({
        url: "http://" + location.hostname + ":" + location.port + "/rest/visage/animations"
    }).then(function(data) {
    	// Suppression de toutes les lignes du tableau excepté l'entête
    	$('#tableListeAnimationsVisage tr').not(function(){ return !!$(this).has('th').length; }).remove();
    	$.each(data, function( index, animation ) {
    		mapAnimations[animation.idAnimation] = animation;
    		// Ajout de l'animation au tableau
    		$("#tableListeAnimationsVisage").append(
    				  "<tr>"
                    	+ "<td>" + animation.idAnimation + "</td>"
                    	+ "<td>" + animation.libelleAnimation + "</td>"
                    	+ "<td>"
                    		+ "<button class=\"btn btn-xs\" onclick=\"jouerExpression('" + animation.idAnimation + "');playbackAnimation('" + animation.idAnimation + "'); return false;\"><span class=\"glyphicon glyphicon-play\"></span></button>"
                    		+ "<button class=\"btn btn-xs\" data-toggle=\"modal\" data-target=\"#modalAnimation\" data-idanimation=\"" + animation.idAnimation + "\"><span class=\"glyphicon glyphicon-pencil\"></span></button>"
                    	+ "</td>"
                    + "</tr>"
    		);
    	});
    	
    });
}

/**
 * Charge une animation dans la popup de saisie.
 * @param idAnimation identifiant de l'animation
 */
function chargerAnimation(idAnimation) {
	var animation = mapAnimations[idAnimation];
	var modal = $("#modalAnimation");
	modal.find(".modal-title").text(animation.libelleAnimation);
	modal.find("#animationCode").val(animation.idAnimation);
	modal.find("#animationLibelle").val(animation.libelleAnimation);
	// On vide le conteneur des images
	$("#animationListeImages").empty();
	// Chargement des images
	$.each(animation.listeImages, function( index, image ) {
		// Ajout de l'image
		$("#animationListeImages").append("<img style=\"width: 100px;\" src=\"http://" + location.hostname + ":" + location.port + "/rest/visage/images/" + image.idImage + "\">");
	});
	/*modal.find(".modal-body").css({
		width:'auto', //probably not needed
		height:'auto', //probably not needed 
		'max-height':'100%'
	});*/
}

/**
 * Joue une animation.
 * @param idAnimation
 */
function playbackAnimation(idAnimation) {
	var animation = mapAnimations[idAnimation];
	var tempsPause = 0;
	$.each(animation.listeImages, function( index, image ){
		setTimeout(function() {
			$("#previewAnimationVisage").attr("src","http://" + location.hostname + ":" + location.port + "/rest/visage/images/" + image.idImage);
		}, tempsPause);
		tempsPause = tempsPause + image.tempsPause;
	});
}

function ouvrirFenetreModificationAnimation(idAnimation) {
	if (idAnimation && idAnimation != null) {
		var animation = mapAnimations[idAnimation];
		
	}
}

/**
 * Charge la liste des images des visages
 */
function chargerListeImagesVisage() {
	$.ajax({
        url: "http://" + location.hostname + ":" + location.port + "/rest/visage/images"
    }).then(function(data) {
    	// On vide le conteneur
		$("#conteneurImagesVisage").empty();
    	$.each(data, function( index, image ) {
    		// Ajout de l'image
    		$("#conteneurImagesVisage").append("<div class=\"col-lg-4 col-sm-4 col-xs-6\"><a title=\"" + image.nom + "\" href=\"#\"><img style=\"height: auto;\" class=\"thumbnail img-responsive\" src=\"http://" + location.hostname + ":" + location.port + "/rest/visage/images/" + image.nom + "\"></a></div>");
    	});
    	
    });
}

/**
 * Initialise le joystick de contrôle de la tête.
 */
function initialiserJoystick() {
	var joystick = nipplejs.create({
        zone: document.getElementById("divVideo"),
        color: "blue",
        threshold: 0.7
    });
	joystick.on('end move', function(evt, data) {
		traiterJoystickEvent(evt, data);
	});
}

/**
 * Traitement des évènements du joystick.
 * @param evt l'évènement
 * @param data les données liées à l'évènement
 */
var mouvementGaucheDroitePrecedent = "stop";
var mouvementHautBasPrecedent = "stop";
function traiterJoystickEvent(evt, data) {
	var mouvementGaucheDroite;
	var mouvementHautBas;
	// Type Mouvement
	if (evt.type == "move") {
		// Déplacement en fonction de l'angle
		var angle = data.angle.degree;
		if ((angle >= 0 && angle < 20) || (angle >= 340 && angle <0)) {
			// Droite
			mouvementGaucheDroite = "droite";
			mouvementHautBas = "stop";
		} else if (angle >= 20 && angle < 70) {
			// Haut - Droite
			mouvementGaucheDroite = "droite";
			mouvementHautBas = "haut";
		} else if (angle >= 70 && angle < 110) {
			// Haut
			mouvementGaucheDroite = "stop";
			mouvementHautBas = "haut";
		} else if (angle >= 110 && angle < 160) {
			// Haut - Gauche
			mouvementGaucheDroite = "gauche";
			mouvementHautBas = "haut";
		} else if (angle >= 160 && angle < 200) {
			// Gauche
			mouvementGaucheDroite = "gauche";
			mouvementHautBas = "stop";
		} else if (angle >= 200 && angle < 250) {
			// Bas - Gauche
			mouvementGaucheDroite = "gauche";
			mouvementHautBas = "bas";
		} else if (angle >= 250 && angle < 290) {
			// Bas
			mouvementGaucheDroite = "stop";
			mouvementHautBas = "bas";
		} else if (angle >= 290 && angle < 340) {
			// Bas - Droite
			mouvementGaucheDroite = "droite";
			mouvementHautBas = "bas";
		}
	} else if (evt.type == "end") {
		// On stoppe tout
		mouvementGaucheDroite = "stop";
		mouvementHautBas = "stop";
	}
	
	if (mouvementGaucheDroite != mouvementGaucheDroitePrecedent) {
		mouvementGaucheDroitePrecedent = mouvementGaucheDroite;
		// Si le mouvement "Gauche - Droite" est différent du précédent : envoi d'un évènement
		if (mouvementGaucheDroite == "stop") {
			stopperTeteGaucheDroite();
		} else if (mouvementGaucheDroite == "gauche") {
			tournerAGauche();
		} else if (mouvementGaucheDroite == "droite") {
			tournerADroite();
		}
	}
	
	if (mouvementHautBas != mouvementHautBasPrecedent) {
		mouvementHautBasPrecedent = mouvementHautBas;
		// Si le mouvement "Haut - Bas" est différent du précédent : envoi d'un évènement
		if (mouvementHautBas == "stop") {
			stopperTeteHautBas();
		} else if (mouvementHautBas == "haut") {
			tournerEnHaut();
		} else if (mouvementHautBas == "bas") {
			tournerEnBas();
		}
	}
}


function initialiserDropzone() {
	$("#conteneurImagesVisage").dropzone({
		url: "/rest/visage/images",
		paramName: "files",
		uploadMultiple: true,
		parallelUploads: 10,
		createImageThumbnails: false,
		previewsContainer: "div.dropzone-preview",
		acceptedFiles: "image/png",
		addedFile: function(file) { console.log(file); },
		successmultiple: chargerListeImagesVisage
	});
}

function traiterAudio(audioBytes) {
	var source = contexteAudio.createBufferSource();
	contexteAudio.decodeAudioData(audioBytes, function(buffer) {
		source.buffer = buffer;
	    source.connect(contexteAudio.destination); 
	    source.start(contexteAudio.currentTime);
	});
}

/**
 * Initialise les sliders des yeux.
 */
function initialiserSlidersYeux() {
	var sliderOeilGauche = $("#oeilGauche").slider({
		min: -45,
		max: 15,
		value: 0,
		step: 1,
		reversed : true,
		tooltip_position: "left",
		orientation: "vertical"
	});
	sliderOeilGauche.on("slide", function (slideEvt) {
		var mouvementYeuxEvent = {};
		mouvementYeuxEvent.eventType = "MouvementYeux";
		mouvementYeuxEvent.positionOeilGauche = slideEvt.value;
		wsRobotEvent.send(JSON.stringify(mouvementYeuxEvent));
		var sliderAttache = $("#sliderAttache")[0].checked;
		if (sliderAttache) {
			var mouvementYeuxEvent2 = {};
			mouvementYeuxEvent2.eventType = "MouvementYeux";
			mouvementYeuxEvent2.positionOeilDroit = slideEvt.value;
			wsRobotEvent.send(JSON.stringify(mouvementYeuxEvent2));
			$("#oeilDroit").slider("setValue", slideEvt.value);
		}
	});
	sliderOeilGauche.on("change", function (slideEvt) {
		var mouvementYeuxEvent = {};
		mouvementYeuxEvent.eventType = "MouvementYeux";
		mouvementYeuxEvent.positionOeilGauche = slideEvt.value.newValue;
		wsRobotEvent.send(JSON.stringify(mouvementYeuxEvent));
		var sliderAttache = $("#sliderAttache")[0].checked;
		if (sliderAttache) {
			var mouvementYeuxEvent2 = {};
			mouvementYeuxEvent2.eventType = "MouvementYeux";
			mouvementYeuxEvent2.positionOeilDroit = slideEvt.value.newValue;
			wsRobotEvent.send(JSON.stringify(mouvementYeuxEvent2));
			$("#oeilDroit").slider("setValue", slideEvt.value.newValue);
		}
	});
	var sliderOeilDroit = $("#oeilDroit").slider({
		min: -45,
		max: 15,
		value: 0,
		step: 1,
		reversed : true,
		tooltip_position: "right",
		orientation: "vertical"
	});
	sliderOeilDroit.on("slide", function (slideEvt) {
		var mouvementYeuxEvent = {};
		mouvementYeuxEvent.eventType = "MouvementYeux";
		mouvementYeuxEvent.positionOeilDroit = slideEvt.value;
		wsRobotEvent.send(JSON.stringify(mouvementYeuxEvent));
		var sliderAttache = $("#sliderAttache")[0].checked;
		if (sliderAttache) {
			var mouvementYeuxEvent2 = {};
			mouvementYeuxEvent2.eventType = "MouvementYeux";
			mouvementYeuxEvent2.positionOeilGauche = slideEvt.value;
			wsRobotEvent.send(JSON.stringify(mouvementYeuxEvent2));
			$("#oeilGauche").slider("setValue", slideEvt.value);
		}
	});
	sliderOeilDroit.on("change", function (slideEvt) {
		var mouvementYeuxEvent = {};
		mouvementYeuxEvent.eventType = "MouvementYeux";
		mouvementYeuxEvent.positionOeilDroit = slideEvt.value.newValue;
		wsRobotEvent.send(JSON.stringify(mouvementYeuxEvent));
		var sliderAttache = $("#sliderAttache")[0].checked;
		if (sliderAttache) {
			var mouvementYeuxEvent2 = {};
			mouvementYeuxEvent2.eventType = "MouvementYeux";
			mouvementYeuxEvent2.positionOeilGauche = slideEvt.value.newValue;
			wsRobotEvent.send(JSON.stringify(mouvementYeuxEvent2));
			$("#oeilGauche").slider("setValue", slideEvt.value.newValue);
		}
	});
}
	
//	.slider()
//		.on("slide", function (slideEvt) {
//			var mouvementYeuxEvent = {};
//			mouvementYeuxEvent.eventType = "MouvementYeux";
//			mouvementYeuxEvent.positionOeilGauche = slideEvt.value;
//			wsRobotEvent.send(JSON.stringify(mouvementYeuxEvent));
//		}
//		).data("slider");
//}
