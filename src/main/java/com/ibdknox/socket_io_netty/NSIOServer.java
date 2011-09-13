package com.ibdknox.socket_io_netty;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import com.ibdknox.socket_io_netty.flashpolicy.FlashPolicyServer;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NSIOServer {

    private static Logger logger = LoggerFactory.getLogger(NSIOServer.class);
    private ServerBootstrap bootstrap;
    private Channel serverChannel;
    private int port;
    private int flashPolicyPort;
    private boolean running;
    private INSIOHandler handler;
    private WebSocketServerHandler socketHandler;

    public NSIOServer(INSIOHandler handler, int port) {
        this(handler, port, 843);
    }

    public NSIOServer(INSIOHandler handler, int port, int flashPolicyPort) {
        this.port = port;
        this.handler = handler;
        this.running = false;
        this.flashPolicyPort = flashPolicyPort;
        Runtime.getRuntime().addShutdownHook(new ShutdownHook(this));
    }

    public boolean isRunning() {
        return this.running;
    }

    public void start() {
        bootstrap = new ServerBootstrap(
                new NioServerSocketChannelFactory(
                        Executors.newCachedThreadPool(),
                        Executors.newCachedThreadPool()));

        // Set up the event pipeline factory.
        socketHandler = new WebSocketServerHandler(handler);
        bootstrap.setPipelineFactory(new WebSocketServerPipelineFactory(socketHandler));
        // Bind and start to accept incoming connections.
        this.serverChannel = bootstrap.bind(new InetSocketAddress(port));
        this.running = true;
        try {
            FlashPolicyServer.start(flashPolicyPort);
        } catch (Exception e) { //TODO: this should not be exception
            if (logger.isErrorEnabled()) {
                logger.error("You must run as sudo for flash policy server. X-Domain flash will not currently work.", e);
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Server Started at port [{}]", port);
        }
    }

    public void stop() {
        if (!this.running) return;

        if (logger.isDebugEnabled()) {
            logger.debug("Server shutting down.");
        }
        this.socketHandler.prepShutDown();
        this.handler.OnShutdown();
        this.serverChannel.close();
        this.bootstrap.releaseExternalResources();
        if (logger.isDebugEnabled()) {
            logger.debug("**SHUTDOWN**");
        }
        this.serverChannel = null;
        this.bootstrap = null;
        this.running = false;
    }

    public void broadcast(INSIOClient client, String message) {
        socketHandler.broadcast(client, message);
    }
}
