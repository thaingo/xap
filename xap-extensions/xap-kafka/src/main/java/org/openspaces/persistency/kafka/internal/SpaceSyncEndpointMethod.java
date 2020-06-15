package org.openspaces.persistency.kafka.internal;

import com.gigaspaces.api.InternalApi;

@InternalApi
public enum SpaceSyncEndpointMethod {
    onTransactionConsolidationFailure,
    onTransactionSynchronization,
    onOperationsBatchSynchronization,
    onAddIndex,
    onIntroduceType
}
