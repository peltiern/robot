package fr.roboteek.robot.server;

import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

/**
 * Servlet interceptant les évènements de la websocket.
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */

@SuppressWarnings("serial")
public class ServoMotorEventServlet extends WebSocketServlet {
    
    public ServoMotorEventServlet() {
        super();
    }
    
    @Override
    public void configure(WebSocketServletFactory factory)
    {
        factory.getPolicy().setIdleTimeout(3600000);
        factory.register(ServoMotorEventSocket.class);
    }
}
