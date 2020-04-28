package ba.unsa.etf.si.routes;

import ba.unsa.etf.si.App;
import ba.unsa.etf.si.utility.server.HttpUtils;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.function.Consumer;

import static ba.unsa.etf.si.App.DOMAIN;

public class CashRegisterRoutes {

    private CashRegisterRoutes() {}

    public static void openCashRegister(String token, Consumer<String> callback, Runnable error) {
        HttpRequest.BodyPublisher bodyPublisher = HttpRequest.BodyPublishers.ofString("");
        HttpRequest POST = HttpUtils.POST(bodyPublisher, DOMAIN + "/api/cash-register/open?cash_register_id=" + App.getCashRegisterID(),
                "Authorization", "Bearer " + token);
        HttpUtils.send(POST, HttpResponse.BodyHandlers.ofString(), callback, error);
    }

    public static void closeCashRegister(String token, Consumer<String> callback, Runnable err) {
        HttpRequest.BodyPublisher bodyPublisher = HttpRequest.BodyPublishers.ofString("");
        HttpRequest POST = HttpUtils.POST(bodyPublisher, DOMAIN + "/api/cash-register/close?cash_register_id=" + App.getCashRegisterID(),
                "Authorization", "Bearer " + token);
        HttpUtils.send(POST, HttpResponse.BodyHandlers.ofString(), callback, err);
    }
}