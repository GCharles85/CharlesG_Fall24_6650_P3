import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.Naming;
import java.util.logging.*;

public class Server {
    private static final Logger LOGGER = Logger.getLogger(Server.class.getName());

    public static void main(String[] args) {
        try {
            if (args.length < 2) {
                System.err.println("Usage: java Server <serverId> <nextServer>");
                return;
            }
            String serverId = "server" + args[0];
            LOGGER.log(Level.INFO, "Server " + serverId + "coming online");
            String nextServer = "server" + args[1];

            // Create the HandleRequests object with serverId 
            HandleRequests obj = new HandleRequests(serverId, nextServer);
            
            System.setProperty("java.rmi.server.hostname", serverId);
            // Create and connect to registry on Server1
            Registry registry = LocateRegistry.createRegistry(1099);
            HandleRequestsInterface stub  = (HandleRequestsInterface) UnicastRemoteObject.exportObject(obj, 0);
            registry.bind("HandleRequests-" + serverId, stub);

            // Locate remote object in the RMI registry
            
            HandleRequestsInterface currentServer = (HandleRequestsInterface) Naming.lookup("rmi://"+serverId+":1099/HandleRequests-" + serverId);
            if ("server1".equals(serverId)) {
                //give server1 the token
                currentServer.setInitToken();
            }else{
                //start that a thread that blows up occasionally
                
                Thread blowUpThread = new Thread(() -> {
                    try{
                        Thread.sleep(15000);
                        LOGGER.log(Level.SEVERE, String.format("\n %s Blowing up\n", serverId));
                        throw new RuntimeException();
                    }catch(InterruptedException | RuntimeException e){
                        LOGGER.log(Level.SEVERE, e.getMessage() + "\nBlow up thread interrupted or blown up.");
                    } 
                });
                blowUpThread.start();  
        }
        

            while(true){
                currentServer.passToken();
                Thread.sleep(1000);
            }
        
        } catch (Exception e ) {
            System.err.println("Server exception: " + e.toString());
        }
    }
}
