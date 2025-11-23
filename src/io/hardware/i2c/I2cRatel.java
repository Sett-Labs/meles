package io.hardware.i2c;

import com.diozero.api.DigitalInputEvent;
import com.diozero.api.RuntimeIOException;
import com.diozero.api.function.DeviceEventConsumer;
import io.Writable;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.EventLoopGroup;
import org.apache.commons.lang3.ArrayUtils;
import org.tinylog.Logger;
import util.tools.TimeTools;
import util.tools.Tools;
import util.xml.XMLdigger;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class I2cRatel extends I2cDevice implements Writable, DeviceEventConsumer<DigitalInputEvent> {

    protected ArrayList<Writable> targets = new ArrayList<>();
    private String eol="\r\n";
    private final byte[] eolBytes;
    private final ByteBuf readBuffer = Unpooled.buffer(256,2048);

    private int eolFound = 0;
    private enum WAITING_FOR {IDLE,STATUS,CONF,UNKNOWN}

    private WAITING_FOR requested = WAITING_FOR.IDLE;

    private final ArrayList<byte[]> writeData=new ArrayList<>();
    private boolean debug=true;

    public I2cRatel(XMLdigger dig, I2cBus bus){
        super(dig,bus);
        Logger.info("(ratel) -> Building ratel device");
        eol = Tools.getDelimiterString(dig.peekAt("eol").value(eol));
        eolBytes=eol.getBytes();
    }
    public void setDebug( boolean debug ){
        this.debug=debug;
    }

    public void useBus(EventLoopGroup scheduler){
        // Write data to the device
        byte[] data=null;
        if( debug )
            Logger.info(id+" (ratel) -> Using bus");
        if( device==null){
            Logger.error(id+"(ratel) -> Device still null, can't do anything.");
        }
        // Read data from it.
        if( debug )
            Logger.info( id+"(ratel) -> Trying to read from reg 0x22" );

        var size = (int)device.readWordSwapped(0x22); // Read the size of the used buffer
        size = Tools.toUnsignedWord(size);

        // split it
        if( debug )
            Logger.info("Size: "+size);
        if ( size != 0) {
            try {
                data = new byte[size];
                device.readNoStop((byte) 0x50, data, false);
            } catch (RuntimeIOException e) {
                Logger.error(id + "(ratel) -> Runtime exception when trying to read: " + e.getMessage());
            }
        }

        bus.doNext(); // Release the bus

        if( data!=null ) { // If no data read, no need tor process
            if( debug )
                Logger.info(id + "(ratel) -> Read " + size + " bytes for uart.");
            updateTimestamp();
            processRead(data);
        }
    }
    private void processRead( byte[] data ){
        for ( byte datum : data) {
            readBuffer.writeByte(datum);
            if (datum != eolBytes[eolFound]) {
                eolFound = 0;
                continue;
            }
            eolFound++;
            if (eolFound == eolBytes.length) { // Got whole eol
                var rec = new byte[readBuffer.readableBytes() - eolFound];
                readBuffer.readBytes(rec); // Read the bytes, but omit the eol
                readBuffer.clear(); // ignore the eol
                var res = new String(rec);
                Logger.tag("RAW").warn(id() + "\t" + res);
                forwardData(res);
                eolFound = 0;
            }
        }
    }
    public byte[] getData(){
        var size = writeData.get(0).length;
        return ArrayUtils.insert(0,writeData.remove(0),(byte)2,(byte)size);
    }
    public String getStatus(){
        String age = getAge() == -1 ? "Not used yet" : TimeTools.convertPeriodToString(getAge(), TimeUnit.SECONDS);
        return (valid?"":"!!")+"RATEL ["+id+"] "+getAddr()+"\t"+age+" [-1]";
    }
    @Override
    public boolean writeString(String data) {
        return writeBytes( data.getBytes() );
    }

    @Override
    public boolean writeLine(String origin, String data) {
        return writeString(data+eol);
    }

    @Override
    public boolean writeBytes(byte[] data) {
        return true;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public boolean isConnectionValid() {
        return true;
    }

    @Override
    public void accept(DigitalInputEvent digitalInputEvent) {
        Logger.info("IRQ triggered: "+digitalInputEvent.toString());
        requested= WAITING_FOR.UNKNOWN;
        bus.requestSlot(this);
    }
    public void requestSlot(){
        bus.requestSlot(this);
    }
}
