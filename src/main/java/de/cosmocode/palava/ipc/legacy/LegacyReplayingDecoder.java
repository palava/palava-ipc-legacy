package de.cosmocode.palava.ipc.legacy;

import java.nio.ByteBuffer;

import javax.annotation.concurrent.NotThreadSafe;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.replay.ReplayingDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;

import de.cosmocode.palava.bridge.Header;
import de.cosmocode.palava.bridge.call.CallType;

/**
 * Legacy {@link ReplayingDecoder} to support the legacy php protocol which looks like:<br />
 * {@code <type>://<aliasedName>/<sessionId>/(<contentLength>)?<content>}.
 *
 * @since 1.0
 * @author Willi Schoenborn
 */
@NotThreadSafe
final class LegacyReplayingDecoder extends ReplayingDecoder<Part> {

    private static final Logger LOG = LoggerFactory.getLogger(LegacyReplayingDecoder.class);
    
    private CallType type;
    
    private String name;
    
    private String sessionId;
    
    private int length;
    
    private ByteBuffer content;
    
    public LegacyReplayingDecoder() {
        super(Part.TYPE);
    }

    // Reducing cyclomatic complexity would dramatically reduce readability
    // Fall-throughs are the fastest way here
    /* CHECKSTYLE:OFF */
    @Override
    protected Object decode(ChannelHandlerContext context, Channel channel, 
        ChannelBuffer buffer, Part part) throws Exception {
        
        switch (part) {
            case TYPE: {
                type = readType(buffer);
                LOG.trace("Read type '{}'", type);
                checkpoint(Part.COLON);
                // intended fall-through
            }
            case COLON: {
                final byte c = buffer.getByte(buffer.readerIndex());
                checkState(c == ':', "Expected : but was %s", c);
                buffer.skipBytes(1);
                checkpoint(Part.FIRST_SLASH);
                // intended fall-through
            }
            case FIRST_SLASH: {
                final byte c = buffer.getByte(buffer.readerIndex());
                checkState(c == '/', "Expected first / but was %s", c);
                buffer.skipBytes(1);
                checkpoint(Part.SECOND_SLASH);
                // intended fall-through
            }
            case SECOND_SLASH: {
                final byte c = buffer.getByte(buffer.readerIndex());
                checkState(c == '/', "Expected second / but was %s", c);
                buffer.skipBytes(1);
                checkpoint(Part.NAME);
                // intended fall-through
            }
            case NAME: {
                name = readName(buffer);
                LOG.trace("Read name '{}'", name);
                checkpoint(Part.THIRD_SLASH);
                // intended fall-through
            }
            case THIRD_SLASH: {
                final byte c = buffer.getByte(buffer.readerIndex());
                checkState(c == '/', "Expected third / but was %s", c);
                buffer.skipBytes(1);
                checkpoint(Part.SESSION_ID);
                // intended fall-through
            }
            case SESSION_ID: {
                sessionId = readSessionId(buffer);
                LOG.trace("Read sessionId '{}'", sessionId);
                checkpoint(Part.FOURTH_SLASH);
                // intended fall-through
            }
            case FOURTH_SLASH: {
                final byte c = buffer.getByte(buffer.readerIndex());
                checkState(c == '/', "Expected fourth / but was %s", c);
                buffer.skipBytes(1);
                checkpoint(Part.LEFT_PARENTHESIS);
                // intended fall-through
            }
            case LEFT_PARENTHESIS: {
                final byte c = buffer.getByte(buffer.readerIndex());
                checkState(c == '(', "Expected ( but was %s", c);
                buffer.skipBytes(1);
                checkpoint(Part.CONTENT_LENGTH);
                // intended fall-through
            }
            case CONTENT_LENGTH: {
                length = readLength(buffer);
                checkpoint(Part.RIGHT_PARENTHESIS);
                // intended fall-through
            }
            case RIGHT_PARENTHESIS: {
                final byte c = buffer.getByte(buffer.readerIndex());
                checkState(c == ')', "Expected ) but was %s", c);
                buffer.skipBytes(1);
                checkpoint(Part.QUESTION_MARK);
                // intended fall-through
            }
            case QUESTION_MARK: {
                final byte c = buffer.getByte(buffer.readerIndex());
                checkState(c == '?', "Expected ? but was %s", c);
                buffer.skipBytes(1);
                checkpoint(Part.CONTENT);
                // intended fall-through
            }
            case CONTENT: {
                return contentOf(buffer);
            }
            default: {
                throw new AssertionError("Default case matched part " + part);
            }
        }

    }
    /* CHECKSTYLE:ON */
    
    private CallType readType(ChannelBuffer buffer) {
        return CallType.valueOf(readUntil(buffer, ':').toUpperCase());
    }
    
    private String readName(ChannelBuffer buffer) {
        return readUntil(buffer, '/');
    }
    
    private String readSessionId(ChannelBuffer buffer) {
        return readUntil(buffer, '/');
    }
    
    private int readLength(ChannelBuffer buffer) {
        return Integer.parseInt(readUntil(buffer, ')'));
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
        try {
            return buffer.toString(buffer.readerIndex(), currentIndex - buffer.readerIndex(), Charsets.UTF_8);
        } finally {
            buffer.readerIndex(currentIndex);
        }
    }

    private void checkState(boolean state, String format, byte c) {
        if (state) {
            return;
        } else {
            throw new IllegalArgumentException(String.format(format, (char) c)); 
        }
    }
    
    private Header contentOf(ChannelBuffer buffer) {
        content = buffer.toByteBuffer(buffer.readerIndex(), length);
        LOG.trace("Setting content to {}", content);
        buffer.skipBytes(length);
        final Header header = InternalHeader.copyOf(this);
        LOG.trace("Incoming call {}", header);
        reset();
        return header;
    }
    
    private void reset() {
        setState(Part.TYPE);
        type = null;
        name = null;
        sessionId = null;
        length = 0;
        content = null;
    }
    
    /**
     * Internal implementation of the {@link Header} interface.
     *
     * @since 
     * @author Willi Schoenborn
     */
    private static final class InternalHeader implements Header {
        
        private final CallType callType;
        
        private final String name;
        
        private final String sessionId;
        
        private final int length;
        
        private final ByteBuffer content;
        
        private InternalHeader(LegacyReplayingDecoder decoder) {
            this.callType = decoder.type;
            this.name = decoder.name;
            this.sessionId = decoder.sessionId;
            this.length = decoder.length;
            this.content = decoder.content;
        }
        
        @Override
        public CallType getCallType() {
            return callType;
        }
        
        @Override
        public String getAliasedName() {
            return name;
        }
        
        @Override
        public String getSessionId() {
            return sessionId;
        }
        
        @Override
        public int getContentLength() {
            return length;
        }
        
        @Override
        public ByteBuffer getContent() {
            return content;
        }

        @Override
        public String toString() {
            return String.format("Header [callType=%s, name=%s, sessionId=%s, contentLength=%s, content=%s]",
                callType, getAliasedName(), getSessionId(), getContentLength(), getContent()
            );
        }
        
        public static Header copyOf(LegacyReplayingDecoder decoder) {
            return new InternalHeader(decoder);
        }
        
    }
    
}
