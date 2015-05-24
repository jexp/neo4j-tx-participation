package org.neo4j.server.extension.tx;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

/**
 * @author mh
 * @since 23.05.15
 */
@Path("/")
public class TransactionParticipationResource {
    @GET
    public String index() {
        return "Ok";
    }
}
