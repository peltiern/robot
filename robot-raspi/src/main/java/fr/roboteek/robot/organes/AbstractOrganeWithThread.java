package fr.roboteek.robot.organes;

/**
 * Classe abstraite représentant un organe fonctionnant avec un thread.
 *
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
public abstract class AbstractOrganeWithThread extends AbstractOrgane {

    protected Thread thread;

    /**
     * Constructeur.
     */
    public AbstractOrganeWithThread(String threadName) {
        thread = new Thread(this::loop, threadName);
    }

    public void start() {
        thread.start();
    }

    public abstract void loop();

    @Override
    public void arreter() {
        thread.interrupt();
    }
}
