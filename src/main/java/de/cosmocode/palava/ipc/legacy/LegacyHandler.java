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

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.concurrent.ThreadSafe;

import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandler.Sharable;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import de.cosmocode.palava.bridge.Content;
import de.cosmocode.palava.bridge.call.Arguments;
import de.cosmocode.palava.bridge.call.Call;
import de.cosmocode.palava.bridge.call.CallType;
import de.cosmocode.palava.bridge.content.JsonContent;
import de.cosmocode.palava.bridge.request.HttpRequest;
import de.cosmocode.palava.bridge.scope.Scopes;
import de.cosmocode.palava.bridge.session.HttpSession;
import de.cosmocode.palava.core.Registry.Proxy;
import de.cosmocode.palava.core.Registry.SilentProxy;
import de.cosmocode.palava.core.lifecycle.Initializable;
import de.cosmocode.palava.core.lifecycle.LifecycleException;
import de.cosmocode.palava.ipc.IpcCallCreateEvent;
import de.cosmocode.palava.ipc.IpcCallDestroyEvent;
import de.cosmocode.palava.ipc.IpcCallScope;
import de.cosmocode.palava.ipc.IpcConnectionCreateEvent;
import de.cosmocode.palava.ipc.IpcConnectionDestroyEvent;
import de.cosmocode.palava.ipc.IpcSession;
import de.cosmocode.palava.ipc.IpcSessionNotAttachedException;
import de.cosmocode.palava.ipc.IpcSessionProvider;
import de.cosmocode.palava.scope.AbstractScopeContext;

/**
 * Handler which process the legacy palava php protocol.
 *
 * @since 1.0
 * @author Willi Schoenborn
 */
@Sharable
@ThreadSafe
@SuppressWarnings("deprecation")
final class LegacyHandler extends SimpleChannelHandler implements Initializable {
    
    static final String REQUEST_URI = "REQUEST_URI";
    static final String HTTP_REFERER = "HTTP_REFERER";
    static final String REMOTE_ADDR = "REMOTE_ADDR";
    static final String HTTP_USER_AGENT = "HTTP_USER_AGENT";

    private static final Logger LOG = LoggerFactory.getLogger(LegacyHandler.class);
    
    private final ConcurrentMap<Channel, InternalHttpRequest> requests = Maps.newConcurrentMap();
    
    private final IpcConnectionCreateEvent connectionCreateEvent;
    
    private final IpcConnectionDestroyEvent connectionDestroyEvent;
    
    private final IpcCallCreateEvent callCreateEvent;
    
    private final IpcCallDestroyEvent callDestroyEvent;
    
    private final IpcSessionProvider sessionProvider;

    private final IpcCallScope scope;
    
    private final Executor executor;
    
    /**
     * When set to true {@link Channel}s will be set un-readable during
     * processing to prevent {@link OutOfMemoryError}s. 
     */
    private boolean throttle;
    
    @Inject
    public LegacyHandler(
        @Proxy IpcConnectionCreateEvent connectionCreateEvent, 
        @SilentProxy IpcConnectionDestroyEvent connectionDestroyEvent,
        @Proxy IpcCallCreateEvent callCreateEvent, 
        @SilentProxy IpcCallDestroyEvent callDestroyEvent,
        IpcSessionProvider sessionProvider, 
        IpcCallScope scope, 
        Executor executor) {
        this.connectionCreateEvent = Preconditions.checkNotNull(connectionCreateEvent, "CreateEvent");
        this.connectionDestroyEvent = Preconditions.checkNotNull(connectionDestroyEvent, "DestroyEvent");
        this.callCreateEvent = Preconditions.checkNotNull(callCreateEvent, "CreateEvent");
        this.callDestroyEvent = Preconditions.checkNotNull(callDestroyEvent, "DestroyEvent");
        this.sessionProvider = Preconditions.checkNotNull(sessionProvider, "SessionProvider");
        this.scope = Preconditions.checkNotNull(scope, "Scope");
        this.executor = Preconditions.checkNotNull(executor, "Executor");
    }

    @Inject(optional = true)
    void setThrottle(@Named(LegacyNettyConfig.THROTTLE) boolean throttle) {
        this.throttle = throttle;
    }
    
    @Override
    public void initialize() throws LifecycleException {
        LOG.info("Throttling is set to {}", throttle);
    }
    
    @Override
    public void channelConnected(ChannelHandlerContext context, ChannelStateEvent event) throws Exception {
        requests.put(event.getChannel(), new InternalHttpRequest());
    }
    
    @Override
    public void messageReceived(ChannelHandlerContext context, MessageEvent event) throws Exception {
        final Object message = event.getMessage();
        if (message instanceof Call) {
            final Call call = Call.class.cast(message);
            final CallType type = call.getHeader().getCallType();
            final Channel channel = event.getChannel();
            
            if (type == CallType.CLOSE) {
                channel.write(ChannelBuffers.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
                // nothing to do anymore
                return;
            }
            
            if (throttle) {
                channel.setReadable(false);
            }
            
            final Content content;
            
            if (type == CallType.OPEN) {
                content = open(call, channel);
            } else if (call instanceof DetachedCall) {
                final DetachedCall detachedCall = DetachedCall.class.cast(call);
                final HttpRequest request = requests.get(channel);
                detachedCall.attachTo(request);
                content = call(detachedCall);
            } else {
                throw new IllegalStateException(String.format("%s is of unknown type", call));
            }

            final ChannelFuture future = channel.write(content);
            
            if (LOG.isDebugEnabled()) {
                future.addListener(ProgressLogger.INSTANCE);
            }
            
            if (throttle) {
                future.addListener(SetReadable.INSTANCE);
            }
        }
    }
    
    private Content open(Call call, Channel channel) {
        final InternalHttpRequest request = requests.get(channel);
        final Arguments arguments = call.getArguments();
        request.setReferer(arguments.getString(HTTP_REFERER, null));
        final String remoteAddress = arguments.getString(REMOTE_ADDR, null);
        request.setRemoteAddress(remoteAddress);
        request.setRequestUri(arguments.getString(REQUEST_URI, null));
        request.setUserAgent(arguments.getString(HTTP_USER_AGENT, null));
        
        if (!request.isAttached()) {
            final String sessionId = call.getHeader().getSessionId();
            final IpcSession session = sessionProvider.getSession(sessionId, remoteAddress);
            final HttpSession httpSession = new LegacyHttpSessionAdapter(session);
            request.attachTo(httpSession);
        }
        
        connectionCreateEvent.eventIpcConnectionCreate(request);
        final String sessionId = request.getSession().getSessionId();
        return new JsonContent(Collections.singletonMap("sessionId", sessionId));
    }
    
    private Content call(final Call call) {
        try {
            callCreateEvent.eventIpcCallCreate(call);
            scope.enter(call);
            Scopes.setCurrentCall(call);
            return executor.execute(call);
        } finally {
            Scopes.clean();
            callDestroyEvent.eventIpcCallDestroy(call);
            scope.exit();
        }
    }
    
    @Override
    public void channelClosed(ChannelHandlerContext context, ChannelStateEvent event) throws Exception {
        final Channel channel = event.getChannel();
        final DetachedHttpRequest request = requests.remove(channel);
        LOG.trace("Closing connection {}", request);
        connectionDestroyEvent.eventIpcConnectionDestroy(request);
        request.clear();
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext context, ExceptionEvent event) throws Exception {
        final Channel channel = event.getChannel();
        final DetachedHttpRequest request = requests.get(channel);
        final Object remoteAddress = request == null ? channel.getRemoteAddress() : request.getRemoteAddress();
        LOG.error("Uncaught exception while communicating with " + remoteAddress, event.getCause());
        channel.close();
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
            if (session == null) {
                throw new IpcSessionNotAttachedException();
            } else {
                return session;
            }
        }
        
        @Override
        public IpcSession getSession() {
            if (getHttpSession() instanceof LegacyHttpSessionAdapter) {
                return LegacyHttpSessionAdapter.class.cast(session).getSession();
            } else {
                return session;
            }
        }
        
        @Override
        public void attachTo(HttpSession s) {
            this.session = Preconditions.checkNotNull(s, "Session");
        }
        
        @Override
        public boolean isAttached() {
            return session != null;
        }

        @Override
        public String getReferer() {
            return referer;
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
        public String getRequestUri() {
            return requestUri;
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
