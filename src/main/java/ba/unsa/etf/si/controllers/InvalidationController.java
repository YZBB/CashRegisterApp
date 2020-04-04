package ba.unsa.etf.si.controllers;


import ba.unsa.etf.si.App;
import ba.unsa.etf.si.models.Product;
import ba.unsa.etf.si.models.Receipt;
import ba.unsa.etf.si.persistance.ReceiptRepository;
import ba.unsa.etf.si.utility.HttpUtils;
import ba.unsa.etf.si.utility.IKonverzija;
import com.jfoenix.controls.JFXListView;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.json.JSONArray;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.function.Consumer;

import static ba.unsa.etf.si.App.DOMAIN;
import static ba.unsa.etf.si.controllers.PrimaryController.currentUser;

public class InvalidationController {


    @FXML private TextField searchField;
    @FXML private JFXListView<Receipt> receiptList;

    public static ArrayList<Product> productList = new ArrayList<Product>();
    String TOKEN = currentUser.getToken();

    Consumer<String> callback = (String str) -> {
        ArrayList<Receipt> receipts = getReceipts(new JSONArray(str));
        Platform.runLater(() -> receiptList.setItems(FXCollections.observableList(receipts)));
        fileLocalDatabse(receipts);

        receiptList.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent click) {
                if (click.getClickCount() == 2) {
                    Receipt selectedReceipt = receiptList.getSelectionModel().getSelectedItem();
                    receiptList.getSelectionModel().clearSelection();
                    FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("fxml/dialog.fxml"));
                    Parent parent = null;
                    try {
                        parent = fxmlLoader.load();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    DialogController dialogController = fxmlLoader.<DialogController>getController();
                    dialogController.setId(selectedReceipt.getTimestampID());

                    Scene scene = new Scene(parent);
                    Stage stage = new Stage();

                    stage.initStyle(StageStyle.UNDECORATED);
                    stage.initModality(Modality.APPLICATION_MODAL);
                    stage.setScene(scene);
                    stage.showAndWait();
                    dialogHandler(dialogController);

                }
            }
        });
    };

    private void fileLocalDatabse(ArrayList<Receipt> receipts) {
        ReceiptRepository receiptRepository = new ReceiptRepository();
        for(Receipt r : receipts) receiptRepository.add(r);
    }

    Consumer<String> callback1 = (String str) -> {
        productList = IKonverzija.getProductArrayFromJSON(str);
        System.out.println("Lista produkta učitana: " + productList.size());
        HttpRequest getSuppliesData = HttpUtils.GET(DOMAIN + "/api/receipts?cash_register_id=1", "Authorization", "Bearer " + TOKEN);

        HttpUtils.send(getSuppliesData, HttpResponse.BodyHandlers.ofString(), callback, () -> {
            System.out.println("Something went wrong.");
        });

    };
    @FXML
    public void initialize() {
        receiptList.setCellFactory(new ReceiptCellFactory());
        HttpRequest getSuppliesData = HttpUtils.GET(DOMAIN + "/api/products", "Authorization", "Bearer " + TOKEN);
        HttpUtils.send(getSuppliesData, HttpResponse.BodyHandlers.ofString(), callback1, () -> {
            System.out.println("Something went wrong.");
        });

    }

    public static class ReceiptCell extends ListCell<Receipt>{

        @FXML private Label receiptID, date, cashier, amount;

        public ReceiptCell() {
            loadFXML();
        }

        private void loadFXML() {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("fxml/invalidation.fxml"));
            loader.setController(this);
            loader.setRoot(this);
            try {
                loader.load();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected void updateItem(Receipt receipt, boolean empty) {
            super.updateItem(receipt, empty);
            if(empty) {
                setText(null);
                setContentDisplay(ContentDisplay.TEXT_ONLY);
            }
            else {
                receiptID.setText(receipt.getTimestampID());
                date.setText(receipt.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm")));
                cashier.setText(receipt.getCashier());
                amount.setText(Double.toString(receipt.getAmount()));
                setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
            }
        }
    }

    private void dialogHandler (DialogController dialogController) {
        DialogController.DialogStatus stat = dialogController.getStatus();
        if (stat.isCancel()) {
            FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("fxml/informationDialog.fxml"));
            Parent parent = null;
            try {
                parent = fxmlLoader.load();
            } catch (Exception e) {
                e.printStackTrace();
            }
            InfoDialogController infoDialogController = fxmlLoader.<InfoDialogController>getController();

            Scene scene = new Scene(parent);
            Stage stage = new Stage();

            stage.initStyle(StageStyle.UNDECORATED);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(scene);
            stage.showAndWait();
            HttpRequest getSuppliesData = HttpUtils.GET(DOMAIN + "/api/products", "Authorization", "Bearer " + TOKEN);

            HttpUtils.send(getSuppliesData, HttpResponse.BodyHandlers.ofString(), callback1, () -> {
                System.out.println("Something went wrong.");
            });
        }
    }

    private static class ReceiptCellFactory implements javafx.util.Callback<javafx.scene.control.ListView<Receipt>, javafx.scene.control.ListCell<Receipt>> {
        @Override
        public ListCell<Receipt> call(ListView<Receipt> receiptListView) {
            return new ReceiptCell();
        }
    }



    private ArrayList<Receipt> getReceipts(JSONArray arr) {
        ArrayList<Receipt> receipts = new ArrayList<>();
        ReceiptRepository repo = new ReceiptRepository();
        for (int i =0 ; i<arr.length() ; i++) {
            Receipt newRecp = new Receipt(arr.getJSONObject(i), productList);
            receipts.add(newRecp);
        }
        return receipts;
    }
}
