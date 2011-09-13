package com.ibdknox.socket_io_netty.flashpolicy;

/*
 * Copyright 2010 Bruce Mitchener.
 *
 * Bruce Mitchener licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.timeout.ReadTimeoutException;
import org.jboss.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="http://www.waywardmonkeys.com/">Bruce Mitchener</a>
 */
public class FlashPolicyServerHandler extends SimpleChannelUpstreamHandler {

    private static Logger logger = LoggerFactory.getLogger(FlashPolicyServer.class);
    private static final String NEWLINE = "\r\n";

    private String domain = "*";
    private String ports = "*";

    public FlashPolicyServerHandler(String domain, String ports) {
        super();
        this.domain = domain;
        this.ports = ports;
        policyFile = buildPolicyFile();
    }

    public FlashPolicyServerHandler() {
        super();
        policyFile = buildPolicyFile();
    }

    private String buildPolicyFile() {
        StringBuilder builder = new StringBuilder();
        builder.append("<?xml version=\"1.0\"?>").append(NEWLINE)
                .append("<!DOCTYPE cross-domain-policy SYSTEM \"/xml/dtds/cross-domain-policy.dtd\">").append(NEWLINE)
                .append("<cross-domain-policy> ").append(NEWLINE)
                .append("   <site-control permitted-cross-domain-policies=\"master-only\"/>").append(NEWLINE)
                .append("   <allow-access-from domain=\"").append(domain).append(" to-ports=\"").append(ports).append("\" />").append(NEWLINE)
                .append("</cross-domain-policy>");
        return builder.toString();
    }

    private String policyFile;

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        Object msg = e.getMessage();
        ChannelFuture f = e.getChannel().write(this.getPolicyFileContents());
        f.addListener(ChannelFutureListener.CLOSE);
    }

    private ChannelBuffer getPolicyFileContents() throws Exception {
        return ChannelBuffers.copiedBuffer(policyFile, CharsetUtil.US_ASCII);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
            throws Exception {
        Throwable cause = e.getCause();
        if (cause instanceof ReadTimeoutException) {
            if (logger.isErrorEnabled()) {
                logger.error("Connection timed out.", cause);
            }
            e.getChannel().close();
        } else {
            if (logger.isErrorEnabled()) {
                logger.error(cause.getMessage(), cause);
            }
            e.getChannel().close();
        }
    }
}
