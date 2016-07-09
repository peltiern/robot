package fr.roboteek.robot.decisionnel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import com.google.common.collect.Iterables;

import fr.roboteek.robot.util.objet.Personne;

/**
 * Contexte de la reconnaissance faciale.
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
public class Contexte {
    
    /** Date de la dernière reconnaissance. */
    private Calendar dateDerniereReconnaissance;
    
    /** Liste des personnes reconnues. */
    private List<Personne> listePersonnesReconnues;
    
    /**
     * Récupère la valeur de dateDerniereReconnaissance.
     * @return la valeur de dateDerniereReconnaissance
     */
    public Calendar getDateDerniereReconnaissance() {
        return dateDerniereReconnaissance;
    }

    /**
     * Définit la valeur de dateDerniereReconnaissance.
     * @param dateDerniereReconnaissance la nouvelle valeur de dateDerniereReconnaissance
     */
    public void setDateDerniereReconnaissance(Calendar dateDerniereReconnaissance) {
        this.dateDerniereReconnaissance = dateDerniereReconnaissance;
    }
    
    /**
     * Récupère la valeur de listePersonnesReconnues.
     * @return la valeur de listePersonnesReconnues
     */
    public List<Personne> getListePersonnesReconnues() {
        if (listePersonnesReconnues == null) {
            listePersonnesReconnues = new ArrayList<Personne>();
        }
        return listePersonnesReconnues;
    }
    
    /**
     * Ajoute une personne comme ayant été reconnue.
     * @param personne la personne reconnue
     */
    public void ajouterPersonneReconnue(Personne personne) {
        personne.aEteReconnue();
        getListePersonnesReconnues().add(personne);
    }
    
    /**
     * Récupère la dernière personne reconnue.
     * @return la dernière personne reconnue
     */
    public Personne getDernierePersonneVue() {
        return Iterables.getLast(getListePersonnesReconnues(), null);
    }
    
    /**
     * Récupère la dernière date de la dernière personne reconnue.
     * @return la valeur de dateDerniereReconnaissanceFaciale
     */
    public Calendar getDerniereDateDernierePersonneReconnue() {
        final Personne dernierePersonneVue = getDernierePersonneVue();
        if (dernierePersonneVue != null) {
            return dernierePersonneVue.getDerniereDateReconnaissance();
        } else {
            return null;
        }
    }

}
