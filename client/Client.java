package cs455.scaling.client;

import cs455.scaling.server.ClientInfo;
import cs455.scaling.server.ServerClient;
import cs455.scaling.utility.OpsChangeRequest;
import cs455.scaling.utility.Util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.*;

/**
 * A Server class to listen to incoming connections from clients, accept data in the form
 * of a byte array, compute the hash for that data and acknowledge the receipt of this data
 * by responding with the hash code.
 *
 * @author ahrtmn, 24 02 2014
 */
public class Client implements Runnable, HashHandler, ServerClient
{
    /**
     * SocketChannel to listen to incoming connections
     */
    SocketChannel socketChannel;

    /**
     * Selector to be used for incoming connections
     */
    private Selector selector;

    /**
     * String containing the server address the client is to connect to
     */
    private final InetAddress serverAddress;

    /**
     * int of the port number the server is listening on
     */
    private final int portNum;

    /**
     * int for the message rate
     */
    private int messageRate;

    /**
     * Set of sent data Hashes maintained for verification from server before removal
     */
    private final Set<String> dataHashSet = new HashSet<String>();


    /**
     * Queue for pending selector key changes
     */
    private final Queue<OpsChangeRequest> pendingChanges = new LinkedList<OpsChangeRequest>();

    /**
     * Constructor for the Server class
     *
     * @param serverAddress <code>InetAddress</code> to the server the client is to connect to.
     * @param portNum       <code>int</code> of the port number to listen for incoming connections on.
     */
    public Client(InetAddress serverAddress, int portNum, int messageRate)
    {
        this.serverAddress = serverAddress;
        this.portNum = portNum;
        this.messageRate = messageRate;

        try
        {

            selector = SelectorProvider.provider().openSelector();

            socketChannel = initiateConnection();
        }
        catch (IOException ioe)
        {
            System.out.println(ioe.getMessage());
            ioe.printStackTrace();
        }


    }

    /**
     * Method to initiate a connection to the server and return a SocketChannel with the connection.
     *
     * @return <code>SocketChannel</code> containing the connection to the server.
     * @throws IOException
     */
    private SocketChannel initiateConnection() throws IOException
    {
        // Create a non-blocking socket channel
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);


        socketChannel.connect(new InetSocketAddress(serverAddress, portNum));


        if(!socketChannel.isConnected())
            socketChannel.finishConnect();

        /**
         * ClientInfo of the server we are writing to. Also contains pending write list to be used
         * when server is available for writing.
         */
        ClientInfo serverInfo = new ClientInfo(socketChannel);


        socketChannel.register(selector, SelectionKey.OP_CONNECT, serverInfo);

        try
        {
            Thread.currentThread().sleep(1000);
        }
        catch(InterruptedException ie)
        {
            ie.printStackTrace();
        }

        return socketChannel;
    }

    /**
     * Method to finish a connection
     *
     * @param key <code>SelectionKey</code> of initiated connection to finish.
     * @throws IOException
     */
    private void connect(SelectionKey key) throws IOException
    {


        SocketChannel channel = (SocketChannel) key.channel();

        // IF the connection wasn't completed
        if (channel.isConnectionPending())
            channel.finishConnect();

        /**
         * ClientInfo containing the address, port, and channel of the client
         */
        ClientInfo serverInfo = new ClientInfo(channel);


        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_READ, serverInfo);


    }

    /**
     * Method to read data from a given channel associated with a SelectionKey
     *
     * @param key <code>SelectionKey</code> of channel to be read from.
     * @throws IOException
     */
    private void read(SelectionKey key) throws IOException
    {


        /**
         * SocketChannel for the current key
         */
        SocketChannel channel = (SocketChannel) key.channel();

        /**
         * ByteBuffer to store the data read from client
         */
        ByteBuffer byteBuffer = ByteBuffer.allocate(Util.HASH_BUFFER_SIZE);

        /**
         * String to hold the hash
         */
        String hashString;


        int read = 0;

        /**
         * Try to read the data from the client
         */
        try
        {
            // WHILE there is more to read
            while (byteBuffer.hasRemaining() && read != -1)
            {
                read = channel.read(byteBuffer);
            }

        }
        catch (IOException e)
        {
            /**
             * Abnormal termination
             */
            ((ClientInfo) key.attachment()).disconnect();

            key.cancel();
            System.err.println("IOException caught");

            return;
        }
        if (read == -1)
        {
            /* Connection was terminated by the client. */
            ((ClientInfo) key.attachment()).disconnect();

            System.err.println("Server terminated connection");

            System.exit(0);


            key.cancel();
            return;
        }

        byteBuffer.flip();

        byte[] bufferBytes = new byte[Util.HASH_BUFFER_SIZE];

        byteBuffer.get(bufferBytes);

        hashString = new String(bufferBytes);


        synchronized (dataHashSet)
        {
            dataHashSet.remove(hashString);
        }

        System.out.println("Data read. Hash removed from list: " + hashString + "\n");


        addOpsChangeRequest(new OpsChangeRequest(socketChannel, OpsChangeRequest.CHANGEOPS, SelectionKey.OP_WRITE));

    }

    private void write(SelectionKey key) throws IOException
    {

        /**
         * SocketChannel associated with this key
         */
        SocketChannel channel = (SocketChannel) key.channel();


        /**
         * ByteBuffer to read data from when its available
         */
        ByteBuffer byteBuffer = ByteBuffer.allocate(Util.HASH_BUFFER_SIZE);

        /**
         * ClientInfo of the current key
         */
        ClientInfo clientInfo = (ClientInfo) key.attachment();

        /**
         * Clients hash that is being written
         */
        byte[] hash;


        // WHILE there is more to be written for this client
        while (clientInfo.hasWrites())
        {
            hash = clientInfo.getPendingWrite();

            byteBuffer = ByteBuffer.wrap(hash);

            channel.write(byteBuffer);

            System.out.println("Data written to client [Address: " + clientInfo.getRemoteAddress() + "]\n");
        }

        byteBuffer.flip();

//        key.interestOps(SelectionKey.OP_READ);
        addOpsChangeRequest(new OpsChangeRequest(socketChannel, OpsChangeRequest.CHANGEOPS,
                SelectionKey.OP_READ));

    }


    /**
     * Method to initiate the server listening for connections
     */
    public void run()
    {

        /**
         * Start the sending daemon
         */
        SenderDaemon senderDaemon = new SenderDaemon(socketChannel.keyFor(selector), messageRate, this, this);
        Thread senderDaemonThread = new Thread(senderDaemon);
        senderDaemonThread.start();

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

//                // Process any pending changes
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

                    // IF the key was connectable by the ServerSocketChannel
                    if (key.isConnectable())
                    {
                        connect(key);
                        selector.wakeup();
                    }
                    // ELSE IF the key is ready to read from
                    else if (key.isReadable())
                    {
                        read(key);
                        selector.wakeup();
                    }
                    // ELSE IF the key is ready to be written to
                    else if (key.isWritable())
                    {
                        write(key);
                        selector.wakeup();
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
     * Method to store a sent hash code
     *
     * @param hash <code>String</code> containing the hash code to be tracked.
     */
    @Override
    public void addHash(String hash)
    {
        synchronized (dataHashSet)
        {
            dataHashSet.add(hash);
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

            String serverAddress;
            int portNum;
            int messageRate;

            try
            {
                serverAddress = args[0];
                portNum = Integer.parseInt(args[1]);
                messageRate = Integer.parseInt(args[2]);


                Client client = new Client(InetAddress.getByName(serverAddress), portNum, messageRate);
                Thread clientThread = new Thread(client);


                clientThread.start();

            }
            catch (NumberFormatException e)
            {
                System.err.println("Argument" + " must be an integer");
                System.exit(1);
            }
            catch (UnknownHostException uhe)
            {
                uhe.printStackTrace();
            }

        }


    }
}
