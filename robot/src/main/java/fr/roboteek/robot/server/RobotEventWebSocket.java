//
//  ========================================================================
//  Copyright (c) 1995-2015 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package fr.roboteek.robot.server;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import fr.roboteek.robot.server.event.RobotEventDecoder;
import fr.roboteek.robot.systemenerveux.event.RobotEvent;
import fr.roboteek.robot.systemenerveux.event.RobotEventBus;

@ServerEndpoint(value = "/robotEvents/",
		decoders = {RobotEventDecoder.class}
)
public class RobotEventWebSocket {
    private static Set<Session> clients = Collections.synchronizedSet(new HashSet<Session>());

    @OnOpen
    public void onOpen(Session session)
    {
        clients.add(session);
        System.out.println(this +  " : = Nouvelle session : " + session.getUserProperties());
    }
    
    @OnMessage
    public void onMessage(RobotEvent event)
    {
    	RobotEventBus.getInstance().publish(event);
    }

    @OnClose
    public void onClose(Session session)
    {
    	System.out.println(this +  " : = Fermeture session : " + session.getUserProperties());
    	clients.remove(session);
    }

	public static Set<Session> getClients() {
		return clients;
	}
}
