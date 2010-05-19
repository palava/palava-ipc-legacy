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

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;

import de.cosmocode.palava.bridge.MimeType;
import de.cosmocode.palava.ipc.IpcCommandExecutionException;
import de.cosmocode.palava.ipc.client.AbstractIpcClientConnection;
import de.cosmocode.palava.ipc.client.IpcClient;
import de.cosmocode.palava.ipc.client.IpcClientConnection;
import de.cosmocode.palava.ipc.client.SocketIpcClient;

/**
 * Legacy socket based {@link IpcClient} implementation.
 *
 * @since 
 * @author Willi Schoenborn
 */
public final class LegacySocketIpcClient extends SocketIpcClient {

    private static final Logger LOG = LoggerFactory.getLogger(LegacySocketIpcClient.class);
    
    private static final String JSON = MimeType.JSON.toString();
    
    private static final TypeReference<Map<String, Object>> MAP_REFERENCE = 
        new TypeReference<Map<String, Object>>() {
        
        };
    
    // FIXME dependency
    private final ObjectMapper mapper = new ObjectMapper();
    
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public LegacySocketIpcClient() {
        
    }
    
    public LegacySocketIpcClient(InetSocketAddress address) {
        super(address);
    }

    @Override
    protected IpcClientConnection newConnection(Socket socket) {
        LOG.debug("Opening connection using socket {}", socket);
        return new LegacySocketIpcClientConnection(socket);
    }
    
    /**
     * Legacy based {@link IpcClientConnection} implementation.
     *
     * @since 1.0
     * @author Willi Schoenborn
     */
    private final class LegacySocketIpcClientConnection extends AbstractIpcClientConnection {
        
        private final Socket socket;
        
        public LegacySocketIpcClientConnection(Socket socket) {
            this.socket = socket;
        }
        
        @Override
        public Future<Map<String, Object>> executeAsync(String name, Map<String, Object> arguments)
            throws IpcCommandExecutionException, IOException {
            
            // TODO configure
            final String sessionId = "";
            final String content = mapper.writeValueAsString(arguments);
            
            final int length = content.length();
            // TODO what about other types?
            final String request = String.format("json://%s/%s/(%s)?%s", name, sessionId, length, content);
            
            socket.getOutputStream().write(request.getBytes(Charsets.UTF_8));
            return executor.submit(new ExecutionTask(socket.getInputStream()));
        }
        
        /**
         * Internal {@link Future} implementation used as a return
         * value for {@link IpcClientConnection#executeAsync(String, Map)}.
         *
         * @since 1.0
         * @author Willi Schoenborn
         */
        private final class ExecutionTask implements Callable<Map<String, Object>> {

            private final InputStream stream;
            
            private Part part = Part.MIME_TYPE;
            
            public ExecutionTask(InputStream stream) {
                this.stream = stream;
            }
            
            /* CHECKSTYLE:OFF */
            @Override
            /* CHECKSTYLE:ON */
            public Map<String, Object> call() throws Exception {
                final ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
                int length = 0;
                while (true) {
                    final byte c = read();
                    buffer.writeByte(c);
                    switch (part) {
                        case MIME_TYPE: {
                            if (c == ':') {
                                final String type = buffer.toString(
                                    buffer.readerIndex(), buffer.readableBytes() - 1, Charsets.UTF_8
                                );
                                Preconditions.checkArgument(JSON.equals(type), "Expected json but was %s", type);
                                buffer.skipBytes(buffer.readableBytes());
                                part = Part.COLON;
                            }
                            break;
                        }
                        case COLON: {
                            checkState(c == '/', "Expected / but was %s", c);
                            buffer.skipBytes(1);
                            part = Part.FIRST_SLASH;
                            break;
                        }
                        case FIRST_SLASH: {
                            checkState(c == '/', "Expected / but was %s", c);
                            buffer.skipBytes(1);
                            part = Part.SECOND_SLASH;
                            break;
                        }
                        case SECOND_SLASH: {
                            checkState(c == '(', "Expected ( but was %s", c);
                            buffer.skipBytes(1);
                            part = Part.LEFT_PARENTHESIS;
                            break;
                        }
                        case LEFT_PARENTHESIS: {
                            checkState(Character.isDigit(c), "Expected digit but was", c);
                            part = Part.CONTENT_LENGTH;
                            break;
                        }
                        case CONTENT_LENGTH: {
                            if (c == ')') {
                                length = Integer.parseInt(buffer.toString(
                                    buffer.readerIndex(), buffer.readableBytes() - 1, Charsets.UTF_8)
                                );
                                buffer.skipBytes(buffer.readableBytes());
                                part = Part.RIGHT_PARENTHESIS;
                            } else {
                                checkState(Character.isDigit(c), "Expected digit but was", c);
                            }
                            break;
                        }
                        case RIGHT_PARENTHESIS: {
                            checkState(c == '?', "Expected ? but was %s", c);
                            buffer.skipBytes(1);
                            part = Part.QUESTION_MARK;
                            break;
                        }
                        case QUESTION_MARK: {
                            part = Part.CONTENT;
                            break;
                        }
                        case CONTENT: {
                            if (buffer.readableBytes() >= length) {
                                final String content = buffer.toString(Charsets.UTF_8);
                                return mapper.readValue(content, MAP_REFERENCE);
                            }
                            break;
                        }
                        default: {
                            throw new AssertionError("Default case matched part " + part);
                        }
                    }
                }
            }
            
            private byte read() throws IpcCommandExecutionException {
                try {
                    final byte b = (byte) stream.read();
                    if (b == -1) {
                        throw new IpcCommandExecutionException(new IOException("Reached end of stream"));
                    }
                    return b;
                } catch (IOException e) {
                    throw new IpcCommandExecutionException(e);
                }
            }

            private void checkState(boolean state, String format, byte c) {
                if (state) {
                    return;
                } else {
                    throw new IllegalArgumentException(String.format(format, (char) c)); 
                }
            }
            
        }
        
        @Override
        public void disconnect() throws IOException {
            socket.close();
        }
        
    }
    
}
