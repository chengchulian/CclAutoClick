package org.ccl.cclautoclick.controller;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.util.StringConverter;
import org.ccl.cclautoclick.config.ConfigManager;
import org.ccl.cclautoclick.engine.KeyStateMachine;
import org.ccl.cclautoclick.listener.GlobalInputListener;
import org.ccl.cclautoclick.listener.JnaInputListener;
import org.ccl.cclautoclick.model.Key;
import org.ccl.cclautoclick.model.KeyConfig;
import org.ccl.cclautoclick.scheduler.TaskScheduler;

import java.util.ArrayList;
import java.util.List;

/**
 * 连点器主控制器
 */
public class AutoClickController {

    // 新架构组件
    private final TaskScheduler scheduler;
    private final KeyStateMachine stateMachine;
    private final TriggerController triggerController;
    private final GlobalInputListener inputListener;
    private final ConfigManager configManager;
    private final ObservableList<ClickConfigRow> configRows;
    @FXML
    private TableView<ClickConfigRow> configTable;
    @FXML
    private TableColumn<ClickConfigRow, Boolean> enabledColumn;
    @FXML
    private TableColumn<ClickConfigRow, String> keyColumn;
    @FXML
    private TableColumn<ClickConfigRow, Integer> intervalColumn;
    @FXML
    private TableColumn<ClickConfigRow, Integer> delayColumn;
    @FXML
    private RadioButton holdModeRadio;
    @FXML
    private RadioButton toggleModeRadio;
    @FXML
    private ToggleGroup triggerModeGroup;
    @FXML
    private Button startButton;
    @FXML
    private Button stopButton;
    @FXML
    private Label statusLabel;
    @FXML
    private Button captureHotkeyButton;
    @FXML
    private Label hotkeyHintLabel;
    private boolean isRunning = false;
    private ClickConfigRow capturingRow; // 正在捕获按键的行
    private Key startStopHotkey; // 全局启动/停止快捷键
    private boolean capturingHotkey = false; // 是否正在捕获快捷键

    public AutoClickController() {
        this.scheduler = new TaskScheduler();
        this.stateMachine = new KeyStateMachine(scheduler);
        this.triggerController = new TriggerController(scheduler, stateMachine);
        this.inputListener = new JnaInputListener();
        this.configManager = new ConfigManager();
        this.configRows = FXCollections.observableArrayList();
    }

    @FXML
    public void initialize() {
        // 初始化触发模式单选按钮组
        triggerModeGroup = new ToggleGroup();
        holdModeRadio.setToggleGroup(triggerModeGroup);
        toggleModeRadio.setToggleGroup(triggerModeGroup);
        holdModeRadio.setSelected(true); // 默认选择按住激活

        // 监听触发模式变化，触发自动保存
        triggerModeGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                triggerAutoSave();
            }
        });

        // 初始化表格
        setupTable();

        // 尝试加载配置，如果失败则使用默认配置
        if (!loadConfiguration()) {
            addDefaultConfig();
        }

        updateStatus("就绪");

        // 初始化快捷键显示
        updateHotkeyDisplay();

        // 启动全局快捷键监听（始终启用）
        startGlobalHotkeyListener();
    }

    /**
     * 设置表格
     */
    private void setupTable() {
        configTable.setEditable(true);
        configTable.setItems(configRows);

        // 启用列
        enabledColumn.setCellValueFactory(new PropertyValueFactory<>("enabled"));
        enabledColumn.setCellFactory(CheckBoxTableCell.forTableColumn(enabledColumn));
        enabledColumn.setEditable(true);
        // 监听启用状态变化，触发自动保存
        configRows.addListener((javafx.collections.ListChangeListener.Change<? extends ClickConfigRow> c) -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    for (ClickConfigRow row : c.getAddedSubList()) {
                        row.enabledProperty().addListener((obs, oldVal, newVal) -> triggerAutoSave());
                    }
                }
            }
        });
        // 为现有行添加监听器
        for (ClickConfigRow row : configRows) {
            row.enabledProperty().addListener((obs, oldVal, newVal) -> triggerAutoSave());
        }

        // 按键列 - 使用键盘监听
        keyColumn.setCellValueFactory(new PropertyValueFactory<>("keyName"));
        keyColumn.setCellFactory(col -> {
            TableCell<ClickConfigRow, String> cell = new TableCell<>() {
                private final Label label = new Label();
                private final Button captureButton = new Button("点击捕获");

                {
                    label.setStyle("-fx-alignment: center-left;");
                    captureButton.setStyle("-fx-font-size: 10px; -fx-padding: 2 5 2 5;");
                    captureButton.setOnAction(e -> {
                        if (getItem() != null && getTableRow() != null) {
                            ClickConfigRow row = getTableRow().getItem();
                            if (row != null && !row.isFixed()) {
                                startKeyCapture(row);
                            }
                        }
                    });
                }

                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setGraphic(null);
                    } else {
                        ClickConfigRow row = getTableRow() != null ? getTableRow().getItem() : null;
                        boolean isCapturing = capturingRow == row;

                        if (isCapturing) {
                            // 正在捕获状态
                            captureButton.setText("按任意键...");
                            captureButton.setDisable(true);
                            setGraphic(captureButton);
                        } else if (row != null && row.isFixed()) {
                            // 固定行只显示标签
                            label.setText(item);
                            setGraphic(label);
                        } else {
                            // 非固定行：显示按键名称和捕获按钮
                            HBox hbox = new HBox(5);
                            hbox.setAlignment(Pos.CENTER_LEFT);

                            // 显示按键名称
                            Label keyLabel = new Label(item);
                            keyLabel.setStyle("-fx-alignment: center-left;");
                            HBox.setHgrow(keyLabel, Priority.ALWAYS);

                            // 捕获按钮
                            captureButton.setText("重新捕获");
                            captureButton.setDisable(false);

                            hbox.getChildren().addAll(keyLabel, captureButton);
                            setGraphic(hbox);
                        }
                    }
                }
            };
            return cell;
        });

        // 间隔列
        intervalColumn.setCellValueFactory(new PropertyValueFactory<>("interval"));
        intervalColumn.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerConverter()));
        intervalColumn.setOnEditCommit(event -> {
            ClickConfigRow row = event.getRowValue();
            int newValue = event.getNewValue();
            if (newValue > 0) {
                row.setInterval(newValue);
                row.getConfig().setInterval(newValue);
                triggerAutoSave(); // 触发自动保存
            }
        });

        // 延迟列
        delayColumn.setCellValueFactory(new PropertyValueFactory<>("delay"));
        delayColumn.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerConverter()));
        delayColumn.setOnEditCommit(event -> {
            ClickConfigRow row = event.getRowValue();
            int newValue = Math.max(0, event.getNewValue());
            row.setDelay(newValue);
            row.getConfig().setDelay(newValue);
            triggerAutoSave(); // 触发自动保存
        });
    }

    /**
     * 添加默认配置
     */
    private void addDefaultConfig() {
        // 添加固定的鼠标左键配置
        KeyConfig leftClickConfig = new KeyConfig(Key.MOUSE_LEFT);
        leftClickConfig.setEnabled(true);
        ClickConfigRow leftClickRow = new ClickConfigRow(leftClickConfig);
        leftClickRow.setFixed(true); // 标记为固定行
        leftClickRow.enabledProperty().addListener((obs, oldVal, newVal) -> triggerAutoSave());
        configRows.add(leftClickRow);
        triggerController.registerConfig(Key.MOUSE_LEFT, leftClickConfig);

        // 添加固定的鼠标右键配置
        KeyConfig rightClickConfig = new KeyConfig(Key.MOUSE_RIGHT);
        rightClickConfig.setEnabled(false);
        ClickConfigRow rightClickRow = new ClickConfigRow(rightClickConfig);
        rightClickRow.setFixed(true); // 标记为固定行
        rightClickRow.enabledProperty().addListener((obs, oldVal, newVal) -> triggerAutoSave());
        configRows.add(rightClickRow);
        triggerController.registerConfig(Key.MOUSE_RIGHT, rightClickConfig);
    }

    /**
     * 启动全局快捷键监听（始终运行）
     */
    private void startGlobalHotkeyListener() {
        inputListener.setEventListener(event -> {
            Platform.runLater(() -> {
                // 检查是否是启动/停止快捷键
                if (startStopHotkey != null && event.getKey() == startStopHotkey && event.isKeyDown()) {
                    toggleStartStop();
                    return;
                }

                // 如果正在运行，处理连点器逻辑
                if (isRunning) {
                    triggerController.handleKeyEvent(event);
                    updateStatusDisplay();
                }
            });
        });

        inputListener.start();
    }

    /**
     * 启动按钮点击事件
     */
    @FXML
    private void onStartClicked() {
        if (isRunning) {
            return;
        }

        // 获取触发模式
        TriggerController.TriggerMode mode = holdModeRadio.isSelected() ?
                TriggerController.TriggerMode.HOLD : TriggerController.TriggerMode.TOGGLE;
        triggerController.setMode(mode);

        // 检查是否有启用的配置
        boolean hasEnabledConfig = false;
        for (ClickConfigRow row : configRows) {
            if (row.isEnabled()) {
                hasEnabledConfig = true;
                break;
            }
        }

        if (!hasEnabledConfig) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("警告");
            alert.setHeaderText(null);
            alert.setContentText("请至少启用一个按键配置！");
            alert.showAndWait();
            return;
        }

        isRunning = true;
        updateUIState(true);

        if (mode == TriggerController.TriggerMode.TOGGLE) {
            updateStatus("等待按键... (按一下启用的按键开始/停止)");
        } else {
            updateStatus("等待按键... (按住按键开始，松开停止)");
        }
    }

    /**
     * 停止按钮点击事件
     */
    @FXML
    private void onStopClicked() {
        stopAllTasks();
    }

    /**
     * 停止所有任务和监听器
     */
    private void stopAllTasks() {
        // 不停止监听器，只停止连点任务
        triggerController.stopAll();
        isRunning = false;
        updateUIState(false);
        updateStatus("已停止");
    }

    /**
     * 切换启动/停止状态（供快捷键调用）
     */
    private void toggleStartStop() {
        if (isRunning) {
            onStopClicked();
        } else {
            onStartClicked();
        }
    }

    /**
     * 触发自动保存
     */
    private void triggerAutoSave() {
        try {
            List<KeyConfig> configs = new ArrayList<>();
            for (ClickConfigRow row : configRows) {
                configs.add(row.getConfig());
            }

            KeyConfig.TriggerMode triggerMode = holdModeRadio.isSelected() ?
                    KeyConfig.TriggerMode.HOLD : KeyConfig.TriggerMode.TOGGLE;

            configManager.triggerAutoSave(configs, triggerMode, startStopHotkey);
        } catch (Exception e) {
            System.err.println("触发自动保存失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 更新快捷键显示
     */
    private void updateHotkeyDisplay() {
        if (captureHotkeyButton != null) {
            if (startStopHotkey != null) {
                captureHotkeyButton.setText(startStopHotkey.getName());
            } else {
                captureHotkeyButton.setText("点击设置");
            }
        }
    }

    /**
     * 开始捕获全局快捷键
     */
    @FXML
    private void onCaptureHotkeyClicked() {
        if (capturingHotkey) {
            return;
        }

        capturingHotkey = true;
        captureHotkeyButton.setText("按任意键...");
        captureHotkeyButton.setDisable(true);
        updateStatus("请按任意键作为启动/停止快捷键...");

        // 临时修改事件监听器来捕获按键
        inputListener.setEventListener(event -> {
            if (event.isKeyDown()) {
                Key key = event.getKey();

                // 在 JavaFX 线程中更新 UI
                Platform.runLater(() -> {
                    startStopHotkey = key;
                    capturingHotkey = false;

                    // 更新显示
                    updateHotkeyDisplay();
                    captureHotkeyButton.setDisable(false);
                    updateStatus("已设置快捷键: " + key.getName());

                    // 恢复正常的监听器
                    startGlobalHotkeyListener();

                    // 触发自动保存
                    triggerAutoSave();
                });
            }
        });
    }

    /**
     * 清除全局快捷键
     */
    @FXML
    private void onClearHotkeyClicked() {
        startStopHotkey = null;
        updateHotkeyDisplay();
        updateStatus("已清除快捷键");
        triggerAutoSave(); // 触发自动保存
    }

    /**
     * 添加新配置行
     */
    @FXML
    private void onAddRowClicked() {
        // 默认使用 KEY_A，用户需要捕获按键
        KeyConfig config = new KeyConfig(Key.KEY_A);
        ClickConfigRow row = new ClickConfigRow(config);
        row.enabledProperty().addListener((obs, oldVal, newVal) -> triggerAutoSave());
        configRows.add(row);
        triggerController.registerConfig(Key.KEY_A, config);
        triggerAutoSave(); // 触发自动保存
    }

    /**
     * 删除选中行
     */
    @FXML
    private void onDeleteRowClicked() {
        ClickConfigRow selected = configTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // 固定行不允许删除
            if (selected.isFixed()) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("警告");
                alert.setHeaderText(null);
                alert.setContentText("鼠标左键和右键是固定按键，不能删除！");
                alert.showAndWait();
                return;
            }
            configRows.remove(selected);
            triggerAutoSave(); // 触发自动保存
        }
    }

    /**
     * 更新UI状态
     */
    private void updateUIState(boolean running) {
        Platform.runLater(() -> {
            startButton.setDisable(running);
            stopButton.setDisable(!running);
            configTable.setDisable(running);
        });
    }

    /**
     * 更新状态标签
     */
    private void updateStatus(String status) {
        Platform.runLater(() -> {
            statusLabel.setText("状态: " + status);
        });
    }

    /**
     * 更新状态显示（显示正在运行的按键）
     */
    private void updateStatusDisplay() {
        StringBuilder sb = new StringBuilder();
        for (ClickConfigRow row : configRows) {
            if (row.isEnabled() && triggerController.isRunning(row.getConfig().getKey())) {
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(row.getKeyName()).append("运行中");
            }
        }

        if (sb.length() > 0) {
            updateStatus(sb.toString());
        } else {
            TriggerController.TriggerMode mode = triggerController.getMode();
            if (mode == TriggerController.TriggerMode.HOLD) {
                updateStatus("等待按键... (按住按键开始，松开停止)");
            } else {
                updateStatus("等待按键... (按一下启用的按键开始/停止)");
            }
        }
    }

    /**
     * 开始按键捕获
     */
    private void startKeyCapture(ClickConfigRow row) {
        if (capturingRow != null) {
            return;
        }

        capturingRow = row;
        configTable.refresh(); // 刷新表格显示
        updateStatus("请按任意键...");

        // 临时修改事件监听器来捕获按键
        inputListener.setEventListener(event -> {
            if (event.isKeyDown()) {
                Key key = event.getKey();

                // 在 JavaFX 线程中更新 UI
                Platform.runLater(() -> {
                    // 更新行的按键信息
                    row.setKeyName(key.getName());
                    row.getConfig().setKey(key);

                    // 重新注册配置（因为key变了）
                    triggerController.registerConfig(key, row.getConfig());

                    capturingRow = null;

                    // 刷新表格
                    configTable.refresh();
                    updateStatus("就绪 - 已捕获: " + key.getName());

                    // 恢复正常的监听器
                    startGlobalHotkeyListener();

                    // 触发自动保存
                    triggerAutoSave();
                });
            }
        });
    }

    /**
     * 清理资源
     */
    public void cleanup() {
        if (inputListener != null && inputListener.isListening()) {
            inputListener.stop();
        }
        triggerController.stopAll();
        scheduler.shutdown();
        configManager.shutdown(); // 关闭自动保存执行器
    }

    /**
     * 保存配置按钮点击事件
     */
    @FXML
    private void onSaveConfigClicked() {
        try {
            // 收集所有配置
            List<KeyConfig> configs = new ArrayList<>();
            for (ClickConfigRow row : configRows) {
                configs.add(row.getConfig());
            }

            // 获取当前触发模式
            KeyConfig.TriggerMode triggerMode = holdModeRadio.isSelected() ?
                    KeyConfig.TriggerMode.HOLD : KeyConfig.TriggerMode.TOGGLE;

            // 保存配置（包括全局快捷键）
            configManager.saveConfig(configs, triggerMode, startStopHotkey);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("成功");
            alert.setHeaderText(null);
            alert.setContentText("配置已保存到本地！");
            alert.showAndWait();

            updateStatus("配置已保存");
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("错误");
            alert.setHeaderText(null);
            alert.setContentText("保存配置失败: " + e.getMessage());
            alert.showAndWait();
            e.printStackTrace();
        }
    }

    /**
     * 加载配置按钮点击事件
     */
    @FXML
    private void onLoadConfigClicked() {
        try {
            if (isRunning) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("警告");
                alert.setHeaderText(null);
                alert.setContentText("请先停止运行再加载配置！");
                alert.showAndWait();
                return;
            }

            if (loadConfiguration()) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("成功");
                alert.setHeaderText(null);
                alert.setContentText("配置已从本地加载！");
                alert.showAndWait();
            } else {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("提示");
                alert.setHeaderText(null);
                alert.setContentText("未找到配置文件，使用默认配置");
                alert.showAndWait();
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("错误");
            alert.setHeaderText(null);
            alert.setContentText("加载配置失败: " + e.getMessage());
            alert.showAndWait();
            e.printStackTrace();
        }
    }

    /**
     * 加载配置
     *
     * @return 是否成功加载
     */
    private boolean loadConfiguration() {
        ConfigManager.ConfigData configData = configManager.loadConfig();
        if (configData == null || configData.getConfigs() == null || configData.getConfigs().isEmpty()) {
            return false;
        }

        // 清空现有配置
        configRows.clear();

        // 恢复触发模式
        if ("HOLD".equals(configData.getTriggerMode())) {
            holdModeRadio.setSelected(true);
        } else {
            toggleModeRadio.setSelected(true);
        }

        // 恢复全局快捷键
        if (configData.getStartStopHotkey() != null && !configData.getStartStopHotkey().isEmpty()) {
            try {
                startStopHotkey = Key.valueOf(configData.getStartStopHotkey());
            } catch (Exception e) {
                System.err.println("加载全局快捷键失败: " + e.getMessage());
                startStopHotkey = null;
            }
        } else {
            startStopHotkey = null;
        }

        // 恢复按键配置
        for (ConfigManager.SavedKeyConfig saved : configData.getConfigs()) {
            try {
                Key key = Key.valueOf(saved.getKeyName());
                KeyConfig config = new KeyConfig(key);
                config.setEnabled(saved.isEnabled());
                config.setInterval(saved.getInterval());
                config.setDelay(saved.getDelay());
                if (saved.getTriggerMode() != null) {
                    config.setTriggerMode(KeyConfig.TriggerMode.valueOf(saved.getTriggerMode()));
                }

                ClickConfigRow row = new ClickConfigRow(config);
                // 鼠标左右键标记为固定行
                if (key == Key.MOUSE_LEFT || key == Key.MOUSE_RIGHT) {
                    row.setFixed(true);
                }
                // 添加自动保存监听器
                row.enabledProperty().addListener((obs, oldVal, newVal) -> triggerAutoSave());

                configRows.add(row);
                triggerController.registerConfig(key, config);
            } catch (Exception e) {
                System.err.println("加载配置项失败: " + saved.getKeyName() + ", 错误: " + e.getMessage());
            }
        }

        // 如果没有加载到任何配置，返回false
        if (configRows.isEmpty()) {
            return false;
        }

        return true;
    }

    /**
     * 配置行包装类（用于表格显示）
     */
    public static class ClickConfigRow {
        private final BooleanProperty enabled;
        private final StringProperty keyName;
        private final IntegerProperty interval;
        private final IntegerProperty delay;
        private final KeyConfig config;
        private boolean fixed; // 是否为固定行

        public ClickConfigRow(KeyConfig config) {
            this.config = config;
            this.enabled = new SimpleBooleanProperty(config.isEnabled());
            this.keyName = new SimpleStringProperty(config.getKey().getName());
            this.interval = new SimpleIntegerProperty(config.getInterval());
            this.delay = new SimpleIntegerProperty(config.getDelay());

            // 绑定属性
            this.enabled.addListener((obs, oldVal, newVal) -> config.setEnabled(newVal));
            this.interval.addListener((obs, oldVal, newVal) -> config.setInterval(newVal.intValue()));
            this.delay.addListener((obs, oldVal, newVal) -> config.setDelay(newVal.intValue()));
        }

        public BooleanProperty enabledProperty() {
            return enabled;
        }

        public StringProperty keyNameProperty() {
            return keyName;
        }

        public IntegerProperty intervalProperty() {
            return interval;
        }

        public IntegerProperty delayProperty() {
            return delay;
        }

        public boolean isEnabled() {
            return enabled.get();
        }

        public void setEnabled(boolean value) {
            enabled.set(value);
        }

        public String getKeyName() {
            return keyName.get();
        }

        public void setKeyName(String value) {
            keyName.set(value);
        }

        public int getInterval() {
            return interval.get();
        }

        public void setInterval(int value) {
            interval.set(value);
        }

        public int getDelay() {
            return delay.get();
        }

        public void setDelay(int value) {
            delay.set(value);
        }

        public KeyConfig getConfig() {
            return config;
        }

        public boolean isFixed() {
            return fixed;
        }

        public void setFixed(boolean fixed) {
            this.fixed = fixed;
        }
    }

    /**
     * 整数转换器
     */
    private static class IntegerConverter extends StringConverter<Integer> {
        @Override
        public String toString(Integer object) {
            return object == null ? "0" : object.toString();
        }

        @Override
        public Integer fromString(String string) {
            if (string == null || string.isEmpty()) {
                return 0;
            }
            try {
                return Integer.parseInt(string);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
    }
}
