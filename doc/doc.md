# 零、项目概述

CclAutoClick 是一个基于 JavaFX 和 JNA 的 Windows 平台自动连点器，已实现以下核心功能：

### 已实现功能

- ✅ 多按键配置（鼠标左键、右键及任意键盘按键）
- ✅ 双触发模式（HOLD 按住激活 / TOGGLE 单击切换）
- ✅ 全局启动/停止快捷键（可自定义，如 F2）
- ✅ 实时按键捕获界面
- ✅ 配置持久化（自动保存 + 手动保存/加载）
- ✅ 并发多任务调度（线程池管理）
- ✅ 输入事件过滤（避免递归触发）
- ✅ 低延迟全局输入监听（JNA Hook）

### 技术架构

- Java 11+ / JavaFX / JNA / Maven
- 分层架构：UI → 触发控制 → 状态机 → 调度 → 执行 → 监听

---

# 一、整体架构（强化版）

## 1.1 架构图

```
┌──────────────┐
│  UI 层       │  ← AutoClickController (JavaFX)
└──────┬───────┘
       ↓ 用户操作
┌──────────────┐
│ 触发控制层   │  ← TriggerController (HOLD/TOGGLE 模式判断)
└──────┬───────┘
       ↓ 状态转换
┌──────────────┐
│ 状态机层     │  ← KeyStateMachine (每键独立状态)
└──────┬───────┘
       ↓ 任务启停
┌──────────────┐
│ 调度层       │  ← TaskScheduler (ScheduledExecutorService)
└──────┬───────┘
       ↓ 定时执行
┌──────────────┐
│ 执行层       │  ← InputSender (JNA SendInput)
└──────────────┘
       ↑
┌──────────────┐
│ 监听层       │  ← GlobalInputListener (JNA Hook)
└──────┬───────┘
       ↓ 输入事件
    系统输入
```

## 1.2 数据流

```
用户按键 → JNA Hook → KeyEvent → TriggerController → KeyStateMachine 
         → TaskScheduler → ClickTask → InputSender → 系统
```

---

# 二、核心类设计（UML）

## 2.1 完整类图

```
┌─────────────────────────┐
│ AutoClickController     │  ← UI 控制器
├─────────────────────────┤
│ - scheduler             │
│ - stateMachine          │
│ - triggerController     │
│ - inputListener         │
│ - configManager         │
├─────────────────────────┤
│ + initialize()          │
│ + onStartClicked()      │
│ + onStopClicked()       │
│ + onCaptureHotkey()     │
└──────────┬──────────────┘
           | 使用
           v
┌─────────────────────────┐
│ GlobalInputListener     │  ← 全局输入监听接口
├─────────────────────────┤
│ + start()               │
│ + stop()                │
│ + setEventListener()    │
└──────────┬──────────────┘
           | 实现
           v
┌─────────────────────────┐
│ JnaInputListener        │  ← JNA Hook 实现
├─────────────────────────┤
| - hook                  |
├─────────────────────────┤
| + onKeyDown(key)        |
| + onKeyUp(key)          |
└──────────┬──────────────┘
           | 发送事件
           v
┌─────────────────────────┐
│ TriggerController       │  ← 触发控制层
├─────────────────────────┤
| - mode: HOLD/TOGGLE     |
| - configMap             |
| - stateMachine          |
| - scheduler             |
├─────────────────────────┤
| + handleKeyDown(key)    |
| + handleKeyUp(key)      |
| + setMode(mode)         |
└──────────┬──────────────┘
           | 调用
           v
┌─────────────────────────┐
│ KeyStateMachine         │  ← 状态机
├─────────────────────────┤
| - stateMap              |
| - scheduler             |
├─────────────────────────┤
| + onKeyDown(key)        |
| + onKeyUp(key)          |
| + toggle(key)           |
| + stopAll()             |
└──────────┬──────────────┘
           | 调用
           v
┌─────────────────────────┐
│ TaskScheduler           │  ← 任务调度器
├─────────────────────────┤
| - executor              |
| - taskMap               |
| - configMap             |
├─────────────────────────┤
| + startTask(key)        |
| + stopTask(key)         |
| + registerConfig()      |
└──────────┬──────────────┘
           | 创建
           v
┌─────────────────────────┐
│ ClickTask               │  ← 点击任务
├─────────────────────────┤
| - config                |
├─────────────────────────┤
| + run()                 |
└──────────┬──────────────┘
           | 调用
           v
┌─────────────────────────┐
│ InputSender             │  ← 输入执行器
├─────────────────────────┤
| + sendClick(key)        |
└─────────────────────────┘
```

---

# 三、核心数据结构（必须这样设计）

## 3.1 按键枚举

```java
enum Key {
    MOUSE_LEFT, MOUSE_RIGHT,
    KEY_A, KEY_B, ...,KEY_Z,
    KEY_0,KEY_1,...,KEY_9,
    KEY_F1,KEY_F2,...,KEY_F12,
    // ... 其他按键
}
```

---

## 3.2 按键配置

```java
class KeyConfig {
    Key key;              // 按键类型
    boolean enabled;      // 是否启用
    int interval;         // 点击间隔（毫秒）
    int delay;            // 初始延迟（毫秒）
    TriggerMode mode;     // 触发模式（HOLD/TOGGLE）
}
```

---

## 3.3 按键运行状态

```java
enum KeyState {
    IDLE,      // 空闲
    RUNNING    // 运行中
}
```

---

## 3.4 状态存储（重点）

```java
// 状态机维护的状态映射
Map<Key, KeyState> stateMap;

// 调度器维护的任务映射
Map<Key, ScheduledFuture<?>> taskMap;

// 配置映射
Map<Key, KeyConfig> configMap;
```

---

## 3.5 事件模型

```java
class KeyEvent {
    Key key;              // 按键
    EventType type;       // DOWN / UP
    long timestamp;       // 时间戳
}
```

---

# 四、全局按键监听设计（JNA Hook）

## 4.1 监听器接口

```java
interface GlobalInputListener {
    void start();

    void stop();

    void setEventListener(Consumer<KeyEvent> listener);

    boolean isListening();
}
```

---

## 4.2 JNA Hook 实现要点

```java
class JnaInputListener implements GlobalInputListener {

    private LowLevelKeyboardProc keyboardHook;
    private LowLevelMouseProc mouseHook;

    // 安装钩子
    HHOOK hook = User32.INSTANCE.SetWindowsHookEx(
            WH_KEYBOARD_LL,
            keyboardHook,
            null,
            0
    );

    // 回调处理
    LRESULT callback(int nCode, WPARAM wParam, LPARAM lParam) {
        if (nCode >= 0) {
            // 解析按键信息
            Key key = parseKey(lParam);
            EventType type = parseType(wParam);

            // 过滤自身产生的事件
            if (!isSelfGenerated(key)) {
                listener.accept(new KeyEvent(key, type));
            }
        }
        return User32.INSTANCE.CallNextHookEx(hook, nCode, wParam, lParam);
    }
}
```

---

## 4.3 事件分发流程

```java
// AutoClickController 中的事件处理
inputListener.setEventListener(event ->{
        Platform.

runLater(() ->{
        // 1. 检查是否是全局快捷键
        if(event.

getKey() ==startStopHotkey &&event.

isKeyDown()){

toggleStartStop();
            return;
                    }

                    // 2. 如果正在运行，处理连点逻辑
                    if(isRunning){
        triggerController.

handleKeyEvent(event);

updateStatusDisplay();
        }
                });
                });
```

---

# 五、触发控制层（核心逻辑）

## 5.1 模式枚举

```java
enum TriggerMode {
    HOLD,     // 按住激活：按下开始，松开停止
    TOGGLE    // 单击切换：按一次开始，再按一次停止
}
```

---

## 5.2 控制逻辑（关键）

```java
class TriggerController {

    private TriggerMode mode = TriggerMode.TOGGLE;
    private Map<Key, KeyConfig> configMap;
    private KeyStateMachine stateMachine;
    private TaskScheduler scheduler;

    /**
     * 处理按键按下事件
     */
    void handleKeyDown(Key key) {
        KeyConfig config = configMap.get(key);
        if (config == null || !config.isEnabled()) {
            return;  // 未配置或未启用，忽略
        }

        if (mode == TriggerMode.HOLD) {
            // HOLD 模式：按下时启动
            stateMachine.onKeyDown(key);
        } else if (mode == TriggerMode.TOGGLE) {
            // TOGGLE 模式：按下时切换状态
            stateMachine.toggle(key);
        }
    }

    /**
     * 处理按键释放事件
     */
    void handleKeyUp(Key key) {
        KeyConfig config = configMap.get(key);
        if (config == null || !config.isEnabled()) {
            return;
        }

        if (mode == TriggerMode.HOLD) {
            // HOLD 模式：松开时停止
            stateMachine.onKeyUp(key);
        }
        // TOGGLE 模式下不处理 keyup
    }
}
```

---

## 5.3 模式切换

在 UI 层通过单选按钮切换：

```java

@FXML
private void onModeChanged() {
    TriggerMode mode = holdModeRadio.isSelected() ?
            TriggerMode.HOLD : TriggerMode.TOGGLE;
    triggerController.setMode(mode);
    triggerAutoSave();  // 自动保存配置
}
```

---

# 六、状态机设计（核心中的核心）

## 6.1 状态转换图

```
HOLD 模式：
┌──────┐   DOWN    ┌─────────┐
│ IDLE │ ────────> │ RUNNING │
└──────┘           └─────────┘
     ▲                  │
     └────── UP ────────┘

TOGGLE 模式：
┌──────┐   DOWN    ┌─────────┐
│ IDLE │ <───────> │ RUNNING │
└──────┘           └─────────┘
        DOWN
```

---

## 6.2 状态机实现

```java
class KeyStateMachine {

    private Map<Key, KeyState> stateMap = new ConcurrentHashMap<>();
    private TaskScheduler scheduler;

    /**
     * HOLD 模式：按下启动
     */
    void onKeyDown(Key key) {
        KeyState state = stateMap.getOrDefault(key, KeyState.IDLE);
        if (state == KeyState.IDLE) {
            stateMap.put(key, KeyState.RUNNING);
            scheduler.startTask(key);  // 启动任务
        }
    }

    /**
     * HOLD 模式：松开停止
     */
    void onKeyUp(Key key) {
        KeyState state = stateMap.getOrDefault(key, KeyState.IDLE);
        if (state == KeyState.RUNNING) {
            stateMap.put(key, KeyState.IDLE);
            scheduler.stopTask(key);   // 停止任务
        }
    }

    /**
     * TOGGLE 模式：切换状态
     */
    void toggle(Key key) {
        KeyState state = stateMap.getOrDefault(key, KeyState.IDLE);
        if (state == KeyState.IDLE) {
            stateMap.put(key, KeyState.RUNNING);
            scheduler.startTask(key);  // 启动任务
        } else {
            stateMap.put(key, KeyState.IDLE);
            scheduler.stopTask(key);   // 停止任务
        }
    }

    /**
     * 停止所有任务
     */
    void stopAll() {
        for (Key key : stateMap.keySet()) {
            stateMap.put(key, KeyState.IDLE);
            scheduler.stopTask(key);
        }
    }

    /**
     * 查询状态
     */
    boolean isRunning(Key key) {
        return stateMap.getOrDefault(key, KeyState.IDLE) == KeyState.RUNNING;
    }
}
```

---

## 6.3 状态机关键点

1. **线程安全**：使用 `ConcurrentHashMap` 保证并发安全
2. **幂等性**：重复触发同一状态不会造成问题
3. **独立性**：每个按键状态独立，互不影响
4. **可查询**：提供状态查询接口供 UI 显示

---

# 七、多任务调度设计

## 7.1 调度器架构

```java
class TaskScheduler {

    // 线程池：根据按键数量动态调整
    private ScheduledExecutorService executor =
            Executors.newScheduledThreadPool(8);

    // 任务映射
    private Map<Key, ScheduledFuture<?>> taskMap = new ConcurrentHashMap<>();

    // 配置映射
    private Map<Key, KeyConfig> configMap = new ConcurrentHashMap<>();
}
```

---

## 7.2 注册配置

```java
void registerConfig(Key key, KeyConfig config) {
    configMap.put(key, config);
}
```

---

## 7.3 启动任务

```java
void startTask(Key key) {
    KeyConfig config = configMap.get(key);
    if (config == null) {
        return;
    }

    // 如果已有任务在运行，先停止
    stopTask(key);

    // 创建新任务
    ClickTask task = new ClickTask(config);

    // 调度执行：delay 后开始，每隔 interval 执行一次
    ScheduledFuture<?> future = executor.scheduleAtFixedRate(
            task,
            config.getDelay(),      // 初始延迟
            config.getInterval(),   // 执行间隔
            TimeUnit.MILLISECONDS
    );

    taskMap.put(key, future);
}
```

---

## 7.4 停止任务

```java
void stopTask(Key key) {
    ScheduledFuture<?> future = taskMap.remove(key);
    if (future != null) {
        future.cancel(false);  // 不中断正在执行的任务
    }
}
```

---

## 7.5 关闭调度器

```java
void shutdown() {
    // 停止所有任务
    stopAll();

    // 关闭线程池
    executor.shutdown();
    try {
        if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
            executor.shutdownNow();
        }
    } catch (InterruptedException e) {
        executor.shutdownNow();
    }
}
```

---

## 7.6 并发控制要点

1. **任务去重**：启动前检查并停止旧任务
2. **优雅关闭**：等待任务完成后再关闭
3. **资源清理**：确保 `taskMap` 及时清理
4. **异常处理**：任务异常不应影响其他任务

---

# 八、执行层（点击任务）

## 8.1 ClickTask 实现

```java
class ClickTask implements Runnable {

    private final KeyConfig config;
    private final InputSender sender;

    @Override
    public void run() {
        try {
            // 执行点击
            sender.sendClick(config.getKey());
        } catch (Exception e) {
            // 记录异常但不中断任务
            System.err.println("Click task error: " + e.getMessage());
        }
    }
}
```

---

## 8.2 InputSender 实现（JNA）

```java
class InputSender {

    /**
     * 发送鼠标点击
     */
    void sendMouseClick(MouseButton button) {
        // 按下
        sendMouseEvent(button, MOUSEEVENTF_DOWN);
        // 抬起
        sendMouseEvent(button, MOUSEEVENTF_UP);
    }

    /**
     * 发送键盘点击
     */
    void sendKeyClick(VirtualKeyCode keyCode) {
        // 按下
        sendKeyboardEvent(keyCode, KEYEVENTF_KEYDOWN);
        // 抬起
        sendKeyboardEvent(keyCode, KEYEVENTF_KEYUP);
    }

    private void sendMouseEvent(...) {
        INPUT input = new INPUT();
        input.type = INPUT_MOUSE;
        input.mi.dwFlags = flags;
        User32.INSTANCE.SendInput(1, input, input.size());
    }
}
```

---

## 8.3 输入过滤（防递归）

```java
class InputFilter {

    private static final ThreadLocal<Boolean> isSending =
            ThreadLocal.withInitial(() -> false);

    /**
     * 标记当前线程正在发送输入
     */
    static void markSending() {
        isSending.set(true);
    }

    /**
     * 清除标记
     */
    static void clearSending() {
        isSending.set(false);
    }

    /**
     * 检查是否是自身发送的事件
     */
    static boolean isSelfGenerated() {
        return isSending.get();
    }
}

// 在监听器中使用
if(InputFilter.

isSelfGenerated()){
        return;  // 忽略自身产生的事件
        }
```

---

# 九、完整事件流（你必须理解这个）

## 9.1 HOLD 模式（左键）

```
用户按下左键
 ↓
JNA Hook 捕获 KeyDown 事件
 ↓
GlobalInputListener 分发事件
 ↓
AutoClickController 接收（Platform.runLater）
 ↓
检查：是否全局快捷键？否
检查：是否正在运行？是
 ↓
TriggerController.handleKeyDown(MOUSE_LEFT)
 ↓
KeyStateMachine.onKeyDown(MOUSE_LEFT)
  状态：IDLE → RUNNING
 ↓
TaskScheduler.startTask(MOUSE_LEFT)
  创建 ClickTask
  调度：delay=0ms, interval=50ms
 ↓
ClickTask 循环执行（每 50ms）
  InputSender.sendClick(MOUSE_LEFT)
  InputFilter.markSending()
  User32.SendInput()
  InputFilter.clearSending()

用户松开左键
 ↓
JNA Hook 捕获 KeyUp 事件
 ↓
TriggerController.handleKeyUp(MOUSE_LEFT)
 ↓
KeyStateMachine.onKeyUp(MOUSE_LEFT)
  状态：RUNNING → IDLE
 ↓
TaskScheduler.stopTask(MOUSE_LEFT)
  future.cancel(false)
  taskMap.remove(MOUSE_LEFT)
```

---

## 9.2 TOGGLE 模式（R 键）

```
第一次按下 R
 ↓
KeyDown 事件
 ↓
TriggerController.handleKeyDown(KEY_R)
 ↓
KeyStateMachine.toggle(KEY_R)
  状态：IDLE → RUNNING
 ↓
TaskScheduler.startTask(KEY_R)
  开始连点

第二次按下 R
 ↓
KeyDown 事件
 ↓
TriggerController.handleKeyDown(KEY_R)
 ↓
KeyStateMachine.toggle(KEY_R)
  状态：RUNNING → IDLE
 ↓
TaskScheduler.stopTask(KEY_R)
  停止连点
```

---

## 9.3 全局快捷键（F2）

```
按下 F2（无论程序是否在前台）
 ↓
JNA Hook 捕获 KeyDown 事件
 ↓
AutoClickController 接收
 ↓
检查：event.getKey() == startStopHotkey？是
 ↓
toggleStartStop()
  if (isRunning) {
      stopAllTasks();
  } else {
      onStartClicked();
  }
 ↓
更新 UI 状态
```

---

## 9.4 配置持久化流程

```
用户修改配置（启用/禁用/间隔/延迟）
 ↓
PropertyChangeListener 触发
 ↓
triggerAutoSave()
 ↓
ConfigManager.triggerAutoSave()
  收集所有配置
  写入临时文件
  原子替换 config.json
 ↓
下次启动时
 ↓
loadConfiguration()
 ↓
恢复所有配置到内存
```

---

# 十、关键坑位（你一定会踩）

## ⚠️ 1. 重复触发（最常见）

**问题**：

- 键盘长按会连续触发 keyDown 事件
- 可能导致任务重复创建

**解决**：

```java
// 方案 1：状态机保护
void onKeyDown(Key key) {
    if (state == RUNNING) return;  // 已在运行，忽略
    state = RUNNING;
    scheduler.startTask(key);
}

// 方案 2：任务去重
void startTask(Key key) {
    stopTask(key);  // 先停止旧任务
    // 再创建新任务
}
```

---

## ⚠️ 2. 线程泄漏

**问题**：

- 不调用 `cancel()` 会导致任务一直运行
- `taskMap` 不删除会导致内存泄漏

**解决**：

```java
void stopTask(Key key) {
    ScheduledFuture<?> future = taskMap.remove(key);  // 同时删除
    if (future != null) {
        future.cancel(false);  // 优雅取消
    }
}
```

---

## ⚠️ 3. UI 线程问题

**问题**：

- JNA Hook 回调在非 UI 线程
- 直接更新 UI 会抛出异常

**解决**：

```java
inputListener.setEventListener(event ->{
        Platform.

runLater(() ->{

// 所有 UI 操作放在这里
updateStatus(...);
        configTable.

refresh();
    });
            });
```

---

## ⚠️ 4. 精度问题

**问题**：

- `scheduleAtFixedRate` 不是精确计时
- 高频（<10ms）会漂移
- 任务执行时间会影响下一次调度

**解决**：

```java
// 方案 1：接受误差（推荐）
// 对于大多数应用场景，±5ms 误差可接受

// 方案 2：使用高精度定时器（复杂）
// 需要结合 System.nanoTime() 手动补偿

// 方案 3：限制最小间隔
if(interval< 10){
interval =10;  // 强制最小 10ms
        }
```

---

## ⚠️ 5. 递归触发

**问题**：

- 自己发送的输入被自己监听到
- 导致无限循环

**解决**：

```java
// 使用 ThreadLocal 标记
InputFilter.markSending();
User32.INSTANCE.

SendInput(...);
InputFilter.

clearSending();

// 监听器中检查
if(InputFilter.

isSelfGenerated()){
        return;  // 忽略
        }
```

---

## ⚠️ 6. 配置文件损坏

**问题**：

- 写入过程中程序崩溃
- 导致 JSON 文件损坏

**解决**：

```java
// 原子写入：先写临时文件，再替换
Path tempFile = Files.createTempFile("config", ".json");
Files.

writeString(tempFile, jsonContent);
Files.

move(tempFile, configFile, REPLACE_EXISTING);
```

---

# 十一、进阶优化（建议你做）

## 1️⃣ 防抖（Debounce）

**场景**：防止短时间内重复触发

```java
class Debouncer {
    private Map<Key, Long> lastTriggerMap = new ConcurrentHashMap<>();
    private static final long DEBOUNCE_MS = 50;

    boolean shouldTrigger(Key key) {
        long now = System.currentTimeMillis();
        long last = lastTriggerMap.getOrDefault(key, 0L);

        if (now - last < DEBOUNCE_MS) {
            return false;  // 防抖期内，忽略
        }

        lastTriggerMap.put(key, now);
        return true;
    }
}
```

---

## 2️⃣ 抖动随机化

**场景**：避免被检测为机器人

```java
class RandomizedScheduler {

    long calculateInterval(long baseInterval) {
        // ±5ms 随机抖动
        long jitter = (long) (Math.random() * 10) - 5;
        return baseInterval + jitter;
    }
}
```

---

## 3️⃣ 优先级线程池

**场景**：确保点击任务高优先级执行

```java
ThreadFactory highPriorityFactory = r -> {
    Thread t = new Thread(r);
    t.setPriority(Thread.MAX_PRIORITY);
    t.setDaemon(true);
    return t;
};

ScheduledExecutorService executor =
        Executors.newScheduledThreadPool(8, highPriorityFactory);
```

---

## 4️⃣ 性能监控

**场景**：检测任务执行情况

```java
class TaskMetrics {
    private Map<Key, AtomicInteger> clickCount = new ConcurrentHashMap<>();
    private Map<Key, AtomicLong> totalDuration = new ConcurrentHashMap<>();

    void recordClick(Key key, long duration) {
        clickCount.computeIfAbsent(key, k -> new AtomicInteger(0)).incrementAndGet();
        totalDuration.computeIfAbsent(key, k -> new AtomicLong(0)).addAndGet(duration);
    }

    double getAverageInterval(Key key) {
        int count = clickCount.get(key).get();
        long duration = totalDuration.get(key).get();
        return count > 0 ? (double) duration / count : 0;
    }
}
```

---

## 5️⃣ 配置热更新

**场景**：运行时修改配置立即生效

```java
void updateConfig(Key key, KeyConfig newConfig) {
    // 如果任务正在运行，重启任务
    boolean wasRunning = stateMachine.isRunning(key);

    configMap.put(key, newConfig);

    if (wasRunning) {
        scheduler.stopTask(key);
        scheduler.startTask(key);
    }
}
```

---

# 十二、总结（核心抽象）

## 12.1 系统本质

```
事件驱动系统 + 状态机 + 定时任务引擎
```

## 12.2 关键三点

1. **监听是入口（Hook）**
    - JNA 实现全局键盘/鼠标监听
    - 输入过滤避免递归触发
    - 事件分发到控制器

2. **状态机是核心（每键独立）**
    - HOLD/TOGGLE 两种模式
    - IDLE/RUNNING 两种状态
    - 线程安全的状态管理

3. **调度是执行（线程池）**
    - ScheduledExecutorService 定时调度
    - 任务生命周期管理
    - 并发控制和资源清理

## 12.3 设计原则

- **分层清晰**：UI → 控制 → 状态 → 调度 → 执行 → 监听
- **职责单一**：每个类只负责一个功能
- **松耦合**：通过接口和事件解耦
- **可扩展**：新增按键或模式无需大改

## 12.4 技术亮点

✅ 全局快捷键支持（应用级控制）  
✅ 实时按键捕获（用户体验优化）  
✅ 配置持久化（自动保存 + 手动管理）  
✅ 并发安全（ConcurrentHashMap + 线程池）  
✅ 输入过滤（防递归触发）  
✅ 优雅关闭（资源清理）

---

# 十三、参考资料

- [JNA Documentation](https://github.com/java-native-access/jna)
- [JavaFX Documentation](https://openjfx.io/)
- [Windows Hook API](https://docs.microsoft.com/en-us/windows/win32/api/_winapi/)
- [ScheduledExecutorService](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ScheduledExecutorService.html)

---

