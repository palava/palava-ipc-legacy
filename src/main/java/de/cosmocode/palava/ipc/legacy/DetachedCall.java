package de.cosmocode.palava.ipc.legacy;

import de.cosmocode.palava.bridge.call.Call;
import de.cosmocode.palava.bridge.request.HttpRequest;
import de.cosmocode.palava.ipc.IpcCall;
import de.cosmocode.palava.ipc.IpcConnection;

/**
 * An extension to the {@link IpcCall} interface which allows
 * attaching detached call instances to a connection.
 *
 * @since 
 * @author Willi Schoenborn
 */
interface DetachedCall extends Call {

    /**
     * Attaches this call to the specified connection.
     * 
     * @since 1.0
     * @param request the surrounding connection
     * @throws NullPointerException if request is null
     */
    void attachTo(HttpRequest request);
    
    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this call is not yet attached to a connection
     */
    @Override
    IpcConnection getConnection();

    /**
     * {@inheritDoc}
     * @throws IllegalStateException if this call is not yet attached to a request
     */
    @Override
    HttpRequest getHttpRequest();
    
}
