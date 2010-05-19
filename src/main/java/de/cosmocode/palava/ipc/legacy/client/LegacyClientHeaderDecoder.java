/**
 * Copyright 2010 CosmoCode GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.cosmocode.palava.ipc.legacy.client;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import de.cosmocode.palava.bridge.MimeType;

/**
 * A decoder which decodes {@link ClientHeader}s with application/json mimetype into {@link Map}s
 * or {@link List}s.
 *
 * @since 1.0
 * @author Willi Schoenborn
 */
final class LegacyClientHeaderDecoder extends OneToOneDecoder {

    private static final Logger LOG = LoggerFactory.getLogger(LegacyClientHeaderDecoder.class);

    private static final String JSON = MimeType.JSON.toString();
    
    private final ObjectMapper mapper;
    
    @Inject
    public LegacyClientHeaderDecoder(ObjectMapper mapper) {
        this.mapper = Preconditions.checkNotNull(mapper, "Mapper");
    }
    
    @Override
    protected Object decode(ChannelHandlerContext context, Channel channel, Object message) throws Exception {
        if (message instanceof ClientHeader) {
            final ClientHeader header = ClientHeader.class.cast(message);
            if (JSON.equals(header.getMimeType())) {
                final ByteBuffer buffer = header.getContent();
                final byte[] bytes = new byte[buffer.limit()];
                buffer.get(bytes);
                final String content = new String(bytes, Charsets.UTF_8);
                final char first = content.charAt(0);
                if (first == '[') {
                    LOG.trace("Decoding list {}", content);
                    return mapper.readValue(content, List.class);
                } else if (first == '{') {
                    LOG.trace("Decoding map {}", content);
                    return mapper.readValue(content, Map.class);
                } else {
                    throw new IllegalArgumentException("Unknown json content " + content);
                }
            } else {
                // message was client header but no json
                return message;
            }
        } else {
            return message;
        }
    }

}
