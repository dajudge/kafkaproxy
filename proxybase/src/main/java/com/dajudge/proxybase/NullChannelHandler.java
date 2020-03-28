package com.dajudge.proxybase;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerAdapter;

@ChannelHandler.Sharable
public class NullChannelHandler extends ChannelHandlerAdapter {
}
