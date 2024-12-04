import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.*;
import java.rmi.registry.Registry;
import java.sql.Array;
import java.rmi.registry.LocateRegistry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ConcurrentHashMap.KeySetView;
import java.util.concurrent.BlockingQueue;

public class HandleRequests implements HandleRequestsInterface {

    private ConcurrentHashMap<String, String> keyValueStore_original;
    private ConcurrentHashMap<String, String> keyValueStore_current;
    private String serverId;
    private String nextServerId; // To store the address of the next server in the token ring
    private boolean hasToken = false; // Indicates if this server has the token
    private BlockingQueue<Map.Entry<String, Long>> job_queue;
    private Set<String> requests;
    private static final long TOKEN_TIMER_MS = 1000; // 1000 ms
    private ArrayList<String> participants;
    private Decision decision = HandleRequestsInterface.Decision.NONE;
    private long currentSequenceNumber = 0L;
    private static final Logger LOGGER = Logger.getLogger(Server.class.getName());

    public HandleRequests(String serverId, String nextServer) {
        this.keyValueStore_current= new ConcurrentHashMap<>();
        this.keyValueStore_original= new ConcurrentHashMap<>();
        this.serverId = serverId;
        this.nextServerId = nextServer;
        job_queue = new LinkedBlockingQueue<>();
        requests = new HashSet<>();
        LOGGER.setLevel(Level.ALL);

        participants = new ArrayList<String>(Arrays.asList(System.getenv("PARTICIPANTS").split(",")));
    }

    String getParticipants(){
        return "";
    }

    // Two-Phase Commit States
    private AtomicBoolean commitState = new AtomicBoolean(false);

    // Token Ring Logic to Enter Critical Section
    @Override
    public synchronized void receiveToken() throws RemoteException {
        LOGGER.log(Level.INFO, "Server " + serverId + " received the token.");
        hasToken = true;
        if(!job_queue.isEmpty()){
            processJobs();
        }else if(serverId.equals(System.getenv("CENTRAL_REGISTRY_HOST"))){
            if(canCommit() && this.haveCommitted()){
                decision = Decision.COMMIT;
                doCommit();
                LOGGER.log(Level.INFO, "All committed");     
            }else{
                decision = Decision.ABORT;
                this.abort();
                doAbort();
                LOGGER.log(Level.INFO, "All aborted");
                System.out.println("All aborted");
            }
        } 
    }

    @Override
    public synchronized void passToken() {
        if(hasToken){
            hasToken = false;
            // Logic to pass token to next server (e.g., through RMI)
            LOGGER.log(Level.INFO, serverId + " passing token to " + nextServerId);
            
            try{
                Registry registry = LocateRegistry.getRegistry(nextServerId, 1099);
                HandleRequestsInterface nextServer = 
                (HandleRequestsInterface) registry.lookup("HandleRequests-" + nextServerId);
                nextServer.receiveToken();
            }catch(Exception e){
                LOGGER.log(Level.SEVERE, e.getMessage());
                e.printStackTrace();
            }
            
        
            
        }
        //LOGGER.log(Level.INFO, serverId + " Doesn't have token");
    }

    @Override
    public void setInitToken(){
        hasToken = true;
    }

    public synchronized void processJobs(){
        long startTime = System.currentTimeMillis();
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (System.currentTimeMillis() - startTime >= TOKEN_TIMER_MS) {
                    //LOGGER.log(Level.INFO, "Server " + serverId + " reached time limit, passing token");
                    timer.cancel();
                    passToken();
                }else{
                    while(!job_queue.isEmpty() && hasToken == true){
                        try {
                            //save current state
                            saveCurrentState();
                            //send the request to all other servers
                            Map.Entry<String, Long> request = job_queue.poll();
                            sendRequestToAllOtherServers(request);
                            System.out.println(validateRequest(request.getKey()));
                        }catch(Exception e){
                            LOGGER.log(Level.SEVERE, e.getMessage());
                            e.printStackTrace();
                        }
                    }
                    timer.cancel();
                    passToken();
                }
            }
        }, 0, 100); // Check every 100ms
    }

    private synchronized void sendRequestToAllOtherServers(Map.Entry<String, Long> request){
        //Ensure that the request is not null and use the sequence number to ensure we have not seen it already
        long newRequestSequenceNumber = request.getValue();
        if (request == null || newRequestSequenceNumber <= currentSequenceNumber) {
            return;
        } 
        currentSequenceNumber = newRequestSequenceNumber;
        for(String participant : participants){
            try{
                if(!participant.equals(serverId)){
                    Registry registry = LocateRegistry.getRegistry(participant, 1099);
                    HandleRequestsInterface particpantServer = 
                    (HandleRequestsInterface) registry.lookup("HandleRequests-" + participant);
                    particpantServer.processRequest(request.getKey(), 1);  
                }
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    public synchronized void saveCurrentState(){
        //copy current kv store to original kv store
        keyValueStore_original = new ConcurrentHashMap<String, String>(keyValueStore_current);
    }


    @Override
    public synchronized String put(String key, String value) throws RemoteException {
            keyValueStore_current.put(key, value);
            return "Key " + key + " with value " + value + " successfully put.";
      
    }

    @Override
    public synchronized String get(String key) throws RemoteException {
        String value = keyValueStore_current.get(key);
        return (value != null) ? "Value for key " + key + " is " + value : "Key not found.";
    }

    @Override
    public synchronized String delete(String key) throws RemoteException {
            keyValueStore_current.remove(key);
            return "Key " + key + " successfully deleted."; 
    }

    //Two-phase commit methods
    @Override
    public Boolean canCommit() throws RemoteException {
        boolean allCanCommit = true; // Assume all participants can commit initially
        
        for (String participant : participants) {
            try {
                // Look up the registry for the participant only if the participant's container name has the "a" suffix meaning they are an acceptor
                if(participant.matches("\\ba\\>")){
                    Registry registry = LocateRegistry.getRegistry(participant, 1099);
                    HandleRequestsInterface participantServer = 
                        (HandleRequestsInterface) registry.lookup("HandleRequests-" + participant);
        
                    // Check if the participant can commit
                    if (!participantServer.responseToCanCommit()) {
                        // If any participant cannot commit, set to false
                        allCanCommit = false;
                        break;
                    }
                }
            } catch (NotBoundException e) {
                // Print stack trace and set allCanCommit to false if an exception occurs 
                LOGGER.log(Level.SEVERE, e.getMessage());
                allCanCommit = false;
                e.printStackTrace();
                break;
            }
        }
        allCanCommit = this.responseToCanCommit();
        return allCanCommit;
    }
    

    public Boolean abort() {
        // Revert to the original state
        // todo rewrite log
        keyValueStore_current = new ConcurrentHashMap<String, String>(keyValueStore_original);
        requests.clear();
        LOGGER.log(Level.INFO, "Abort received by " + serverId +", Reverted to original state: ");
        return true;
    }

    @Override
    public Boolean responseToCanCommit() throws RemoteException {
        // Call from participant to coordinator to ask whether it can commit a transaction.
        // Participant replies with its vote.
    
        // Process jobs if the job queue is not empty
        if (!job_queue.isEmpty()) {
            this.processJobs();
        }
    
        // Start a thread to monitor the commitState
        Thread monitorThread = new Thread(() -> {
            long startTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime < 5000) { // 5 seconds
                if (commitState.get()) {
                    //LOGGER.log(Level.INFO, "Commit state is true. Terminating monitoring thread.");
                    return; // Exit the thread
                }
                try {
                    Thread.sleep(100); // Check commitState every 100ms
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // Restore interruption status
                    LOGGER.log(Level.SEVERE, e.getMessage() + "\nMonitoring thread interrupted.");
                    return;
                }
            }
            // If 5 seconds pass and commitState is not true, call getDecision
            try {
                //LOGGER.log(Level.INFO, "5 seconds elapsed. Calling getDecision...");
                getDecision();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    
        monitorThread.start(); // Start the monitoring thread
    
        // Return true as the response to the canCommit request
        return true;
    }

    @Override
    public Boolean haveCommitted() throws RemoteException{
        keyValueStore_original = new ConcurrentHashMap<String, String>(keyValueStore_current);
        requests.clear();
        return true;
    }

    @Override
    public void doCommit() throws RemoteException {
        
        //Call from coordinator to participant to tell participant to commit its part of a transaction
        commitState.set(true);
        for(String participant : participants){
            try{
                Registry registry = LocateRegistry.getRegistry(participant, 1099);
                HandleRequestsInterface particpantServer = 
                (HandleRequestsInterface) registry.lookup("HandleRequests-" + participant);

                particpantServer.haveCommitted();
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    @Override
    public Boolean doAbort() throws RemoteException {
        //Call from coordinator to participant to tell participant to abort its part of a transaction
        for (String participant : participants) {
            try {
                // Look up the registry for the participant
                Registry registry = LocateRegistry.getRegistry(participant, 1099);
                HandleRequestsInterface participantServer = 
                    (HandleRequestsInterface) registry.lookup("HandleRequests-" + participant);
    
                if(!participantServer.abort()){
                    return false;
                };
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, e.getMessage());
                break;
            }
        }
        return true;
    }

    @Override
    public void getDecision() throws RemoteException{
//         Call from participant to coordinator to ask for the decision on a transaction when it
//         has voted Yes but has still had no reply after some delay. Used to recover from server
//         crash or delayed messages.
        
        // Look up the registry for the participant
        Registry registry = LocateRegistry.getRegistry(System.getenv("CENTRAL_REGISTRY_HOST"), 1099);
        HandleRequestsInterface coordinator;
        try {
            coordinator = (HandleRequestsInterface) registry.lookup("HandleRequests-" + System.getenv("CENTRAL_REGISTRY_HOST"));
            //Get the decision
            if(coordinator.sendDecision() == HandleRequestsInterface.Decision.ABORT){
                this.abort();
            }else if(coordinator.sendDecision() == HandleRequestsInterface.Decision.COMMIT){
                this.responseToCanCommit();
            }else{
                this.responseToCanCommit();
            }
        } catch (AccessException e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
        } catch (RemoteException e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
        } catch (NotBoundException e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
        }
        
        
    }

    public HandleRequestsInterface.Decision sendDecision() {
        return decision;
    }

    // Method to validate the request
    @Override
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
    @Override
    public void processRequest(String request, long requestID) throws RemoteException {
        // put request-id pair in job queue and requestID in memory
        if(requests.add(request)){
            //LOGGER.log(Level.INFO, "Adding request "+ requestID + " to Server " + String.valueOf(serverId));
            job_queue.add(Map.entry(request, requestID));
        }
    }

    public static void Main(String[] args){

    }

}
