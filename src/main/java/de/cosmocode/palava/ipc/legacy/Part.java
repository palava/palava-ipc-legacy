package de.cosmocode.palava.ipc.legacy;

/**
 * Identifies the different parts of the protocol structure.
 *
 * @author Willi Schoenborn
 */
enum Part {

    TYPE, 
    
    COLON, 
    
    FIRST_SLASH, 
    
    SECOND_SLASH, 
    
    NAME, 
    
    THIRD_SLASH,
    
    SESSION_ID, 
    
    FOURTH_SLASH, 
    
    LEFT_PARENTHESIS, 
    
    CONTENT_LENGTH,
    
    RIGHT_PARENTHESIS, 
    
    QUESTION_MARK, 
    
    CONTENT;
    
}
