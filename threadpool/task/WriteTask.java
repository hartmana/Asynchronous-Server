package cs455.scaling.threadpool.task;

import cs455.scaling.server.ClientInfo;
import cs455.scaling.server.ServerClient;
import cs455.scaling.utility.OpsChangeRequest;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * A WriteTask class
 *
 * @author ahrtmn, 04 03 2014
 */
public class WriteTask implements Taskable
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
    public WriteTask(SelectionKey key, ServerClient serverClient)
    {
        this.key = key;
        this.serverClient = serverClient;
        socketChannel = (SocketChannel) key.channel();
    }


    @Override
    public void run() throws IOException
    {

        /**
         * ByteBuffer to read data from when its available
         */
        ByteBuffer byteBuffer;          // = ByteBuffer.allocate(Util.HASH_BUFFER_SIZE);

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

            socketChannel.write(byteBuffer);

            System.out.println("Hash: " + new String(hash) + " written to client [Address: " + clientInfo.getRemoteAddress() + "]\n");
        }

        serverClient.addOpsChangeRequest(new OpsChangeRequest(socketChannel, OpsChangeRequest.CHANGEOPS,
                SelectionKey.OP_READ));

//        key.interestOps(SelectionKey.OP_READ);
        // need to recognize the changes
        key.selector().wakeup();
    }

}
