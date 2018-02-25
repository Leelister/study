package org.rpc.remoting.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import org.rpc.comm.UnresolvedAddress;
import org.rpc.exception.RemotingConnectException;
import org.rpc.exception.RemotingException;
import org.rpc.remoting.api.*;
import org.rpc.remoting.api.channel.ChannelGroup;
import org.rpc.remoting.api.channel.DirectoryChannelGroup;
import org.rpc.remoting.api.payload.ByteHolder;
import org.rpc.remoting.api.payload.RequestBytes;
import org.rpc.remoting.api.payload.ResponseBytes;
import org.rpc.remoting.channel.NettyChannelGroup;
import org.rpc.remoting.netty.event.ChannelEvent;
import org.rpc.remoting.netty.event.ChannelEventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.util.concurrent.*;

import static com.google.common.base.Preconditions.checkNotNull;

public class NettyClient extends NettyServiceAbstract implements RpcClient {

    private static final Logger logger = LoggerFactory.getLogger(NettyClient.class);

    private final Bootstrap bootstrap = new Bootstrap();

    private final NettyEncoder encoder = new NettyEncoder();

    private final NettyConnectManageHandler nettyConnectManageHandler = new NettyConnectManageHandler();

    private final NioEventLoopGroup nioEventLoopGroupWorker = new NioEventLoopGroup();

    private final NettyClientConfig config;

    private final ConcurrentMap<String, ChannelGroup> addressGroups = new ConcurrentHashMap<>();

    private final DirectoryChannelGroup directoryChannelGroup = new DirectoryChannelGroup();

    private final ChannelEventListener channelEventListener;

    public NettyClient(NettyClientConfig config) {
        this(config, null);
    }

    public NettyClient(NettyClientConfig config, ChannelEventListener listener) {
        super(config.getClientAsyncSemaphoreValue(), config.getClientOnewaySemaphoreValue());
        this.config = config;
        this.channelEventListener = listener;
    }

    @Override
    public void connect(UnresolvedAddress address) throws InterruptedException, RemotingConnectException {
        checkNotNull(address);

        ChannelFuture future = bootstrap.connect(address.getHost(), address.getPort());

        if (future.awaitUninterruptibly(config.getConnectTimeoutMillis(), TimeUnit.MILLISECONDS)) {
            if (future.channel() != null && future.channel().isActive()) {
                future.channel().pipeline().addLast(new NettyClientHandler(address));
                group(address).addChannel(future.channel());
                logger.info("connect with: {}", future.channel());

            } else {
                throw new RemotingConnectException(address.toString());
            }
        } else {
            throw new RemotingConnectException(address.toString());
        }
    }

    @Override
    public ChannelGroup group(UnresolvedAddress address) {

        ChannelGroup group = addressGroups.get(address);
        if (group == null) {
            ChannelGroup newGroup = new NettyChannelGroup(address);
            group = addressGroups.putIfAbsent(address.toString(), newGroup);
            if (group == null) {
                group = newGroup;
            }
        }
        return group;
    }

    @Override
    public boolean hasAvailableChannelGroup(UnresolvedAddress address) {
        return group(address).isAvailable();
    }

    @Override
    public boolean addChannelGroup(Directory directory, UnresolvedAddress address) {
        ChannelGroup group = group(address);
        CopyOnWriteArrayList groups = directoryChannelGroup.find(directory);
        boolean added = groups.addIfAbsent(group);
        if (added) {
            if (logger.isInfoEnabled()) {
                logger.info("Added channel group: {} to {}.", group, directory.directory());
            }
        }
        return added;
    }

    @Override
    public boolean removeChannelGroup(Directory directory, UnresolvedAddress address) {
        ChannelGroup group = group(address);
        CopyOnWriteArrayList groups = directoryChannelGroup.find(directory);
        boolean removed = groups.remove(group);
        if (removed) {
            if (logger.isInfoEnabled()) {
                logger.info("Removed channel group: {} to {}.", group, directory.directory());
            }
        }
        return removed;
    }

    @Override
    public CopyOnWriteArrayList<ChannelGroup> directory(Directory directory) {
        return directoryChannelGroup.find(directory);
    }

    @Override
    public boolean isDirectoryAvailable(Directory directory) {
        CopyOnWriteArrayList<ChannelGroup> groups = directory(directory);
        for (ChannelGroup g : groups) {
            if (g.isAvailable()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ResponseBytes invokeSync(Channel channel, RequestBytes request, long timeoutMillis) throws RemotingException, InterruptedException {
        return invokeSync0(channel, request, timeoutMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public void invokeAsync(Channel channel, RequestBytes request, long timeoutMillis, InvokeCallback<ResponseBytes> invokeCallback) throws RemotingException, InterruptedException {
        invokeAsync0(channel, request, timeoutMillis, TimeUnit.MILLISECONDS, invokeCallback);
    }

    @Override
    public ResponseBytes invokeSync(final UnresolvedAddress address, RequestBytes request, long timeoutMillis) throws RemotingException, InterruptedException {
        return invokeSync0(group(address).next(), request, timeoutMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public void invokeAsync(final UnresolvedAddress address, RequestBytes request,
                            long timeoutMillis, InvokeCallback<ResponseBytes> invokeCallback) throws RemotingException, InterruptedException {
        invokeAsync0(group(address).next(), request, timeoutMillis, TimeUnit.MILLISECONDS, invokeCallback);
    }

    @Override
    public void registerRequestProcess(RequestProcessor requestProcessor, ExecutorService executor) {
        defaultProcessor.setA(requestProcessor);
        defaultProcessor.setB(executor);
    }

    @Override
    public void start() {
        bootstrap.group(nioEventLoopGroupWorker)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, false)
                .option(ChannelOption.SO_SNDBUF, config.getClientSocketSndBufSize())
                .option(ChannelOption.SO_RCVBUF, config.getClientSocketRcvBufSize())
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        socketChannel.pipeline().addLast(encoder);
                        socketChannel.pipeline().addLast(new NettyDecoder());
                        socketChannel.pipeline().addLast(new IdleStateHandler(0, 0, config.getIdleAllSeconds()));
                        socketChannel.pipeline().addLast(nettyConnectManageHandler);
                    }
                });

        if (channelEventListener != null) {
            new Thread(channelEventExecutor).start();
        }
    }

    @ChannelHandler.Sharable
    class NettyClientHandler extends SimpleChannelInboundHandler<ByteHolder> implements TimerTask {

        private Timer timer = new HashedWheelTimer();

        private UnresolvedAddress address;

        public NettyClientHandler(UnresolvedAddress address) {
            this.address = address;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ByteHolder msg) throws Exception {
            processMessageReceived(ctx, msg);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            timer.newTimeout(this, 2000, TimeUnit.MILLISECONDS);
        }

        @Override
        public void run(Timeout timeout) throws Exception {
            bootstrap.connect(address.getHost(), address.getPort())
                    .addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture future) throws Exception {
                            future.channel().pipeline().addLast(NettyClientHandler.this);
                            boolean isSuccess = future.isSuccess();
                            logger.warn("Reconnects with {}, {}.", address, isSuccess ? "succeed" : "failed");
                            if (!isSuccess) {
                                future.channel().pipeline().fireChannelInactive();
                            } else {
                                group(address).addChannel(future.channel());
                            }
                        }
                    });
        }
    }

    @ChannelHandler.Sharable
    class NettyConnectManageHandler extends ChannelDuplexHandler {
        @Override
        public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress,
                            ChannelPromise promise) throws Exception {
            final String local = localAddress == null ? "UNKNOWN" : localAddress.toString();
            final String remote = remoteAddress == null ? "UNKNOWN" : remoteAddress.toString();
            logger.debug("NETTY CLIENT PIPELINE: CONNECT  {} => {}", local, remote);

            super.connect(ctx, remoteAddress, localAddress, promise);

            if (NettyClient.this.channelEventListener != null) {
                NettyClient.this.putChannelEvent(new ChannelEvent(ChannelEventType.CONNECT, remote, ctx.channel()));
            }
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            final String remoteAddress = ctx.channel().remoteAddress().toString();
            logger.debug("NETTY SERVER PIPELINE: channelActive, the channel[{}]", remoteAddress);
            super.channelActive(ctx);

            if (NettyClient.this.channelEventListener != null) {
                NettyClient.this.putChannelEvent(new ChannelEvent(ChannelEventType.ACTIVE, remoteAddress, ctx.channel()));
            }
        }

        @Override
        public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
            final String remoteAddress = ctx.channel().remoteAddress().toString();
            logger.debug("NETTY CLIENT PIPELINE: DISCONNECT {}", remoteAddress);
            ctx.channel().close();
            super.disconnect(ctx, promise);

            if (NettyClient.this.channelEventListener != null) {
                NettyClient.this.putChannelEvent(new ChannelEvent(ChannelEventType.CLOSE, remoteAddress, ctx.channel()));
            }
        }

        @Override
        public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
            final String remoteAddress = ctx.channel().remoteAddress().toString();
            logger.debug("NETTY CLIENT PIPELINE: CLOSE {}", remoteAddress);
            ctx.channel().close();
            super.close(ctx, promise);

            if (NettyClient.this.channelEventListener != null) {
                NettyClient.this.putChannelEvent(new ChannelEvent(ChannelEventType.CLOSE, remoteAddress, ctx.channel()));
            }
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof IdleStateEvent) {
                IdleStateEvent event = (IdleStateEvent) evt;
                if (event.state().equals(IdleState.ALL_IDLE)) {
                    final String remoteAddress = ctx.channel().remoteAddress().toString();
                    logger.warn("NETTY CLIENT PIPELINE: IDLE exception [{}]", remoteAddress);
                    // ctx.channel().close();
                    if (NettyClient.this.channelEventListener != null) {
                        NettyClient.this
                                .putChannelEvent(new ChannelEvent(ChannelEventType.IDLE, remoteAddress, ctx.channel()));
                    }
                }
            }
            ctx.fireUserEventTriggered(evt);
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            final String remoteAddress = ctx.channel().remoteAddress().toString();
            logger.warn("NETTY CLIENT PIPELINE: exceptionCaught {}", remoteAddress);
            logger.warn("NETTY CLIENT PIPELINE: exceptionCaught exception.", cause);
            ctx.channel().close();
            if (NettyClient.this.channelEventListener != null) {
                NettyClient.this.putChannelEvent(new ChannelEvent(ChannelEventType.EXCEPTION, remoteAddress, ctx.channel()));
            }
        }
    }

    private void putChannelEvent(ChannelEvent channelEvent) {
        this.channelEventExecutor.putChannelEvent(channelEvent);
    }

    @Override
    protected ChannelEventListener getChannelEventListener() {
        return this.channelEventListener;
    }

    @Override
    public void shutdown() {

    }

}
