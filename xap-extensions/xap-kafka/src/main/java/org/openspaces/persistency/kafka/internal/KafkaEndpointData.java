package org.openspaces.persistency.kafka.internal;

import com.gigaspaces.api.InternalApi;
import com.gigaspaces.sync.DataSyncOperation;

import java.io.Serializable;
import java.util.Arrays;

@InternalApi
public interface KafkaEndpointData extends Serializable {
    SpaceSyncEndpointMethod getSyncEndpointMethod();

    default DataSyncOperation[] convertDataSyncOperations(DataSyncOperation[] dataSyncOperations){
        return Arrays.stream(dataSyncOperations).map(KafkaDataSyncOperation::new).toArray(DataSyncOperation[]::new);
    }
}
