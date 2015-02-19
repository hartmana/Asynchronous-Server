package cs455.scaling.utility;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * A Util class
 *
 * @author ahrtmn, 04 03 2014
 */
public class Util
{
    /**
     * Constant definining the buffer size for the transmission of data between client and server
     */
    public static final int BUFFER_SIZE = 8192;

    /**
     * Constant defining the buffer size for the transmission of hashes between server and client
     */
    public static final int HASH_BUFFER_SIZE = 40;

    /**
     * Method to calculate the SHA1 Hash of a given byte array.
     *
     * @param data <code>byte[]</code> for hash to be computed on.
     * @return <code>String</code> of hash value in hex format.
     * @throws java.security.NoSuchAlgorithmException
     */
    public static String SHA1FromBytes(byte[] data) throws NoSuchAlgorithmException
    {

        MessageDigest digest = MessageDigest.getInstance("SHA1");

        byte[] hash = digest.digest(data);

        BigInteger hashInt = new BigInteger(1, hash);

        return hashInt.toString(16);
    }
}
