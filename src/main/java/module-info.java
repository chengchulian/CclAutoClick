module org.ccl.cclautoclick {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires com.sun.jna;
    requires com.google.gson;

    opens org.ccl.cclautoclick to javafx.fxml;
    opens org.ccl.cclautoclick.controller to javafx.fxml;
    opens org.ccl.cclautoclick.config to com.google.gson;

    exports org.ccl.cclautoclick;
    exports org.ccl.cclautoclick.controller;
    exports org.ccl.cclautoclick.engine;
    exports org.ccl.cclautoclick.model;
    exports org.ccl.cclautoclick.jna;
    exports org.ccl.cclautoclick.listener;
    exports org.ccl.cclautoclick.scheduler;
    exports org.ccl.cclautoclick.sender;
    exports org.ccl.cclautoclick.config;
}
