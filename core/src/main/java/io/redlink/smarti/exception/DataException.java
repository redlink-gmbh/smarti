package io.redlink.smarti.exception;

/**
 * Interface that allows to provide structured information
 * about the cause of an exception.
 * 
 * @author Rupert Westenthaler
 *
 * @param <T>
 */
public interface DataException<T> {
    
    /**
     * Getter for structured information describing the cause for the exception
     * @return the structured information or <code>null</code> if not available
     */
    T getData();
    
}
