package org.openspaces.persistency.kafka.internal;

import com.gigaspaces.sync.SynchronizationSourceDetails;

import java.io.Serializable;

public class KafkaSynchronizationSourceDetails implements SynchronizationSourceDetails, Serializable {
    private static final long serialVersionUID = 8571013274007308800L;
    private final String name;

    public KafkaSynchronizationSourceDetails(SynchronizationSourceDetails synchronizationSourceDetails) {
        name = synchronizationSourceDetails.getName();
    }

    @Override
    public String getName() {
        return null;
    }
}
