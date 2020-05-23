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

import fr.roboteek.robot.server.event.ImageWithRecognizedFacesCodec;

import javax.websocket.EncodeException;
import javax.websocket.OnClose;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@ServerEndpoint(value = "/liveVideo/",
        encoders = {ImageWithRecognizedFacesCodec.class}
)
public class VideoWebSocket {
    private static Set<Session> clients = Collections.synchronizedSet(new HashSet<Session>());

    @OnOpen
    public void onOpen(Session session) {
        session.setMaxBinaryMessageBufferSize(1024 * 512);
        clients.add(session);
        System.out.println(this + " : = Nouvelle session : " + session.getUserProperties());
    }

    @OnClose
    public void onClose(Session session) {
        System.out.println(this + " : = Fermeture session : " + session.getUserProperties());
        clients.remove(session);
    }

    public static Set<Session> getClients() {
        return clients;
    }

    public static synchronized void broadcastImage(byte[] image) {
        for (Session session : getClients()) {
            try {
                ByteBuffer buf = ByteBuffer.wrap(image);
                session.getBasicRemote().sendBinary(buf);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public static synchronized void broadcastImageWithDetectionInfos(ImageWithDetectedObjects imageWithDetectedObjects) {
        for (Session session : getClients()) {
            try {
                session.getBasicRemote().sendObject(imageWithDetectedObjects);
            } catch (EncodeException | IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
