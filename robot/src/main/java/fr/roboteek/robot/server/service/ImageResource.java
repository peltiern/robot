/**
 * 
 */
package fr.roboteek.robot.server.service;

import java.io.File;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import fr.roboteek.robot.Constantes;

/**
 * Services Web liés aux images.
 * @author Nicolas
 *
 */
@Path("/images")
public class ImageResource {

	@GET
	@Path("image-{id}")
	@Produces("image/png") 
	public Response getTrain(@PathParam("id") String idImage) {
		File file = new File(Constantes.DOSSIER_VISAGE + File.separator + "images" + File.separator + idImage + ".png");  
		ResponseBuilder response = Response.ok((Object) file);  
		response.header("Content-Disposition","attachment; filename=\"" + idImage + ".png\"");  
		return response.build();  
	}

}
