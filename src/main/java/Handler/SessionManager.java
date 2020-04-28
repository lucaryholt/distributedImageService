package Handler;

import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class SessionManager {
    private Map<Integer, Socket> clients = new HashMap<Integer, Socket>();

    public Map<Integer, Socket> getClients() {
        return clients;
    }

    public Socket getClient(int key) {
        return clients.get(key);
    }

    public void addClient(Socket socket, int id) {
       clients.put(id, socket);
    }

    public void removeClient(int index) {
        clients.remove(index);
    }
}
