package com.GUI;

import com.company.DataProcessor;
import com.company.Properties;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import java.io.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.MouseAdapter;

//Class-singleton
public class Controller
{
    @FXML
    private AnchorPane pane;

    @FXML
    private TextField nameField;

    @FXML
    private TextField passwordField;

    @FXML
    private Text statusText;

    @FXML
    private ImageView toggleButton;

    @FXML
    private HBox titleBar;

    @FXML
    private ImageView minimizeButton;

    @FXML
    private ImageView closeButton;

    @FXML
    private Text errorMessage;

    @FXML
    private ImageView registerButton;

    private Stage window;

    private Mouse mouse = new Mouse();

    private boolean isTrayIconExist = false;

    private static Controller thisController = null;

    private static final String stylePath = "com/GUI/style/";

    private DataProcessor dataProcessor = new DataProcessor();

    @FXML
    void onRegisterReleased(MouseEvent event)
    {
        if (dataProcessor.isConnected())
        {
            dataProcessor.register(Properties.getInstance().getServAdr(), Properties.getInstance().getPort(), nameField.getText(), passwordField.getText());
        }
        else
        {
            Controller.getInstance().onTurnedOff();
        }
    }

    @FXML
    private void onCloseClicked(MouseEvent event)
    {
        //window = (Stage) (closeButton).getScene().getWindow();
        if (dataProcessor.getIsServiceToggledOff())
        {
            closeApp();
        } else
        {
            window.hide();
            if (!isTrayIconExist)
            {
                javax.swing.SwingUtilities.invokeLater(this::addAppToTray);
                isTrayIconExist = true;
            }
        }
    }

    @FXML
    private void onMinimizeClicked(MouseEvent event)
    {
        window.setIconified(true);
    }

    @FXML
    private void onToggleSwitch(MouseEvent event)
    {
        toggleSwitch();
    }

    public String getUserName()
    {
        if (nameField == null)
            return null;
        return nameField.getText();
    }

    public static Controller getInstance()
    {
        return thisController;
    }

    public void setDataProcessor(DataProcessor dataProcessor)
    {
        this.dataProcessor = dataProcessor;
    }

    public void setWindow(Stage window)
    {
        this.window = window;
    }

    public void showErrorMessage(String error)
    {
        errorMessage.setText(error);
        errorMessage.setVisible(true);
    }

    public void showErrorMessage(String error, Paint paint)
    {
        errorMessage.setText(error);
        errorMessage.setFill(paint);
        errorMessage.setVisible(true);
    }

    //Modified source https://gist.github.com/jonyfs/b279b5e052c3b6893a092fed79aa7fbe#file-javafxtrayiconsample-java-L86
    private void addAppToTray()
    {
        try
        {
            // ensure awt toolkit is initialized.
            java.awt.Toolkit.getDefaultToolkit();

            // app requires system tray support, just exit if there is no support.
            if (!java.awt.SystemTray.isSupported())
            {
                System.out.println("No system tray support, application exiting.");
                closeApp();
            }

            // set up a system tray icon.
            java.awt.SystemTray tray = java.awt.SystemTray.getSystemTray();
            File imageFile = new File("src/" + stylePath + "trayIconSmallAngry.png");
            java.awt.Image image = ImageIO.read(imageFile);
            java.awt.TrayIcon trayIcon = new java.awt.TrayIcon(image);

            // if the user double-clicks on the tray icon, show the main app stage.
            trayIcon.addActionListener(event -> Platform.runLater(this::showStage));

            // if the user selects the default menu item (which includes the app name),
            // show the main app stage.
            java.awt.MenuItem openItem = new java.awt.MenuItem("Pop up");
            openItem.addActionListener(event -> Platform.runLater(this::showStage));

            // toggle logging via tray, add item to popup menu, change it's displayed label
            java.awt.MenuItem toggleItem = new java.awt.MenuItem("Turn off");
            toggleItem.addActionListener(event ->
            {
                toggleSwitch();
            });

            //Tray icon listener. Used to show tray label correctly - toggle off when switched on and vise versa
            trayIcon.addMouseMotionListener(new MouseAdapter()
            {
                @Override
                public void mouseMoved(java.awt.event.MouseEvent e)
                {
                    if (dataProcessor.getIsServiceToggledOff())
                    {
                        toggleItem.setLabel("Turn on");
                    } else
                    {
                        toggleItem.setLabel("Turn off");
                    }
                }
            });

            // the convention for tray icons seems to be to set the default icon for opening
            // the application stage in a bold font.
            java.awt.Font defaultFont = java.awt.Font.decode(null);
            java.awt.Font boldFont = defaultFont.deriveFont(java.awt.Font.BOLD);
            openItem.setFont(boldFont);

            // to finally exit the application, user must go to the system tray icon
            // and select the exit option, this will shutdown JavaFX and remove the
            // tray icon (removing the tray icon will also shut down AWT).
            java.awt.MenuItem exitItem = new java.awt.MenuItem("Exit");
            exitItem.addActionListener(event ->
            {
                tray.remove(trayIcon);
                closeApp();
            });

            // setup the popup menu for the application.
            final java.awt.PopupMenu popup = new java.awt.PopupMenu();
            popup.add(toggleItem);
            popup.add(openItem);
            popup.addSeparator();
            popup.add(exitItem);
            trayIcon.setPopupMenu(popup);

            // add the application tray icon to the system tray.
            tray.add(trayIcon);
        } catch (AWTException | IOException e)
        {
            //Show window
            ((Stage) (closeButton).getScene().getWindow()).show();
            showErrorMessage("Couldn't minimize application to tray");
        }
    }

    public void showStage()
    {
        if (window != null)
        {
            window.show();
            window.setIconified(false);
            window.toFront();
        }
    }

    public void closeService()
    {
        if (dataProcessor != null)
        {
            dataProcessor.finishConnection();

            if (dataProcessor.isAlive() && !dataProcessor.isInterrupted())
            {
                Thread.UncaughtExceptionHandler h = (th, ex) ->
                {
                };
                dataProcessor.setUncaughtExceptionHandler(h);
                dataProcessor.interrupt();
                Controller.getInstance().showErrorMessage("Emergency service interrupt.");
            }
        }
    }

    public void closeApp()
    {
        closeService();
        Properties.serializeProperties();
        Platform.exit();
        System.exit(0);
    }

    public void onTurnedOn()
    {
        statusText.setFill(Paint.valueOf("#9de05c"));
        statusText.setText("Turn off");
        toggleButton.setImage(new Image(stylePath + "turnOnButtonSmall.png"));
        DropShadow greenShadow = new DropShadow(BlurType.THREE_PASS_BOX, Color.rgb(157, 224, 92, 0.5), 10, 0, 0, 0);
        toggleButton.setEffect(greenShadow);
        nameField.setDisable(true);
        passwordField.setDisable(true);
        registerButton.setImage(new Image(stylePath + "registerButtonSmallRed.png"));
        registerButton.setDisable(true);
    }

    public void onTurnedOff()
    {
        statusText.setFill(Paint.valueOf("#f8902f"));
        statusText.setText("Turn on");
        toggleButton.setImage(new Image(stylePath + "turnOffButtonSmall.png"));
        DropShadow orangeShadow = new DropShadow(BlurType.THREE_PASS_BOX, Color.rgb(221, 157, 102, 0.5), 10, 0, 0, 0);
        toggleButton.setEffect(orangeShadow);
        nameField.setDisable(false);
        passwordField.setDisable(false);
        registerButton.setImage(new Image(stylePath + "registerButtonSmallGreen.png"));
        registerButton.setDisable(false);
    }

    public void launchService()
    {
        if (dataProcessor.getIsServiceToggledOff())
        {
            dataProcessor.collectAndSendData(Properties.getInstance().getServAdr(), Properties.getInstance().getPort(), Controller.getInstance().getUserName(), Controller.getInstance().passwordField.getText(), Properties.getInstance().getCollectInterval());
        }
        /*if (dataProcessor.isConnected() && dataProcThread == null)
        {
            dataProcThread = new Thread(() -> dataProcessor.run());
            dataProcThread.start();
        }*/
    }

    private void toggleSwitch()
    {
        if (dataProcessor == null || dataProcessor.getIsServiceToggledOff())
        {
            onTurnedOn();
            launchService();
        } else
        {
            onTurnedOff();
            closeService();
        }
    }

    public boolean isContain(MouseEvent mouseEvent, ImageView image)
    {
        if (mouseEvent.getX() >= image.getBoundsInParent().getCenterX() - (image.getFitWidth() / 2) && mouseEvent.getX() <= image.getBoundsInParent().getCenterX() + (image.getFitWidth() / 2) && mouseEvent.getY() >= image.getBoundsInParent().getCenterY() - (image.getFitHeight() / 2) && mouseEvent.getY() <= image.getBoundsInParent().getCenterY() + (image.getFitHeight() / 2))
        {
            return true;
        }
        return false;
    }

    public void initialize()
    {
        thisController = this;
        this.window = window;

        titleBar.setOnMousePressed(new EventHandler<MouseEvent>()
        {
            @Override
            public void handle(MouseEvent t)
            {
                if (isContain(t, minimizeButton) || isContain(t, closeButton))
                {
                    mouse.setX(-1);
                    mouse.setY(-1);
                } else
                {
                    mouse.setX(t.getX());
                    mouse.setY(t.getY());
                }
            }
        });

        titleBar.setOnMouseDragged(new EventHandler<MouseEvent>()
        {
            @Override
            public void handle(MouseEvent t)
            {
                if (mouse.getX() != -1)
                {
                    titleBar.getScene().getWindow().setX(t.getScreenX() - mouse.getX());
                    titleBar.getScene().getWindow().setY(t.getScreenY() - mouse.getY());
                }
            }
        });

        pane.setOnMousePressed(new EventHandler<MouseEvent>()
        {
            @Override
            public void handle(MouseEvent t)
            {
                errorMessage.setVisible(false);
            }
        });

        Properties.deserializeProperties();
        Properties.getInstance().setInvalidFieldsToDefault();
        nameField.setText(Properties.getInstance().getName());
    }
}
