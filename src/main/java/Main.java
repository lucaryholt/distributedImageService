import Handler.RequestHandler;
import Handler.ResponseHandler;
import Handler.SessionManager;
import Service.Distributor;

public class Main {

    public static void main(String[] args) {
        SessionManager sessionManager = new SessionManager();
        Distributor distributor = new Distributor();
        new RequestHandler(6000, sessionManager, distributor);
        new ResponseHandler(sessionManager);
    }

}
