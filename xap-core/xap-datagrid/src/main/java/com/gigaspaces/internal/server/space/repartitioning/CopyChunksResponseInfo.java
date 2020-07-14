package com.gigaspaces.internal.server.space.repartitioning;

import com.gigaspaces.internal.io.IOUtils;
import com.gigaspaces.internal.space.responses.SpaceResponseInfo;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class CopyChunksResponseInfo implements SpaceResponseInfo {

    private int partitionId;
    private Map<Short,Exception> partitionException;
    private Map<Short, AtomicInteger> movedToPartition;


    @SuppressWarnings("WeakerAccess")
    public CopyChunksResponseInfo() {
    }

    CopyChunksResponseInfo(Set<Integer> keys) {
        this.movedToPartition = new HashMap<>(keys.size());
        this.partitionException = new ConcurrentHashMap<>(keys.size());
        for (int key : keys) {
            this.movedToPartition.put((short) key, new AtomicInteger(0));
        }
    }

    public Map<Short, Exception> getPartitionException() {
        return partitionException;
    }

    public Map<Short, AtomicInteger> getMovedToPartition() {
        return movedToPartition;
    }

    public int getPartitionId() {
        return partitionId;
    }

    public void setPartitionId(int partitionId) {
        this.partitionId = partitionId;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        IOUtils.writeInt(out, partitionId);
        IOUtils.writeShort(out, (short) partitionException.size());
        for (Map.Entry<Short, Exception> entry : partitionException.entrySet()) {
            IOUtils.writeShort(out, entry.getKey());
            IOUtils.writeObject(out, entry.getValue());
        }
        IOUtils.writeShort(out, (short) movedToPartition.size());
        for (Map.Entry<Short, AtomicInteger> entry : movedToPartition.entrySet()) {
            IOUtils.writeShort(out, entry.getKey());
            IOUtils.writeInt(out, entry.getValue().get());
        }
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        this.partitionId = IOUtils.readInt(in);
        short size = IOUtils.readShort(in);
        if (size > 0) {
            this.partitionException = new HashMap<>(size);
            for (int i = 0; i < size; i++) {
                this.partitionException.put(IOUtils.readShort(in), IOUtils.readObject(in));
            }
        }
        size = IOUtils.readShort(in);
        if (size > 0) {
            this.movedToPartition = new HashMap<>(size);
            for (int i = 0; i < size; i++) {
                this.movedToPartition.put(IOUtils.readShort(in), new AtomicInteger(IOUtils.readInt(in)));
            }
        }
    }
}
