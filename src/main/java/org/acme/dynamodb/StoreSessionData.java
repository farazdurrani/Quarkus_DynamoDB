package org.acme.dynamodb;

import java.util.List;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/storesessiondata")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class StoreSessionData {

    @Inject
    SessionService service;

    @GET
    public List<ShareData> getAll() {
	return service.findAll();
    }

    @GET
    @Path("{name}")
    public ShareData getSingle(@PathParam("name") String name) {
	return service.get(name);
    }

    @POST
    public List<ShareData> add(ShareData data) {
	service.add(data);
	return getAll();
    }
}