import com.example.Rmi.WhatsUTRemote;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class TestClient {

    private static final String HOST = "localhost";
    private static final int PORT = 1099;
    private static final String SERVICE_NAME = "WhatsUT";

    public static void main(String[] args) {
        try {
            Registry registry = LocateRegistry.getRegistry(HOST, PORT);

            WhatsUTRemote service = (WhatsUTRemote) registry.lookup(SERVICE_NAME);

            System.out.println("Conectado ao servidor!");

            // Teste das funções
            /*service.registerUser("testuser1", "password123");
            boolean authenticated = service.authenticate("testuser", "password123");
            System.out.println("Autenticação bem-sucedida: " + authenticated);*/

            service.listUsers().forEach(user -> System.out.println("Usuário registrado: " + user.GetName()));


        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}