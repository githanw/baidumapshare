package cn.githan.mapshareandroidclient;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderAdapter;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

/**
 * Created by BW on 16/6/17.
 */
public class ByteArrayEncoder extends ProtocolEncoderAdapter {
    @Override
    public void encode(IoSession ioSession, Object o, ProtocolEncoderOutput protocolEncoderOutput) throws Exception {
        byte[] bytes = (byte[]) o;

        IoBuffer buffer = IoBuffer.allocate(256);
        buffer.setAutoExpand(true);

        buffer.put(bytes);
        buffer.flip();

        protocolEncoderOutput.write(buffer);
        protocolEncoderOutput.flush();

        buffer.free();
    }
}
