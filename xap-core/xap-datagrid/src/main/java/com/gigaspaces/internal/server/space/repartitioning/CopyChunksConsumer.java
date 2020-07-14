package com.gigaspaces.internal.server.space.repartitioning;

import com.gigaspaces.internal.client.spaceproxy.ISpaceProxy;
import com.j_spaces.core.client.Modifiers;
import net.jini.core.lease.Lease;
import net.jini.core.transaction.TransactionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.rmi.RemoteException;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class CopyChunksConsumer implements Runnable {

    private static Logger logger = LoggerFactory.getLogger(CopyChunksResponseInfo.class.getName());

    private Map<Integer, ISpaceProxy> proxyMap;
    private BlockingQueue<Batch> batchQueue;
    private AtomicBoolean finished;
    private CopyChunksResponseInfo responseInfo;

    CopyChunksConsumer(Map<Integer, ISpaceProxy> proxyMap, BlockingQueue<Batch> batchQueue, AtomicBoolean finished, CopyChunksResponseInfo responseInfo) {
        this.proxyMap = proxyMap;
        this.batchQueue = batchQueue;
        this.finished = finished;
        this.responseInfo = responseInfo;
    }

    @Override
    public void run() {
        while (!finished.get() || !batchQueue.isEmpty()) {
            Batch batch;
            try {
                batch = batchQueue.poll(5, TimeUnit.SECONDS);
                if(batch != null) {
                    try {
                        ISpaceProxy spaceProxy = proxyMap.get(batch.getPartitionId());
                        spaceProxy.writeMultiple(batch.getEntries().toArray(), null, Lease.FOREVER, Modifiers.BACKUP_ONLY);
                        responseInfo.getMovedToPartition().get((short) batch.getPartitionId()).addAndGet(batch.getEntries().size());
                    } catch (RemoteException | TransactionException e) {
                        responseInfo.getPartitionException().put((short) batch.getPartitionId(), e);
                    }
                }
            } catch (InterruptedException ignored) {
                break;
            }
        }
    }
}
