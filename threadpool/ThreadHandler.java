package cs455.scaling.threadpool;


/**
 * A ThreadHandler interface to define functionality needed by
 * working threads in the pool.
 *
 * @author ahrtmn, 24 02 2014
 */
public interface ThreadHandler
{
    /**
     * Method to add a thread who has finished their job back to the available
     * queue.
     */
    public void processCompletedThread(Thread thread);

}
