package fr.roboteek.robot.activites;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Moteur du jeu "Plus - Moins".
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
public class MoteurJeuPlusMoins {

    /** Modes de jeu. */
    public static enum MODE {DEVINER, FAIRE_DEVINER};

    /** Résultats possibles. */
    public static enum RESULTAT {PLUS, MOINS, PLUS_DEJA_PROPOSE, MOINS_DEJA_PROPOSE, GAGNE, PAS_DANS_INTERVALLE};

    /** Modes de recherche. */
    private static enum RECHERCHE {DICHOTOMIE, HUMAIN, STUPIDE}

    /** Mode de jeu. */
    private MODE mode;

    /** Mode de recherche. */
    private RECHERCHE modeRecherche;

    /** Nombre minimum. */
    private int minimum = 0;

    /** Nombre maximum. */
    private int maximum = 100;

    /** Nombre à faire deviner. */
    private int nbAFaireDeviner;

    /** Liste des nombres proposés. */
    private List<Integer> listeNbProposes = new ArrayList<Integer>();

    /** Minimum proposé. */
    private int minimumPropose;

    /** Maximum proposé. */
    private int maximumPropose;

    /** Dernier nombre proposé. */
    private int dernierNombrePropose;

    /** Constructeur. */
    public MoteurJeuPlusMoins() { }

    /**
     * Initialise le moteur de jeu en indiquant le mode et l'intervalle de recherche.
     * @param mode mode de jeu
     * @param minimum nombre minimum
     * @param maximum nombre maximum
     */
    public void initialiser(MODE mode, int minimum, int maximum) {
        // Initialisation des variables
        this.mode = mode;
        this.minimum = minimum;
        this.maximum = maximum;
        listeNbProposes = new ArrayList<Integer>();
        dernierNombrePropose = -1;
        if (mode == MODE.DEVINER) {
            // Le robot doit deviner
            nbAFaireDeviner = 0;
            minimumPropose = minimum - 1;
            maximumPropose = maximum + 1;
            // Calcul du mode de recherche
            int idxModeRecherche = getNombreAleatoire(1, 3);
            if (idxModeRecherche == 1) {
                modeRecherche = RECHERCHE.DICHOTOMIE;
            } else if (idxModeRecherche == 2) {
                modeRecherche = RECHERCHE.HUMAIN;
            } else {
                modeRecherche = RECHERCHE.STUPIDE;
            }
            System.out.println("Mode de recherche = " + modeRecherche);
        } else {
            // Le robot doit faire deviner
            nbAFaireDeviner = getNombreAleatoire(minimum, maximum);
            System.out.println("Nb à faire deviner = " + nbAFaireDeviner);
        }
    }

    /**
     * Permet de proposer un nombre au robot (en mode FAIRE_DEVINER)
     * @param nombrePropose nombre proposé
     * @return le résultat
     */
    public RESULTAT proposerNombre(int nombrePropose) {
        if (mode == MODE.FAIRE_DEVINER) {
            dernierNombrePropose = nombrePropose;
            if (nombrePropose < minimum || nombrePropose > maximum) {
                // Nombre hors intervalle
                return RESULTAT.PAS_DANS_INTERVALLE;
            } else {
                if (nombrePropose == nbAFaireDeviner) {
                    return RESULTAT.GAGNE;
                } else if (nombrePropose > nbAFaireDeviner) {
                    if (listeNbProposes.contains(nombrePropose)) {
                        return RESULTAT.MOINS_DEJA_PROPOSE;
                    } else {
                        listeNbProposes.add(nombrePropose);
                        return RESULTAT.MOINS;
                    }
                } else {
                    if (listeNbProposes.contains(nombrePropose)) {
                        return RESULTAT.PLUS_DEJA_PROPOSE;
                    } else {
                        listeNbProposes.add(nombrePropose);
                        return RESULTAT.PLUS;
                    }
                }
            }
        } else {
            // Méthode non autorisée dans ce mode
            throw new UnsupportedOperationException();
        }
    }

    public int devinerNombre(RESULTAT plusMoins) {
        // 3 modes
        // Mode Recherche par dichotomie
        // Mode recherche complètement aléatoire entre min et max
        // Mode recherche aléatoire entre min et max proposés
        // Retourner nombre + GAGNE ou INCOHERENT
        if (mode == MODE.DEVINER) {
            // Si c'est le premier essai : on met à jour le dernier nombre proposé par min ou max
            if (dernierNombrePropose == -1) {
                if (plusMoins == RESULTAT.PLUS) {
                    dernierNombrePropose = minimum;
                } else {
                    dernierNombrePropose = maximum;
                }
            }

            if (plusMoins == RESULTAT.PLUS) {
                // Si PLUS : on met à jour le minimum proposé par le dernier nombre proposé
                minimumPropose = dernierNombrePropose;
            } else {
                // Si MOINS : on met à jour le maximum proposé par le dernier nombre proposé
                maximumPropose = dernierNombrePropose;
            }

            // Calcul du nombre à proposer
            int nbAProposer = calculerNombreAProposer(plusMoins);

            // Ajout du nombre à la liste des nombres proposés
            listeNbProposes.add(nbAProposer);
            // Mise à jour du dernier nombre proposé
            dernierNombrePropose = nbAProposer;

            return nbAProposer;

        } else {
            // Méthode non autorisée dans ce mode
            throw new UnsupportedOperationException();
        }
        // 3 modes
        // Mode Recherche par dichotomie
        // Mode recherche complètement aléatoire entre min et max
        // Mode recherche aléatoire entre min et max proposés
        // Retourner nombre + GAGNE ou INCOHERENT
    }

    /**
     * Récupère la valeur de dernierNombrePropose.
     * @return la valeur de dernierNombrePropose
     */
    public int getDernierNombrePropose() {
        return dernierNombrePropose;
    }

    private int calculerNombreAProposer(RESULTAT plusMoins) {
        if (modeRecherche == RECHERCHE.STUPIDE) {
            // Mode de recherche stupide : nombre compris entre min et max, selon le dernier nombre proposé
            int nbAProposer = -1;
            if (plusMoins == RESULTAT.PLUS) {
                // TODO Problème si dernierNombrePropose + 1 > maximum
                nbAProposer = getNombreAleatoire(dernierNombrePropose + 1, maximum);
            } else {
                nbAProposer = getNombreAleatoire(minimum, dernierNombrePropose - 1);
            }
            // On ne propose pas un nombre déjà proposé
            if (listeNbProposes.contains(nbAProposer)) {
                // On recalcul un nombre si déjà proposé
                nbAProposer = calculerNombreAProposer(plusMoins);
            }

            return nbAProposer;

        } else if (modeRecherche == RECHERCHE.HUMAIN) {
            // Mode de recherche humain : nombre compris entre le minimum proposé (exclu) et le maximum proposé (exclu), selon le dernier nombre proposé
            int nbAProposer = -1;
            if (plusMoins == RESULTAT.PLUS) {
                // TODO Problème si dernierNombrePropose + 1 > maximum
                nbAProposer = getNombreAleatoire(dernierNombrePropose + 1, maximumPropose - 1);
            } else {
                nbAProposer = getNombreAleatoire(minimumPropose + 1, dernierNombrePropose - 1);
            }
            // On ne propose pas un nombre déjà proposé
            if (listeNbProposes.contains(nbAProposer)) {
                // On recalcul un nombre si déjà proposé
                nbAProposer = calculerNombreAProposer(plusMoins);
            }

            return nbAProposer;
        } else {
            // Mode de recherche dichotomique
            return (int) ((maximumPropose + minimumPropose) / 2);
        }
    }

    /**
     * Calcule un nombre aléatoire compris dans un intervalle.
     * @param minimum minmum de l'intervalle
     * @param maximum maximum de l'intervalle
     * @return le nombre aléatoire
     */
    private static int getNombreAleatoire(int minimum, int maximum) {
        return new Random().nextInt((maximum - minimum) + 1) + minimum;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub
        System.out.println(getNombreAleatoire(1, 2));
    }

}
