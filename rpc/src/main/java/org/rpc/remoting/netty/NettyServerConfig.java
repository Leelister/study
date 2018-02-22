package org.rpc.remoting.netty;

public class NettyServerConfig {

    private int serverOnewaySemaphoreValue = 256;
    private int serverAsyncSemaphoreValue = 64;

    private int idleAllSeconds = NettySystemConfig.IO_IDLE_ALL_TIME_SECONDS;

    private int serverSocketSndBufSize = NettySystemConfig.socketSndbufSize;
    private int serverSocketRcvBufSize = NettySystemConfig.socketRcvbufSize;

    private int port = 9180;

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getIdleAllSeconds() {
        return idleAllSeconds;
    }

    public void setIdleAllSeconds(int idleAllSeconds) {
        this.idleAllSeconds = idleAllSeconds;
    }

    public int getServerSocketSndBufSize() {
        return serverSocketSndBufSize;
    }

    public void setServerSocketSndBufSize(int serverSocketSndBufSize) {
        this.serverSocketSndBufSize = serverSocketSndBufSize;
    }

    public int getServerSocketRcvBufSize() {
        return serverSocketRcvBufSize;
    }

    public void setServerSocketRcvBufSize(int serverSocketRcvBufSize) {
        this.serverSocketRcvBufSize = serverSocketRcvBufSize;
    }

    public int getServerOnewaySemaphoreValue() {
        return serverOnewaySemaphoreValue;
    }

    public void setServerOnewaySemaphoreValue(int serverOnewaySemaphoreValue) {
        this.serverOnewaySemaphoreValue = serverOnewaySemaphoreValue;
    }

    public int getServerAsyncSemaphoreValue() {
        return serverAsyncSemaphoreValue;
    }

    public void setServerAsyncSemaphoreValue(int serverAsyncSemaphoreValue) {
        this.serverAsyncSemaphoreValue = serverAsyncSemaphoreValue;
    }
}
