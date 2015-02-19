package cs455.scaling.threadpool.task;

import cs455.scaling.server.ClientInfo;
import cs455.scaling.server.ServerClient;
import cs455.scaling.utility.OpsChangeRequest;
import cs455.scaling.utility.Util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.security.NoSuchAlgorithmException;

/**
 * A WriteTask class
 *
 * @author ahrtmn, 04 03 2014
 */
public class ReadTask implements Taskable
{
    /**
     * SelectionKey associated with the channel to be read from
     */
    private SelectionKey key;

    /**
     * ServerSocketChannel from the connection we are to accept
     */
    private SocketChannel socketChannel;

    /**
     * ServerClient managing change ops requests
     */
    private ServerClient serverClient;


    /**
     * Constructor
     *
     * @param key <code>SelectionKey</code> representing the connection to accept.
     */
    public ReadTask(SelectionKey key, ServerClient serverClient)
    {
        this.key = key;
        this.serverClient = serverClient;
        socketChannel = (SocketChannel) key.channel();
    }


    @Override
    public void run() throws IOException
    {
        /**
         * ByteBuffer to store the data read from client
         */
        ByteBuffer buffer = ByteBuffer.allocate(Util.BUFFER_SIZE);

        /**
         * ClientInfo of the current key
         */
        ClientInfo clientInfo = (ClientInfo) key.attachment();


        int read = 0;

        /**
         * Try to read the data from the client
         */
        try
        {
            // WHILE there is more to read
            while (buffer.hasRemaining() && read != -1)
                read = socketChannel.read(buffer);

        }
        catch (IOException e)
        {
            /**
             * Abnormal termination
             */
            clientInfo.disconnect();

            key.cancel();

            return;
        }
        if (read == -1)
        {
            /**
             * Connection was terminated by client
             */
            clientInfo.disconnect();

            key.cancel();
            return;
        }


        buffer.flip();

        byte[] bufferBytes = new byte[Util.BUFFER_SIZE];

        buffer.get(bufferBytes);

        try
        {
            byte[] hash = Util.SHA1FromBytes(buffer.array()).getBytes();

            if (hash.length < 40)
            {
                byte[] fullHash = new byte[40];

                // get the number of zeroes we need to pad
                int difference = 40 - hash.length;

                // pad with zeroes
                for (int i = 0; i < difference; ++i)
                    fullHash[i] = 0;

                // add the remaining hash
                for (int i = difference, j = 0; i < 40; ++i, ++j)
                    fullHash[i] = hash[j];

                hash = fullHash;


            }

            // Get the client info associated with this key and add the hash of the data read to it
            clientInfo.addPendingWrite(hash);

            System.out.println("Data read from client [Address: " + clientInfo.getRemoteAddress() + "].\n\tCreating Hash..\n");
        }
        catch (NoSuchAlgorithmException nsae)
        {
            System.err.println(nsae.getMessage());
            nsae.printStackTrace();
        }

        serverClient.addOpsChangeRequest(new OpsChangeRequest(socketChannel, OpsChangeRequest.CHANGEOPS,
                SelectionKey.OP_WRITE));

        // change the interested operation to writing
//        key.interestOps(SelectionKey.OP_WRITE);

        // need to recognize the changes
        key.selector().wakeup();


    }

}
