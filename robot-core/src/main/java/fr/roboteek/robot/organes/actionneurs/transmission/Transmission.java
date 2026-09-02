package fr.roboteek.robot.organes.actionneurs.transmission;

/**
 * Ce qui sépare l'angle d'un organe de la position de son moteur.
 * <p>
 * Un servo ne tourne presque jamais autant que la pièce qu'il entraîne. Le panoramique du cou
 * passe par un couple de pignons, l'inclinaison par une boîte Stingray, les yeux par un
 * quadrilatère articulé où le rapport n'est même pas constant. Jusqu'ici chaque organe portait sa
 * conversion à la main — {@code zero - position}, recopié onze fois entre {@code Yeux} et
 * {@code Cou}, une fois à l'aller et une fois au retour, sans jamais dire qu'il s'agissait de la
 * même notion.
 * <p>
 * Ce que ça a coûté est visible dans {@code robot.properties} : la clé
 * {@code robot.regard.panoramique.commande.par.degre.vu} ne vaut ni un rapport de transmission ni
 * un gain de boucle, mais leur <b>produit</b>, étalonné en bout de chaîne. Trois campagnes de
 * mesure ont été nécessaires pour le régler, et il compense au passage un champ de vision de
 * caméra probablement faux, sans que rien ne le dise.
 * <p>
 * <b>Le contrat</b>, à respecter par toute implémentation :
 * <ul>
 *   <li>{@link #versMoteur} et {@link #depuisMoteur} sont <b>réciproques</b> ;</li>
 *   <li>la fonction est <b>strictement monotone</b> ;</li>
 *   <li>elle peut être <b>décroissante</b> — c'est le cas de quatre des cinq axes du robot — et
 *   rien n'autorise donc à supposer que le minimum d'organe donne le minimum moteur. C'est
 *   l'inverse.</li>
 * </ul>
 * <p>
 * <b>Ce que cette interface n'est pas</b> : elle ne vit pas dans {@code util/phidgets}, et
 * {@link fr.roboteek.robot.util.phidgets.PhidgetsServoMotor} ne la connaît pas. Un pilote de
 * servo-contrôleur doit parler degrés moteur et rien d'autre : ses butées, ses bornes de vitesse
 * lues sur le matériel et ses messages d'erreur n'ont de sens que là. Convertir une position, un
 * déplacement relatif et une vitesse sont d'ailleurs trois opérations différentes — une classe
 * dont l'API est {@code setPositionCible}, {@code rotate} et {@code setVitesse} les ferait toutes
 * les trois en silence. Mieux vaut que l'organe les fasse, visiblement.
 */
public interface Transmission {

    /** Position à commander au moteur pour que l'organe atteigne cet angle. */
    double versMoteur(double angleOrgane);

    /** Angle qu'a l'organe quand son moteur est à cette position. */
    double depuisMoteur(double positionMoteur);

    /**
     * Combien de degrés moteur vaut un degré d'organe, à cet endroit de la course.
     * <p>
     * C'est ce qui convertit une <b>vitesse</b> : une consigne exprimée en degrés d'organe par
     * seconde doit être multipliée par ce gain avant d'atteindre le contrôleur, dont les bornes
     * sont en unités moteur. Toujours positif — une vitesse n'a pas de signe, même sur une
     * transmission décroissante.
     * <p>
     * La dérivée numérique suffit : pour une transmission affine elle est exacte, et pour le
     * quadrilatère des yeux — dont le rapport varie de 1,25 à 3,70 le long de la course — une
     * vitesse n'a jamais été un contrat, seulement un confort.
     */
    default double gain(double angleOrgane) {
        double pas = 0.01;
        return Math.abs((versMoteur(angleOrgane + pas) - versMoteur(angleOrgane - pas)) / (2 * pas));
    }

    /**
     * Transmission à rapport constant : {@code moteur = zero + angle / degresParUniteMoteur}.
     *
     * @param zeroMoteur            position moteur où l'organe est à 0
     * @param degresParUniteMoteur  degrés d'organe pour une unité de position moteur, <b>signé</b> :
     *                              négatif quand le montage inverse le sens. C'est la grandeur qui
     *                              se mesure directement sur le robot — « une unité vaut tant de
     *                              degrés » — et non son inverse.
     */
    static Transmission affine(double zeroMoteur, double degresParUniteMoteur) {
        return new TransmissionAffine(zeroMoteur, degresParUniteMoteur);
    }
}
