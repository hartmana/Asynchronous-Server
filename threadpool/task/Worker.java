package cs455.scaling.threadpool.task;

import cs455.scaling.threadpool.ThreadHandler;

import java.io.IOException;

/**
 * A Worker class
 *
 * @author ahrtmn, 24 02 2014
 */
public class Worker implements Runnable
{
    /**
     * Runnable for the job that is to be loaded in
     */
    private Taskable task;

    /**
     * ThreadHandler reference to notify manager when we have finished a job
     */
    private final ThreadHandler caller;


    /**
     * Constructor
     */
    public Worker(ThreadHandler caller)
    {
        this(null, caller);
    }

    /**
     * Constructor
     *
     * @param task   <code>Taskable</code> to be completed by the thread holding this worker.
     * @param caller <code>ThreadHandler</code> reference to the manager of this thread.
     */
    public Worker(Taskable task, ThreadHandler caller)
    {
        this.task = task;
        this.caller = caller;
    }

    @Override
    public synchronized void run()
    {

        while (!Thread.currentThread().isInterrupted())
        {
            try
            {
                // IF there is a task available
                if (task != null)
                {
                    task.run();

                    // we have completed our task
                    task = null;

                    // notify the thread pool manager we have completed
                    caller.processCompletedThread(Thread.currentThread());
                }

                wait();
            }
            catch (InterruptedException ie)
            {
                System.err.println(ie.getMessage());
                ie.printStackTrace();
            }
            catch (IOException ioe)
            {
                System.err.println(ioe.getMessage());
                ioe.printStackTrace();
            }

        }
    }


    public void setTask(Taskable task)
    {
        this.task = task;
    }

}
