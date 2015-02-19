package cs455.scaling.threadpool;

import cs455.scaling.threadpool.exceptions.InvalidThreadCountException;
import cs455.scaling.threadpool.task.Worker;

import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;

/**
 * A ThreadPool class
 *
 * @author ahrtmn, 24 02 2014
 */
public class ThreadPool implements ThreadHandler
{
    /**
     * Queue for the available threads
     */
    private final Deque<Thread> freeQueue;

    /**
     * Queue for the working threads
     */
    private final Deque<Thread> workingQueue;

    /**
     * HashMap to contain all of the current workers linked by thread ID
     */
    private final HashMap<String, Worker> workerHashMap;

    /**
     * int for the number of threads this pool is to manage.
     */
    private final int numTotalThreads;

    /**
     * int for the number of threads currently available
     */
    private volatile int numAvailableThreads;


    /**
     * Constructor
     *
     * @param initialThreadCount <code>int</code> of the number of initial threads the pool is to have.
     */
    public ThreadPool(int initialThreadCount) throws InvalidThreadCountException
    {
        if (initialThreadCount < 1)
            throw new InvalidThreadCountException("Threadpool cannot be created with less than 1 thread!");

        numTotalThreads = initialThreadCount;
        freeQueue = new LinkedList<Thread>();
        workingQueue = new LinkedList<Thread>();
        workerHashMap = new HashMap<String, Worker>();
    }

    /**
     * Method to retrieve a thread available for work.
     *
     * @return <code>Thread</code> currently available for work.
     */
    public synchronized Worker getWorker()
    {

        if (numAvailableThreads == 0) return null;


        Thread thread;
        Worker worker;

        synchronized (freeQueue)
        {
            thread = freeQueue.poll();
        }

        synchronized (workingQueue)
        {
            workingQueue.add(thread);
        }

        synchronized (workerHashMap)
        {
            worker = workerHashMap.get(thread.getName());
        }

        --numAvailableThreads;


        return worker;

    }

    /**
     * Method to take a thread from the working queue and put them on the free queue
     *
     * @param thread <code>Thread</code> that has finished its task.
     */
    @Override
    public void processCompletedThread(Thread thread)
    {
        synchronized (workingQueue)
        {
            workingQueue.remove(thread);
        }

        synchronized (freeQueue)
        {
            freeQueue.add(thread);
        }


        ++numAvailableThreads;

//        System.out.println("Thread [" + thread.getName() + "] returned to pool.");
    }

    /**
     * Method to create the threads to be used in our thread pool
     */
    public void initializer()
    {
        /**
         * Creates n amount of threads for our pool
         */
        for (int i = 0; i < numTotalThreads; ++i)
        {
            String threadID = Integer.toString(i);
            Worker newWorker = new Worker(this);
            Thread newThread = new Thread(newWorker, threadID);

            newThread.start();

            freeQueue.add(newThread);
            workerHashMap.put(threadID, newWorker);

            ++numAvailableThreads;
        }


    }

    /**
     * Method to terminate all threads and destroy references to its queue items.
     */
    public void halt()
    {
        synchronized (freeQueue)
        {
            for (Thread t : freeQueue)
            {
                t.interrupt();
            }
        }

        synchronized (workingQueue)
        {
            for (Thread t : workingQueue)
            {
                t.interrupt();
            }
        }

        workerHashMap.clear();

        Thread.currentThread().interrupt();

    }
}
