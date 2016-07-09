package fr.roboteek.robot.util.callback;

/**
 * Objet permettant un retour suite à l'appel d'une méthode asynchrone.
 * @author Nicolas Peltier (nico.peltier@gmail.com)
 */
 public interface AsyncCallback<T> {

     /**
      * Méthode appelée une fois le traitement terminé.
      * @param resultat le résultat de la méthode appelé en asynchrone
      */
    void onSuccess(T resultat);
    
    /**
     * Méthode appelé en cas d'erreur.
     * @param throwable l'objet représentant l'erreur
     */
    void onError(Throwable throwable);
}
