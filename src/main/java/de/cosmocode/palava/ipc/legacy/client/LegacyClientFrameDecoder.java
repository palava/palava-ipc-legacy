package de.cosmocode.palava.ipc.legacy.client;

import java.nio.ByteBuffer;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.replay.ReplayingDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;

/**
 * Legacy  client{@link ReplayingDecoder} to support the legacy php protocol from
 *  server to client which looks like:<br />
 * {@code <mimetype>://(<contentLength>)?<content>}.
 *
 * @since 1.0
 * @author Willi Schoenborn
 */
public final class LegacyClientFrameDecoder extends ReplayingDecoder<Part> {

    private static final Logger LOG = LoggerFactory.getLogger(LegacyClientFrameDecoder.class);

    private String mimeType;
    
    private int length;
    
    private ByteBuffer content;
    
    public LegacyClientFrameDecoder() {
        super(Part.MIME_TYPE);
    }

    // Fall-throughs are the fastest way here
    /* CHECKSTYLE:OFF */
    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer, Part part)
        throws Exception {

        switch (part) {
            case MIME_TYPE: {
                mimeType = readMimeType(buffer);
                checkpoint(Part.COLON);
                // intended fall-through
            }
            case COLON: {
                final byte c = buffer.readByte();
                checkState(c == ':', "Expected : but was %s", c);
                checkpoint(Part.FIRST_SLASH);
                // intended fall-through
            }
            case FIRST_SLASH: {
                final byte c = buffer.readByte();
                checkState(c == '/', "Expected / but was %s", c);
                checkpoint(Part.SECOND_SLASH);
                // intended fall-through
            }
            case SECOND_SLASH: {
                final byte c = buffer.readByte();
                checkState(c == '/', "Expected / but was %s", c);
                checkpoint(Part.LEFT_PARENTHESIS);
                // intended fall-through
            }
            case LEFT_PARENTHESIS: {
                final byte c = buffer.readByte();
                checkState(c == '(', "Expected ( but was %s", c);
                checkpoint(Part.CONTENT_LENGTH);
                // intended fall-through
            }
            case CONTENT_LENGTH: {
                length = readLength(buffer);
                checkpoint(Part.RIGHT_PARENTHESIS);
                // intended fall-through
            }
            case RIGHT_PARENTHESIS: {
                final byte c = buffer.readByte();
                checkState(c == ')', "Expected ) but was %s", c);
                checkpoint(Part.QUESTION_MARK);
                // intended fall-through
            }
            case QUESTION_MARK: {
                final byte c = buffer.readByte();
                checkState(c == '?', "Expected ? but was %s", c);
                checkpoint(Part.CONTENT);
                // intended fall-through
            }
            case CONTENT: {
                content = readContent(buffer);
                checkpoint(Part.MIME_TYPE);
                return InternalClientHeader.copyOf(this);
            }
            default: {
                throw new AssertionError("Default case matched part " + part);
            }
        }
        
    }
    /* CHECKSTYLE:ON */
    
    private String readMimeType(ChannelBuffer buffer) {
        final String value = readUntil(buffer, ':');
        LOG.trace("Read  mime type '{}'", value);
        return value;
    }
    
    private int readLength(ChannelBuffer buffer) {
        final String value = readUntil(buffer, ')');
        LOG.trace("Read length {}", value);
        return Integer.parseInt(value);
    }
    
    private ByteBuffer readContent(ChannelBuffer buffer) {
        final ByteBuffer value = buffer.toByteBuffer(buffer.readerIndex(), length);
        buffer.skipBytes(length);
        LOG.trace("Read content {}", value);
        return value;
    }
    
    private String readUntil(ChannelBuffer buffer, char c) {
        int i = buffer.readerIndex();
        while (true) {
            if (c == buffer.getByte(i)) {
                return readAndIncrement(buffer, i);
            }
            i++;
        }
    }
    
    private String readAndIncrement(ChannelBuffer buffer, int currentIndex) {
        final int size = currentIndex - buffer.readerIndex();
        final String value = buffer.toString(buffer.readerIndex(), size, Charsets.UTF_8);
        buffer.readerIndex(currentIndex);
        return value;
    }

    private void checkState(boolean state, String format, byte c) {
        if (state) {
            return;
        } else {
            throw new IllegalArgumentException(String.format(format, (char) c)); 
        }
    }
    
    /**
     * Internal {@link ClientHeader} implementation.
     *
     * @since 1.0
     * @author Willi Schoenborn
     */
    private static final class InternalClientHeader implements ClientHeader {
        
        private final String mimeType;
        
        private final ByteBuffer content;
        
        private InternalClientHeader(LegacyClientFrameDecoder decoder) {
            this.mimeType = decoder.mimeType;
            this.content = decoder.content;
        }
        
        @Override
        public String getMimeType() {
            return mimeType;
        }

        @Override
        public ByteBuffer getContent() {
            return content;
        }
        
        public static ClientHeader copyOf(LegacyClientFrameDecoder decoder) {
            return new InternalClientHeader(decoder);
        }
        
    }
    
}
