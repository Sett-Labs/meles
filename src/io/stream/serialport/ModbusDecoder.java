package io.stream.serialport;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public class ModbusDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        // Wait for the minimal header size (Modbus Application Header has 6 bytes)
        if (in.readableBytes() < 6) {
            return;
        }

        in.markReaderIndex();

        // Read the PDU length from the header (bytes 4 and 5)
        int pduLength = in.getUnsignedShort(in.readerIndex() + 4);

        // Check if the full message is available
        if (in.readableBytes() < 6 + pduLength) { // add the header length to the pdu length
            in.resetReaderIndex();
            return; // Wait for more data
        }

        // Read the entire message
        byte[] modbusMessage = new byte[6 + pduLength];
        in.readBytes(modbusMessage);

        // The decoder emits a complete message object to the next handler
        out.add(modbusMessage);
    }
}
