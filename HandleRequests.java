import java.rmi.RemoteException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.rmi.registry.Registry;
import java.rmi.registry.LocateRegistry;

// Define the interface for your RPC methods (PUT, GET, DELETE)
public class HandleRequests implements HandleRequestsInterface {

    private ConcurrentHashMap<String, String> keyValueStore;
    private String serverId;
    private String nextServerId; // To store the address of the next server in the token ring
    private boolean hasToken = false; // Indicates if this server has the token

    public HandleRequests(String serverId, String nextServer) {
        this.keyValueStore = new ConcurrentHashMap<>();
        this.serverId = serverId;
        this.nextServerId = nextServer;
    }

    // Two-Phase Commit States
    private AtomicBoolean isPrepared = new AtomicBoolean(false);
    private AtomicBoolean commitState = new AtomicBoolean(false);

    // Token Ring Logic to Enter Critical Section
    public synchronized void receiveToken() {
        hasToken = true;
        System.out.println("Server " + serverId + " received the token.");
    }

    public synchronized void passToken() {
        hasToken = false;
        // Logic to pass token to next server (e.g., through RMI)
        System.out.println(serverId + " passing token to " + nextServerId);
        
        try{
            Registry registry = LocateRegistry.getRegistry(nextServerId, 1099);
            HandleRequestsInterface nextServer = 
            (HandleRequestsInterface) registry.lookup("HandleRequests-" + nextServerId);
            nextServer.receiveToken();
        }catch(Exception e){
            e.printStackTrace();
        }
        
        this.hasToken = false;  // Pass token, so this server no longer has it
        System.out.println("Server " + serverId + " passing token to " + nextServerId);
    }

    @Override
    public synchronized String put(String key, String value) throws RemoteException {
        // Prepare phase
        if (isPrepared.get()) {
            keyValueStore.put(key, value);
            commitState.set(true); // Simulate successful commit
            return "Key " + key + " with value " + value + " successfully committed.";
        }
        return "Operation failed: Server not in prepared state.";
    }

    @Override
    public synchronized String get(String key) throws RemoteException {
        String value = keyValueStore.get(key);
        return (value != null) ? "Value for key " + key + " is " + value : "Key not found.";
    }

    @Override
    public synchronized String delete(String key) throws RemoteException {
        if (isPrepared.get()) {
            keyValueStore.remove(key);
            commitState.set(true); // Simulate successful commit
            return "Key " + key + " successfully deleted.";
        }
        return "Operation failed: Server not in prepared state.";
    }

    // Two-Phase Commit Methods
    public String prepare() throws RemoteException {
        isPrepared.set(true);
        return "PREPARE phase complete. Ready to commit.";
    }

    public String commit() throws RemoteException {
        if (isPrepared.get()) {
            commitState.set(true);
            isPrepared.set(false);
            return "COMMIT phase complete.";
        }
        return "Operation failed: Not in PREPARE phase.";
    }

    public String abort() throws RemoteException {
        if (isPrepared.get()) {
            commitState.set(false);
            isPrepared.set(false);
            return "ABORT phase complete. Changes rolled back.";
        }
        return "Operation failed: Not in PREPARE phase.";
    }

    // Method to validate the request
    public String validateRequest(String input) throws RemoteException {
        if (input == null || input.isEmpty()) {
            return "Error: Request is empty or null.";
        }
        input = input.trim();

        if (input.matches("(?i)^put\\(([^,]*),\\s*([^\\)]*)\\)")) { //this regex will match requests that look like PUT(), put(), put(key, value), PUT(key, value) where key and value are any string
            int openParenIndex = input.indexOf('(');
            int commaIndex = input.indexOf(',');
            String key = input.substring(openParenIndex + 1, commaIndex); //we use openParenIndex and commaIndex to grab the chars from after the first parenthesis up to the comma
            int closeParenIndex = input.indexOf(')', commaIndex);
            String value = input.substring(commaIndex + 1, closeParenIndex); //we grab the char from after the comma to the closing parenthesis

            if (key.isEmpty() || value.isEmpty()) {
                return "Error: PUT request is missing a key or value.";
            }
            return put(key, value);
        } else if (input.matches("(?i)^get\\(([^\\)]*)\\)")) { //this regex will match requests that look like GET(), get(), get(key), GET(key) where key is any string
            int openParenIndex = input.indexOf('(');
            int closeParenIndex = input.indexOf(')');
            String key = input.substring(openParenIndex + 1, closeParenIndex); //we use openParenIndex and closeParenIndex to grab the chars between the parentheses

            if (key.isEmpty()) {
                return "Error: GET request is missing a key.";
            }
            
            return get(key);
        } else if (input.matches("(?i)^delete\\(([^\\)]*)\\)")) { //this regex will match requests that look like DELETE(), delete(), delete(key), DELETE(key) where key is any string
            int openParenIndex = input.indexOf('(');
            int closeParenIndex = input.indexOf(')');
            String key = input.substring(openParenIndex + 1, closeParenIndex); //we use openParenIndex and closeParenIndex to grab the chars between the parentheses

            if (key.isEmpty()) {
                return "Error: DELETE request is missing a key.";
            }
            
            return delete(key);
        } else {
            return "Error: Invalid request type. Supported types are PUT(key, value), GET(key), DELETE(key).";
        }
        
    }

    // Method to process requests
    public String processRequest(String request) throws RemoteException {
        return validateRequest(request);
    }

}
