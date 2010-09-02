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

package de.cosmocode.palava.ipc.legacy;

import javax.annotation.concurrent.ThreadSafe;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandler.Sharable;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;

import de.cosmocode.palava.bridge.Content;

/**
 * An encoder which encodes {@link Content} to {@link ChannelBuffer} into the following form.
 * 
 * <p>
 *   {@code mimeType://(contentLength)?content}
 * </p>
 *
 * @since 1.0
 * @author Willi Schoenborn
 */
@Sharable
@ThreadSafe
final class LegacyContentEncoder extends OneToOneEncoder {

    private static final Logger LOG = LoggerFactory.getLogger(LegacyContentEncoder.class);
    
    private static final byte[] COLON_SLASHES = "://(".getBytes(Charsets.UTF_8);
    private static final byte[] QUESTION_MARK = ")?".getBytes(Charsets.UTF_8);
    
    @Override
    protected Object encode(ChannelHandlerContext context, Channel channel, Object message) throws Exception {
        if (message instanceof Content) {
            
            if (!channel.isWritable()) {
                LOG.warn("Channel {} is not writable, this might be causing OutOfMemoryErrors", channel);
            }
            
            final Content content = Content.class.cast(message);
            final byte[] bytes = content.getBytes();
            
            final byte[] mimeType = content.getMimeType().getName().getBytes(Charsets.UTF_8);
            final byte[] length = Integer.toString(bytes.length).getBytes(Charsets.UTF_8);
            
            // avoid an extra string builder
            final ChannelBuffer buffer = ChannelBuffers.buffer(
                mimeType.length + 
                COLON_SLASHES.length + 
                length.length + 
                QUESTION_MARK.length
            );
            
            buffer.writeBytes(mimeType);
            buffer.writeBytes(COLON_SLASHES);
            buffer.writeBytes(length);
            buffer.writeBytes(QUESTION_MARK);
            
            return ChannelBuffers.wrappedBuffer(
                buffer, 
                ChannelBuffers.wrappedBuffer(bytes)
            );
        } else {
            return message;
        }
    }

}
