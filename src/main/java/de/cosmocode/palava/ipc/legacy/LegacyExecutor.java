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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.gag.annotation.disclaimer.LegacySucks;
import com.google.inject.Inject;

import de.cosmocode.palava.bridge.Content;
import de.cosmocode.palava.bridge.call.Call;
import de.cosmocode.palava.bridge.command.Job;
import de.cosmocode.palava.bridge.content.ErrorContent;
import de.cosmocode.palava.bridge.content.JsonContent;
import de.cosmocode.palava.ipc.IpcCommand;
import de.cosmocode.palava.ipc.IpcCommandExecutionException;
import de.cosmocode.palava.ipc.IpcCommandExecutor;

/**
 * Legacy implementation of the {@link Executor} interface.
 *
 * @since 1.1
 * @author Willi Schoenborn
 */
@SuppressWarnings("deprecation")
@LegacySucks
final class LegacyExecutor implements Executor {

    private static final Logger LOG = LoggerFactory.getLogger(LegacyExecutor.class);
    
    private final IpcCommandExecutor commandExecutor;
    
    private final JobExecutor jobExecutor;
    
    private final CommandLoader loader;
    
    @Inject
    public LegacyExecutor(IpcCommandExecutor commandExecutor, JobExecutor jobExecutor, 
        CommandLoader loader) {
        this.commandExecutor = Preconditions.checkNotNull(commandExecutor, "CommandExecutor");
        this.jobExecutor = Preconditions.checkNotNull(jobExecutor, "JobExecutor");
        this.loader = Preconditions.checkNotNull(loader, "Loader");
    }
    
    @Override
    public Content execute(Call call) {
        try {
            final String name = call.getHeader().getAliasedName();
            final Object raw = loader.load(name);
            
            if (raw instanceof IpcCommand) {
                LOG.trace("Executing ipc command {}", raw);
                final Map<String, Object> result = commandExecutor.execute(raw.getClass().getName(), call);
                return new JsonContent(result);
            } else if (raw instanceof Job) {
                final Job job = Job.class.cast(raw);
                LOG.trace("Processing job {}", job);
                return jobExecutor.execute(job, call);
            } else {
                throw new IllegalArgumentException("Unknown class " + raw);
            }
        } catch (IpcCommandExecutionException e) {
            return ErrorContent.create(e);
        /* CHECKSTYLE:OFF */
        } catch (Exception e) {
        /* CHECKSTYLE:ON */
            return ErrorContent.create(e);
        }
    }

}
