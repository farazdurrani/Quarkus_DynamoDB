package org.acme.dynamodb;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/storesessiondata")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class StoreSessionData {

    @Inject
    SessionService service;

    @GET
    public List<ResponseData> getSingle(Request req) {
	return service.get(req);
    }

    @POST
    public Response add(ShareData data) {
	service.add(data);
	return Response.ok().build();
    }
}