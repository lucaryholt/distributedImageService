package Handler;


import org.json.simple.JSONObject;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class ResponseHandler implements Runnable {

    private SessionManager sessionManager;
    private JSONObject result;
    private int clientId;

    public ResponseHandler(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public void run() {
        if (clientId > 0) {
            try {
                Socket socket = sessionManager.getClient(clientId);
                PrintWriter out = new PrintWriter(socket.getOutputStream());
                out.println(result.toJSONString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void setResult(JSONObject result, int clientId) {
        this.result = result;
        this.clientId = clientId;
    }
}
