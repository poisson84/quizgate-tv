# D17 · Device Owner 可用性（2026-11-20，Android 11）

## 设备与包

| 项 | 值 |
| --- | --- |
| 型号 | `MiTV-MFTR0` |
| 系统 | Android 11（API 30） |
| 固件 | `OS2.0.23.0.RFTMATV` |
| 包 | debug `quizgate-1.3.0-debug.apk`（含 `AdminReceiver`） |
| 连接 | `192.168.1.106:5555` |

## 结论

**该固件不支持 Device Owner**：没有声明 `android.software.device_admin` 特性，
`dpm set-device-owner` 直接失败。应用内的 Device Owner 代码在这台电视上始终是 no-op。

## 复现

```bash
adb shell pm has-feature android.software.device_admin   # false
adb shell pm has-feature android.software.managed_users  # false
adb shell dpm set-device-owner app.quizgate.tv/.AdminReceiver
```

关键输出：

```text
$ adb shell pm list features | grep -i device_admin
（无输出）

$ adb shell pm has-feature android.software.device_admin
false

$ adb shell dpm set-device-owner app.quizgate.tv/.AdminReceiver
java.lang.RuntimeException: Can't set package
  ComponentInfo{app.quizgate.tv/app.quizgate.tv.AdminReceiver} as device owner.
```

原始输出见 `dpm_output.txt`。

## 影响

- `DevicePolicyManagerService.setDeviceOwner()` 在 `mHasFeature == false` 时直接返回 false，
  与账号数、包状态、adb 权限无关；
- 因此这台电视无法通过 Device Owner 禁止「设置 → 恢复出厂」或卸载；
- 应用里的「设备所有者」应显示「固件不支持」，不要显示成可 provisioning 的状态。

## 后续

- 这台电视只能把控制点放到电视外面：路由器/网关按时间段或心跳封外网、小米账号/FRP、
  智能插座/物理锁；详见 `docs/device-owner.md` 与 README。
- 若换到声明了 `android.software.device_admin` 的机型，再按 `docs/device-owner.md` 执行
  provisioning；目前没有该类机型的实测数据。
- Android 14 固件（D2）未做本项测试，不能套用 Android 11 的结论。
