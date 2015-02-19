package cs455.scaling.threadpool.task;

/**
 * A Protocol interface
 *
 * @author ahrtmn, 26 02 2014
 */
public interface Protocol
{
    /**
     * Message types and their corresponding integer values
     */
    public static final int TEST_TASK = 2000;
    public static final int ACCEPT_TASK = 3000;
    public static final int READ_TASK = 4000;
    public static final int WRITE_TASK = 5000;
}
