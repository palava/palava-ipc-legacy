package de.cosmocode.palava.ipc.legacy;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelHandler.Sharable;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;

import com.google.common.base.Charsets;

import de.cosmocode.palava.bridge.Content;
import de.cosmocode.palava.ipc.netty.ChannelBuffering;
import de.cosmocode.patterns.ThreadSafe;

/**
 * An encoder which encodes {@link Content} to {@link ChannelBuffer}.
 *
 * @since 1.0
 * @author Willi Schoenborn
 */
@Sharable
@ThreadSafe
final class LegacyEncoder extends OneToOneEncoder {

    @Override
    protected Object encode(ChannelHandlerContext context, Channel channel, Object message) throws Exception {
        if (message instanceof Content) {
            final Content content = Content.class.cast(message);
            final String mimeType = content.getMimeType().toString();
            final int length = (int) content.getLength();

            final byte[] header = String.format("%s://(%s)?", 
                mimeType, Integer.toString(length)
            ).getBytes(Charsets.UTF_8);
            
            final ChannelBuffer buffer = ChannelBuffers.dynamicBuffer(header.length + length);
            
            buffer.writeBytes(header);
            content.write(ChannelBuffering.asOutputStream(buffer));
            
            return buffer;
        } else {
            return message;
        }
    }

}
