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
import java.util.Map;

import org.junit.Test;

import com.google.common.collect.Maps;

import de.cosmocode.palava.core.Framework;
import de.cosmocode.palava.core.Palava;
import de.cosmocode.palava.ipc.IpcCommandExecutionException;
import de.cosmocode.palava.ipc.client.IpcClient;
import de.cosmocode.palava.ipc.client.IpcClientConnection;
import de.cosmocode.palava.ipc.legacy.client.LegacySocketIpcClient;

/**
 * Tests legacy boot, etc.
 *
 * @since 1.0 
 * @author Willi Schoenborn
 */
public final class LegacyTest {

    /**
     * Tests boot.
     */
    @Test
    public void boot() {
        final Framework framework = Palava.newFramework();
        framework.start();
        
        framework.stop();
    }

    /**
     * Tests send.
     * 
     * @throws IOException should not happen
     * @throws IpcCommandExecutionException should not happen 
     */
    @Test
    public void send() throws IOException, IpcCommandExecutionException {
        final Framework framework = Palava.newFramework();
        framework.start();
        
        
        final IpcClient client = new LegacySocketIpcClient();
        final IpcClientConnection connection = client.connect("localhost", 8081);
        
        try {
            final Map<String, Object> arguments = Maps.newHashMap();
            arguments.put("name", getClass().getName());
            connection.execute(Echo.class, arguments);
        } finally {
            connection.disconnect();
            framework.stop();
        }
    }
    
}
