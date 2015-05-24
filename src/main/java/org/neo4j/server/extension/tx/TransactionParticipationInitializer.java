/**
 * Copyright (c) 2002-2014 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.server.extension.tx;

import org.apache.commons.configuration.Configuration;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.server.AbstractNeoServer;
import org.neo4j.server.NeoServer;
import org.neo4j.server.plugins.Injectable;
import org.neo4j.server.plugins.SPIPluginLifecycle;
import org.neo4j.server.rest.transactional.TransactionAccessor;
import org.neo4j.server.rest.transactional.TransactionRegistry;
import org.neo4j.server.web.WebServer;

import java.util.Arrays;
import java.util.Collection;

import static org.neo4j.server.extension.tx.TransactionConstants.FILTER_SPEC;

public class TransactionParticipationInitializer implements SPIPluginLifecycle {
    private TransactionParticipationFilter transactionParticipationFilter;
    private WebServer webServer;

    @Override
    public Collection<Injectable<?>> start(final GraphDatabaseService graphDatabaseService, final Configuration config) {
        throw new IllegalAccessError();
    }

    @Override
    public void stop() {
        if (transactionParticipationFilter != null) {
            webServer.removeFilter(transactionParticipationFilter, FILTER_SPEC);
        }
    }

    @Override
    public Collection<Injectable<?>> start(final NeoServer neoServer) {
        webServer = getWebServer(neoServer);
        TransactionRegistry transactionRegistry = neoServer.getTransactionRegistry();

        TransactionAccessor transactionAccessor = new TransactionAccessor(transactionRegistry);
        transactionParticipationFilter = new TransactionParticipationFilter(transactionAccessor);
        webServer.addFilter(transactionParticipationFilter, FILTER_SPEC);

        return Arrays.asList();
    }

    private WebServer getWebServer(final NeoServer neoServer) {
        if (neoServer instanceof AbstractNeoServer) {
            return ((AbstractNeoServer) neoServer).getWebServer();
        }
        throw new IllegalArgumentException("expected AbstractNeoServer");
    }
}
