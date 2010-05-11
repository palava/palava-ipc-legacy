package de.cosmocode.palava.ipc.legacy;

import com.google.gag.annotation.disclaimer.LegacySucks;

import de.cosmocode.palava.bridge.Content;
import de.cosmocode.palava.bridge.call.Call;
import de.cosmocode.palava.bridge.command.Job;

/**
 * An executor for {@link Job}s.
 *
 * @since 1.0
 * @author Willi Schoenborn
 */
@SuppressWarnings("deprecation")
@LegacySucks
interface JobExecutor {

    /**
     * Executes the given job using the specified call.
     * 
     * @since 1.0
     * @param job the job being executed
     * @param call the incoming call
     * @return the produced content or error content in case
     *         execution failed
     */
    Content execute(Job job, Call call);
    
}
