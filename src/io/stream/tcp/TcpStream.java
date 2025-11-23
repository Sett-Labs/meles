package io.stream.tcp;

import io.Writable;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.FixedLengthFrameDecoder;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.stream.BaseStream;
import jdk.jfr.Event;
import org.tinylog.Logger;
import util.LookAndFeel;
import util.tools.NettyTools;
import util.tools.TimeTools;
import util.tools.Tools;
import util.xml.XMLdigger;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

public class TcpStream extends BaseStream implements Writable {

    protected InetSocketAddress ipsock;
    ByteBuf[] deli;
    protected Bootstrap bootstrap;                // Bootstrap for TCP connections
    static int bufferSize = 2048;     // How many bytes are stored before a dump
    protected Channel channel;

    private static int MAX_RECONNECT_ATTEMPTS=100;

    public TcpStream(XMLdigger stream, EventLoopGroup group) {
        super(stream);
        bootstrap = createBootstrap(group);
    }
    protected Bootstrap createBootstrap(EventLoopGroup group){
        var bootstrap = new Bootstrap();
        bootstrap.group(group).channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 6000);
        return bootstrap;
    }

    protected String getType(){
        return "tcp";
    }

    public void setChannel( Channel channel ){
        this.channel=channel;
    }
    @Override
    public void setReaderIdleTime(long seconds){
        this.readerIdleSeconds = seconds;
        if( channel!=null)
            NettyTools.updateReaderOnlyIdleTime(channel,seconds);
    }

    @Override
    public boolean initAndConnect() {
        if( ipsock == null ){
            Logger.error(id()+" -> No proper ipsock");
            return false;
        }
        Logger.info("Initiating to "+id());
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) {
                try{
                    // In bound traffic: low to high level (aka received data)
                    if( deli != null ){
                        ch.pipeline().addLast("framer",  new DelimiterBasedFrameDecoder(bufferSize,deli) );
                    }else{
                        Logger.error(id + " -> Deli still null, assuming fixed size...");
                        ch.pipeline().addLast("framer", new FixedLengthFrameDecoder(3) );
                    }
                    ch.pipeline().addLast("decoder", new ByteArrayDecoder() );
                    ch.pipeline().addLast("idleStateHandler", new IdleStateHandler(readerIdleSeconds, 0, 0, TimeUnit.SECONDS));
                    ch.pipeline().addLast("tinylogadapter", new TinyLogAdapter(id) );
                    ch.pipeline().addLast("streamhandler", new TcpStreamHandler( id, TcpStream.this ) );

                    // Outbound traffic (send data)
                    ch.pipeline().addLast( "encoder", new ByteArrayEncoder() );

                }catch( io.netty.channel.ChannelPipelineException e ){
                    Logger.error(id + " -> Issue trying to use handler");
                    Logger.error( e );
                }
            }
        });
        connect(false);
        return true;
    }
    // A method to initiate the reconnection
    public void connect(boolean reconnect) {
        if( ipsock == null ){
            Logger.error("No proper ipsock");
            return;
        }
        long backoffDelay = 0;

        if( reconnect ) {
            connectionAttempts++;
            if (connectionAttempts > MAX_RECONNECT_ATTEMPTS) {
                // Give up after too many attempts
                Logger.warn("Gave up connecting to "+id()+" after "+connectionAttempts);
                return;
            }
            backoffDelay = getBackoffDelay();
            if (LookAndFeel.isNthAttempt(connectionAttempts))
                Logger.info("Scheduling reconnect attempt " + connectionAttempts + " in " + TimeTools.convertPeriodToString(backoffDelay, TimeUnit.MILLISECONDS));
        }
        // Schedule the connection attempt on the event loop
        bootstrap.config().group().schedule(() -> {
            bootstrap.connect(ipsock).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    connectionAttempts = 0; // Reset the counter
                } else {
                    // Reconnection failed, try again after a delay
                    if (LookAndFeel.isNthAttempt(connectionAttempts)) {
                        String cause = String.valueOf(future.cause());
                        Logger.error(id + " -> Failed to connect: " + cause.substring(cause.indexOf(":") + 1));
                    }
                    connect(true);
                }
            });
        }, backoffDelay, TimeUnit.MILLISECONDS);
    }
    private long getBackoffDelay() {
        // Implement exponential backoff
        return (long) Math.pow(2, connectionAttempts) * 1000;
    }

    @Override
    public boolean isConnectionValid() {
        return channel!=null && channel.isActive();
    }

    @Override
    protected boolean readExtraFromXML(XMLdigger stream) {
        // Address
        String address = stream.peekAt("address").value("");
        if( !address.contains(":") ){
            Logger.error(id+" -> Not proper ip:port for "+id+" -> "+address);
            return false;
        }
        ipsock = new InetSocketAddress(address.substring(0, address.lastIndexOf(":")),
                Tools.parseInt(address.substring(address.lastIndexOf(":") + 1), -1));

        // Alter eol
        if( eol.isEmpty() ){
            Logger.error(id + " -> No EOL defined");
            return false;
        }
        deli = new ByteBuf[]{ Unpooled.copiedBuffer( eol.getBytes())};
        return true;
    }

    @Override
    public String getInfo() {
        return "TCP ["+id+"] "+ ipsock.toString();
    }

    @Override
    public boolean writeLine(String origin, String data) {
        return writeLine(data);
    }
    public boolean writeLine(String data) {
       return writeString(data+eol);
    }
    public boolean writeString(String data) {
        if( channel==null || !channel.isActive() )
            return false;
        var f = channel.writeAndFlush(data.getBytes());
        f.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) {
                if (!future.isSuccess()) {
                    Logger.error(future.cause());
                }
            }
        });
        return true;
    }
    public boolean writeBytes(byte[] data) {
        if( channel==null || !channel.isActive() )
            return false;
        channel.writeAndFlush(data);
        return true;
    }
    @Override
    public boolean disconnect(){
        if( channel != null ){
            channel.disconnect();
            return true;
        }
        return false;
    }
}
