import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;
/* TODO ensure data consistency
 *      Fix code duplication with handlereqinterface
 * 
*/

// Define the interface for remote methods
public interface HandleRequestsInterface extends Remote, Serializable {
    enum Decision{COMMIT, ABORT, NONE}

    String put(String key, String value) throws RemoteException;

    String get(String key) throws RemoteException;

    String delete(String key) throws RemoteException;

    String processRequest(String request, int requestID) throws RemoteException;

    String validateRequest(String input) throws RemoteException;

    // Two-Phase Commit Methods
    Boolean canCommit() throws RemoteException; //done for now

    void getDecision() throws RemoteException;

    Decision sendDecision() throws RemoteException;

    void doCommit() throws RemoteException;

    Boolean doAbort() throws RemoteException;

    Boolean haveCommitted() throws RemoteException;

    public Boolean responseToCanCommit() throws RemoteException;

    public Boolean abort() throws RemoteException;

    // Token Ring Methods
    void receiveToken() throws RemoteException;

    void passToken() throws RemoteException;

    void setInitToken() throws RemoteException;
}
