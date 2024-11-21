import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;
/* TODO Implement methods 
 *      Fix code duplication with handlereqinterface
 * 
*/

// Define the interface for remote methods
public interface HandleRequestsInterface extends Remote, Serializable {
    String put(String key, String value) throws RemoteException;

    String get(String key) throws RemoteException;

    String delete(String key) throws RemoteException;

    String processRequest(String request) throws RemoteException;

    String validateRequest(String input) throws RemoteException;

    // Two-Phase Commit Methods
    Boolean canCommit() throws RemoteException; //done for now

    Boolean getDecision() throws RemoteException;

    String doCommit() throws RemoteException;

    String doAbort() throws RemoteException;

    Boolean haveCommitted() throws RemoteException;

    public Boolean responseToCanCommit() throws RemoteException;

    // Token Ring Methods
    void receiveToken() throws RemoteException;

    void passToken() throws RemoteException;

    void setInitToken() throws RemoteException;
}
