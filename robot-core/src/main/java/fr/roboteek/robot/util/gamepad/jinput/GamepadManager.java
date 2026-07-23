package fr.roboteek.robot.util.gamepad.jinput;

import fr.roboteek.robot.util.gamepad.shared.AbstractGamepadManager;
import fr.roboteek.robot.util.gamepad.shared.GamepadListener;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

public class GamepadManager extends AbstractGamepadManager<LogitechListener> {

    private static final Logger logger = LoggerFactory.getLogger(GamepadManager.class);

    List<GamepadController> listeAuthorizedGamepadDummies = new ArrayList<>();
    private volatile boolean running = false;

    // TODO Liste de controller autorisé (spécifier un identifiant reconnaissable dans le Controller)
    private Map<String, GamepadController> connectedControllers = new HashMap<>();

    public GamepadManager(Class<? extends GamepadController>... authorizedClasses) {
        setAuthorizedGamepads(authorizedClasses);
    }

    public static void main(String[] args) {
        GamepadManager gamepadManager = new GamepadManager();
        gamepadManager.start();
    }

    @Override
    public void start() {
        Controller[] controllers = ControllerEnvironment.getDefaultEnvironment().getControllers();
        Arrays.stream(controllers).forEach(this::addGamepad);
    }

    public synchronized void addGamepad(Controller controller) {
        if (controller != null) {
            logger.debug("Tentative d'ajout d'un gamepad : {}, {}", controller.getType(), controller.getName());
//            // On n'autorise qu'une seule manette Playstation pour le projet
//            if (connectedControllers.isEmpty()) {
            if (controller.getType() == Controller.Type.GAMEPAD) {
                // Le gamepad doit faire partie des gamepads autorisés
                Optional<GamepadController> authorizedGamepad = listeAuthorizedGamepadDummies.stream().filter(gamepadController -> gamepadController.getType().toUpperCase().equals(controller.getName().toUpperCase())).findFirst();
                authorizedGamepad.ifPresent(gamepadControllerDummy -> {
                    String identifier = controller.getName();
                    if (!connectedControllers.containsKey(identifier)) {
                        try {
                            logger.info("Ajout du gamepad autorisé : {}", controller.getName());
                            // TODO gérer la liste des gamepads autorisés
                            GamepadController gamepadController = gamepadControllerDummy.getClass().getConstructor(Controller.class).newInstance(controller);

                            List<GamepadListener> filteredListeners = listeners.stream()
                                    .filter(gamepadListener -> gamepadController.forListener().isInstance(gamepadListener))
                                    .collect(Collectors.toList());
                            filteredListeners.forEach(gamepadListener -> gamepadController.addListener(gamepadListener));
                            connectedControllers.put(identifier, gamepadController);
                            new Thread(gamepadController).start();
                        } catch (InstantiationException e) {
                            e.printStackTrace();
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        } catch (InvocationTargetException e) {
                            e.printStackTrace();
                        } catch (NoSuchMethodException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
//            }
        }
    }

//    @Override
//    public void run() {
//        running = true;
//        while (running) {

    public synchronized void removeGamepad(Controller controller) {
        String identifier = controller.getName();
        if (connectedControllers.containsKey(identifier)) {
            logger.info("Déconnexion du gamepad {}", identifier);
            connectedControllers.remove(identifier);
        }
    }

    /// /            // Vérifie la déconnexion de gamepads
    /// /            List<Controller> deconnectedGamepads = new ArrayList<>();
    /// /            connectedControllers.values().stream().filter(gamepadController -> !gamepadController.isConnected()).map(GamepadController::getController).forEach(deconnectedGamepads::add);
    /// /            deconnectedGamepads.forEach(this::removeGamepad);
    /// /
    /// /            // Vérifie la connexion de nouveaux gamepads
    /// /            Controller[] controllers = ControllerEnvironment.getDefaultEnvironment().getControllers();
    /// /            Arrays.stream(controllers).forEach(this::addGamepad);
//
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
//        }
//    }
    private void setAuthorizedGamepads(Class<? extends GamepadController>... authorizedClasses) {
        if (authorizedClasses != null && authorizedClasses.length > 0) {
            listeAuthorizedGamepadDummies.clear();
            Arrays.stream(authorizedClasses).forEach(aClass -> {
                try {
                    GamepadController gamepadController = aClass.getConstructor(Controller.class).newInstance((Controller) null);
                    listeAuthorizedGamepadDummies.add(gamepadController);
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            });
        }


    }
}
