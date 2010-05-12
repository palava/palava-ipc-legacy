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

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.Maps;

import de.cosmocode.palava.bridge.call.CallType;
import de.cosmocode.palava.core.Framework;
import de.cosmocode.palava.core.Palava;

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
//    @Test
    public void boot() {
        final Framework framework = Palava.newFramework();
        framework.start();
        
        framework.stop();
    }

    /**
     * Tests send.
     */
    @Test
    public void send() {
        final Framework framework = Palava.newFramework();
        framework.start();
        
        final LegacyClient client = new LegacyNettyClient();
        final LegacyClientConnection  connection = client.connect("localhost", 8081);
        
        try {
            connection.send(CallType.OPEN, "", "123", Maps.<String, Object>newHashMap());
            
            final Map<String, Object> arguments = Maps.newHashMap();
            arguments.put("class", getClass());
            final String response = connection.send(CallType.JSON, Echo.class.getName(), "", arguments);
            Assert.assertTrue(response.contains(getClass().getName()));
            
            connection.send(CallType.CLOSE, "", "", Maps.<String, Object>newHashMap());
            
        } finally {
            connection.disconnect();
            client.shutdown();
            framework.stop();
        }
    }
    
}
