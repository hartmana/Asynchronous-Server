package cs455.scaling.threadpool;


import cs455.scaling.threadpool.exceptions.InvalidThreadCountException;
import cs455.scaling.threadpool.task.Taskable;
import cs455.scaling.threadpool.task.Worker;

import java.util.Deque;
import java.util.LinkedList;

/**
 * A ThreadPoolManager class
 *
 * @author ahrtmn, 24 02 2014
 */
public class ThreadPoolManager extends Thread
{
    /**
     * ThreadPool to be used by the manager
     */
    private ThreadPool threadPool;

    /**
     * Queue to be used for tasks to be done
     */
    private final Deque<Taskable> pendingTasks;

    /**
     * Object for lock
     */
    private final Object lock;


    /**
     * Constructor
     *
     * @param numThreads <code>int</code> denoting the number of threads this manager is supposed to coordinate.
     */
    public ThreadPoolManager(int numThreads) throws InvalidThreadCountException
    {
        threadPool = new ThreadPool(numThreads);
        pendingTasks = new LinkedList<Taskable>();
        lock = new Object();
    }

    /**
     * Method to add tasks to the server pool as worker threads are available.
     */
    @Override
    public void run()
    {
        // initialize all of the thread pools threads
        threadPool.initializer();



        while (!Thread.currentThread().isInterrupted())
        {

            // IF we have tasks that have not been completed
            if (pendingTasks.size() > 0)
            {
                Worker worker = threadPool.getWorker();


                // IF the worker was not null
                if (worker != null)
                {
                    // assign the task to the worker
                    worker.setTask(getNextTask());

                    synchronized (worker)
                    {
                        // notify the worker of its new task
                        worker.notify();
                    }

//                    System.out.println("Thread [" + worker.getID() + "] tasked with job.");

                }
//                else
//                    System.err.println("WORKER NULL");


            }
            else
            {

                /**
                 * Wait for the queue to grow
                 */
                try
                {
                    synchronized (lock)
//                    synchronized (this)
                    {
                        lock.wait();
                    }
                }
                catch (InterruptedException ie)
                {
                    ie.printStackTrace();
                }

            }
        }

    }

    /**
     * Method to get the next Taskable job
     *
     * @return <code>Taskable</code> job.
     */
    private Taskable getNextTask()
    {
        Taskable task;

        synchronized (pendingTasks)
        {
            task = pendingTasks.poll();
        }

        return task;
    }

    /**
     * Method to add a task to be processed by the thread pool.
     *
     * @param task <code>Taskable</code> the Thread is to execute.
     */
    public void addTask(Taskable task)
    {
        synchronized (lock)
        {


            synchronized (pendingTasks)
            {
                pendingTasks.add(task);
            }


            lock.notify();
        }


    }

}
