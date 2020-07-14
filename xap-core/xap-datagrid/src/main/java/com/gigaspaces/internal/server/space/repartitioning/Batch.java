package com.gigaspaces.internal.server.space.repartitioning;

import com.gigaspaces.internal.transport.IEntryPacket;

import java.util.List;

class Batch {
    private final int partitionId;
    private final List<IEntryPacket> entries;

    Batch(int partitionId, List<IEntryPacket> entries) {
        this.partitionId = partitionId;
        this.entries = entries;
    }

    public int getPartitionId() {
        return partitionId;
    }

    public List<IEntryPacket> getEntries() {
        return entries;
    }
}
