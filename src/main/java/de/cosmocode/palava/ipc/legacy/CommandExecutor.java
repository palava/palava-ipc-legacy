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

import de.cosmocode.palava.bridge.Content;
import de.cosmocode.palava.bridge.call.Call;
import de.cosmocode.palava.bridge.command.Command;

/**
 * An executor for {@link Command}s.
 *
 * @since 1.0
 * @author Willi Schoenborn
 */
interface CommandExecutor {

    /**
     * Executes the given command using the specified call.
     * 
     * @since 1.0
     * @param command the command being executed
     * @param call the incoming call
     * @return the produced content or error content in case
     *         execution failed
     */
    Content execute(Command command, Call call);
    
}
