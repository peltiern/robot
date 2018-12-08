package fr.roboteek.robot.server;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.servlet.ServletException;
import javax.websocket.DeploymentException;
import javax.websocket.server.ServerContainer;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import fr.roboteek.robot.organes.actionneurs.Cou;
import fr.roboteek.robot.server.test.CapteurVisionWebSocket;
import fr.roboteek.robot.systemenerveux.event.RobotEventBus;

public class RobotServer {
	
	/** Instance de la classe (singleton). */
    private static RobotServer instance;
    
    private RobotServer() {
    	
    }
    
    /**
     * Récupère l'instance de la classe (le singleton).
     * @return l'instance de la classe
     */
    public synchronized static RobotServer getInstance() {
        if (instance == null) {
            instance = new RobotServer();
        }
        return instance;
    }

    public void run() {
    	try {
        Server server = new Server(8080);

        URL webRootLocation = this.getClass().getResource("/webContent/index.html");
        if (webRootLocation == null)
        {
            throw new IllegalStateException("Unable to determine webroot URL location");
        }

        URI webRootUri = URI.create(webRootLocation.toURI().toASCIIString().replaceFirst("/index.html$","/"));
        System.err.printf("Web Root URI: %s%n",webRootUri);

        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");
        context.setBaseResource(Resource.newResource(webRootUri));
        context.setWelcomeFiles(new String[] { "index.html" });

        context.getMimeTypes().addMimeMapping("txt","text/plain;charset=utf-8");
        context.getMimeTypes().addMimeMapping("eot","application/vnd.ms-fontobject");
        context.getMimeTypes().addMimeMapping("svg","image/svg+xml");
        context.getMimeTypes().addMimeMapping("ttf","application/octet-stream");
        context.getMimeTypes().addMimeMapping("woff","application/font-woff");
        context.getMimeTypes().addMimeMapping("woff2","application/font-woff2");
        System.out.println("Type Mime = " + context.getMimeTypes().getMimeMap());
        
//        server.setHandler(context);
        context.addServlet(DefaultServlet.class,"/");
//        ServletHolder holderPwd = new ServletHolder("default", DefaultServlet.class);
//        holderPwd.setInitParameter("resourceBase","/webContent/");
//        holderPwd.setInitParameter("dirAllowed","true");
//        holderPwd.
//        context.addServlet(holderPwd,"/");
        
        final ServletContextHandler webSocketServletContext = getWebSocketContext();
        final ContextHandlerCollection contexts = new ContextHandlerCollection();
        contexts.setHandlers(new Handler[]{
        		context,
        		webSocketServletContext,
        		getRestServletContext()
        });
        server.setHandler(contexts);
        initializeWebSocketServlet(webSocketServletContext);
        
        // Start Server
        server.start();
        server.join();
        
    	} catch (URISyntaxException e) {
    		e.printStackTrace();
    	} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ServletException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DeploymentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    
    private static void initializeWebSocketServlet(final ServletContextHandler webSocketServletContext)
            throws DeploymentException, ServletException {
        final ServerContainer webSocketContainer = WebSocketServerContainerInitializer.configureContext(webSocketServletContext);
        // Garde la session ouverte
        webSocketContainer.setDefaultMaxSessionIdleTimeout(0);
        webSocketContainer.addEndpoint(TimeSocket.class);
        webSocketContainer.addEndpoint(VideoWebSocket.class);
        webSocketContainer.addEndpoint(AudioWebSocket.class);
        webSocketContainer.addEndpoint(RobotEventWebSocket.class);
    }

    private static ServletContextHandler getWebSocketContext() {
        final ServletContextHandler webSocketServletContext = new ServletContextHandler(ServletContextHandler.SESSIONS);
        webSocketServletContext.setContextPath("/ws");
        return webSocketServletContext;
    }

    private static ServletContextHandler getRestServletContext() {
        final ServletContextHandler restServletContext = new ServletContextHandler();
        restServletContext.setContextPath("/rest");
        restServletContext.addServlet(getRestServlet(), "/*");
        return restServletContext;
    }

    private static ServletHolder getRestServlet() {
    	ResourceConfig config = new ResourceConfig();
        config.packages("fr.roboteek.robot.server.service");
        config.register(MultiPartFeature.class);
        ServletHolder servlet = new ServletHolder(new ServletContainer(config));
        return servlet;
    }

    
    
    
    public static void main(String[] args) throws Throwable
    {
        try
        {
        	// Démarrage du streaming video
            CapteurVisionWebSocket capteur = new CapteurVisionWebSocket();
            capteur.initialiser();
            
            // Tete
            Cou cou = new Cou();
            cou.initialiser();
            RobotEventBus.getInstance().subscribe(cou);
            
            new RobotServer().run();
        }
        catch (Throwable t)
        {
            t.printStackTrace();
        }
    }
}
