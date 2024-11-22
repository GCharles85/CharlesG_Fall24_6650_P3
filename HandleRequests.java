import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.*;
import java.rmi.registry.Registry;
import java.sql.Array;
import java.rmi.registry.LocateRegistry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.BlockingQueue;

// Define the interface for your RPC methods (PUT, GET, DELETE)
//todo DONE FOR NOW add override annotation to appropriate methods
public class HandleRequests implements HandleRequestsInterface {

    private ConcurrentHashMap<String, String> keyValueStore;
    private String serverId;
    private String nextServerId; // To store the address of the next server in the token ring
    private boolean hasToken = false; // Indicates if this server has the token
    private BlockingQueue<String> job_queue;
    private static final long TOKEN_TIMER_MS = 1000; // 1000 ms
    private ArrayList<String> participants;
    private static final Logger LOGGER = Logger.getLogger(Server.class.getName());

    public HandleRequests(String serverId, String nextServer) {
        this.keyValueStore = new ConcurrentHashMap<>();
        this.serverId = serverId;
        this.nextServerId = nextServer;
        job_queue = new LinkedBlockingQueue<String>();
        participants = new ArrayList<String>();
        LOGGER.setLevel(Level.SEVERE);
    }

    // Two-Phase Commit States
    private AtomicBoolean isPrepared = new AtomicBoolean(false);
    private AtomicBoolean commitState = new AtomicBoolean(false);

    // Token Ring Logic to Enter Critical Section
    @Override
    public synchronized void receiveToken() {
        System.out.println("Server " + serverId + " received the token.");
        hasToken = true;
        if(!job_queue.isEmpty()){
            processJobs();
        }
        
    }

    @Override
    public void setInitToken(){
        hasToken = true;
    }

    public synchronized void processJobs(){
        Timer timer = new Timer();
        long startTime = System.currentTimeMillis();

        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (System.currentTimeMillis() - startTime >= TOKEN_TIMER_MS) {
                    System.out.println("Server " + serverId + " reached time limit, passing token");
                    timer.cancel();
                    passToken();
                }else{
                    while(!job_queue.isEmpty() && hasToken == true){
                        System.out.println("doing job cause I have token");
                        try {
                            System.out.println(validateRequest(job_queue.take()));
                        }catch(Exception e){
                            e.printStackTrace();
                        }
                    }
                    timer.cancel();
                    passToken();
                }
            }
        }, 0, 100); // Check every 100ms
    }

    @Override
    public synchronized void passToken() {
        if(hasToken){
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
            
        
            
        }
        System.out.println(serverId + " Doesnt have token");
    }

    @Override
    public synchronized String put(String key, String value) throws RemoteException {
        // Prepare phase
        
            keyValueStore.put(key, value);
            //commitState.set(true); // Simulate successful commit
            return "Key " + key + " with value " + value + " successfully committed.";
      
    }

    @Override
    public synchronized String get(String key) throws RemoteException {
        String value = keyValueStore.get(key);
        return (value != null) ? "Value for key " + key + " is " + value : "Key not found.";
    }

    @Override
    public synchronized String delete(String key) throws RemoteException {
        
            keyValueStore.remove(key);
            //commitState.set(true); // Simulate successful commit
            return "Key " + key + " successfully deleted.";
       
    }

    @Override
    public Boolean canCommit() {
        boolean allCanCommit = true; // Assume all participants can commit initially
    
        for (String participant : participants) {
            try {
                // Look up the registry for the participant
                Registry registry = LocateRegistry.getRegistry(participant, 1099);
                HandleRequestsInterface participantServer = 
                    (HandleRequestsInterface) registry.lookup("HandleRequests-" + participant);
    
                // Check if the participant can commit
                if (!participantServer.responseToCanCommit()) {
                    // If any participant cannot commit, set to false
                    allCanCommit = false;
                    break;
                }
            } catch (Exception e) {
                // Print stack trace and set allCanCommit to false if an exception occurs
                e.printStackTrace();
                allCanCommit = false;
                break;
            }
        }
    
        return allCanCommit; // Return the final decision
    }
    

    // Two-Phase Commit Methods
    @Override
    public Boolean responseToCanCommit() throws RemoteException {
        //Call from coordinator to participant to ask whether it can commit a transaction.
//Participant replies with its vote.
        //maybe if has token
        if(job_queue.isEmpty()){
            //isPrepared.set(true);
            
            //wait 5 sec for command from coordinator
            // Create a thread to simulate the 5-second wait
            Thread waitThread = new Thread(() -> {
                try {
                    // Wait for 5 seconds
                    for (int i = 0; i < 5; i++) {
                        if (commitState.get()) {
                            System.out.println("Coordinator's call received!");
                            return;
                        }
                        Thread.sleep(1000); // Sleep for 1 second
                    }

                    // If no call received after 5 seconds, call getDecision
                    System.out.println("No call from coordinator after 5 seconds. Calling getDecision...");
                    try{
                        getDecision();
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                    
                } catch (InterruptedException e) {
                    System.err.println("Error while waiting for coordinator: " + e.getMessage());
                }
            });

            waitThread.start();
            return true;
        }

        return false;
    }

    @Override
    public Boolean haveCommitted() throws RemoteException{
        return true;
    }

    @Override
    public void doCommit() throws RemoteException {
        
        //Call from coordinator to participant to tell participant to commit its part of a
//transaction
        //if (isPrepared.get()) {
            commitState.set(true);
            //isPrepared.set(false);
            for(String participant : participants){
                try{
                    Registry registry = LocateRegistry.getRegistry(participant, 1099);
                    HandleRequestsInterface particpantServer = 
                    (HandleRequestsInterface) registry.lookup("HandleRequests-" + participant);
    
                    particpantServer.haveCommitted(); //todo 
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
            
        //}
        //return "Operation failed: Not in PREPARE phase.";
    }

    @Override
    public String doAbort() throws RemoteException {//todo
        //Call from coordinator to participant to tell participant to abort its part of a transaction
        try{
            Registry registry = LocateRegistry.getRegistry(nextServerId, 1099);
            HandleRequestsInterface nextServer = 
            (HandleRequestsInterface) registry.lookup("HandleRequests-" + nextServerId);
            nextServer.receiveToken();
        }catch(Exception e){
            e.printStackTrace();
        }
        return ""; //todo
                // if (isPrepared.get()) {
        //     commitState.set(false);
        //     isPrepared.set(false);
        //     return "ABORT phase complete. Changes rolled back.";
        // }
        // return "Operation failed: Not in PREPARE phase.";
    }

    @Override
    public Boolean getDecision() throws RemoteException{
//         Call from participant to coordinator to ask for the decision on a transaction when it
// has voted Yes but has still had no reply after some delay. Used to recover from server
// crash or delayed messages.
        return true;
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
    public String processRequest(String request) throws RemoteException {
        // put in queue

        job_queue.add(request);
        return "";

        //return validateRequest(request);
    }

    public static void Main(String[] args){

    }

}
