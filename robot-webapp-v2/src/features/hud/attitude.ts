/**
 * Identifiants des mesures dessinées par `CarteAttitude`, à retirer de la grille d'anneaux.
 *
 * Exception assumée à la découverte de capacités : un anneau dit « combien », et un angle se lit
 * « dans quel sens ». Sur −180 / +180, un robot à plat donnait un anneau à moitié plein, qui se
 * vidait d'un côté et se remplissait de l'autre ; et la couleur de seuil passait à l'ambre selon
 * la direction où il regardait.
 */
export const MESURES_ATTITUDE = ['roulis', 'tangage', 'cap']
