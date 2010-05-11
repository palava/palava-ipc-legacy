package de.cosmocode.palava.ipc.legacy;

import com.google.gag.annotation.disclaimer.LegacySucks;

import de.cosmocode.palava.bridge.Content;
import de.cosmocode.palava.bridge.call.Call;
import de.cosmocode.palava.bridge.command.Command;
import de.cosmocode.palava.bridge.command.Job;
import de.cosmocode.palava.ipc.IpcCommand;

/**
 * A generic executor for {@link IpcCommand}s, {@link Job}s and
 * {@link Command}s.
 *
 * @since 1.0
 * @author Willi Schoenborn
 */
@SuppressWarnings("deprecation")
@LegacySucks
interface Executor {

    /**
     * Executes the name command/job using the specified call.
     * 
     * @since 1.0
     * @param call the incoming call
     * @return the produced content or error content in case execution failed
     */
    Content execute(Call call);
    
}
