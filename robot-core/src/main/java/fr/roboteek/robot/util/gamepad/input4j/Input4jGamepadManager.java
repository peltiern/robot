package fr.roboteek.robot.util.gamepad.input4j;

import de.gurkenlabs.input4j.ControllerType;
import de.gurkenlabs.input4j.InputDevice;
import de.gurkenlabs.input4j.InputDevicePlugin;
import de.gurkenlabs.input4j.InputDevices;
import fr.roboteek.robot.util.gamepad.shared.AbstractGamepadManager;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.Optional;

/**
 * Découverte et polling de la manette Logitech F710 via input4j (mode XInput).
 * Remplace l'ancien {@code GamepadManager} jinput (natifs + hack librarypath).
 * <p>
 * Sélectionne la première manette de type XBOX (la F710 en mode X est vue comme une
 * manette Xbox par le noyau) ou dont le nom évoque une F710, y branche le
 * {@link LogitechControllerInput4j} et lance une boucle de polling à ~60 Hz dans un
 * thread virtuel. Les callbacks input4j se déclenchent pendant {@code poll()}.
 */
public class Input4jGamepadManager extends AbstractGamepadManager<LogitechListener> {

    private static final Logger logger = LoggerFactory.getLogger(Input4jGamepadManager.class);

    private static final long POLL_INTERVAL_MS = 16;

    private InputDevicePlugin plugin;
    private Thread pollThread;
    private volatile boolean running = false;

    @Override
    public void start() {
        try {
            plugin = InputDevices.init();
        } catch (Exception e) {
            logger.error("Impossible d'initialiser input4j (manette désactivée)", e);
            return;
        }

        Optional<InputDevice> device = trouverManette();
        if (device.isEmpty()) {
            logger.warn("Aucune manette compatible détectée (input4j)");
            return;
        }

        InputDevice manette = device.get();
        logger.info("Manette détectée : {} (type {})", manette.getDisplayName(), manette.getControllerType());
        manette.getBatteryInfo()
                .filter(b -> b.isAvailable() && b.getPercentage() >= 0)
                .ifPresent(b -> logger.info("Batterie manette : {}%", b.getPercentage()));

        LogitechControllerInput4j controller = new LogitechControllerInput4j();
        listeners.forEach(controller::addListener);
        controller.register(manette);

        running = true;
        pollThread = Thread.ofVirtual().name("GamepadPolling").start(() -> {
            while (running && !Thread.currentThread().isInterrupted()) {
                try {
                    manette.poll();
                    Thread.sleep(POLL_INTERVAL_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    logger.warn("Erreur de polling manette : {}", e.getMessage());
                }
            }
        });
    }

    public void stop() {
        running = false;
        if (pollThread != null) {
            pollThread.interrupt();
        }
        if (plugin != null) {
            try {
                plugin.close();
            } catch (Exception e) {
                logger.warn("Erreur à la fermeture d'input4j : {}", e.getMessage());
            }
        }
    }

    private Optional<InputDevice> trouverManette() {
        var devices = plugin.getAll();
        // Log en INFO pour pouvoir diagnostiquer si la manette n'est pas reconnue
        // (comment le noyau nomme la F710 en mode XInput varie).
        devices.forEach(d -> logger.info("Périphérique d'entrée détecté : {} / {} (type {})", d.getName(), d.getProductName(), d.getControllerType()));
        return devices.stream().filter(this::estManetteCompatible).findFirst();
    }

    private boolean estManetteCompatible(InputDevice device) {
        if (device.getControllerType() == ControllerType.XBOX) {
            return true;
        }
        String nom = (StringUtils.defaultString(device.getName()) + " " + StringUtils.defaultString(device.getProductName()))
                .toLowerCase(Locale.ROOT);
        return nom.contains("f710") || nom.contains("logitech") || nom.contains("x-box") || nom.contains("xbox") || nom.contains("360");
    }
}
