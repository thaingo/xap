package org.openspaces.persistency.kafka.internal;

import com.gigaspaces.api.InternalApi;
import com.gigaspaces.internal.metadata.ITypeDesc;
import com.gigaspaces.metadata.SpaceTypeDescriptor;
import com.gigaspaces.sync.DataSyncOperation;
import com.gigaspaces.sync.IntroduceTypeData;
import com.gigaspaces.sync.OperationsBatchData;
import com.gigaspaces.sync.SynchronizationSourceDetails;

@InternalApi
public class KafkaIntroduceTypeData implements IntroduceTypeData, KafkaEndpointData{

    private static final long serialVersionUID = 258825999388261637L;

    private final ITypeDesc spaceTypeDescritor;
    private final SpaceSyncEndpointMethod spaceSyncEndpointMethod;

    public KafkaIntroduceTypeData(IntroduceTypeData introduceTypeData, SpaceSyncEndpointMethod spaceSyncEndpointMethod) {
        this.spaceTypeDescritor = (ITypeDesc) introduceTypeData.getTypeDescriptor();
        this.spaceSyncEndpointMethod = spaceSyncEndpointMethod;
    }

    @Override
    public SpaceTypeDescriptor getTypeDescriptor() {
        return spaceTypeDescritor;
    }

    @Override
    public SpaceSyncEndpointMethod getSyncEndpointMethod() {
        return spaceSyncEndpointMethod;
    }
}
