package fr.roboteek.robot.organes.actionneurs;

public enum RobotSound {

    OH("oh.ogg"),
    SAD("sad.ogg"),
    WALLE("walle.ogg"),
    WOW("wow.ogg");

    /**
     * Fichier associé.
     */
    private String fileName;

    RobotSound(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }
}
