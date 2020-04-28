import Handler.RequestHandler;
import Handler.ResponseHandler;
import Handler.SessionManager;
import Service.Distributor;

public class Main {

    public static void main(String[] args) {
        SessionManager sessionManager = new SessionManager();
        ResponseHandler responseHandler = new ResponseHandler(sessionManager);
        Distributor distributor = new Distributor(responseHandler);
        new RequestHandler(6000, sessionManager, distributor);
    }

}
