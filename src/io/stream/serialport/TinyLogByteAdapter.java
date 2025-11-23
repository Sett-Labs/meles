package io.stream.serialport;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.tinylog.Logger;
import util.tools.Tools;

public class TinyLogByteAdapter extends ChannelInboundHandlerAdapter {
    private final String id;

    public TinyLogByteAdapter(String id) {
        this.id = id;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if( msg instanceof byte[] data)
            Logger.tag("RAW").warn( id + "\t[hex] " + Tools.fromBytesToHexString(data,0,data.length) );
        ctx.fireChannelRead(msg); // Important: pass the data to the next handler
    }
}
