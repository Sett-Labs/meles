package io.stream.tcp;

import io.Writable;
import io.netty.channel.ChannelHandlerContext;
import org.tinylog.Logger;
import util.tools.Tools;

import java.time.Instant;
import java.util.Arrays;
import java.util.StringJoiner;

public class ModbusTCP extends TcpHandler{

    private final byte[] header=new byte[]{0,1,0,0,0,0,0};
    private final String[] origin = new String[]{"","","","reg","AI",""};
    private ModbusTCPStream stream;

    public ModbusTCP(String id) {
        super(id);
    }
    public ModbusTCP( String id, ModbusTCPStream stream ){
        this(id);
        this.stream=stream;
    }
    @Override
    public void channelRead0(ChannelHandlerContext ctx, byte[] data) {

        stream.setTimestamp( Instant.now().toEpochMilli() );            // Store the timestamp of the received message

        if( data[7] == 0x10 )
            return;

        if( !(data[7]==0x03 || data[7]==0x04 || data[7]==0x06) ){
            Logger.warn(id+"(mb) -> Received unknown type");
            return;
        }

        int reg = data[8];

        StringJoiner join = new StringJoiner(",");
        for( int a=9;a<data.length;a+=2){
            int i0= data[a]<0?-1*((data[a]^0xFF) + 1):data[a];
            int i1= data[a+1]<0?-1*((data[a+1]^0xFF) + 1):data[a+1];
            join.add(origin[data[7]]+reg+":"+(i0*256+i1));
            reg++;
        }
        if( !stream.getTargets().isEmpty() ){
            stream.getTargets().forEach(dt -> eventLoopGroup.submit(() -> dt.writeLine(id, join.toString())));
            stream.getTargets().removeIf(wr -> !wr.isConnectionValid() ); // Clear inactive
        }
    }


    /**
     * Writes the given bytes with the default header prepended (00 01 00 00 00 xx 01, x is data length)
     * Header followed with
     *  1B -> function code (0x03=AI, 0x04=Reg etc)
     *  2B -> Address
     *  2B -> Addresses to read (each contain 2B)
     * @param data The data to append to the header
     * @return True if written
     */
    public boolean writeBytes(byte[] data) {
        if( channel==null || !channel.isActive() )
            return false;
        header[5] = (byte) (data.length+1);
        channel.write(header);
        channel.writeAndFlush(data);
        return true;
    }
    public boolean writeLine(String data) {
       return writeBytes(data.getBytes());
    }
    public byte[] getHeader(){
        return header;
    }
}
