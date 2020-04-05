package ba.unsa.etf.si.controllers;

import ba.unsa.etf.si.App;
import ba.unsa.etf.si.models.Receipt;
import ba.unsa.etf.si.models.ReceiptItem;
import ba.unsa.etf.si.models.status.PaymentMethod;
import ba.unsa.etf.si.server.CreditCardServer;
import ba.unsa.etf.si.utility.HttpUtils;
import ba.unsa.etf.si.utility.QRUtils;
import ba.unsa.etf.si.utility.interfaces.MessageReceiver;
import com.jfoenix.controls.JFXProgressBar;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.CreditCardValidator;
import org.json.JSONObject;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static ba.unsa.etf.si.App.DOMAIN;
import static ba.unsa.etf.si.App.primaryStage;

public class PaymentProcessingController {
    @FXML
    private JFXProgressBar paymentProgress;
    @FXML
    private Text infoText, statusText;
    @FXML
    private ImageView qrCode;

    private String qrCodeString;
    private PaymentController paymentController;
    private PaymentMethod paymentMethod;

    private class CreditInfoReceiver implements MessageReceiver {

        Double priceToPay;

        public CreditInfoReceiver(Double priceToPay) {
            this.priceToPay = priceToPay;
        }

        @Override
        public void onMessageReceived(String msg) {
            JSONObject infoJson = new JSONObject(msg);
            if ((!infoJson.has("error"))) fillProgressBar(checkIfValid(infoJson), "Credit card is not valid!");
            else fillProgressBar(false, "Credit card not inserted!");
        }

        private boolean checkIfValid(JSONObject infoJson) {
            try {
                CreditCardValidator creditCardValidator = new CreditCardValidator(CreditCardValidator.MASTERCARD + CreditCardValidator.VISA);
                String[] expiryDate = infoJson.getString("expiryDate").split("/");
                int expiryMonth = Integer.parseInt(expiryDate[0]);
                int expiryYear = Integer.parseInt(expiryDate[1]);


                return LocalDate.now().compareTo(LocalDate.of(expiryYear, expiryMonth, 28)) > 0 && infoJson.getString("cvvCode").length() == 3
                        && creditCardValidator.isValid(infoJson.getString("creditCardNumber")) && infoJson.getDouble("balance") >= priceToPay;
            } catch (Exception e) {
                System.out.println("IZUZETAK: " + e.getMessage());
                return false;
            }
        }
    }

    @FXML
    public void initialize() {

    }

    public void setQRTypeAndCode(Receipt receipt, boolean isDynamic) {
        if (isDynamic) {

            StringBuilder receiptItems = new StringBuilder();
            List<ReceiptItem> receiptItemArrayList = receipt.getReceiptItems();
            for (ReceiptItem receiptItem : receiptItemArrayList) {
                int itemQuantity = (int) receiptItem.getQuantity();
                if(itemQuantity > 1)
                    receiptItems.append(receiptItem.getName()).append("(").append(itemQuantity).append(")");
                else
                    receiptItems.append(receiptItem.getName());

                receiptItems.append(",");
            }
            String receiptItemsString = StringUtils.chop(receiptItems.toString());

            qrCodeString = "{\n" +
                    "cashRegisterId: " + App.getCashRegisterID() + ",\n" +
                    "officeId: " + App.getBranchID() + ",\n" +
                    "bussinessName: \"BINGO\",\n" +
                    "receiptId: " + receipt.getReceiptID() + ",\n" +
                    "service: \"" + receiptItemsString + "\",\n" +
                    "totalPrice: " + receipt.getAmount() + "\n" +
                    "}";
        } else {
            qrCodeString = "{\n" +
                    "cashRegisterId: " + App.getCashRegisterID() + ",\n" +
                    "officeId: " + App.getBranchID() + ",\n" +
                    "bussinessName: \"BINGO\",\n" +
                    "}";
        }
    }

    public void processPayment(PaymentMethod paymentMethod, PaymentController paymentController, double totalAmount) {
        this.paymentMethod = paymentMethod;
        this.paymentController = paymentController;

        if (paymentMethod == PaymentMethod.CASH || paymentMethod == PaymentMethod.PAY_APP)
            fillProgressBar(true, "");
        else
            fetchCreditCardInfo(totalAmount);
    }

    public void fetchCreditCardInfo(Double totalAmount) {
        CreditInfoReceiver creditInfoReceiver = new CreditInfoReceiver(totalAmount);
        CreditCardServer creditCardServer = new CreditCardServer(5000, creditInfoReceiver);
        new Thread(creditCardServer).start();
    }

    public void fillProgressBar(boolean isValid, String creditCardInfo) {
        new Thread(() -> {
            try {
                for (int i = 0; i <= 100; i++) {
                    double progress = i * 0.01;
                    Platform.runLater(() -> paymentProgress.setProgress(progress));
                    Thread.sleep(15);
                }

                if (paymentMethod == PaymentMethod.CASH) {
                    CompletableFuture.runAsync(() -> paymentController.saveReceipt())
                            .handle((obj, ex) -> {
                                showMessage(ex == null);
                               return null;
                            });
                }
                if (paymentMethod == PaymentMethod.PAY_APP) {
                    CompletableFuture.runAsync(() -> paymentController.saveReceipt())
                            .thenRun(() -> Platform.runLater(() -> {
                                        try {
                                            paymentProgress.setVisible(false);
                                            qrCode.setVisible(true);
                                            qrCode.setImage(QRUtils.getQRImage(qrCodeString, 300, 300));
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    })

                            ).thenRun(() -> {
                        try {
                            Thread.sleep(10000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }).thenRun(() -> paymentController.pollForResponse())
                            .handle((obj, ex) -> {
                                showMessage(ex == null);
                                return null;
                            });
                }
                if (paymentMethod == PaymentMethod.CREDIT_CARD) {
                    if (!isValid) {
                        infoText.setText(creditCardInfo);
                        paymentProgress.setVisible(false);
                        showMessage(false);
                    } else {
                        CompletableFuture.runAsync(() -> paymentController.saveReceipt())
                                .handle((obj, ex) -> {
                                    showMessage(ex == null);
                                    return null;
                                });
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void showMessage(boolean valid) {
        if(valid) statusText.setText("Transaction successful!");
        else statusText.setText("Transaction failed! Please try again!");
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Platform.runLater(() -> ((Stage) statusText.getScene().getWindow()).close());
        paymentController.onPaymentProcessed(valid);
    }
}