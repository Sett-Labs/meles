package io.stream.tcp;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.FixedLengthFrameDecoder;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import io.stream.serialport.ModbusDecoder;
import io.stream.serialport.TinyLogByteAdapter;
import org.apache.commons.lang3.math.NumberUtils;
import org.tinylog.Logger;
import util.LookAndFeel;
import util.xml.XMLdigger;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

public class ModbusTCPStream extends TcpStream{

    public ModbusTCPStream(XMLdigger stream, EventLoopGroup group) {
        super(stream,group);
    }
    public String getType(){
        return "modbus";
    }
    @Override
    public boolean readExtraFromXML(XMLdigger stream) {
        // Address
        var address = stream.peekAt("address").value("");
        if (!address.contains(":"))
            address+=":502";

        ipsock = new InetSocketAddress( address.substring(0,address.lastIndexOf(":")),
                NumberUtils.toInt(address.substring(address.lastIndexOf(":") + 1), -1));
        return true;
    }
    @Override
    public boolean initAndConnect() {

        bootstrap.handler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch){
                try{
                    // In bound traffic: low to high level (aka received data)
                    ch.pipeline().addLast("modbusdecoder", new ModbusDecoder());
                    ch.pipeline().addLast("tinylogadapter", new TinyLogByteAdapter(id) );
                    ch.pipeline().addLast("idleStateHandler", new IdleStateHandler(readerIdleSeconds, 0, 0, TimeUnit.SECONDS));
                    ch.pipeline().addLast("registerhandler",new ModbusTCP(id,ModbusTCPStream.this) );

                    // Outbound traffic (send data)
                    ch.pipeline().addLast( "encoder", new ByteArrayEncoder() );
                }catch( io.netty.channel.ChannelPipelineException e ){
                    Logger.error(id+" -> Issue trying to use handler for "+id);
                    Logger.error( e );
                }
            }
        });
        connect(false);
        return true;
    }

}
