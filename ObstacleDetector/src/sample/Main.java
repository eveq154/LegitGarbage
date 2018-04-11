package sample;

import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import org.opencv.core.Core;

public class Main extends Application
{
    @Override
    public void start(Stage primaryStage)
    {
        try
        {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("sample.fxml"));
            BorderPane rootElement = (BorderPane) loader.load();
            Scene scene = new Scene(rootElement, 1920, 1080);
            primaryStage.setTitle("Video processing");
            primaryStage.setScene(scene);
            primaryStage.show();

            Controller controller = loader.getController();
            primaryStage.setOnCloseRequest((new EventHandler<WindowEvent>() {
                public void handle(WindowEvent we)
                {
                    controller.setClosed();
                }
            }));

        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public static void main(String[] args)
    {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

        launch(args);
    }
}
