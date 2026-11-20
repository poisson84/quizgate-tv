# Device Owner：阻止恢复出厂与卸载（可选）

> **MiTV-MFTR0 / Android 11 实测不支持**：固件没有声明 `android.software.device_admin`
> （`pm has-feature` 返回 `false`），`dpm set-device-owner` 直接报
> `Can't set package ... as device owner`。这台电视上本功能始终是 no-op，只能改用
> 路由器/账号/物理层。证据见 [`evidence/D17-2026-11-20/`](../evidence/D17-2026-11-20/README.md)。
>
> 其余机型：代码已实现，但**未验证**；并且即使 provisioning 成功，Recovery 双清仍可能绕过。

## 它解决什么

普通第三方应用无法在「恢复出厂设置」后存活：`/data`、APK、adb 授权、开发者选项都会被清空。
把本应用设为 **Device Owner** 后，应用可以向系统添加这些限制：

| 限制 | API | 效果 |
| --- | --- | --- |
| 禁止恢复出厂 | `UserManager.DISALLOW_FACTORY_RESET` | 设置里的「恢复出厂设置」被禁用 |
| 禁止安全模式 | `UserManager.DISALLOW_SAFE_BOOT` | 不能靠安全模式停用第三方应用 |
| 禁止改时间 | `UserManager.DISALLOW_CONFIG_DATE_TIME` | 系统时间/日期不可修改 |
| 禁止卸载本包 | `DevicePolicyManager.setUninstallBlocked` | 用户无法卸载「答题续时」 |

代码入口：`AdminReceiver`（设备管理员）、`DeviceOwner.apply()`（进程启动时落地限制）。
没有 provisioning 时全部是 no-op，普通安装不受影响。

## 前提

Device Owner 只能在设备**没有账号**时设置，通常意味着要恢复一次出厂：

1. 备份需要保留的东西（本应用的口令/恢复码无法备份，需重新设置）；
2. 电视恢复出厂；
3. 设置向导里**不要登录小米账号 / Google 账号**；
4. 装好本应用；
5. 打开电视的网络调试（开发者选项）。

> 只要设备上还有账号，`dpm set-device-owner` 会失败。这是 Android 的限制，不是本应用的。

## Provisioning

电脑与电视在同一局域网：

```bash
adb connect <电视IP>:5555
adb install -r quizgate-<版本>-debug.apk
adb shell dpm set-device-owner app.quizgate.tv/.AdminReceiver
```

成功时输出类似：

```text
Success: Device owner set to package ComponentInfo{app.quizgate.tv/app.quizgate.tv.AdminReceiver}
Active admin set to component {app.quizgate.tv/app.quizgate.tv.AdminReceiver}
```

检查（Android 11 的 `dpm` 没有 `list-owners` 子命令）：

```bash
adb shell dumpsys device_policy | grep -i -A 4 'device owner'
```

应用内：设置 →「设备所有者」应显示「已启用（防恢复出厂）」。
进程启动时 `DeviceOwner.apply()` 会重新确认四项限制。

## 它防不住什么

- **Recovery 模式双清**：按键进入 Recovery 的 `wipe data/factory reset` 不受 Android 应用层限制。
  如果孩子会这一招，device owner 也可能被清掉；这时只能靠电视外面的控制点（路由器/智能插座）。
- **固件限制**：部分小米/厂商固件会拒绝 `set-device-owner`，或以自己的账号体系覆盖。
- **adb**：`setUninstallBlocked` 主要拦设置里的卸载；拥有 adb 的人仍可能用
  `dpm remove-active-admin` 或刷机绕过。不要把 adb 长期开着给孩子用。
- **卸载留痕不等于防重置**：`Settings.Secure` 里的锚点也会被恢复出厂清掉。

## 不支持 Device Owner 的机型

先跑：

```bash
adb shell pm has-feature android.software.device_admin
```

返回 `false` 就不要再试 `dpm set-device-owner`，它只会返回
`Can't set package ... as device owner`。MiTV-MFTR0 / Android 11 就是这种情况；应用内
「设备所有者」会显示「固件不支持」。

这类机型只能把控制点放到电视外面：

- 路由器 / 网关按时间段封电视外网，或做「App 心跳不存在就断网」；
- 小米账号 / FRP、电视自带儿童模式；
- 智能插座定时断电、遥控器收起来、电视柜上锁。

Device Owner 代码保留给声明了该特性的机型，不影响普通安装。

## 撤销

一旦成为 Device Owner，设置里的「取消激活」不可用，普通卸载也会被阻止。

**撤销基本上只能恢复出厂设置。** `adb shell dpm remove-active-admin` 在 Android 11 上要求
应用在清单里声明 `android:testOnly`；本应用没有声明（声明了会限制正常安装和升级），
所以这条命令通常会失败：

```text
Error: Attempt to remove non-test admin ...
```

如果只是在测试机上短期试用，可以单独构建一个 `android:testOnly="true"` 的测试包再
provisioning；正式包不要这么做。

## 推荐组合

- Device Owner：阻止「设置 → 恢复出厂」和「卸载」；
- 路由器/网关：按时间段或心跳封电视外网，初始化后电视没网；
- 小米账号 / 电视自带儿童模式 / 智能插座：物理与账号层的兜底。

只靠任何一层都不够；先明确孩子会做到哪一步，再决定投入。
