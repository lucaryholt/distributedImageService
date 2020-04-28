package Handler;

import Model.Result;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class ResponseHandler implements Runnable {

    private SessionManager sessionManager;
    private Result result;

    public ResponseHandler(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public void run() {
        while (true) {
            int clientId = result.getId();
            if (clientId > 0) {
                try {
                    // Get socket from session manager
                    Socket socket = sessionManager.getClient(clientId);

                    // Instantiate PrintWriter to send response to client
                    PrintWriter out = new PrintWriter(socket.getOutputStream());

                    // Instantiate JSONObject
                    JSONObject jsonObject = new JSONObject();
                    // Populate JSONObject
                    jsonObject.put("result", result.getResult());
                    jsonObject.put("id", clientId);

                    // Send JSONObject to client
                    out.println(jsonObject.toJSONString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void setResult(Result result) {
        this.result = result;
    }
}
