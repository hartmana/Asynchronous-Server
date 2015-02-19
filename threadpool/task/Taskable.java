package cs455.scaling.threadpool.task;

import java.io.IOException;

/**
 * A Taskable interface
 *
 * @author ahrtmn, 25 02 2014
 */
public interface Taskable
{
    /**
     * Method containing the actual task to be run
     */
    public void run() throws IOException;

}
