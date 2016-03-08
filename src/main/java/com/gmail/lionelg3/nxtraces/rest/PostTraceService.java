package com.gmail.lionelg3.nxtraces.rest;

import com.gmail.lionelg3.nxtraces.db.Repository;
import org.apache.commons.codec.binary.Base32;
import org.apache.commons.io.IOUtils;
import org.jboss.logging.Logger;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

/**
 * Collect mail traces from Unix systems
 *
 * @author Lionel G (lionelg3@gmail.com)
 *         Date: 18/12/15, Time: 16:43
 *         <p>
 *         cat src/test/resources/mail1.txt | curl --form mail=@- http://localhost:8080/api/post
 *         <p>
 */
@Path("/post")
public class PostTraceService {

    private static final Logger LOGGER = Logger.getLogger(PostTraceService.class);

    @GET
    public Response getmail() {
        return Response.status(Response.Status.FORBIDDEN).build();
    }

    @POST
    @Produces("text/plain")
    public Response postmail(MultipartFormDataInput input) {
        LOGGER.info("Postmail " + input.getPreamble());
        String filename = null;
        Map<String, List<InputPart>> form = input.getFormDataMap();
        if (!form.containsKey("mail")) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Usage : cat mail1.txt | curl --form mail=@- http://$URL/api/post")
                    .build();
        } else {
            List<InputPart> inputParts = form.get("mail");
            InputPart inputPart = inputParts.get(0);
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] bytes = IOUtils.toByteArray(inputPart.getBody(InputStream.class, null));
                byte[] digest = md.digest(bytes);
                filename = new Base32().encodeAsString(digest);
                Repository.singleton().insertMessage(filename, bytes);
            } catch (IOException | NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            return Response.status(Response.Status.OK).entity("OK " + filename).build();
        }
    }
}
