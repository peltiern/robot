package fr.roboteek.robot.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.ssl.SslContextFactory;

/**
 * Serveur permettant les interactions avec la Web Speech API lancée dans le navigateur Chrome.
 * @author Java Developer
 */
public class WebSpeechServer {
    
    /** Instance de la classe (singleton). */
    private static WebSpeechServer instance;
    
    private Server server;
    
    /** Liste des listeners. */
    private List<WebSpeechServerListener> listeListeners = new ArrayList<WebSpeechServerListener>();
    
    /** Websocket spécifique à la communication avec la page HTML. */
    private WebSpeechEventSocket webSpeechEventSocket;

    /**
     * Constructeur privé.
     */
    private WebSpeechServer() {
        server = new Server();

        ServerConnector connector = new ServerConnector(server);
        connector.setPort(8080);

        HttpConfiguration https = new HttpConfiguration();
        https.addCustomizer(new SecureRequestCustomizer());

        SslContextFactory sslContextFactory = new SslContextFactory();
        sslContextFactory.setKeyStorePath(System.getProperty("robot.dir") + "/keystore2/keystore");
        sslContextFactory.setKeyStorePassword("123456");
        sslContextFactory.setKeyManagerPassword("123456");

        ServerConnector sslConnector = new ServerConnector(server,
            new SslConnectionFactory(sslContextFactory, "http/1.1"),
            new HttpConnectionFactory(https));
        sslConnector.setPort(8081);

        server.setConnectors(new Connector[] { connector, sslConnector });


        ResourceHandler resourceHandler = new ResourceHandler();
        // Configure the ResourceHandler. Setting the resource base indicates where the files should be served out of.
        // In this example it is the current directory but it can be configured to anything that the jvm has access to.
        resourceHandler.setDirectoriesListed(true);
        resourceHandler.setWelcomeFiles(new String[]{ "speech.html" });
        resourceHandler.setResourceBase(System.getProperty("robot.dir") + "/reco-synthese-vocale/client");

        // Setup the basic application "context" for this application at "/"
        // This is also known as the handler tree (in jetty speak)
        ServletContextHandler servletcontextHandler = new ServletContextHandler(ServletContextHandler.SESSIONS);
        servletcontextHandler.setContextPath("/");
        server.setHandler(servletcontextHandler);

        // Add a websocket to a specific path spec
        ServletHolder holderEvents = new ServletHolder("speech-events", WebSpeechEventServlet.class);
        servletcontextHandler.addServlet(holderEvents, "/events/*");
        
        ServletHolder holderServoMotorEvents = new ServletHolder("servo-motor-events", ServoMotorEventServlet.class);
        servletcontextHandler.addServlet(holderServoMotorEvents, "/sm-events/*");

        // Add the ResourceHandler to the server.
        HandlerList handlers = new HandlerList();
        handlers.setHandlers(new Handler[] { resourceHandler, servletcontextHandler, new DefaultHandler() });
        server.setHandler(handlers);

    }
    
    /**
     * Récupère l'instance de la classe (le singleton).
     * @return l'instance de la classe
     */
    public synchronized static WebSpeechServer getInstance() {
        if (instance == null) {
            instance = new WebSpeechServer();
        }
        return instance;
    }
    
    public void initialiser() {
        try {
            server.start();
//            server.join();
            try {
                Runtime.getRuntime().exec("google-chrome  https://localhost:8081/speech.html");
                
              System.out.println("En attente du démarrage du serveur vocal ...");
              // On bloque tant que le serveur n'est pas démarré
              while (webSpeechEventSocket == null) {
                  Thread.sleep(50);
              }
              System.out.println("Serveur vocal démarré");
                
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        
    }
    
    public synchronized void setWebSpeechEventSocket(WebSpeechEventSocket webSpeechEventSocket) {
        this.webSpeechEventSocket = webSpeechEventSocket;
    }
    
    /**
     * Réception d'un résultat de la socket. L'ensemble des listeners sont avertis.
     * @param message le message reçu de la socket
     */
    public void onSpeechResult(String speechResult) {
        if (listeListeners != null && !listeListeners.isEmpty()) {
            for (WebSpeechServerListener listener : listeListeners) {
                listener.onSpeechResult(speechResult);
            }
        }
    }
    
    /**
     * Ajoute un listener.
     * @param listener le listener à ajouter
     */
    public void addListener(WebSpeechServerListener listener) {
        listeListeners.add(listener);
    }
    
    /**
     * Lit un message en utilisant la synthèse vocale de Web Speech API.
     * @param message le message à lire
     */
    public void lire(String message) {
        try {
            if (webSpeechEventSocket != null && webSpeechEventSocket.isConnected()) {
                webSpeechEventSocket.getRemote().sendString(message);
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    

    public static void main(String[] args) {
        System.setProperty("robot.dir", "/home/npeltier/Robot/Programme");
        final WebSpeechServer webSpeechServer = WebSpeechServer.getInstance();
        webSpeechServer.initialiser();
        webSpeechServer.lire("Je suis une voix de synthèse en cours d'expérimentation. Arrives-tu à me comprendre ?");
        
     // Création du chat
//        String botname="amy";
//        String path=System.getProperty("robot.dir");
//        Bot bot = new Bot(botname, path);
//        final Chat chat = new Chat(bot);
//        
        WebSpeechServer.getInstance().addListener(new WebSpeechServerListener() {
            
            public void onSpeechResult(String speechResult) {
//                WebSpeechServer.getInstance().lire("Bonjour tout le monde ! Comment ça va ?");
//                final String reponse = chat.multisentenceRespond(speechResult);
                webSpeechServer.lire("Je suis une voix de synthèse en cours d'expérimentation. Arrives-tu à me comprendre ?");
//                WebSpeechServer.getInstance().lire(speechResult);
            }
        });
    }
    
   

}
