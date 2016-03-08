package com.gmail.lionelg3.nxtraces.rest;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

/**
 * Collect mail traces from Unix systems
 *
 * @author Lionel G (lionelg3@gmail.com)
 *         Date: 18/12/15, Time: 16:43
 *         <p>
 *         curl http://localhost:8080/api/ping
 *         <p>
 */
@Path("/ping")
public class PingService {

    @GET
    @Produces("application/json")
    public JsonObject ping() {
        JsonObjectBuilder reponse = Json.createObjectBuilder();
        reponse.add("api", "ping success");
        return reponse.build();
    }
}
