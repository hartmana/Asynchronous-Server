package cs455.scaling.server;

import cs455.scaling.threadpool.ThreadPoolManager;
import cs455.scaling.threadpool.exceptions.InvalidThreadCountException;
import cs455.scaling.threadpool.task.Protocol;
import cs455.scaling.threadpool.task.TaskFactory;
import cs455.scaling.threadpool.task.Taskable;
import cs455.scaling.utility.OpsChangeRequest;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

/**
 * A Server class to listen to incoming connections from clients, accept data in the form
 * of a byte array, compute the hash for that data and acknowledge the receipt of this data
 * by responding with the hash code.
 *
 * @author ahrtmn, 24 02 2014
 */
public class Server implements Runnable, ServerClient
{

    /**
     * ServerSocketChannel to listen to incoming connections
     */
    ServerSocketChannel serverSocketChannel;

    /**
     * Selector to be used for incoming connections
     */
    private Selector selector;

    /**
     * ThreadPoolManager to be used to process incoming messages
     */
    private ThreadPoolManager threadPoolManager;

    /**
     * Queue for pending selector key changes
     */
    private final Queue<OpsChangeRequest> pendingChanges;

    /**
     * TaskFactory reference
     */
    private final TaskFactory taskFactory = TaskFactory.getFactoryInstance();

    private int count;


    /**
     * Constructor for the Server class
     *
     * @param portNum        <code>int</code> of the port number to listen for incoming connections on.
     * @param threadPoolSize <code>int</code> denoting the total number of threads to be used by the threadpool.
     */
    public Server(int portNum, int threadPoolSize)
    {

        count = 0;
        pendingChanges = new LinkedList<OpsChangeRequest>();

        try
        {
            threadPoolManager = new ThreadPoolManager(threadPoolSize);

            selector = SelectorProvider.provider().openSelector();

            /**
             * Open the server socket channel and prepare for receiving connections
             */
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.socket().bind(new InetSocketAddress(portNum));
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        }
        catch (InvalidThreadCountException itce)
        {
            System.out.println(itce.getMessage());
            itce.printStackTrace();
        }
        catch (IOException ioe)
        {
            System.out.println(ioe.getMessage());
            ioe.printStackTrace();
        }


    }

    /**
     * Method to accept a connection from a client.
     *
     * @param key <code>SelectionKey</code> containing SocketChannel to be accepted.
     * @throws IOException
     */
    private void accept(SelectionKey key) throws IOException
    {
        // For an accept to be pending the channel must be a server socket channel.
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();

        // Accept the connection and make it non-blocking
        SocketChannel socketChannel = serverSocketChannel.accept();
        socketChannel.configureBlocking(false);


        /**
         * ClientInfo containing the address, port, and channel of the client
         */
        ClientInfo client = new ClientInfo(socketChannel);


        socketChannel.register(this.selector, SelectionKey.OP_READ, client);

        count++;

        System.out.println("CONNECTED: " + count);




    }


    /**
     * Method to initiate the server listening for connections
     */
    public void run()
    {

        // start the thread pool manager
        threadPoolManager.start();

        /**
         * Try to be a Server
         */
        try
        {


            while (!Thread.currentThread().isInterrupted())
            {
                /**
                 * Before we select the keys, make changes to interest ops
                 */

                // Process any pending changes
                synchronized (pendingChanges)
                {
                    // FOR every change we have to process
                    for (OpsChangeRequest change : pendingChanges)
                    {

                        switch (change.type)
                        {
                            case OpsChangeRequest.CHANGEOPS:
                                SelectionKey key = change.socket.keyFor(selector);
                                key.interestOps(change.ops);
                        }
                    }
                    pendingChanges.clear();
                    selector.wakeup();
                }

                // Get the number of available channels
                int readyChannels = selector.select();

                // IF there is nothing to do yet
                if (readyChannels == 0) continue;


                /**
                 * Set of all selected keys to be checked
                 */
                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> keyIterator = selectedKeys.iterator();




                while (keyIterator.hasNext())
                {

                    SelectionKey key = keyIterator.next();
                    keyIterator.remove();


                    if (!key.isValid())
                    {
                        continue;
                    }



                    Taskable task = null;

                    // IF the key was acceptable by the ServerSocketChannel
                    if (key.isAcceptable())
                    {
                        accept(key);

                    }
                    // ELSE IF the key is ready to read from
                    else if (key.isReadable())
                    {
                        key.interestOps(0);
                        selector.wakeup();
                        task = taskFactory.createTask(Protocol.READ_TASK, key, this);
                        threadPoolManager.addTask(task);



                    }
                    // ELSE IF the key is ready to be written to
                    else if (key.isWritable())
                    {
                        key.interestOps(0);
                        selector.wakeup();
                        task = taskFactory.createTask(Protocol.WRITE_TASK, key, this);
                        threadPoolManager.addTask(task);

                    }





                }
            }

        }
        catch (IOException ioe)
        {
            ioe.printStackTrace();
        }


    }

    /**
     * Method to add an ops change request to the calling selector thread.
     *
     * @param opsChangeRequest <code>OpsChangeRequest</code> with the desired selector change request.
     */
    public void addOpsChangeRequest(OpsChangeRequest opsChangeRequest)
    {
        synchronized (pendingChanges)
        {
            pendingChanges.add(opsChangeRequest);
        }
    }

    /**
     * main method
     *
     * @param args command line arguments
     */
    public static void main(String[] args)
    {
        if (args.length > 0)
        {

            int portNum;
            int threadPoolSize;

            try
            {
                portNum = Integer.parseInt(args[0]);
                threadPoolSize = Integer.parseInt(args[1]);

                Server server = new Server(portNum, threadPoolSize);
                Thread serverThread = new Thread(server);

                serverThread.start();
            }
            catch (NumberFormatException e)
            {
                System.err.println("Argument" + " must be an integer");
                System.exit(1);
            }


        }

    }

}

