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

import de.cosmocode.palava.bridge.call.CallType;
import de.cosmocode.palava.ipc.netty.ClientConnection;

/**
 * Extension of the {@link ClientConnection} interface which
 * adds legacy specific send methods.
 *
 * @since 1.0
 * @author Willi Schoenborn
 */
public interface LegacyClientConnection extends ClientConnection {

    /**
     * Sends a call of the specified type to the named job/command using
     * the specified arguments.
     * 
     * @since 1.0
     * @param <T> generic return value
     * @param type the call type
     * @param name the job/commmand name
     * @param arguments the arguments
     * @return the content/result
     */
    <T> T send(CallType type, String name, Map<String, Object> arguments);

    /**
     * Sends a call of the specified type to the named job/command using
     * the specified arguments.
     * 
     * @since 1.0
     * @param <T> generic return value
     * @param type the call type
     * @param name the job/commmand name
     * @param sessionId the session id
     * @param arguments the arguments
     * @return the content/result
     */
    <T> T send(CallType type, String name, String sessionId, Map<String, Object> arguments);
    
}
