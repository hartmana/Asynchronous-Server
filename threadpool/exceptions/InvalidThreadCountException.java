package cs455.scaling.threadpool.exceptions;

/**
 * A InvalidThreadCountException exception class to be used when
 * invalid thread numbers are specified to the thread pool
 * manager or thread pool itself.
 *
 * @author ahrtmn, 24 02 2014
 */
public class InvalidThreadCountException extends Exception
{
    public InvalidThreadCountException(String message)
    {
        super(message);
    }
}
