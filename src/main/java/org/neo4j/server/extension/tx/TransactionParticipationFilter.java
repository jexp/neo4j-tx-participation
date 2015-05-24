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

import org.neo4j.server.rest.transactional.*;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

import static org.neo4j.server.extension.tx.TransactionConstants.TX_HEADER;

/**
 * @author tbaum
 * @since 23.01.11
 */
public class TransactionParticipationFilter implements Filter {

    private final TransactionAccessor transactionAccessor;

    public TransactionParticipationFilter(TransactionAccessor transactionAccessor) {
        this.transactionAccessor = transactionAccessor;
    }

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {
    }

    public void doFilter(final ServletRequest req, final ServletResponse res, final FilterChain chain)
            throws ServletException, IOException {

        Long txId = getTxId(req);
        TransactionHandle handle = transactionAccessor.resumeTransaction(txId);
        chain.doFilter(req, res);
        transactionAccessor.suspendTransaction(txId,handle);
    }

    private Long getTxId(ServletRequest req) {
        if (!(req instanceof HttpServletRequest)) return null;

        final String header = ((HttpServletRequest) req).getHeader(TX_HEADER);
        return (header != null) ? Long.parseLong(header) : null;
    }

    public void destroy() {
    }
}
