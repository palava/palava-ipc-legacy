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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.gag.annotation.disclaimer.LegacySucks;

import de.cosmocode.palava.bridge.Content;
import de.cosmocode.palava.bridge.Server;
import de.cosmocode.palava.bridge.call.Call;
import de.cosmocode.palava.bridge.command.Command;
import de.cosmocode.palava.bridge.command.CommandException;
import de.cosmocode.palava.bridge.command.Job;
import de.cosmocode.palava.bridge.command.Response;
import de.cosmocode.palava.bridge.request.HttpRequest;
import de.cosmocode.palava.bridge.session.HttpSession;
import de.cosmocode.patterns.Adapter;

/**
 * {@link Job} to {@link Command} adapter.
 *
 * @author Willi Schoenborn
 */
@SuppressWarnings("deprecation")
@LegacySucks
@Adapter(Command.class)
final class JobCommand implements Command {
    
    private static final Logger LOG = LoggerFactory.getLogger(JobCommand.class); 

    private final Server server;
        
    private final Job job;
    
    public JobCommand(Server server, Job job) {
        this.server = Preconditions.checkNotNull(server, "Server");
        this.job = Preconditions.checkNotNull(job, "Job");
    }
    
    @Override
    public Content execute(Call call) throws CommandException {
        
        final Response response = new DummyResponse();
        final HttpRequest request = call.getHttpRequest();
        LOG.debug("Local request: {}", request);
        final HttpSession session = request == null ? null : request.getHttpSession();
        LOG.debug("Local session: {}", session);

        try {
            job.process(call, response, session, server, null);
        /*CHECKSTYLE:OFF*/
        } catch (RuntimeException e) {
        /*CHECKSTYLE:ON*/
            throw e;
        /*CHECKSTYLE:OFF*/
        } catch (Exception e) {
        /*CHECKSTYLE:ON*/
            throw new CommandException(e);
        }
        
        Preconditions.checkState(response.hasContent(), "No content set");
        
        return response.getContent();
    }
    
    /**
     * Dummy implementation of the {@link Response} interface which allows simple
     * content retrieval.
     *
     * @author Willi Schoenborn
     */
    private final class DummyResponse implements Response {

        private Content content;

        @Override
        public void setContent(Content content) {
            this.content = content;
        }
        
        @Override
        public Content getContent() {
            return content;
        }

        @Override
        public boolean hasContent() {
            return content != null;
        }

    }

}
