package fr.roboteek.robot.organes.actionneurs.transmission;

/**
 * Transmission à rapport constant, celle d'un engrenage ou d'une prise directe.
 * <p>
 * Elle couvre les cinq axes du robot tant que la loi exacte des yeux n'est pas branchée, et elle
 * restera celle du cou : un couple de pignons a un rapport constant. Le paramètre porte aussi le
 * <b>signe</b> du montage — quatre des cinq axes sont inversés, et c'est ce que
 * {@code robot.regard.*.sens.inverse} compensait sans le nommer.
 *
 * @param zeroMoteur           position moteur où l'organe est à 0
 * @param degresParUniteMoteur degrés d'organe pour une unité de position moteur, signé
 */
record TransmissionAffine(double zeroMoteur, double degresParUniteMoteur) implements Transmission {

    TransmissionAffine {
        // Un rapport nul rendrait versMoteur infini et depuisMoteur constant : la réciprocité
        // serait perdue en silence, et l'axe irait en butée sans qu'aucune trace ne l'explique.
        if (degresParUniteMoteur == 0) {
            throw new IllegalArgumentException("Rapport de transmission nul : l'organe ne pourrait plus bouger");
        }
    }

    @Override
    public double versMoteur(double angleOrgane) {
        return zeroMoteur + angleOrgane / degresParUniteMoteur;
    }

    @Override
    public double depuisMoteur(double positionMoteur) {
        return (positionMoteur - zeroMoteur) * degresParUniteMoteur;
    }

    @Override
    public double gain(double angleOrgane) {
        // Exacte plutôt que dérivée : c'est la même valeur, sans le bruit de la différence finie.
        return Math.abs(1 / degresParUniteMoteur);
    }
}
