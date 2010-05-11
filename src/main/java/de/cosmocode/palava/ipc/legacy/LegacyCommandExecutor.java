package de.cosmocode.palava.ipc.legacy;

import de.cosmocode.palava.bridge.Content;
import de.cosmocode.palava.bridge.call.Call;
import de.cosmocode.palava.bridge.command.Command;
import de.cosmocode.palava.bridge.command.CommandException;
import de.cosmocode.palava.bridge.content.ErrorContent;

/**
 * Legacy implementation of the {@link CommandExecutor} interface.
 *
 * @since 1.0
 * @author Willi Schoenborn
 */
final class LegacyCommandExecutor implements CommandExecutor {

    @Override
    public Content execute(Command command, Call call) {
        try {
            return command.execute(call);
        } catch (CommandException e) {
            return ErrorContent.create(e);
        }
    }

}
