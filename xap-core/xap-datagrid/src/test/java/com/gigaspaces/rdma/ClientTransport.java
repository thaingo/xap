package com.gigaspaces.rdma;

import com.ibm.disni.RdmaActiveEndpoint;
import com.ibm.disni.util.DiSNILogger;
import com.ibm.disni.verbs.*;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

// @todo - handle timeouts ?

public class ClientTransport {

    private final RdmaSender rdmaSender;
    private ConcurrentHashMap<Long, CompletableFuture<RdmaMsg>> repMap = new ConcurrentHashMap<>();
    private AtomicLong nextId = new AtomicLong(0);
    private Client.CustomClientEndpoint endpoint;
    private final int BUFFER_SIZE = 1000;
    private final ByteBuffer recv_buf = ByteBuffer.allocateDirect(BUFFER_SIZE);
    private static final LinkedList<ByteBuffer> resources = new LinkedList<>();
    private final ArrayBlockingQueue<RdmaMsg> writeRequests = new ArrayBlockingQueue<>(100);
    private final ArrayBlockingQueue<IbvWC> recvEventQueue = new ArrayBlockingQueue<>(100);
    private final ExecutorService recvHandler = Executors.newFixedThreadPool(1);
    private final ExecutorService sendHandler = Executors.newFixedThreadPool(1);
    private final RdmaResourceManager resourceManager;

    public ClientTransport(Client.CustomClientEndpoint endpoint) throws IOException {
        this.endpoint = endpoint;

        resourceManager = new RdmaResourceManager(endpoint, 5, 100);

        recvHandler.submit(new RdmaReceiver(recvEventQueue, repMap, endpoint));
        rdmaSender = new RdmaSender(resourceManager, writeRequests);
        sendHandler.submit(rdmaSender);
        try {
            endpoint.postRecv(endpoint.getWrList_recv()).execute().free();
        } catch (IOException e) {
            e.printStackTrace();//TODO
        }
    }

    public CompletableFuture<RdmaMsg> send(RdmaMsg req) {
        long id = nextId.incrementAndGet();
        req.setId(id);
        CompletableFuture<RdmaMsg> future = new CompletableFuture<>();
        repMap.put(id, future);
        future.whenComplete((T, throwable) -> repMap.remove(id));
        try {
            writeRequests.add(req);
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    static SVCPostSend rdmaSendBuffer(long id, ByteBuffer buffer, RdmaActiveEndpoint endpoint) throws IOException {
        IbvMr mr = endpoint.registerMemory(buffer).execute().free().getMr();
        LinkedList<IbvSendWR> wr_list = createSendWorkRequest(id, mr);
        return endpoint.postSend(wr_list);
    }

    private static LinkedList<IbvSendWR> createSendWorkRequest(long id, IbvMr mr) {
        LinkedList<IbvSendWR> wr_list = new LinkedList<>();
        IbvSendWR sendWR = new IbvSendWR();
        sendWR.setWr_id(id);
        //@todo send id in the envelop
        LinkedList<IbvSge> sgeLinkedList = new LinkedList<>();
        IbvSge sge = new IbvSge();
        sge.setAddr(mr.getAddr());
        sge.setLength(mr.getLength());
        sge.setLkey(mr.getLkey());
        sgeLinkedList.add(sge);
        sendWR.setSg_list(sgeLinkedList);
        sendWR.setOpcode(IbvSendWR.IBV_WR_SEND);
        sendWR.setSend_flags(IbvSendWR.IBV_SEND_SIGNALED);
        wr_list.add(sendWR);
        return wr_list;
    }

    private LinkedList<IbvRecvWR> createRecvWorkRequest(long id, IbvMr mr) {
        LinkedList<IbvRecvWR> wr_list = new LinkedList<>();
        IbvRecvWR recvWR = new IbvRecvWR();
        recvWR.setWr_id(id);
        LinkedList<IbvSge> sgeLinkedList = new LinkedList<>();
        IbvSge sge = new IbvSge();
        sge.setAddr(mr.getAddr());
        sge.setLength(mr.getLength());
        sge.setLkey(mr.getLkey());
        sgeLinkedList.add(sge);
        recvWR.setSg_list(sgeLinkedList);
        wr_list.add(recvWR);
        return wr_list;
    }

    private ByteBuffer serializeToBuffer(RdmaMsg req, long reqId) throws IOException {
        DiSNILogger.getLogger().info("CLIENT SERIALIZE: reqId = "+reqId);
        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bytesOut);
        oos.writeObject(req);
        oos.flush();
        byte[] bytes = bytesOut.toByteArray();
        bytesOut.close();
        oos.close();
        ByteBuffer direct = ByteBuffer.allocateDirect(bytes.length + 8);
        direct.putLong(reqId);
        direct.put(bytes);
        resources.add(direct);
        return direct;
    }

    static void serializeToBuffer(ByteBuffer buffer, RdmaMsg req, long reqId) throws IOException {
        DiSNILogger.getLogger().info("CLIENT SERIALIZE: reqId = "+reqId);
        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bytesOut);
        oos.writeObject(req);
        oos.flush();
        byte[] bytes = bytesOut.toByteArray();
        bytesOut.close();
        oos.close();
        buffer.putLong(reqId);
        buffer.put(bytes);
    }

    public void onCompletionEvent(IbvWC event) throws IOException {

        if (IbvWC.IbvWcOpcode.valueOf(event.getOpcode()).equals(IbvWC.IbvWcOpcode.IBV_WC_SEND)) {
           resourceManager.releaseResource((short) event.getWr_id());
        }
        if (IbvWC.IbvWcOpcode.valueOf(event.getOpcode()).equals(IbvWC.IbvWcOpcode.IBV_WC_RECV)) {
            recvEventQueue.add(event); //TODO mybe offer? protect capacity
        }
    }

    static RdmaMsg readResponse(ByteBuffer buffer) throws IOException, ClassNotFoundException {
        byte[] arr = new byte[buffer.remaining()];
        buffer.get(arr);
        buffer.clear();
        try (ByteArrayInputStream ba = new ByteArrayInputStream(arr); ObjectInputStream in = new ObjectInputStream(ba)) {
            return (RdmaMsg) in.readObject();
        } catch (Exception e) {
            DiSNILogger.getLogger().error(" failed to read object from stream", e);
            throw e;
        }

    }
}
