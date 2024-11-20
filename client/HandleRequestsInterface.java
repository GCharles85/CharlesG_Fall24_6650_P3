import java.rmi.Remote;
import java.rmi.RemoteException;

// Define the interface for remote methods
public interface HandleRequestsInterface extends Remote {
    String put(String key, String value) throws RemoteException;

    String get(String key) throws RemoteException;

    String delete(String key) throws RemoteException;

    String processRequest(String request) throws RemoteException;

    String validateRequest(String input) throws RemoteException;

    // Two-Phase Commit Methods
    String prepare() throws RemoteException;

    String commit() throws RemoteException;

    String abort() throws RemoteException;

    // Token Ring Methods
    void receiveToken() throws RemoteException;

    void passToken() throws RemoteException;
}