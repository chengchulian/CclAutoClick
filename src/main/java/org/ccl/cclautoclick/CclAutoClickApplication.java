package org.ccl.cclautoclick;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.ccl.cclautoclick.controller.AutoClickController;

import java.io.IOException;

public class CclAutoClickApplication extends Application {

    private AutoClickController controller;

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(CclAutoClickApplication.class.getResource("auto-click.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 700, 600);
        stage.setTitle("CclAutoClick - 连点器");
        stage.setScene(scene);
        stage.setResizable(true);

        // 获取控制器引用
        controller = fxmlLoader.getController();

        stage.show();
    }

    @Override
    public void stop() throws Exception {
        // 清理资源
        if (controller != null) {
            controller.cleanup();
        }
        super.stop();
    }
}
