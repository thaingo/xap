package com.gigaspaces.internal.server.space.repartitioning;

import com.gigaspaces.admin.quiesce.QuiesceToken;
import com.gigaspaces.client.SpaceProxyFactory;
import com.gigaspaces.internal.client.QueryResultTypeInternal;
import com.gigaspaces.internal.client.spaceproxy.ISpaceProxy;
import com.gigaspaces.internal.server.space.SpaceImpl;
import com.gigaspaces.internal.server.space.executors.SpaceActionExecutor;
import com.gigaspaces.internal.space.requests.SpaceRequestInfo;
import com.gigaspaces.internal.space.responses.SpaceResponseInfo;
import com.gigaspaces.internal.transport.EmptyQueryPacket;
import com.j_spaces.core.IJSpace;
import com.j_spaces.core.client.FinderException;
import com.j_spaces.core.client.Modifiers;

import java.net.MalformedURLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class SpaceCopyChunksExecutor extends SpaceActionExecutor {

    private static final int QUEUE_SIZE = 10000;
    private static final int BATCH_SIZE = 1000;
    private static final int THREAD_COUNT = 10;

    private ExecutorService executorService = Executors.newFixedThreadPool(SpaceCopyChunksExecutor.THREAD_COUNT);
    private BlockingQueue<Batch> batchQueue = new ArrayBlockingQueue<>(SpaceCopyChunksExecutor.QUEUE_SIZE);
    private AtomicBoolean finished = new AtomicBoolean(false);

    @Override
    public SpaceResponseInfo execute(SpaceImpl space, SpaceRequestInfo requestInfo) {
        CopyChunksRequestInfo info = (CopyChunksRequestInfo) requestInfo;
        CopyChunksResponseInfo responseInfo = new CopyChunksResponseInfo(info.getInstanceIds().keySet());
        //TODO - copy notify templates
        //TODO - register types
        try {
            HashMap<Integer, ISpaceProxy> proxyMap = createProxyMap(info.getSpaceName(), info.getInstanceIds(), info.getToken());
            for (int i = 0; i < SpaceCopyChunksExecutor.THREAD_COUNT; i++) {
                executorService.submit(new CopyChunksConsumer(proxyMap,
                        batchQueue, finished, responseInfo));
            }
            CopyChunksAggregator aggregator = new CopyChunksAggregator(info.getNewMap(), batchQueue, SpaceCopyChunksExecutor.BATCH_SIZE);
            EmptyQueryPacket queryPacket = new EmptyQueryPacket();
            queryPacket.setQueryResultType(QueryResultTypeInternal.NOT_SET);
            space.getEngine().aggregate(queryPacket, Collections.singletonList(aggregator), Modifiers.NONE, requestInfo.getSpaceContext());
            aggregator.getIntermediateResult();
            finished.set(true);
            executorService.shutdown();
            executorService.awaitTermination(5, TimeUnit.MINUTES);
        } catch (Exception e) {
            responseInfo.getPartitionException().put((short) space.getPartitionIdOneBased(), e);
        }
        return responseInfo;
    }

    private HashMap<Integer, ISpaceProxy> createProxyMap(String spaceName, Map<Integer, String> instanceIds, QuiesceToken token) throws MalformedURLException, FinderException {
        HashMap<Integer, ISpaceProxy> proxyMap = new HashMap<>(instanceIds.size());
        SpaceProxyFactory proxyFactory = new SpaceProxyFactory();
        for (Map.Entry<Integer, String> entry : instanceIds.entrySet()) {
            proxyFactory.setInstanceId(entry.getValue());
            ISpaceProxy space = proxyFactory.createSpaceProxy(spaceName, true);
            IJSpace nonClusteredProxy = space.getDirectProxy().getNonClusteredProxy();
            nonClusteredProxy.getDirectProxy().setQuiesceToken(token);
            proxyMap.put(entry.getKey(), (ISpaceProxy) nonClusteredProxy);
        }
        return proxyMap;
    }

}
