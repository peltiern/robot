package fr.roboteek.robot.util.objet;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.google.common.collect.Iterables;

/**
 * Classe représentant une personne.
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
public class Personne {

    /** Identifiant de la personne reconnue. */
    private Integer idPersonne;

    /** Prénom de la personne. */
    private String prenom;

    /** Liste des instants où la personne a été reconnue (ordonnés par date). */
    private List<Calendar> listeDatesReconnaissances;

    /**
     * Récupère la valeur de idPersonne.
     * @return la valeur de idPersonne
     */
    public Integer getIdPersonne() {
        return idPersonne;
    }

    /**
     * Définit la valeur de idPersonne.
     * @param idPersonne la nouvelle valeur de idPersonne
     */
    public void setIdPersonne(Integer idPersonne) {
        this.idPersonne = idPersonne;
    }

    /**
     * Récupère la valeur de prenom.
     * @return la valeur de prenom
     */
    public String getPrenom() {
        return prenom;
    }

    /**
     * Définit la valeur de prenom.
     * @param prenom la nouvelle valeur de prenom
     */
    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    /**
     * Récupère la valeur de listeDatesReconnaissances.
     * @return la valeur de listeDatesReconnaissances
     */
    public List<Calendar> getListeDatesReconnaissances() {
        if (listeDatesReconnaissances == null) {
            listeDatesReconnaissances = new ArrayList<Calendar>();
        }
        return listeDatesReconnaissances;
    }

    /**
     * Définit la valeur de listeDatesReconnaissances.
     * @param listeDatesReconnaissances la nouvelle valeur de listeDatesReconnaissances
     */
    public void setListeDatesReconnaissances(List<Calendar> listeDatesReconnaissances) {
        this.listeDatesReconnaissances = listeDatesReconnaissances;
    }

    /** Signale que la personne a été reconnue en ajoutant une date dans la liste des dates de reconnaissance. */
    public void aEteReconnue() {
        this.getListeDatesReconnaissances().add(Calendar.getInstance());
    }

    /**
     * Récupère la dernière date à laquelle la personne a été reconnue.
     * @return la dernière date à laquelle la personne a été reconnue
     */
    public Calendar getDerniereDateReconnaissance() {
        return Iterables.getLast(getListeDatesReconnaissances(), null);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((idPersonne == null) ? 0 : idPersonne.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Personne other = (Personne) obj;
        if (idPersonne == null) {
            if (other.idPersonne != null) return false;
        } else if (!idPersonne.equals(other.idPersonne)) return false;
        return true;
    }
}
