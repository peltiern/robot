package fr.roboteek.robot.sandbox.controller.ps3.jinput;

import fr.roboteek.robot.sandbox.controller.ps3.shared.GamepadEvent;
import fr.roboteek.robot.sandbox.controller.ps3.shared.GamepadListener;
import net.java.games.input.Controller;
import net.java.games.input.Event;
import net.java.games.input.EventQueue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class GamepadController <E extends GamepadEvent, L extends GamepadListener<E>> implements Runnable {

    private Controller controller;

    private String type;

    private volatile boolean connected = true;

    private List<L> listeners = new ArrayList<>();

    public GamepadController(Controller controller, String type) {
        this.controller = controller;
        this.type = type;
    }

    @Override
    public void run() {
        connected = true;
        while (connected) {
            /* Remember to poll each one */
            if (!controller.poll()) {
                connected = false;
                // TODO gérer l'envoi d'un évènement de déconnexion
                System.out.println("POLL OFF " + controller.getName());
                break;
            }

            /* Get the controllers event queue */
            EventQueue queue = controller.getEventQueue();

            /* Create an event object for the underlying plugin to populate */
            Event event = new Event();

            /* For each object in the queue */
            while (queue.getNextEvent(event)) {

                Optional<E> gamepadEvent = this.mapEvent(event);

                gamepadEvent.ifPresent(e -> listeners.forEach(l -> l.onEvent(e)));

            }

            /*
             * Sleep for 20 milliseconds, in here only so the example doesn't
             * thrash the system.
             */
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public abstract Optional<E> mapEvent(Event event);

    public String getType() {
        return type;
    }

    public void addListener(L listener) {
        listeners.add(listener);
    }

    public abstract Class forListener();
}
