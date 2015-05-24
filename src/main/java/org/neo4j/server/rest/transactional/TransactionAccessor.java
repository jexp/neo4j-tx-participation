package org.neo4j.server.rest.transactional;

import org.neo4j.graphdb.NotInTransactionException;
import org.neo4j.server.rest.transactional.error.TransactionLifecycleException;

import java.lang.reflect.Field;

/**
 * @author mh
 * @since 23.05.15
 */
public class TransactionAccessor {

    private final Field contextField;
    private final TransactionRegistry transactionRegistry;

    public TransactionAccessor(TransactionRegistry transactionRegistry) {
        this.transactionRegistry = transactionRegistry;
        try {
            contextField = TransactionHandle.class.getDeclaredField("context");
            contextField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Error accessing TransactionHandle.context",e);
        }
    }

    public TransactionHandle resumeTransaction(Long txId) {
        if (txId == null) return null;
        TransactionHandle handle = getTransactionHandle(txId);
        getContext(txId, handle).resumeSinceTransactionsAreStillThreadBound();
        return handle;
    }

    public void suspendTransaction(Long txId,TransactionHandle handle) {
        if (txId == null) return;
        getContext(txId,handle).suspendSinceTransactionsAreStillThreadBound();
        transactionRegistry.release(txId, handle);
    }

    private TransitionalTxManagementKernelTransaction getContext(Long txId, TransactionHandle handle) {
        try {
            TransitionalTxManagementKernelTransaction context =
                    (TransitionalTxManagementKernelTransaction)contextField.get(handle);
            if (context == null) throw new NotInTransactionException("No active transaction with id " +txId);
            return context;
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Error Handling Transaction "+txId,e);
        }
    }

    private TransactionHandle getTransactionHandle(long txId) {
        try {
            return transactionRegistry.acquire(txId);
        } catch (TransactionLifecycleException e) {
            throw new RuntimeException("Error Handling Transaction "+txId,e);
        }
    }
}
