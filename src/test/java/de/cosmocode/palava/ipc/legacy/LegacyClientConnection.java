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

public interface LegacyClientConnection extends ClientConnection {

    <T> T send(CallType type, String name, Map<String, Object> content);

    <T> T send(CallType type, String name, String sessionId, Map<String, Object> content);
    
}
