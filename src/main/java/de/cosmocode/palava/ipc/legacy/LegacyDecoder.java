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

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelHandler.Sharable;
import org.jboss.netty.handler.codec.oneone.OneToOneDecoder;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;

import de.cosmocode.collections.utility.AbstractUtilityMap;
import de.cosmocode.collections.utility.UtilityMap;
import de.cosmocode.collections.utility.UtilitySet;
import de.cosmocode.commons.io.ByteBuffers;
import de.cosmocode.json.JSON;
import de.cosmocode.palava.bridge.ConnectionLostException;
import de.cosmocode.palava.bridge.Header;
import de.cosmocode.palava.bridge.call.Arguments;
import de.cosmocode.palava.bridge.call.BinaryCall;
import de.cosmocode.palava.bridge.call.Call;
import de.cosmocode.palava.bridge.call.DataCall;
import de.cosmocode.palava.bridge.call.JsonCall;
import de.cosmocode.palava.bridge.call.MissingArgumentException;
import de.cosmocode.palava.bridge.call.TextCall;
import de.cosmocode.palava.bridge.request.HttpRequest;
import de.cosmocode.palava.ipc.IpcCall;
import de.cosmocode.palava.ipc.IpcConnection;
import de.cosmocode.palava.scope.AbstractScopeContext;
import de.cosmocode.patterns.ThreadSafe;

/**
 * A decoder which decodes {@link Header}s into {@link IpcCall}s.
 *
 * @since 1.0
 * @author Willi Schoenborn
 */
@Sharable
@ThreadSafe
final class LegacyDecoder extends OneToOneDecoder {

    @Override
    protected Object decode(ChannelHandlerContext context, Channel channel, Object message) throws Exception {
        if (message instanceof Header) {
            final Header header = Header.class.cast(message);
            switch (header.getCallType()) {
                case OPEN: {
                    return new OpenCall(header);
                }
                case DATA: {
                    return new InternalDataCall(header);
                }
                case JSON: {
                    return new InternalJsonCall(header);
                }
                case TEXT: {
                    return new InternalTextCall(header);
                }
                case BINARY: {
                    return new InternalBinaryCall(header);
                }
                case CLOSE: {
                    return new CloseCall(header);
                }
                default: {
                    throw new UnsupportedOperationException("Unknown type " + header.getCallType());
                }
            }
        } else {
            return message;
        }
    }
    
    /**
     * Abstract implementation of the {@link Call} interface.
     *
     * @since 1.0
     * @author Willi Schoenborn
     */
    private abstract static class AbstractCall extends AbstractScopeContext implements DetachedCall {
        
        private final Header header;
        
        private Map<Object, Object> context;
        
        private HttpRequest request;
        
        public AbstractCall(Header header) {
            this.header = header;
        }

        @Override
        public void attachTo(HttpRequest r) {
            this.request = Preconditions.checkNotNull(r, "Request");
        }
        
        @Override
        protected Map<Object, Object> context() {
            if (context == null) {
                context = Maps.newHashMap();
            }
            return context;
        }
        
        @Override
        public void discard() throws ConnectionLostException, IOException {
            // nothing to do
        }
        
        @Override
        public Arguments getArguments() {
            throw new UnsupportedOperationException(String.format("%s does not support arguments", getClass()));
        }
        
        @Override
        public IpcConnection getConnection() {
            return getHttpRequest();
        }
        
        @Override
        public Header getHeader() {
            return header;
        }
        
        @Override
        public HttpRequest getHttpRequest() {
            return request;
        }
        
        @Override
        public InputStream getInputStream() {
            return ByteBuffers.asInputStream(getHeader().getContent());
        }
        
    }
    
    /**
     * Internal implementation of the call interface which is used to open a connection.
     *
     * @since 1.0
     * @author Willi Schoenborn
     */
    private static final class OpenCall extends AbstractCall implements Call {
        
        private final JsonCall call;
        
        private OpenCall(Header header) {
            super(header);
            this.call = new InternalJsonCall(header);
        }
        
        @Override
        public Arguments getArguments() {
            return call.getArguments();
        }
        
    }
    
    /**
     * Internal implementation of the {@link DataCall} interface.
     *
     * @since 1.0
     * @author Willi Schoenborn
     */
    private static final class InternalDataCall extends AbstractCall implements DataCall {

        private final JsonCall call;
        
        private final String text;
        
        public InternalDataCall(Header header) {
            super(header);
            this.call = new InternalJsonCall(header);
            this.text = new String(header.getContent().array(), Charsets.UTF_8);
        }

        @Override
        public Arguments getArguments() {
            return call.getArguments();
        }
        
        @Override
        public Map<String, String> getStringedArguments() {
            if (!text.startsWith("{")) {
                // dirty hack to ignore everything that is not a JsonObject
                return new LinkedHashMap<String, String>();
            } else {
                final Map<String, String> stringed = new LinkedHashMap<String, String>();
                for (final Map.Entry<String, Object> entry : getArguments().entrySet()) {
                    final Object value = entry.getValue();
                    stringed.put(entry.getKey(), value == null ? null : value.toString());
                }
                return stringed;
            }
        }
        
    }
    
    /**
     * Internal implementation of the {@link JsonCall} interface.
     *
     * @since 1.0
     * @author Willi Schoenborn
     */
    private static final class InternalJsonCall extends AbstractCall implements JsonCall {

        private JSONObject json;
        private UtilityMap<String, Object> map;
        private Arguments arguments;
        
        public InternalJsonCall(Header header) {
            super(header);
        }

        @Override
        public JSONObject getJSONObject() throws ConnectionLostException, JSONException {
            if (json == null) {
                final String text = new String(getHeader().getContent().array(), Charsets.UTF_8);
                json = new JSONObject(text);
            }
            return json;
        }
        
        @Override
        public Arguments getArguments() {
            if (arguments == null) {
                arguments = new InternalArguments();
            }
            return arguments;
        }
        
        /**
         * Internal implementation of the {@link Arguments} interface.
         *
         * @since 1.0 
         * @author Willi Schoenborn
         */
        private class InternalArguments extends AbstractUtilityMap<String, Object> implements Arguments {
            
            @Override
            public void require(String... keys) throws MissingArgumentException {
                final Set<String> keySet = keySet();
                for (String key : keys) {
                    if (keySet.contains(key)) {
                        continue;
                    } else {
                        throw new MissingArgumentException(key);
                    }
                }                
            }
            
            private void lazyLoad() {
                if (json == null) {
                    try {
                        json = getJSONObject();
                    } catch (JSONException e) {
                        throw new IllegalArgumentException(e);
                    }
                }
                if (map == null) {
                    map = JSON.asMap(json);
                }
            }
            
            @Override
            public UtilitySet<Map.Entry<String, Object>> entrySet() {
                lazyLoad();
                return map.entrySet();
            }
            
            @Override
            public Object put(String key, Object value) {
                lazyLoad();
                try {
                    return json.put(key, value);
                } catch (JSONException e) {
                    throw new IllegalArgumentException(e);
                }
            }
            
        }
        
    }
    
    /**
     * Internal implementation of the {@link TextCall} interface.
     *
     * @since 1.0
     * @author Willi Schoenborn
     */
    private static final class InternalTextCall extends AbstractCall implements TextCall {
        
        private String text;
        
        public InternalTextCall(Header header) {
            super(header);
        }

        @Override
        public String getText() {
            if (text == null) {
                text = new String(getHeader().getContent().array(), Charsets.UTF_8);
            }
            return text;
        }
        
    }
    
    /**
     * Internal implementation of the {@link BinaryCall} interface.
     *
     * @since 1.0
     * @author Willi Schoenborn
     */
    private static final class InternalBinaryCall extends AbstractCall implements BinaryCall {

        public InternalBinaryCall(Header header) {
            super(header);
        }

    }
    
    /**
     * Internal implementation of the call interface which is used to close a connection.
     *
     * @since 1.0
     * @author Willi Schoenborn
     */
    private static final class CloseCall extends AbstractCall implements Call {
        
        private CloseCall(Header header) {
            super(header);
        }
        
    }

}
