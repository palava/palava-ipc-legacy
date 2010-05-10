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

import java.nio.ByteBuffer;

import de.cosmocode.patterns.Immutable;

/**
 * Protocol header definition.
 *
 * @author Willi Schoenborn
 */
@Immutable
public interface Header {

    /**
     * Retrieves the type of the corresponding call.
     * 
     * @since 1.0
     * @return the call type
     */
    CallType getCallType();
    
    /**
     * Retrieves the aliased name of the command.
     * 
     * @since 1.0
     * @return the aliased command name
     */
    String getAliasedName();
    
    /**
     * Retrievs the session id.
     * 
     * @since 1.0
     * @return the aliased session id
     */
    String getSessionId();
    
    /**
     * Retrieves the length of the provided content.
     * 
     * @since 1.0
     * @return the content length
     */
    int getLength();
    
    /**
     * Retrieves the content of this request.
     * 
     * @since 1.0
     * @return the content
     */
    ByteBuffer getContent();
    
}
