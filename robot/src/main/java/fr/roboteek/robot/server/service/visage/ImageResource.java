/**
 * 
 */
package fr.roboteek.robot.server.service.visage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.builder.CompareToBuilder;
import org.glassfish.jersey.media.multipart.BodyPartEntity;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;

import fr.roboteek.robot.Constantes;
import fr.roboteek.robot.server.service.model.visage.Image;

/**
 * Services Web liés aux images.
 * @author Nicolas
 *
 */
@Path("/visage/images")
public class ImageResource {

	/**
	 * Récupère une image par son identifiant.
	 * @param idImage identifiant de l'image
	 * @return l'image au format PNG
	 */
	@GET
	@Path("{id}")
	@Produces("image/png")
	public Response getImage(@PathParam("id") String idImage) {
		File file = new File(Constantes.DOSSIER_VISAGE + File.separator + "images" + File.separator + idImage + ".png");  
		ResponseBuilder response = Response.ok((Object) file);  
		response.header("Content-Disposition","attachment; filename=\"" + idImage + ".png\"");  
		return response.build();  
	}

	/**
	 * Récupère la liste des images.
	 * @return la liste des images
	 */
	@GET
	@Produces({MediaType.APPLICATION_JSON})
	public Response getListeImages() {
		final List<Image> listeImages = new ArrayList<Image>();
		// Accès au dossier des images du visage
		java.nio.file.Path dossierImagesVisages = Paths.get(Constantes.DOSSIER_VISAGE + File.separator + "images");
		// Parcours des fichiers de type PNG
		try {
			DirectoryStream<java.nio.file.Path> stream = Files.newDirectoryStream(dossierImagesVisages,"*.png");
			for (java.nio.file.Path entry : stream) {
				File fichierImage = entry.toFile();
				Image image = new Image(fichierImage.getName().replace(".png", ""), fichierImage.lastModified());
				listeImages.add(image);
			}
		} catch(IOException e) {
			e.printStackTrace();
		}
		// Tri par nom
		Collections.sort(listeImages, new Comparator<Image>() {

			@Override
			public int compare(Image o1, Image o2) {
				return new CompareToBuilder().append(o1.getNom(), o2.getNom()).toComparison();
			}
		});
		return Response.ok(listeImages).build();
	}

//	/**
//	 * Uploade des images dans le répertoire.
//	 * @param bodyParts les parties de la requête contenant les fichiers
//	 * @param fileDispositions
//	 * @return la réponse
//	 */
//	@POST
//	@Consumes(MediaType.MULTIPART_FORM_DATA)
//	public Response uploadImage(@FormDataParam("files") List<FormDataBodyPart> bodyParts, @FormDataParam("files") FormDataContentDisposition fileDispositions) {
//
//		// Vérification des paramètres
//		if (bodyParts == null || fileDispositions == null)
//			return Response.status(400).entity("Invalid form data").build();
//
//		// Dossier des images des visages
//		final String dossierImages = Constantes.DOSSIER_VISAGE + File.separator + "images" + File.separator;
//		// Création du dossier s'il n'existe pas
//		try {
//			createFolderIfNotExists(dossierImages);
//		} catch (SecurityException se) {
//			return Response.status(500).entity("Can not create destination folder on server").build();
//		}
//
//		StringBuffer fileDetails = new StringBuffer("");
//
//		for (int i = 0; i < bodyParts.size(); i++) {
//			
//			BodyPartEntity bodyPartEntity = (BodyPartEntity) bodyParts.get(i).getEntity();
//			String fileName = bodyParts.get(i).getContentDisposition().getFileName();
//			
//			// TODO NP : Vérification du fichier (type, dimensions)
//			String cheminFichier = dossierImages + fileName;
//
//			// Copie du fichier dans le répertoire
//			try {
//				IOUtils.copy(bodyPartEntity.getInputStream(), new FileOutputStream(cheminFichier));
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//
//			fileDetails.append("Fichier sauvegardé : " + dossierImages + fileName);
//		}
//
//
//		return Response.ok(fileDetails.toString()).build();
//	}
	
	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response uploadFiles(final FormDataMultiPart multiPart) {
		
		// Vérification des paramètres
		if (multiPart == null) {
			return Response.status(400).entity("Invalid form data").build();
		}

		// Dossier des images des visages
		final String dossierImages = Constantes.DOSSIER_VISAGE + File.separator + "images" + File.separator;
		// Création du dossier s'il n'existe pas
		try {
			createFolderIfNotExists(dossierImages);
		} catch (SecurityException se) {
			return Response.status(500).entity("Can not create destination folder on server").build();
		}

		StringBuffer fileDetails = new StringBuffer("");

		/* Save multiple files */
		for (Entry<String, List<FormDataBodyPart>> entreeListeBodyPartParNom : multiPart.getFields().entrySet()) {
			if (entreeListeBodyPartParNom.getKey().startsWith("files")) {
				for (FormDataBodyPart formDataBodyPart : entreeListeBodyPartParNom.getValue()) {
					BodyPartEntity bodyPartEntity = (BodyPartEntity) formDataBodyPart.getEntity();
					String cheminFichier = dossierImages + formDataBodyPart.getContentDisposition().getFileName();
					try {
						IOUtils.copy(bodyPartEntity.getInputStream(), new FileOutputStream(cheminFichier));
						fileDetails.append("Fichier sauvegardé : " + cheminFichier);
					} catch (FileNotFoundException e) {
//						 TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		
		

		return Response.ok(fileDetails.toString()).build();
	}

	/**
	 * Creates a folder to desired location if it not already exists
	 * 
	 * @param dirName
	 *            - full path to the folder
	 * @throws SecurityException
	 *             - in case you don't have permission to create the folder
	 */
	private void createFolderIfNotExists(String dirName) throws SecurityException {
		File theDir = new File(dirName);
		if (!theDir.exists()) {
			theDir.mkdir();
		}
	}
}
