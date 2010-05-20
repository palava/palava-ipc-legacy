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
import com.google.inject.multibindings.Multibinder;

import de.cosmocode.palava.bridge.Header;
import de.cosmocode.palava.bridge.Server;
import de.cosmocode.palava.bridge.ServiceManager;
import de.cosmocode.palava.bridge.command.Alias;
import de.cosmocode.palava.bridge.session.HttpSession;
import de.cosmocode.palava.ipc.IpcSession;
import de.cosmocode.palava.ipc.IpcSessionScoped;

/**
 * Binds all legacy decoders.
 *
 * @since 
 * @author Willi Schoenborn
 */
public final class LegacyNettyModule implements Module {

    @Override
    public void configure(Binder binder) {
        // stateful decoders
        binder.bind(LegacyFrameDecoder.class).in(Scopes.NO_SCOPE);
        
        // decoders/encoders
        binder.bind(LegacyHeaderDecoder.class).in(Singleton.class);
        binder.bind(LegacyContentEncoder.class).in(Singleton.class);
        binder.bind(LegacyHandler.class).in(Singleton.class);

        // legacy services
        binder.bind(CommandCache.class).to(LegacyCommandCache.class).in(Singleton.class);
        Multibinder.newSetBinder(binder, Alias.class);
        binder.bind(Executor.class).to(LegacyExecutor.class).in(Singleton.class);
        binder.bind(JobExecutor.class).to(LegacyJobExecutor.class).in(Singleton.class);
        binder.bind(CommandExecutor.class).to(LegacyCommandExecutor.class).in(Singleton.class);
        binder.bind(LegacyServer.class).in(Singleton.class);
        binder.bind(Server.class).to(LegacyServer.class).in(Singleton.class);
        binder.bind(ServiceManager.class).to(LegacyServer.class).in(Singleton.class);
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
    ChannelPipeline provideChannelPipeline(LegacyFrameDecoder frameDecoder, LegacyHeaderDecoder decoder, 
        LegacyContentEncoder encoder, LegacyHandler handler) {
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
    
    /**
     * Provides the current http session.
     * 
     * @since 1.0
     * @param session the underlying ipc session
     * @return a new {@link HttpSession}
     */
    @Provides
    @IpcSessionScoped
    HttpSession provideHttpSession(final IpcSession session) {
        return new LegacyHttpSessionAdapter(session);
    }

}
