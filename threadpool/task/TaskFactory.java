package cs455.scaling.threadpool.task;

import cs455.scaling.server.ServerClient;

import java.nio.channels.SelectionKey;

/**
 * A TaskFactory class
 *
 * @author ahrtmn, 26 02 2014
 */
public class TaskFactory
{

    private static final TaskFactory factoryInstance = new TaskFactory();


    private TaskFactory()
    {

    }


    /**
     * createTask method to return a specific task type determined by the caller
     *
     * @return <code>Taskable</code> of the desired type.
     */
    public static Taskable createTask(int taskType, SelectionKey key, ServerClient serverClient)
    {


        switch (taskType)
        {

            case Protocol.WRITE_TASK:
                return new WriteTask(key, serverClient);

            case Protocol.READ_TASK:
                return new ReadTask(key, serverClient);


            default:
                System.err.println("DEFAULT CASE SHOULDN'T OCCUR AT TASKFACTORY");
                break;
        }


        System.err.println("Error! Should not be here. TaskFactories can't make null Events. This is known.");
        System.err.println("Type: " + taskType);

        return null;

    }


    /**
     * TEST createTask method to return a specific task type determined by the caller
     *
     * @return <code>Taskable</code> of the desired type.
     */
    public static Taskable createTask(int taskType, String ID)
    {


        switch (taskType)
        {

            case Protocol.TEST_TASK:
                return new TestTask(ID);

            default:
                System.out.println("DEFAULT CASE SHOULDN'T OCCUR AT TASKFACTORY");
                break;
        }


        System.err.println("Error! Should not be here. TaskFactories can't make null Events. This is known.");
        System.err.println("Type: " + taskType);

        return null;

    }

    public static TaskFactory getFactoryInstance()
    {
        return factoryInstance;
    }
}
