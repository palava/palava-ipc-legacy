package de.cosmocode.palava.ipc.legacy;

import com.google.common.base.Preconditions;
import com.google.gag.annotation.disclaimer.LegacySucks;
import com.google.inject.Inject;

import de.cosmocode.palava.bridge.Content;
import de.cosmocode.palava.bridge.Server;
import de.cosmocode.palava.bridge.call.Call;
import de.cosmocode.palava.bridge.command.Job;
import de.cosmocode.palava.bridge.command.Response;
import de.cosmocode.palava.bridge.content.ErrorContent;
import de.cosmocode.palava.bridge.session.HttpSession;

/**
 * Legacy implementation of the {@link JobExecutor} interface.
 *
 * @since 1.0
 * @author Willi Schoenborn
 */
@SuppressWarnings("deprecation")
@LegacySucks
final class LegacyJobExecutor implements JobExecutor {

    private final Server server;
    
    @Inject
    public LegacyJobExecutor(Server server) {
        this.server = Preconditions.checkNotNull(server, "Server");
    }
    
    @Override
    public Content execute(Job job, Call call) {
        final Response response = new InternalResponse();
        final HttpSession session = call.getHttpRequest().getHttpSession();
        
        try {
            job.process(call, response, session, server, null);
            Preconditions.checkState(response.hasContent(), "No content set");
            return response.getContent();
        /* CHECKSTYLE:OFF */
        } catch (Exception e) {
        /* CHECKSTYLE:ON */
            return ErrorContent.create(e);
        }
    }
    
    /**
     * Internal implementation of the {@link Response} interface which allows simple
     * content retrieval.
     *
     * @author Willi Schoenborn
     */
    private final class InternalResponse implements Response {

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
