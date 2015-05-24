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

import org.junit.Rule;
import org.junit.Test;
import org.neo4j.harness.junit.Neo4jRule;
import org.neo4j.test.server.HTTP;

import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;
import static org.neo4j.helpers.collection.IteratorUtil.asList;
import static org.neo4j.helpers.collection.MapUtil.stringMap;
import static org.neo4j.server.extension.tx.TransactionConstants.TX_HEADER;

public class TransactionParticipationTest {

    @Rule
    public Neo4jRule neo4j = new Neo4jRule()
            .withExtension( "/tx", getClass().getPackage().getName() );

    @Test
    public void testExecuteCypher() throws Exception {
        String txUrl = url("/db/data/transaction/commit");
        HTTP.Response response = HTTP.POST(txUrl, HTTP.RawPayload.quotedJson("{\"statements\":[{\"statement\":\"create (n:Person) return n\"}]}"));
        assertEquals(200, response.status());
    }

    @Test
    public void testExecuteCypherTransaction() throws Exception {
        String txUrl = url("/db/data/transaction");
        HTTP.Response response = HTTP.POST(txUrl);
        assertEquals(201, response.status());
        String location = response.location();
        System.out.println("location = " + location);
        assertEquals(true,location.matches(txUrl+"/\\d+"));
        HTTP.DELETE(location);
    }

    @Test
    public void testParticipateReadNodeInCypherTransaction() throws Exception {
        HTTP.Response resp = openTransactionAndExecute("create (n:Person) return id(n)");

        String nodeUri = url("/db/data/node/" + extractNodeIdFromResult(resp));

        HTTP.Response response2 = HTTP.withHeaders(TX_HEADER, getTxId(resp.location())).GET(nodeUri);
        assertEquals(200, response2.status());
        HTTP.DELETE(resp.location());
    }

    @Test
    public void testParticipateAddNodeToIndexInCypherTransaction() throws Exception {

        setupFullTextIndex("people");
        HTTP.Response resp = openTransactionAndExecute("create (n:Person) return id(n)");
        String txUrl = resp.location();

        Object nodeId = extractNodeIdFromResult(resp);
        String txId = getTxId(txUrl);
        addNodeToIndex(nodeId, txId, "people", "bio", "Jack was born in London in 1925 ...");

        commitTx(txUrl);

        assertEquals(200, HTTP.GET(url("/db/data/index/node/people/bio/London")).status());

        HTTP.Response response3 = openTransactionAndExecute("planner rule start n=node:people(\\\"bio:London\\\") return id(n)");
        assertEquals(nodeId, extractNodeIdFromResult(response3));
        commitTx(response3.location());

    }

    private void commitTx(String txUrl) {
        HTTP.Response committed = HTTP.POST(txUrl + "/commit");
        assertEquals(200, committed.status());
    }

    private void addNodeToIndex(Object nodeId, String txId, final String indexName, final String key, String text) {
        String indexUri = url("/db/data/index/node/" + indexName);
        String nodeUri = url("/db/data/node/" + nodeId);

        String addToIndexPayload = "{ \"value\" : \""+ text +"\", \"uri\" : \"" + nodeUri + "\", \"key\" : \"" + key + "\" }";
        HTTP.Response response2 = HTTP.withHeaders(TX_HEADER, txId).
                POST(indexUri, HTTP.RawPayload.quotedJson(addToIndexPayload));
        assertEquals(201, response2.status());
    }

    private void setupFullTextIndex(String name) {
        HTTP.RawPayload payload = HTTP.RawPayload.quotedJson("{ \"name\" : \""+name+"\", \"config\" : { \"type\" : \"fulltext\", \"provider\" : \"lucene\" } }");
        String indexUrl = url("/db/data/index/node/");
        assertEquals(201, HTTP.POST(indexUrl, payload).status());
    }

    private HTTP.Response openTransactionAndExecute(final String statement) {
        String path = "/db/data/transaction";
        String txUrl = url(path);
        HTTP.RawPayload payload = HTTP.RawPayload.quotedJson("{\"statements\":[{\"statement\":\"" + statement + "\"}]}");
        HTTP.Response response = HTTP.POST(txUrl, payload);
        assertEquals(201,response.status());
        return response;
    }

    private String url(String path) {
        return neo4j.httpURI().resolve(path).toString();
    }

    private Object extractNodeIdFromResult(HTTP.Response response) {
        Map result = response.content();
        return ((List)((Map)((List)((Map)((List)result.get("results")).get(0)).get("data")).get(0)).get("row")).get(0);
    }

    private String getTxId(String location) {
        return location.substring(location.lastIndexOf("/")+1);
    }
}
