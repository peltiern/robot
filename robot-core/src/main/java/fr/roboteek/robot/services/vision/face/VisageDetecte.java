package fr.roboteek.robot.services.vision.face;

/**
 * Visage détecté dans une image par {@link ServiceDetectionVisage}.
 * <p>
 * {@code ligneBrute} est la ligne rendue par {@code FaceDetectorYN.detect()} (15 colonnes : bbox,
 * 5 points caractéristiques, score), dont {@code FaceRecognizerSF.alignCrop()} a besoin telle
 * quelle.
 * <p>
 * En {@code float[]} et non en {@code Mat} : un {@code Mat} est une poignée vers de la mémoire
 * native qu'OpenCV ne libère qu'au passage du ramasse-miettes, et la faire circuler dans un record
 * revenait à n'en confier la durée de vie à personne. C'est le service de reconnaissance qui en
 * rebâtit un, le temps de son appel.
 *
 * @param ligneBrute les 15 colonnes de la détection ; {@code null} accepté hors reconnaissance
 */
public record VisageDetecte(int x, int y, int width, int height, float score, float[] ligneBrute) {

    /** Position, dans {@link #ligneBrute}, du premier des cinq points caractéristiques. */
    private static final int PREMIER_POINT = 4;

    /**
     * De combien le nez est décalé par rapport au milieu des yeux, en écarts d'yeux : autour de
     * zéro quand la personne est de face, et d'autant plus grand qu'elle se tourne.
     * <p>
     * <b>À quoi ça sert</b> : SFace est entraîné sur des visages à peu près de face. De profil,
     * l'empreinte s'éloigne de celle qu'on a enrôlée au point de tomber parfois plus près de
     * quelqu'un d'autre — le robot appelle alors Nicolas « Julia ». Mieux vaut ne rien dire.
     * <p>
     * Le nez est <b>projeté sur l'axe des yeux</b> plutôt que comparé en abscisse : une tête
     * penchée passerait sinon pour un profil, alors qu'un simple roulis ne gêne pas SFace, qui
     * redresse le visage sur ces mêmes points avant de l'encoder.
     *
     * @return zéro si la détection n'a pas donné ses points, ce qui la laisse passer : la porte
     *         est là pour écarter un profil avéré, pas pour refuser ce qu'elle ne sait pas juger
     */
    public double asymetrieDuNez() {
        if (ligneBrute == null || ligneBrute.length < PREMIER_POINT + 6) {
            return 0;
        }
        double oeilAx = ligneBrute[PREMIER_POINT];
        double oeilAy = ligneBrute[PREMIER_POINT + 1];
        double oeilBx = ligneBrute[PREMIER_POINT + 2];
        double oeilBy = ligneBrute[PREMIER_POINT + 3];
        double nezX = ligneBrute[PREMIER_POINT + 4];
        double nezY = ligneBrute[PREMIER_POINT + 5];

        double axeX = oeilBx - oeilAx;
        double axeY = oeilBy - oeilAy;
        double ecartCarre = axeX * axeX + axeY * axeY;
        if (ecartCarre <= 0) {
            return 0;
        }
        double versLeNezX = nezX - (oeilAx + oeilBx) / 2;
        double versLeNezY = nezY - (oeilAy + oeilBy) / 2;
        return Math.abs((versLeNezX * axeX + versLeNezY * axeY) / ecartCarre);
    }
}
