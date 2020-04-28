package Handler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class RequestHandler {



}


class ReceiveClient implements Runnable{

    private ServerSocket serverSocket;
    private SessionHandler sessionHandler;

    public ReceiveClient(int port, SessionHandler sessionHandler){
        this.sessionHandler = sessionHandler;
        try{
            serverSocket = new ServerSocket(port);
        }catch(IOException e){
            e.printStackTrace();
        }
    }

    private void receive(){
        while(true){
            try {
                Socket socket = serverSocket.accept();
                receiveRequest(socket);
                //sessionHandler.addClient(id, socket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void receiveRequest(Socket socket){
        try {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {

    }
}