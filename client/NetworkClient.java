import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Scanner;

public class NetworkClient {

    public static void main(String[] args) {

        String CENTRAL_REGISTRY_HOST = System.getenv("CENTRAL_REGISTRY_HOST");
        if (CENTRAL_REGISTRY_HOST == null || CENTRAL_REGISTRY_HOST.isEmpty()) {
            System.err.println("Error: CENTRAL_REGISTRY_HOST environment variable is not set.");
            return;
        }

        // Start a separate thread to poll the user
        Thread pollingThread = new Thread(() -> {
            try (Scanner scanner = new Scanner(System.in)) {
                while (true) {
                    System.out.println("Enter the server ID to interact with (or 'exit' to quit): ");
                    String targetServerId = scanner.nextLine();

                    // Exit condition
                    if ("exit".equalsIgnoreCase(targetServerId)) {
                        System.out.println("Exiting client...");
                        break;
                    }

                    try {
                        // Connect to the centralized registry
                        Registry registry = LocateRegistry.getRegistry(CENTRAL_REGISTRY_HOST, 1099);

                        // Look up the target server
                        HandleRequestsInterface serverStub = (HandleRequestsInterface) registry.lookup("HandleRequests-" + targetServerId);

                        // Simulate sending multiple requests to the server
                        System.out.println("Sending requests to server: " + targetServerId);

                        System.out.println(serverStub.processRequest("PUT(key1, value1)"));
                        System.out.println(serverStub.processRequest("PUT(key2, value2)"));
                        System.out.println(serverStub.processRequest("GET(key1)"));
                        System.out.println(serverStub.processRequest("GET(key2)"));
                        System.out.println(serverStub.processRequest("DELETE(key1)"));
                        System.out.println(serverStub.processRequest("DELETE(key2)"));

                    } catch (Exception e) {
                        System.err.println("Error communicating with server " + targetServerId + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                System.err.println("Error in polling thread: " + e.getMessage());
                e.printStackTrace();
            }
        });

        // Start the polling thread
        pollingThread.start();

        // Main thread can perform other tasks or remain idle
        System.out.println("NetworkClient is running. Type 'exit' to quit.");
    }
}