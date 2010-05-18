package de.cosmocode.palava.ipc.legacy.client;

import java.nio.ByteBuffer;

/**
 * 
 *
 * @since 
 * @author Willi Schoenborn
 */
interface ClientHeader {

    String getMimeType();
    
    ByteBuffer getContent();
    
}
