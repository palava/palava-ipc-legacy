package de.cosmocode.palava.ipc.legacy;

/**
 * A cache for commands.
 *
 * @since 1.0
 * @author Willi Schoenborn
 */
interface CommandCache {

    /**
     * Loads an object of the named class.
     * 
     * @since 1.0
     * @param aliasedName the aliased name
     * @return an object of the type addressed by aliasedName
     */
    Object load(String aliasedName);
    
}
