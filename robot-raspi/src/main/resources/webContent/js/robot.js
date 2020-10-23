$(document).ready(function() {
	moment.locale("fr");
	//loadAnimations();
	//chargerListeAnimations();
	//chargerListeImagesVisage();
	initialiserJoystick();
	initialiserSlidersCou();
	initialiserSlidersYeux();
	initialiserDropzone();
});

// Création du contexte audio
window.AudioContext = window.AudioContext||window.webkitAudioContext;
contexteAudio = new AudioContext();

// Websocket pour la vidéo
var wsVideo = new WebSocket("ws://" + location.hostname + ":" + location.port + "/ws/liveVideo/");
wsVideo.onmessage = function(msg) {
    if (!$("#pauseVideo").is(':checked')) {
        var target = document.getElementById("liveVideo");
        var msgJson = JSON.parse(msg.data);
    //	url = window.URL.createObjectURL(msg.data);
    //    target.onload = function() {
    //        window.URL.revokeObjectURL(url);
    //    };
    //    target.src = url;
        target.src = "data:image/jpeg;base64, " + msgJson.imageBase64;
        // On vide le conteneur des visages
        $("#divFaces").empty();
        $("#svgLandmarks").empty();
        $("#divObjects").empty();
        // Chargement des visages
        var showFaces = $("#showFaces").is(':checked');
        var showLandmarks = $("#showLandmarks").is(':checked');
//        if (showFaces || showLandmarks) {
            $.each(msgJson.faces, function( index, face ) {
                // Chargement des repères
//                if (showLandmarks) {
                    // Left eye
                    var pointsLeftEye = "";
                    $.each(face.landmarks.left_eye, function( index, point ) {
                        if (index != 0) {
                            pointsLeftEye += ", ";
                        }
                        pointsLeftEye += point.x + " " + point.y;
                    });
                    if (pointsLeftEye != "") {
                        var polyline = makeSVGElement('polygon', { 'points': '' + pointsLeftEye + '',
                                                                'stroke': 'red',
                                                               'stroke-width': 3,
                                                                'fill': 'transparent' });
                        $("#svgLandmarks").append(polyline);
                    }
                    // Right eye
                    var pointsRightEye = "";
                    $.each(face.landmarks.right_eye, function( index, point ) {
                        if (index != 0) {
                            pointsRightEye += ", ";
                        }
                        pointsRightEye += point.x + " " + point.y;
                    });
                    if (pointsRightEye != "") {
                        var polyline = makeSVGElement('polygon', { 'points': '' + pointsRightEye + '',
                                                                'stroke': 'red',
                                                               'stroke-width': 3,
                                                                'fill': 'transparent' });
                        $("#svgLandmarks").append(polyline);
                    }

                    // Top lip
                    var pointsTopLip = "";
                    $.each(face.landmarks.top_lip, function( index, point ) {
                        if (index != 0) {
                            pointsTopLip += ", ";
                        }
                        pointsTopLip += point.x + " " + point.y;
                    });
                    if (pointsTopLip != "") {
                        var polyline = makeSVGElement('polyline', { 'points': '' + pointsTopLip + '',
                                                                'stroke': 'blue',
                                                               'stroke-width': 3,
                                                                'fill': 'transparent' });
                        $("#svgLandmarks").append(polyline);
                    }

                    // Bottom lip
                    var pointsBottomLip = "";
                    $.each(face.landmarks.bottom_lip, function( index, point ) {
                        if (index != 0) {
                            pointsBottomLip += ", ";
                        }
                        pointsBottomLip += point.x + " " + point.y;
                    });
                    if (pointsBottomLip != "") {
                        var polyline = makeSVGElement('polyline', { 'points': '' + pointsBottomLip + '',
                                                                'stroke': 'blue',
                                                               'stroke-width': 3,
                                                                'fill': 'transparent' });
                        $("#svgLandmarks").append(polyline);
                    }

                    // Nose bridge
                    var pointsNoseBridge = "";
                    $.each(face.landmarks.nose_bridge, function( index, point ) {
                        if (index != 0) {
                            pointsNoseBridge += ", ";
                        }
                        pointsNoseBridge += point.x + " " + point.y;
                    });
                    if (pointsNoseBridge != "") {
                        var polyline = makeSVGElement('polyline', { 'points': '' + pointsNoseBridge + '',
                                                                'stroke': 'yellow',
                                                               'stroke-width': 3,
                                                                'fill': 'transparent' });
                        $("#svgLandmarks").append(polyline);
                    }

                    // Nose bridge
                    var pointsNoseTip = "";
                    $.each(face.landmarks.nose_tip, function( index, point ) {
                        if (index != 0) {
                            pointsNoseTip += ", ";
                        }
                        pointsNoseTip += point.x + " " + point.y;
                    });
                    if (pointsNoseTip != "") {
                        var polyline = makeSVGElement('polyline', { 'points': '' + pointsNoseTip + '',
                                                                'stroke': 'yellow',
                                                               'stroke-width': 3,
                                                                'fill': 'transparent' });
                        $("#svgLandmarks").append(polyline);
                    }
//                }
//                if (showFaces) {
                    // Ajout de l'image
                    $("#divFaces").append(
                        "<div class='face'>"
                            + "<div class='face_bounds' style='left: " + (face.x + 5) + "px; top: " + (face.y + 5) + "px; width: " + face.width + "px; height: " + face.height + "px;'/>"
                            + "<div class='face_label' style='left: " + (face.x + 5) + "px; top: " + (face.y + face.height) + "px; min-width: " + face.width + "px; height: 25px;'>"
                                + "<span>" + face.name + "</span>"
                            + "</div>"
                        + "</div>"
                    );
//                }
            });

            $.each(msgJson.objects, function( index, object ) {
                $("#divObjects").append(
                    "<div class='object'>"
                        + "<div class='object_bounds' style='left: " + (object.x + 5) + "px; top: " + (object.y + 5) + "px; width: " + object.width + "px; height: " + object.height + "px;'/>"
                        + "<div class='object_label' style='left: " + (object.x + 5) + "px; top: " + (object.y + object.height) + "px; min-width: " + object.width + "px; height: 25px;'>"
                            + "<span>" + object.name + "</span>"
                        + "</div>"
                    + "</div>"
                );
            });
//        }
    }
}
$("#showFaces").change(function () {
    var checked = $(this).is(':checked');
    if (checked) {
        $("#divFaces").show();
    } else {
        $("#divFaces").hide();
    }
});
$("#showLandmarks").change(function () {
    var checked = $(this).is(':checked');
    if (checked) {
        $("#svgLandmarks").show();
    } else {
        $("#svgLandmarks").hide();
    }
});

$("#showObjects").change(function () {
    var checked = $(this).is(':checked');
    if (checked) {
        $("#divObjects").show();
    } else {
        $("#divObjects").hide();
    }
});

function makeSVGElement(tag, attrs) {
    var el= document.createElementNS('http://www.w3.org/2000/svg', tag);
    for (var k in attrs) {
        el.setAttribute(k, attrs[k]);
    }
    return el;
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
Mousetrap.bind("i", displayPosition);

function displayPosition() {
	var displayPositionEvent = {};
	displayPositionEvent.eventType = "DisplayPosition";
	wsRobotEvent.send(JSON.stringify(displayPositionEvent));
}
 
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

/**
 * Initialise le joystick de contrôle de la tête.
 */
function initialiserJoystick() {
//	var joystick = nipplejs.create({
//        zone: document.getElementById("divVideo"),
//        color: "blue",
//        threshold: 0.7
//    });
//	joystick.on('end move', function(evt, data) {
//		traiterJoystickEvent(evt, data);
//	});
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
		min: -24,
		max: 20,
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
		min: -24,
		max: 20,
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
	var sliderRoulis = $("#roulis").slider({
		min: -20,
		max: 20,
		value: 0,
		step: 1,
		reversed : false
	});
//	sliderRoulis.on("slide", function (slideEvt) {
//		var mouvementCouEvent = {};
//		mouvementCouEvent.eventType = "MouvementCou";
//		mouvementCouEvent.mouvementRoulis = "HORAIRE";
//		mouvementCouEvent.positionRoulis = slideEvt.value;
//		wsRobotEvent.send(JSON.stringify(mouvementCouEvent));
//	});
	sliderRoulis.on("change", function (slideEvt) {
		var mouvementCouEvent = {};
		mouvementCouEvent.eventType = "MouvementCou";
		mouvementCouEvent.mouvementRoulis = "HORAIRE";
		mouvementCouEvent.positionRoulis = slideEvt.value.newValue;
		wsRobotEvent.send(JSON.stringify(mouvementCouEvent));
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

/**
 * Initialise les sliders du cou.
 */
function initialiserSlidersCou() {
	var sliderCouHautBas = $("#couHautBas").slider({
		min: -35,
		max: 35,
		value: 0,
		step: 1,
		reversed : true,
		tooltip_position: "left",
		orientation: "vertical"
	});
	sliderCouHautBas.on("slide", function (slideEvt) {
		var mouvementCouEvent = {};
		mouvementCouEvent.eventType = "MouvementCou";
		mouvementCouEvent.positionHautBas = slideEvt.value;
		wsRobotEvent.send(JSON.stringify(mouvementCouEvent));
	});
	sliderCouHautBas.on("change", function (slideEvt) {
		var mouvementCouEvent = {};
		mouvementCouEvent.eventType = "MouvementCou";
		mouvementCouEvent.positionHautBas = slideEvt.value.newValue;
		wsRobotEvent.send(JSON.stringify(mouvementCouEvent));
	});
	var sliderCouGaucheDroite = $("#couGaucheDroite").slider({
        min: -65,
        max: 65,
        value: 0,
        step: 1,
        reversed : false,
        tooltip_position: "right",
        orientation: "horizontal"
    });
    sliderCouGaucheDroite.on("slide", function (slideEvt) {
        var mouvementCouEvent = {};
        mouvementCouEvent.eventType = "MouvementCou";
        mouvementCouEvent.positionGaucheDroite = -slideEvt.value;
        wsRobotEvent.send(JSON.stringify(mouvementCouEvent));
    });
    sliderCouGaucheDroite.on("change", function (slideEvt) {
        var mouvementCouEvent = {};
        mouvementCouEvent.eventType = "MouvementCou";
        mouvementCouEvent.positionGaucheDroite = -slideEvt.value.newValue;
        wsRobotEvent.send(JSON.stringify(mouvementCouEvent));
    });
}
