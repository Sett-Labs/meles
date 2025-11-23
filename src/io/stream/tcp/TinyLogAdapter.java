package io.stream.tcp;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.tinylog.Logger;

public class TinyLogAdapter extends ChannelInboundHandlerAdapter {
    private final String id;

    public TinyLogAdapter(String id) {
        this.id = id;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if( msg instanceof byte[] data)
            Logger.tag("RAW").warn(id + "\t" + new String(data));
        ctx.fireChannelRead(msg); // Important: pass the data to the next handler
    }
}
