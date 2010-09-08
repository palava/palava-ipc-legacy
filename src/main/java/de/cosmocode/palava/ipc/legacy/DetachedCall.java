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

import de.cosmocode.palava.bridge.call.Call;
import de.cosmocode.palava.bridge.request.HttpRequest;
import de.cosmocode.palava.ipc.IpcCall;
import de.cosmocode.palava.ipc.IpcConnection;

/**
 * An extension to the {@link IpcCall} interface which allows
 * attaching detached call instances to a connection.
 *
 * @since 
 * @author Willi Schoenborn
 */
@SuppressWarnings("deprecation")
interface DetachedCall extends Call {

    /**
     * Attaches this call to the specified connection.
     * 
     * @since 1.0
     * @param request the surrounding connection
     * @throws NullPointerException if request is null
     */
    void attachTo(HttpRequest request);
    
    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this call is not yet attached to a connection
     */
    @Override
    IpcConnection getConnection();

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this call is not yet attached to a request
     */
    @Override
    HttpRequest getHttpRequest();
    
}
