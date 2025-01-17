package de.staticred.dbv2.info;


/**
 * Object containing all sorts of database info
 *
 * @author Devin
 * @version 1.0.0
 */
public class DataBaseInfo {

    /**
     * if the database is connected
     */
    private final boolean connected;

    /**
     * Constructor to create an DataBaseInfo
     * @param connected to the database
     */
    public DataBaseInfo(boolean connected) {
        this.connected = connected;
    }

    /**
     * @return if the connector is connected to the database
     */
    public boolean isConnected() {
        return connected;
    }
}
