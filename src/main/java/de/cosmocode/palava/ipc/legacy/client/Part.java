package de.cosmocode.palava.ipc.legacy.client;

/**
 * Identifies the different parts of the incoming client protocol structure.
 *
 * @since 1.0
 * @author Willi Schoenborn
 */
public enum Part {

    MIME_TYPE, 
    
    COLON, 
    
    FIRST_SLASH, 
    
    SECOND_SLASH,
    
    LEFT_PARENTHESIS, 
    
    CONTENT_LENGTH, 
    
    RIGHT_PARENTHESIS, 
    
    QUESTION_MARK, 
    
    CONTENT;
    
}
