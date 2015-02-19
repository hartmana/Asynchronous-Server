package cs455.scaling.client;

import cs455.scaling.server.ClientInfo;
import cs455.scaling.server.ServerClient;
import cs455.scaling.utility.OpsChangeRequest;
import cs455.scaling.utility.Util;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Random;

/**
 * A SenderDaemon class
 *
 * @author ahrtmn, 09 03 2014
 */
public class SenderDaemon implements Runnable
{
    /**
     * SelectionKey of recipient to this SenderDaemon's messages.
     * SelectionKey left for possible removal of OpsChangeRequest
     * and needing to change interest ops from here.
     */
    private SelectionKey key;

    /**
     * SelectionKey and channel of message recipient
     */
    private SocketChannel socketChannel;

    /**
     * int for the message rate.
     */
    private int messageRate;

    /**
     * HashHandler to be able to add hashes to the owning Client.
     */
    private HashHandler hashHandler;

    /**
     * ServerClient managing change ops requests
     */
    private ServerClient serverClient;


    /**
     * Constructor
     *
     * @param messageRate <code>int</code> denoting the message rate, where between every sent message the Daemon
     *                    will sleep for 1000ms / messageRate
     * @param hashHandler <code>HashHandler</code> to track the sent hashes.
     */
    public SenderDaemon(SelectionKey key, int messageRate, HashHandler hashHandler, ServerClient serverClient)
    {
        this.key = key;
        this.messageRate = messageRate;
        this.hashHandler = hashHandler;
        this.socketChannel = (SocketChannel) key.channel();
        this.serverClient = serverClient;
    }

    @Override
    public void run()
    {
        /**
         * ClientInfo of the current key
         */
        ClientInfo clientInfo = (ClientInfo) key.attachment();

        while (!Thread.currentThread().isInterrupted())
        {
            /**
             * Randomly fill an 8kb byte array
             */
            Random random = new Random(new Date().getTime());
            byte[] data = new byte[Util.BUFFER_SIZE];
            random.nextBytes(data);

            clientInfo.addPendingWrite(data);


            try
            {
                hashHandler.addHash(Util.SHA1FromBytes(data));

                serverClient.addOpsChangeRequest(new OpsChangeRequest(socketChannel, OpsChangeRequest.CHANGEOPS,
                        SelectionKey.OP_WRITE));
                Thread.sleep(1000 / messageRate);

            }
            catch (InterruptedException ie)
            {
                System.err.println(ie.getMessage());
                ie.printStackTrace();
            }
            catch (NoSuchAlgorithmException nsae)
            {
                nsae.printStackTrace();
            }
        }
    }
}
