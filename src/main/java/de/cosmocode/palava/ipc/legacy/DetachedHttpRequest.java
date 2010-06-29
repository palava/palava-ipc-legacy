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

import de.cosmocode.palava.bridge.request.HttpRequest;
import de.cosmocode.palava.bridge.session.HttpSession;

/**
 * Extension of the {@link HttpRequest} interface
 * which allows late attaching to a session.
 *
 * @since 
 * @author Willi Schoenborn
 */
public interface DetachedHttpRequest extends HttpRequest {

    /**
     * Attaches this request to the specified session.
     * 
     * @since 1.0
     * @param session the session
     * @throws NullPointerException if session is null
     */
    void attachTo(HttpSession session);
    
    /**
     * Checks whether this request is already attached to a session.
     * 
     * @since 1.1
     * @return true if this request is attached to a session, false otherwise
     */
    boolean isAttached();
    
    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this request is not yet attached to a session
     */
    @Override
    HttpSession getHttpSession();
    
}
