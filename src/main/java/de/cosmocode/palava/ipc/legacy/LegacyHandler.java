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

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.ChannelHandler.Sharable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Maps;
import com.google.gag.annotation.disclaimer.LegacySucks;
import com.google.inject.Inject;

import de.cosmocode.collections.Procedure;
import de.cosmocode.palava.bridge.Content;
import de.cosmocode.palava.bridge.Server;
import de.cosmocode.palava.bridge.call.Arguments;
import de.cosmocode.palava.bridge.call.Call;
import de.cosmocode.palava.bridge.call.CallType;
import de.cosmocode.palava.bridge.command.Command;
import de.cosmocode.palava.bridge.command.CommandException;
import de.cosmocode.palava.bridge.command.CommandManager;
import de.cosmocode.palava.bridge.command.Job;
import de.cosmocode.palava.bridge.content.ErrorContent;
import de.cosmocode.palava.bridge.request.HttpRequest;
import de.cosmocode.palava.bridge.session.HttpSession;
import de.cosmocode.palava.core.Registry;
import de.cosmocode.palava.ipc.IpcCallScope;
import de.cosmocode.palava.ipc.IpcCommand;
import de.cosmocode.palava.ipc.IpcConnectionCreateEvent;
import de.cosmocode.palava.ipc.IpcConnectionDestroyEvent;
import de.cosmocode.palava.ipc.IpcSession;
import de.cosmocode.palava.ipc.IpcSessionProvider;
import de.cosmocode.palava.scope.AbstractScopeContext;
import de.cosmocode.patterns.ThreadSafe;

/**
 * Handler which process the legacy palava php protocol.
 *
 * @since 1.0
 * @author Willi Schoenborn
 */
@SuppressWarnings("deprecation")
@LegacySucks
@Sharable
@ThreadSafe
final class LegacyHandler extends SimpleChannelHandler {
    
    private static final String REQUEST_URI = "REQUEST_URI";
    private static final String HTTP_REFERER = "HTTP_REFERER";
    private static final String REMOTE_ADDR = "REMOTE_ADDR";
    private static final String HTTP_USER_AGENT = "HTTP_USER_AGENT";

    private static final Logger LOG = LoggerFactory.getLogger(LegacyHandler.class);
    
    private final ConcurrentMap<Channel, InternalHttpRequest> requests = new MapMaker().makeMap();
    
    private final Registry registry;
    
    private final IpcSessionProvider sessionProvider;

    private final IpcCallScope scope;
    
    private final CommandManager commandManager;
    
    private final Server server;
    
    @Inject
    public LegacyHandler(Registry registry, IpcSessionProvider sessionProvider, 
        IpcCallScope scope, CommandManager commandManager, Server server) {
        this.registry = Preconditions.checkNotNull(registry, "Registry");
        this.sessionProvider = Preconditions.checkNotNull(sessionProvider, "SessionProvider");
        this.scope = Preconditions.checkNotNull(scope, "Scope");
        this.commandManager = Preconditions.checkNotNull(commandManager, "CommandManager");
        this.server = Preconditions.checkNotNull(server, "Server");
    }

    @Override
    public void channelConnected(ChannelHandlerContext context, ChannelStateEvent event) throws Exception {
        final InternalHttpRequest request = new InternalHttpRequest();
        requests.put(event.getChannel(), request);
    }
    
    @Override
    public void messageReceived(ChannelHandlerContext context, MessageEvent event) throws Exception {
        final Object message = event.getMessage();
        if (message instanceof Call) {
            final Call call = Call.class.cast(message);
            final CallType type = call.getHeader().getCallType();
            final Channel channel = event.getChannel();
            
            if (type == CallType.OPEN) {
                open(call, channel);
            } else if (type == CallType.CLOSE) {
                close(channel);
            } else if (call instanceof DetachedCall) {
                final DetachedCall detachedCall = DetachedCall.class.cast(call);
                final HttpRequest request = requests.get(channel);
                call(detachedCall, request);
            } else {
                throw new IllegalStateException(String.format("%s is of unknown type", call));
            }
        }
    }
    
    private void open(Call call, Channel channel) {
        final InternalHttpRequest request = requests.get(channel);
        final Arguments arguments = call.getArguments();
        request.setReferer(arguments.getString(HTTP_REFERER));
        request.setRemoteAddress(arguments.getString(REMOTE_ADDR));
        request.setRequestUri(arguments.getString(REQUEST_URI));
        request.setUserAgent(arguments.getString(HTTP_USER_AGENT));
        
        final String sessionId = call.getHeader().getSessionId();
        final IpcSession session = sessionProvider.getSession(sessionId, null);
        final HttpSession httpSession = new LegacyHttpSessionAdapter(session);
        request.attachTo(httpSession);
        
        registry.notify(IpcConnectionCreateEvent.class, new Procedure<IpcConnectionCreateEvent>() {
            
            @Override
            public void apply(IpcConnectionCreateEvent input) {
                input.eventIpcConnectionCreate(request);
            }
            
        });
    }
    
    private Content call(DetachedCall call, HttpRequest request) {
        call.attachTo(request);
    
        final Object raw;
        
        try {
            raw = commandManager.forName(call.getHeader().getAliasedName());
        /* CHECKSTYLE:OFF */
        } catch (RuntimeException e) {
        /* CHECKSTYLE:ON */
            return ErrorContent.create(e);
        }
        
        final Command command;
        
        if (raw instanceof IpcCommand) {
            command = new IpcCommandCommand(IpcCommand.class.cast(raw));
        } else if (raw instanceof Job) {
            command = new JobCommand(server, Job.class.cast(raw));
        } else if (raw instanceof Command) {
            command = Command.class.cast(raw);
        } else {
            throw new UnsupportedOperationException(String.format("Unknown command %s", raw));
        }
            
        try {
            scope.enter(call);
            return command.execute(call);
        /* CHECKSTYLE:OFF */
        } catch (RuntimeException e) {
        /* CHECKSTYLE:ON */
            LOG.warn("Unexpected exception during execute", e);
            return ErrorContent.create(e);
        } catch (CommandException e) {
            LOG.warn("Exception during execute", e);
            return ErrorContent.create(e);
        } finally {
            scope.exit();
        }
    }
    
    private void close(Channel channel) {
        final HttpRequest request = requests.remove(channel);
        
        registry.notifySilent(IpcConnectionDestroyEvent.class, new Procedure<IpcConnectionDestroyEvent>() {
           
            @Override
            public void apply(IpcConnectionDestroyEvent input) {
                input.eventIpcConnectionDestroy(request);
            }
            
        });
        
        request.clear();
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext context, ExceptionEvent event) throws Exception {
        final Channel channel = event.getChannel();
        final Content content = ErrorContent.create(event.getCause());
        LOG.trace("Writing error content to channel");
        channel.write(content).addListener(new ChannelFutureListener() {
            
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                future.getChannel().close();
            }
            
        });
    }
    
    @Override
    public void channelDisconnected(ChannelHandlerContext context, ChannelStateEvent event) throws Exception {
        Preconditions.checkState(requests.get(event.getChannel()) == null, "Request still set");
    }
    
    /**
     * Internal implementation of the {@link DetachedHttpRequest} interface.
     *
     * @since 1.0
     * @author Willi Schoenborn
     */
    private static final class InternalHttpRequest extends AbstractScopeContext implements DetachedHttpRequest {

        private Map<Object, Object> context;
        
        private HttpSession session;
        
        private String referer;
        
        private String remoteAddress;
        
        private String requestUri;
        
        private String userAgent;
        
        @Override
        protected Map<Object, Object> context() {
            if (context == null) {
                context = Maps.newHashMap();
            }
            return context;
        }

        @Override
        public HttpSession getHttpSession() {
            Preconditions.checkState(session != null, "Not yet attached to a session");
            return session;
        }
        
        @Override
        public IpcSession getSession() {
            return getHttpSession();
        }
        
        @Override
        public void attachTo(HttpSession s) {
            this.session = Preconditions.checkNotNull(s, "Session");
        }

        @Override
        public URL getReferer() {
            try {
                return referer == null ? null : new URL(referer);
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException(referer);
            }
        }
        
        void setReferer(String referer) {
            this.referer = referer;
        }

        @Override
        public String getRemoteAddress() {
            return remoteAddress;
        }
        
        void setRemoteAddress(String remoteAddress) {
            this.remoteAddress = remoteAddress;
        }

        @Override
        public URI getRequestUri() {
            try {
                return requestUri == null ? null : new URI(requestUri);
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException(requestUri);
            }
        }

        void setRequestUri(String requestUri) {
            this.requestUri = requestUri;
        }
        
        @Override
        public String getUserAgent() {
            return userAgent;
        }

        void setUserAgent(String userAgent) {
            this.userAgent = userAgent;
        }
        
    }
    
}
