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

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;

import de.cosmocode.palava.bridge.Header;

/**
 * Binds all legacy decoders.
 *
 * @since 
 * @author Willi Schoenborn
 */
public final class LegacyNettyModule implements Module {

    @Override
    public void configure(Binder binder) {
        // frame decoders are stateful
        binder.bind(LegacyFrameDecoder.class).in(Scopes.NO_SCOPE);
        binder.bind(LegacyDecoder.class).in(Singleton.class);
        binder.bind(LegacyEncoder.class).in(Singleton.class);
        binder.bind(LegacyHandler.class).in(Singleton.class);
        binder.bind(CommandCache.class).to(LegacyCommandCache.class).in(Singleton.class);
        binder.bind(CommandExecutor.class).to(LegacyCommandExecutor.class).in(Singleton.class);
        binder.bind(Executor.class).to(LegacyExecutor.class).in(Singleton.class);
        binder.bind(JobExecutor.class).to(LegacyJobExecutor.class).in(Singleton.class);
    }
    
    /**
     * Provides a channel pipeline containing all required decoders.
     * 
     * @since 1.0
     * @param frameDecoder the frame decoder which decodes chunks into {@link Header}s
     * @param decoder the decoder
     * @param encoder the encoder
     * @param handler the handler
     * @return a new {@link ChannelPipeline}
     */
    @Provides
    ChannelPipeline provideChannelPipeline(LegacyFrameDecoder frameDecoder, LegacyDecoder decoder, 
        LegacyEncoder encoder, LegacyHandler handler) {
        return Channels.pipeline(
            frameDecoder,
            decoder,
            encoder,
            handler
        );
    }
    
    /**
     * Provides a channel pipeline factory producing new channel pipelines.
     * 
     * @since 1.0
     * @param provider the backing provider for {@link ChannelPipeline}s
     * @return a new new {@link ChannelPipelineFactory}
     */
    @Provides
    @Singleton
    ChannelPipelineFactory provideChannelPipelineFactory(final Provider<ChannelPipeline> provider) {
        return new ChannelPipelineFactory() {
            
            @Override
            public ChannelPipeline getPipeline() throws Exception {
                return provider.get();
            }
            
        };
    }

}
