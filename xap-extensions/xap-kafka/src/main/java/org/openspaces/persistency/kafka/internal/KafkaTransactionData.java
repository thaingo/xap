package org.openspaces.persistency.kafka.internal;

import com.gigaspaces.api.InternalApi;
import com.gigaspaces.sync.DataSyncOperation;
import com.gigaspaces.sync.SynchronizationSourceDetails;
import com.gigaspaces.sync.TransactionData;
import com.gigaspaces.transaction.ConsolidatedDistributedTransactionMetaData;
import com.gigaspaces.transaction.TransactionParticipantMetaData;

@InternalApi
public class KafkaTransactionData implements TransactionData, KafkaEndpointData{

    private static final long serialVersionUID = 258825999388261637L;

    private final DataSyncOperation[] dataSyncOperations;
    private final SpaceSyncEndpointMethod spaceSyncEndpointMethod;
    private final SynchronizationSourceDetails synchronizationSourceDetails;

    public KafkaTransactionData(TransactionData transactionData, SpaceSyncEndpointMethod spaceSyncEndpointMethod) {
        this.dataSyncOperations = convertDataSyncOperations(transactionData.getTransactionParticipantDataItems());
        this.spaceSyncEndpointMethod = spaceSyncEndpointMethod;
        synchronizationSourceDetails = new KafkaSynchronizationSourceDetails(transactionData.getSourceDetails());
    }


    @Override
    public boolean isConsolidated() {
        return false;
    }

    @Override
    public TransactionParticipantMetaData getTransactionParticipantMetaData() {
        return null;
    }

    @Override
    public ConsolidatedDistributedTransactionMetaData getConsolidatedDistributedTransactionMetaData() {
        return null;
    }

    @Override
    public DataSyncOperation[] getTransactionParticipantDataItems() {
        return dataSyncOperations;
    }

    @Override
    public SynchronizationSourceDetails getSourceDetails() {
        return synchronizationSourceDetails;
    }

    @Override
    public SpaceSyncEndpointMethod getSyncEndpointMethod() {
        return spaceSyncEndpointMethod;
    }
}
