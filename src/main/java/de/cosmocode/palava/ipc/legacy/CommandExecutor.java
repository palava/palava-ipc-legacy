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
