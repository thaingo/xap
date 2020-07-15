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

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class SpaceCopyChunksExecutor extends SpaceActionExecutor {

    @Override
    public SpaceResponseInfo execute(SpaceImpl space, SpaceRequestInfo requestInfo) {
        int queueSize = 10000;
        int batchSize = 1000;
        int threadCount = 10;

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        BlockingQueue<Batch> batchQueue = new ArrayBlockingQueue<>(queueSize);
        AtomicBoolean finished = new AtomicBoolean(false);
        CopyChunksRequestInfo info = (CopyChunksRequestInfo) requestInfo;
        CopyChunksResponseInfo responseInfo = new CopyChunksResponseInfo(info.getInstanceIds().keySet());
        //TODO - copy notify templates
        //TODO - register types
        try {
            HashMap<Integer, ISpaceProxy> proxyMap = createProxyMap(info.getSpaceName(), info.getInstanceIds(), info.getToken());
            for (int i = 0; i < threadCount; i++) {
                executorService.submit(new CopyChunksConsumer(proxyMap,
                        batchQueue, finished, responseInfo));
            }
            CopyChunksAggregator aggregator = new CopyChunksAggregator(info.getNewMap(), batchQueue, batchSize);
            EmptyQueryPacket queryPacket = new EmptyQueryPacket();
            queryPacket.setQueryResultType(QueryResultTypeInternal.NOT_SET);
            space.getEngine().aggregate(queryPacket, Collections.singletonList(aggregator), Modifiers.NONE, requestInfo.getSpaceContext());
            aggregator.getIntermediateResult();
            finished.set(true);
            executorService.shutdown();
            executorService.awaitTermination(5, TimeUnit.MINUTES);
        } catch (Exception e) {
            responseInfo.setException(new IOException("Copy chunks executor failed", e));
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
