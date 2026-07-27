package fr.roboteek.robot.spring.server.controller.dto;

/**
 * Nature d'un organe du robot, exposée aux clients pour qu'ils adaptent leur rendu
 * sans connaître les organes à l'avance :
 * <ul>
 *   <li>{@link #ACTIONNEUR} : organe pilotable, porteur d'{@code articulations} ;</li>
 *   <li>{@link #CAPTEUR} : organe de mesure, porteur de mesures (à venir).</li>
 * </ul>
 */
public enum TypeOrgane {
    ACTIONNEUR,
    CAPTEUR
}
