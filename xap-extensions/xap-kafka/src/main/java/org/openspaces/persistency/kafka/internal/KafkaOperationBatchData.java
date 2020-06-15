package org.openspaces.persistency.kafka.internal;

import com.gigaspaces.api.InternalApi;
import com.gigaspaces.sync.DataSyncOperation;
import com.gigaspaces.sync.OperationsBatchData;
import com.gigaspaces.sync.SynchronizationSourceDetails;

@InternalApi
public class KafkaOperationBatchData implements OperationsBatchData, KafkaEndpointData{

    private static final long serialVersionUID = 258825999388261637L;

    private final DataSyncOperation[] batchDataItems;
    private final SpaceSyncEndpointMethod spaceSyncEndpointMethod;
    private final SynchronizationSourceDetails synchronizationSourceDetails;

    public KafkaOperationBatchData(OperationsBatchData operationsBatchData, SpaceSyncEndpointMethod spaceSyncEndpointMethod) {
        this.batchDataItems = convertDataSyncOperations(operationsBatchData.getBatchDataItems());
        this.spaceSyncEndpointMethod = spaceSyncEndpointMethod;
        this.synchronizationSourceDetails = new KafkaSynchronizationSourceDetails(operationsBatchData.getSourceDetails());
    }

    @Override
    public DataSyncOperation[] getBatchDataItems() {
        return batchDataItems;
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
