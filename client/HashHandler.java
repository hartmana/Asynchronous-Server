package cs455.scaling.client;

/**
 * A HashHandler interface
 *
 * @author ahrtmn, 04 03 2014
 */
public interface HashHandler
{
    /**
     * Method to store a sent hash code
     *
     * @param hash <code>String</code> containing the hash code to be tracked.
     */
    public void addHash(String hash);
}
