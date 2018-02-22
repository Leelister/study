package org.rpc.remoting.netty;

import io.netty.util.internal.SystemPropertyUtil;

public class NettySystemConfig {

    public static final int CLIENT_ONEWAY_SEMAPHORE_VALUE = SystemPropertyUtil.getInt("client.oneway.semaphore.value", 65535);
    public static final int CLIENT_ASYNC_SEMAPHORE_VALUE = SystemPropertyUtil.getInt("client.async.semaphore.value", 65535);
    public static final int IO_IDLE_ALL_TIME_SECONDS = SystemPropertyUtil.getInt("io.all.idle.time.seconds", 120);

    public static int socketSndbufSize = SystemPropertyUtil.getInt("socket.send.buffer.size", 65535);
    public static int socketRcvbufSize = SystemPropertyUtil.getInt("socket.receive.buffer.size", 65535);;
}
