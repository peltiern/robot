package fr.roboteek.robot.util.phidgets;

/**
 * Où en est un servo lancé vers sa butée, calculé et non lu.
 * <p>
 * La manette fait tourner la tête en donnant la butée pour cible et une vitesse, puis coupe la
 * vitesse au relâchement. Le servo RC ne dit jamais où il est, et {@code getPosition()} ne se
 * rafraîchit qu'à l'atteinte d'une cible — jamais atteinte ici. Après un coup de manette, la
 * seule position « connue » était donc la butée, et c'est d'elle que partaient les pistes
 * relatives d'animation : le 2026-09-30, un « non » s'est fait contre la butée gauche au lieu
 * d'autour de la tête.
 * <p>
 * Or ce n'est pas le servo qui fixe l'allure : c'est le <b>contrôleur</b> qui déplace l'impulsion,
 * exactement à la vitesse et à l'accélération qu'on lui a écrites. Connaissant le départ et la
 * durée, on sait où en est l'impulsion — à la latence USB près, une quinzaine de millisecondes.
 * <p>
 * Toutes les grandeurs sont en unités moteur, celles du contrôleur.
 *
 * @param depart       position au lancement
 * @param sens         +1 vers la butée haute, -1 vers la butée basse
 * @param vitesse      limite de vitesse écrite sur le contrôleur, en unités/s
 * @param acceleration accélération écrite sur le contrôleur, en unités/s²
 * @param debutNanos   instant du lancement, en {@link System#nanoTime()}
 */
record CourseContinue(double depart, int sens, double vitesse, double acceleration, long debutNanos) {

    /**
     * Position de l'impulsion à un instant : rampe de démarrage, puis vitesse de croisière. Sans
     * la rampe, l'estimation aurait une avance constante de v²/2a — 0,4° au panoramique, mais
     * plusieurs degrés sur un axe à accélération douce.
     */
    double positionA(long nanos) {
        double secondes = Math.max(0, nanos - debutNanos) / 1e9;
        if (acceleration <= 0) {
            return depart + sens * vitesse * secondes;
        }
        double dureeRampe = vitesse / acceleration;
        double distance = secondes < dureeRampe
                ? acceleration * secondes * secondes / 2
                : vitesse * secondes - vitesse * vitesse / (2 * acceleration);
        return depart + sens * distance;
    }

    /**
     * La cible à donner pour s'arrêter maintenant, bornée aux butées — données dans n'importe
     * quel ordre : sur l'œil droit, monté en miroir, la butée « min » est la plus grande.
     * <p>
     * <b>Devant</b> la position estimée, de la distance de freinage : lancé à pleine vitesse, le
     * servo ne s'arrête pas sur place. Viser la position elle-même lui ferait dépasser la cible
     * puis revenir en arrière — l'à-coup qu'on cherche justement à éviter. Ainsi il freine et
     * s'arrête pile, comme le faisait la coupure de vitesse.
     */
    double cibleDArretA(long nanos, double positionMin, double positionMax) {
        double freinage = acceleration > 0 ? vitesse * vitesse / (2 * acceleration) : 0;
        double cible = positionA(nanos) + sens * freinage;
        return entre(cible, positionMin, positionMax);
    }

    /**
     * {@link Math#clamp} lève si la borne basse dépasse la haute, et c'est le cas de l'œil droit :
     * le 2026-09-30, chaque appui sur cet œil levait avant d'envoyer quoi que ce soit, et l'œil ne
     * répondait plus.
     */
    static double entre(double valeur, double borne1, double borne2) {
        return Math.clamp(valeur, Math.min(borne1, borne2), Math.max(borne1, borne2));
    }
}
