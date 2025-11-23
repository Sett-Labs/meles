package io.stream.udp;

import io.Writable;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import io.netty.util.concurrent.FutureListener;
import io.stream.tcp.TcpStream;
import io.stream.tcp.TcpStreamHandler;
import org.tinylog.Logger;
import util.LookAndFeel;
import util.xml.XMLdigger;

public class UdpStream extends TcpStream implements Writable {

    public UdpStream(XMLdigger stream, EventLoopGroup group ) {
        super(stream, group);
    }

    protected String getType(){
        return "udpwriter";
    }

    @Override
    protected Bootstrap createBootstrap(EventLoopGroup group){
        bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioDatagramChannel.class)
                .option(ChannelOption.SO_BROADCAST, true);
        return bootstrap;
    }

    @Override
    public boolean initAndConnect() {

        Logger.debug("Port and IP defined for UDP, meaning writing so connecting channel...?");
        bootstrap.option(ChannelOption.SO_REUSEADDR,true);
        bootstrap.handler( new ChannelInitializer<NioDatagramChannel>() {
            @Override
            public void initChannel(NioDatagramChannel ch) {
                // Inbound
                ch.pipeline().addLast("decoder", new ByteArrayDecoder() );
               // ch.pipeline().addLast("idleStateHandler", new IdleStateHandler(readerIdleSeconds, 0, 0, TimeUnit.SECONDS));
               // ch.pipeline().addLast("tinylogadapter", new TinyLogAdapter(id) );
                ch.pipeline().addLast( "streamhandler", new TcpStreamHandler(id, UdpStream.this) );

                // Outbound
                ch.pipeline().addLast( "encoder", new ByteArrayEncoder() );
            }
        });

        ChannelFuture f = bootstrap.connect(ipsock);
        
        f.awaitUninterruptibly();
        f.addListener((FutureListener<Void>) future -> {
            if (f.isSuccess()) {
                Logger.info("Operation complete");
            } else if (LookAndFeel.isNthAttempt(connectionAttempts)) {
                String cause = String.valueOf(future.cause());
                Logger.error(id + " -> Failed to connect: " + cause.substring(cause.indexOf(":") + 1));
            }
        });
        return true;
    }
    @Override
    public String getInfo() {
        return "UDP writer[" + id + (label.isEmpty() ? "" : "|" + label) + "] " + ipsock.toString();
    }
}
