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
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelHandler.Sharable;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;

import com.google.common.base.Charsets;

import de.cosmocode.palava.bridge.Content;
import de.cosmocode.palava.ipc.netty.ChannelBuffering;

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
            
            // this effectively copies the content in memory
            content.write(ChannelBuffering.asOutputStream(buffer));
            
            return buffer;
        } else {
            return message;
        }
    }

}
