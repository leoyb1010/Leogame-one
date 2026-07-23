# Bukov 移动端触控层接线

`BukovTouchControls` 是独立的 Noosa/PixelScene 组件，不依赖旧地牢快捷栏。它提供：

- 左侧移动摇杆；
- 右侧瞄准/射击摇杆（离开 deadzone 后才开火）；
- 交互、换弹、医疗、丢弃、暂停；
- 横屏/竖屏自适应布局与逻辑像素安全区；
- 多指状态、按下反馈、动作边沿消费；
- 窗口打开、应用暂停、旋转和触摸取消时的一次性清空。

## GameScene

仅在 `BukovMode.active() && !DeviceCompat.isDesktop()` 时创建：

```java
bukovTouchControls = new BukovTouchControls()
        .safeInsets(insets.left, insets.top, insets.right, insets.bottom)
        .listener(action -> {
            if (action == BukovTouchState.Action.PAUSE) {
                openBukovPause();
            }
        });
bukovTouchControls.camera = uiCamera;
bukovTouchControls.setRect(0f, 0f, uiCamera.width, uiCamera.height);
add(bukovTouchControls);
```

打开任何 `Window` 前调用：

```java
bukovTouchControls.inputBlocked(true);
```

窗口关闭后调用：

```java
bukovTouchControls.inputBlocked(false);
```

场景暂停、销毁或尺寸变化前调用 `resetInput()`；尺寸变化后重新传入
`safeInsets(...)` 并 `setRect(...)`。触控层已经包含暂停按钮，移动端不要再叠加
`BukovPauseButton`。

## RealtimeInput

建议加入一个非拥有引用：

```java
private BukovTouchControls touchControls;

public void touchControls(BukovTouchControls controls) {
    touchControls = controls;
    cancelTouches(); // 关闭旧的全屏隐形触控分区
}
```

在 `poll(...)` 中优先采样：

```java
BukovTouchState mobile = touchControls == null ? null : touchControls.state();
if (mobile != null) {
    frame.movement.set(mobile.movementX(), mobile.movementY());
    frame.aim.set(mobile.aimX(), mobile.aimY());
    frame.fireHeld = mobile.fireHeld();
    frame.reloadPressed |= touchControls.consumePressed(BukovTouchState.Action.RELOAD);
    frame.interactHeld |= mobile.actionHeld(BukovTouchState.Action.INTERACT);
    frame.interactPressed |= touchControls.consumePressed(BukovTouchState.Action.INTERACT);
    frame.dropPressed |= touchControls.consumePressed(BukovTouchState.Action.DROP);
    // MEDICAL 交给 BukovRealtimeWorld 的医疗系统消费。
}
```

接入可见触控层后，移动端不要再注册 `RealtimeInput.pointerListener` 的旧全屏隐形
左右分区，否则同一次触摸会被采样两次。桌面键鼠和手柄路径保持不变。

## BukovRealtimeWorld

提供两个窄接口即可，不需要把 UI 逻辑放进世界模拟：

```java
public void touchControls(BukovTouchControls controls) {
    input.touchControls(controls);
}

// 固定步内消费医疗动作；动作边沿只消费一次。
if (controls.consumePressed(BukovTouchState.Action.MEDICAL)) {
    useBestAvailableMedicalItem();
}
```

最终接线时应补一条源码门禁测试，确认移动端创建了 `BukovTouchControls`、窗口状态会
调用 `inputBlocked(...)`，且旧 `RealtimeTouchState` 的全屏隐形监听不再与其并存。
