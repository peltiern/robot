package fr.roboteek.robot.sandbox.controller.ps3.jinput;

import fr.roboteek.robot.sandbox.controller.ps3.shared.AbstractGamepadManager;
import fr.roboteek.robot.sandbox.controller.ps3.shared.GamepadListener;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEnvironment;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

public class GamepadManager extends AbstractGamepadManager<PS3Listener> {

    private volatile boolean running = false;

    private Map<String, GamepadController> connectedControllers = new HashMap<>();

    // TODO Liste de controller autorisé (spécifier un identifiant reconnaissable dans le Controller)

    List<GamepadController> listeAuthorizedGamepadDummies = new ArrayList<>();

    public GamepadManager(Class<? extends GamepadController> ... authorizedClasses) {
        setAuthorizedGamepads(authorizedClasses);
    }

    @Override
    public void start() {
        Controller[] controllers = ControllerEnvironment.getDefaultEnvironment().getControllers();
        Arrays.stream(controllers).forEach(this::addGamepad);
    }

    public  synchronized void addGamepad(Controller controller) {
        System.out.println("TENTATIVE AJOUT GAMEPAD : " + controller.getType() + ", " + controller.getName());
        if (controller != null) {
//            // On n'autorise qu'une seule manette Playstation pour le projet
//            if (connectedControllers.isEmpty()) {
                if (controller.getType() == Controller.Type.GAMEPAD) {
                    // Le gamepad doit faire partie des gamepads autorisés
                    listeAuthorizedGamepadDummies.forEach(gamepadController -> System.out.println("DUMMY = " + gamepadController.getType()));
                    Optional<GamepadController> authorizedGamepad = listeAuthorizedGamepadDummies.stream().filter(gamepadController -> gamepadController.getType().toUpperCase().equals(controller.getName().toUpperCase())).findFirst();
                    authorizedGamepad.ifPresent(gamepadControllerDummy -> {
                        String identifier = controller.getName();
                        if (!connectedControllers.containsKey(identifier)) {
                            try {
                            System.out.println("AJOUT GAMEPAD AUTORISE : " + controller.getName());
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

    public synchronized void removeGamepad(Controller controller) {
        String identifier = controller.getName();
        if (connectedControllers.containsKey(identifier)) {
            System.out.println("Déconnexion du gamepad " + identifier);
            connectedControllers.remove(identifier);
        }
    }

//    @Override
//    public void run() {
//        running = true;
//        while (running) {
////            // Vérifie la déconnexion de gamepads
////            List<Controller> deconnectedGamepads = new ArrayList<>();
////            connectedControllers.values().stream().filter(gamepadController -> !gamepadController.isConnected()).map(GamepadController::getController).forEach(deconnectedGamepads::add);
////            deconnectedGamepads.forEach(this::removeGamepad);
////
////            // Vérifie la connexion de nouveaux gamepads
////            Controller[] controllers = ControllerEnvironment.getDefaultEnvironment().getControllers();
////            Arrays.stream(controllers).forEach(this::addGamepad);
//
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
//        }
//    }

    private void setAuthorizedGamepads(Class<? extends GamepadController> ... authorizedClasses) {
        if (authorizedClasses != null && authorizedClasses.length > 0) {
            listeAuthorizedGamepadDummies.clear();
            Arrays.stream(authorizedClasses).forEach(aClass -> {
                try {
                    GamepadController gamepadController = aClass.getConstructor(Controller.class).newInstance((Controller) null);
                    listeAuthorizedGamepadDummies.add(gamepadController);
                    System.out.println("TAILLE DUMMIES = " + listeAuthorizedGamepadDummies.size());
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

    public static void main(String[] args) {
        GamepadManager gamepadManager = new GamepadManager();
        gamepadManager.start();
    }
}
