package cs455.scaling.threadpool.task;

/**
 * A TestTask class
 *
 * @author ahrtmn, 26 02 2014
 */
public class TestTask implements Taskable
{
    private String ID;

    TestTask(String ID)
    {
        this.ID = ID;
    }

    @Override
    public void run()
    {
        if (ID.equals("500") || ID.equals("1500"))
            System.err.println("Test task [" + ID + "] executed");
        else
            System.out.println("Test task [" + ID + "] executed");

    }

}
