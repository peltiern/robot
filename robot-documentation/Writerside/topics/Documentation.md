# Documentation

### Génération des pages de la documentation sur Github
La documentation est générée grâce à une action Github définie dans le fichier _.github/workflows/build-docs.yml_ se situant à la racine du projet.

Pour lancer cette génération :
- Cliquer sur _Releases_ pour se rendre sur la gestion de releases
![generation-documentation-01.png](generation-documentation-01.png)
- Cliquer sur _Draft a new release_ pour accéder à la page de création d'une nouvelle release
![generation-documentation-02.png](generation-documentation-02.png)
- Créer un nouveau tag au format **_doc-x.x.x_** qui permettra à l'action de génération de la documentation de se lancer
![generation-documentation-03.png](generation-documentation-03.png)
- Sélectionner la branche à partir de laquelle créer le tag, mettre un titre de la forme **_Documentation - Version x.x.x_**, et cocher la case _Set as a pre-release_
![generation-documentation-04.png](generation-documentation-04.png)
- Aller sur l'onglet _Actions_ pour suivre le processus l'action _Build documentation_ qui s'est lancée
![generation-documentation-05.png](generation-documentation-05.png)
- Cliquer sur l'action pour suivre les différentes étapes du processus et s'assurer que toutes les coches sont au vert à la fin du processus
![generation-documentation-06.png](generation-documentation-06.png)

La nouvelle version de la documentation est disponible ici : [https://doc.roboteek.fr/](https://doc.roboteek.fr/)