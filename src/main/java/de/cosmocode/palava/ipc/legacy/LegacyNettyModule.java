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
import org.jboss.netty.channel.Channels;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;

import de.cosmocode.palava.bridge.Header;
import de.cosmocode.palava.bridge.Server;
import de.cosmocode.palava.bridge.ServiceManager;
import de.cosmocode.palava.bridge.call.Call;
import de.cosmocode.palava.bridge.command.Alias;
import de.cosmocode.palava.bridge.request.HttpRequest;
import de.cosmocode.palava.bridge.scope.Scopes;
import de.cosmocode.palava.bridge.session.HttpSession;
import de.cosmocode.palava.ipc.Current;
import de.cosmocode.palava.ipc.IpcSession;

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
        binder.bind(LegacyFrameDecoder.class);
        
        // decoders/encoders
        binder.bind(LegacyHeaderDecoder.class).in(Singleton.class);
        binder.bind(LegacyContentEncoder.class).in(Singleton.class);
        binder.bind(LegacyHandler.class).in(Singleton.class);

        // empty set of aliases
        Multibinder.newSetBinder(binder, Alias.class);
        
        // cache used by executors
        binder.bind(CommandLoader.class).to(LegacyCommandLoader.class).in(Singleton.class);
        
        // executors
        binder.bind(Executor.class).to(LegacyExecutor.class).in(Singleton.class);
        binder.bind(JobExecutor.class).to(LegacyJobExecutor.class).in(Singleton.class);
        
        // server
        binder.bind(LegacyServer.class).in(Singleton.class);
        binder.bind(Server.class).to(LegacyServer.class).in(Singleton.class);
        binder.bind(ServiceManager.class).to(LegacyServer.class).in(Singleton.class);
        
        // request/session
        binder.bind(HttpRequest.class).annotatedWith(Current.class).to(HttpRequest.class);
        binder.bind(HttpSession.class).annotatedWith(Current.class).to(HttpSession.class);
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
    @Legacy
    ChannelPipeline provideChannelPipeline(LegacyFrameDecoder frameDecoder, LegacyHeaderDecoder decoder, 
        LegacyContentEncoder encoder, LegacyHandler handler) {
        return Channels.pipeline(frameDecoder, decoder, encoder, handler);
    }
    
    /**
     * Provides the current call.
     * 
     * @since 1.0
     * @return the current call
     */
    @Provides
    @Current
    Call provideCall() {
        return Scopes.getCurrentCall();
    }
    
    /**
     * Provides the current http request.
     * 
     * @since 1.0
     * @param call the current call
     * @return the current http request
     */
    @Provides
    HttpRequest provideHttpRequest(@Current Call call) {
        return call.getHttpRequest();
    }
    
    /**
     * Provides the current http session.
     * 
     * @since 1.0
     * @param session the underlying ipc session
     * @return a new {@link HttpSession}
     */
    @Provides
    HttpSession provideHttpSession(@Current IpcSession session) {
        return new LegacyHttpSessionAdapter(session);
    }

}
