package fr.roboteek.robot.util.convertisseur;


/**
 * Classe utilitaire pour convertir des nombres.
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
public class ConvertisseurNombres {

    private static final String[] UNITES = {"un", "deux", "trois", "quatre", "cinq", "six", "sept", "huit", "neuf"};

    private static final String[] DIZAINES = {"dix", "vingt", "trente", "quarante", "cinquante", "soixante", "soixante_dix", "quatre_vingt", "quatre_vingt_dix"};

    private static final String[] DIX_UNITES = {"onze", "douze", "treize", "quatorze", "quinze", "seize"};

    private static final String[] MAGNITUDES = {"cent", "mille", "million"};

    private static final int[] EQUIVALENTS_UNITES = {1, 2, 3, 4, 5, 6, 7, 8, 9};

    private static final int[] EQUIVALENTS_DIX_UNITES = {11, 12, 13, 14, 15, 16};


    /** Constructeur privé. */
    private ConvertisseurNombres() { }

    /**
     * Convertit un nombre sous forme de chaine de caractères en nombre au format "numérique".
     * @param nombreAConvertir la chaine de caractère représentant le nombre
     * @return le nombre au format "numérique", null si non renseigné ou invalide
     */
    public static Integer convertirNombre(String nombreAConvertir) {
        if (nombreAConvertir != null && !nombreAConvertir.isEmpty()) {
            // Suppression des et + suppression des pluriels + conversion des dizaines spéciales en un seul élément
            final String chaineTraitee = nombreAConvertir.replaceAll(" et ", " ").replaceAll("vingts", "vingt").replaceAll("cents", "cent").replaceAll("milles", "mille").replaceAll("millions", "million")
                    .replaceAll("quatre vingt dix", "quatre_vingt_dix").replaceAll("quatre vingt", "quatre_vingt").replaceAll("soixante dix", "soixante_dix");
            
            return convertirNombreInferieurAMilliard(chaineTraitee.trim());

        } else {
            return null;
        }
    }

    /**
     * Convertit un nombre inférieur à cent.
     * @param nombreAConvertir le nombre à convertir
     * @return le nombre au format "numérique", null si non renseigné ou invalide
     */
    private static Integer convertirNombreInferieurACent(String nombreAConvertir) {
        if (nombreAConvertir != null && !nombreAConvertir.isEmpty()) {
            int resultat = 0;
            // Découpage du nombre en éléments uniques
            String[] elementsNombre = nombreAConvertir.split(" ");

            if (elementsNombre.length == 1) {
                // S'il y a un élément (unité ou dizaine)
                String element = elementsNombre[0];
                boolean trouve = false;
                // Recherche si c'est une unité
                for (int i = 0; i < UNITES.length; i++) {
                    if (element.equals(UNITES[i])) {
                        // Ajout de l'unité au résultat
                        resultat += EQUIVALENTS_UNITES[i];
                        trouve = true;
                        break;
                    }
                }
                // Recherche si c'est une dizaine (si le nombre n'a pas été trouvé)
                if (!trouve) {
                    for (int i = 0; i < DIZAINES.length; i++) {
                        if (element.equals(DIZAINES[i])) {
                            // Ajout de la dizaine au résultat
                            resultat += EQUIVALENTS_UNITES[i] * 10;
                            trouve = true;
                            break;
                        }
                    }
                }
                // Recherche si c'est une unité des dizaines (si le nombre n'a pas été trouvé)
                if (!trouve) {
                    for (int i = 0; i < DIX_UNITES.length; i++) {
                        if (element.equals(DIX_UNITES[i])) {
                            // Ajout de la dizaine au résultat
                            resultat += EQUIVALENTS_DIX_UNITES[i];
                            trouve = true;
                            break;
                        }
                    }
                }

                if (!trouve) {
                    // Nombre invalide
                    return null;
                } else {
                    return resultat;
                }

            } else if (elementsNombre.length == 2) {
                // S'il y a deux éléments (dizaine et unité)
                boolean trouve = false;
                // Premier élément : dizaine
                String dizaine = elementsNombre[0];
                for (int i = 0; i < DIZAINES.length; i++) {
                    if (dizaine.equals(DIZAINES[i])) {
                        // Ajout de la dizaine au résultat
                        resultat += EQUIVALENTS_UNITES[i] * 10;
                        trouve = true;
                        break;
                    }
                }
                if (!trouve) {
                    // Nombre invalide
                    return null;
                }

                // Second élément : unité ou unité de dizaine
                String unite = elementsNombre[1];
                trouve = false;
                // Recherche si c'est une unité
                for (int i = 0; i < UNITES.length; i++) {
                    if (unite.equals(UNITES[i])) {
                        // Ajout de l'unité au résultat
                        resultat += EQUIVALENTS_UNITES[i];
                        trouve = true;
                        break;
                    }
                }
                // Recherche si c'est une unité des dizaines (si le nombre n'a pas été trouvé)
                if (!trouve) {
                    for (int i = 0; i < DIX_UNITES.length; i++) {
                        if (unite.equals(DIX_UNITES[i])) {
                            // Ajout de la dizaine au résultat
                            resultat += EQUIVALENTS_DIX_UNITES[i];
                            trouve = true;
                            break;
                        }
                    }
                }

                if (!trouve) {
                    // Nombre invalide
                    return null;
                } else {
                    return resultat;
                }

            } else {
                // Nombre invalide
                return null;
            }


        } else {
            return 0;
        }
    }

    /**
     * Convertit un nombre inférieur à mille.
     * @param nombreAConvertir le nombre à convertir
     * @return le nombre au format "numérique", null si non renseigné ou invalide
     */
    private static Integer convertirNombreInferieurAMille(String nombreAConvertir) {
        if (nombreAConvertir != null && !nombreAConvertir.isEmpty()) {
            if (nombreAConvertir.contains(MAGNITUDES[0])) {
                // Découpage du nombre par "cent"
                String[] elementsNombreCent = nombreAConvertir.split(MAGNITUDES[0]);

                if (elementsNombreCent.length == 0) {
                    // Si aucun élément : c'est le nombre 100
                    return 100;
                } else if (elementsNombreCent.length == 1) {
                    // C'est un multiplicateur de 100 (ex : deux cent)
                    // Recherche si c'est une unité
                    String unite = elementsNombreCent[0].trim();
                    for (int i = 0; i < UNITES.length; i++) {
                        if (unite.equals(UNITES[i])) {
                            // Le résultat est 100 * unite
                            return 100 * EQUIVALENTS_UNITES[i];
                        }
                    }
                    // Pas trouvé : nombre invalide
                    return null;

                } else if (elementsNombreCent.length == 2) {
                    // Si deux éléments
                    int resultat = 0;
                    // Le premier élément est un multiplicateur de 100 (unité) 
                    // Pour les nombres entre 100 et 200 : il y a deux éléments mais le 1er élément est vide (ex : cent quarante cinq)
                    String premierElement = elementsNombreCent[0].trim();
                    boolean trouve = false;
                    if (!premierElement.isEmpty()) {
                        // Recherche si c'est une unité
                        for (int i = 0; i < UNITES.length; i++) {
                            if (premierElement.equals(UNITES[i])) {
                                // Ajout de l'unité au résultat
                                resultat += 100 * EQUIVALENTS_UNITES[i];
                                trouve = true;
                                break;
                            }
                        }
                        if (!trouve) {
                            // Nombre invalide
                            return null;
                        }
                    } else {
                        // Pas de premier élément : on ajoute 100
                        resultat += 100;
                    }

                    // Le second élément est un nombre inferieur à 100 à ajouter
                    final Integer nombreAAjouter = convertirNombreInferieurACent(elementsNombreCent[1].trim());
                    if (nombreAAjouter != null) {
                        resultat += nombreAAjouter;
                    } else {
                        // Nombre invalide
                        return null;
                    }

                    return resultat;
                } else {
                    // Nombre invalide
                    return null;
                }

            } else {
                // Si le nombre ne contient pas "cent", c'est peut-être un nombre inférieur à cent
                return convertirNombreInferieurACent(nombreAConvertir.trim());
            }

        } else {
            return 0;
        }
    }

    /**
     * Convertit un nombre inférieur à un million.
     * @param nombreAConvertir le nombre à convertir
     * @return le nombre au format "numérique", null si non renseigné ou invalide
     */
    private static Integer convertirNombreInferieurAMillion(String nombreAConvertir) {
        if (nombreAConvertir != null && !nombreAConvertir.isEmpty()) {
            if (nombreAConvertir.contains(MAGNITUDES[1])) {
                // Découpage du nombre par "mille"
                String[] elementsNombreMille = nombreAConvertir.split(MAGNITUDES[1]);

                if (elementsNombreMille.length == 0) {
                    // Si aucun élément : c'est le nombre 1000
                    return 1000;
                } else if (elementsNombreMille.length == 1) {
                    // C'est un multiplicateur de 1000 (nombre inférieur à 1000)
                    final Integer nombreMultiplicateur = convertirNombreInferieurAMille(elementsNombreMille[0].trim());
                    if (nombreMultiplicateur != null) {
                        return 1000 * nombreMultiplicateur;
                    } else {
                        // Nombre invalide
                        return null;
                    }

                } else if (elementsNombreMille.length == 2) {
                    // Si deux éléments
                    int resultat = 0;
                    // Le premier élément est un multiplicateur de 1000 (nombre inférieur à 1000)
                    // Pour les nombres entre 1000 et 2000 : il y a deux éléments mais le 1er élément est vide (ex : mille deux cent quarante cinq)
                    String premierElement = elementsNombreMille[0].trim();
                    if (!premierElement.isEmpty()) {
                        final Integer nombreMultiplicateur = convertirNombreInferieurAMille(premierElement);
                        if (nombreMultiplicateur != null) {
                            resultat += 1000 * nombreMultiplicateur;
                        } else {
                            // Nombre invalide
                            return null;
                        }
                    } else {
                        // Pas de premier élément : on ajoute 100
                        resultat += 1000;
                    }

                    // Le second élément est un nombre inferieur à 1000 à ajouter
                    final Integer nombreAAjouter = convertirNombreInferieurAMille(elementsNombreMille[1].trim());
                    if (nombreAAjouter != null) {
                        resultat += nombreAAjouter;
                    } else {
                        // Nombre invalide
                        return null;
                    }

                    return resultat;
                } else {
                    // Nombre invalide
                    return null;
                }

            } else {
                // Si le nombre ne contient pas "mille", c'est peut-être un nombre inférieur à mille
                return convertirNombreInferieurAMille(nombreAConvertir.trim());
            }

        } else {
            return 0;
        }
    }

    /**
     * Convertit un nombre inférieur à un milliard.
     * @param nombreAConvertir le nombre à convertir
     * @return le nombre au format "numérique", null si non renseigné ou invalide
     */
    private static Integer convertirNombreInferieurAMilliard(String nombreAConvertir) {
        if (nombreAConvertir != null && !nombreAConvertir.isEmpty()) {
            if (nombreAConvertir.contains(MAGNITUDES[2])) {
                // Découpage du nombre par "million"
                String[] elementsNombreMillion = nombreAConvertir.split(MAGNITUDES[2]);

                if (elementsNombreMillion.length == 0) {
                    // Si aucun élément : c'est le nombre 1000000
                    return 1000000;
                } else if (elementsNombreMillion.length == 1) {
                    // C'est un multiplicateur de 1000000 (nombre inférieur à 1000)
                    final Integer nombreMultiplicateur = convertirNombreInferieurAMille(elementsNombreMillion[0].trim());
                    if (nombreMultiplicateur != null) {
                        return 1000000 * nombreMultiplicateur;
                    } else {
                        // Nombre invalide
                        return null;
                    }

                } else if (elementsNombreMillion.length == 2) {
                    // Si deux éléments
                    int resultat = 0;
                    // Le premier élément est un multiplicateur de 1000000 (nombre inférieur à 1000)
                    final Integer nombreMultiplicateur = convertirNombreInferieurAMille(elementsNombreMillion[0].trim());
                    if (nombreMultiplicateur != null) {
                        resultat += 1000000 * nombreMultiplicateur;
                    } else {
                        // Nombre invalide
                        return null;
                    }

                    // Le second élément est un nombre inferieur à 1000000 à ajouter
                    final Integer nombreAAjouter = convertirNombreInferieurAMillion(elementsNombreMillion[1].trim());
                    if (nombreAAjouter != null) {
                        resultat += nombreAAjouter;
                    } else {
                        // Nombre invalide
                        return null;
                    }

                    return resultat;
                } else {
                    // Nombre invalide
                    return null;
                }

            } else {
                // Si le nombre ne contient pas "million", c'est peut-être un nombre inférieur à un million
                return convertirNombreInferieurAMillion(nombreAConvertir.trim());
            }

        } else {
            return 0;
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        long debut = System.currentTimeMillis();
        System.out.println(convertirNombre("sept cents dix neuf million deux cents quatre vingts onze milles huit cents vingt et un"));
        long fin = System.currentTimeMillis();
        System.out.println("Temps = " + (fin - debut));

        //        String[] split = "cent".split("cent");
        //        for (int i = 0; i <split.length; i++) {
        //            System.out.println(i + " = " + split[i]);
        //        }
    }

}



