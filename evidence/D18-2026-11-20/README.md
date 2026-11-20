# D18 · 答题锁定暂停 Bilibili（2026-11-20，Android 11）

## 设备与场景

| 项 | 值 |
| --- | --- |
| 型号 | `MiTV-MFTR0` |
| 系统 | Android 11（API 30） |
| 固件 | `OS2.0.23.0.RFTMATV` |
| 播放器 | Bilibili TV / 云视听小电视 `com.xiaodianshi.tv.yst` |
| 会话 | 本次时长 2 分钟，连对 2 题，延长 5 分钟 |

## 结果

| 场景 | 结果 | 证据 |
| --- | --- | --- |
| 最后 10 秒读秒 | `mini_show remaining=0:10 caption=剩余` | `pause_log.txt` |
| 锁定瞬间暂停 | `media_focus request=1`、`media_pause dispatch key`、`mask_add ok`；Bilibili 播放器由 `state:started` 转为 `state:paused` | `pause_log.txt` |
| 遮罩移除后恢复 | `media_focus release`、`mask_remove`；Bilibili 播放器回到 `state:started` | `pause_log.txt` |
| 连对 2 题延长 | 答完 2 题后 `extendMin=5` 生效，剩余约 5 分钟 | 现场终端输出 |

关键日志：

```text
13534.541 mini_show remaining=0:10 caption=剩余
13544.248 media_focus request=1
13544.256 media_pause dispatch key
13544.382 mask_add ok
13552.150 media_focus release
13552.173 mask_remove
```

Bilibili 音频轨：

```text
22:15:50 player piid:423 state:paused
22:15:58 player piid:423 state:started
```

## 后续调整

- 2026-11-20 22:20 起，读秒阈值从 10 秒改为 5 秒；复测日志：
  `mini_show remaining=0:05 caption=剩余`。本文原始证据保留 10 秒版本的记录。

## 说明

- Bilibili TV 没有注册 MediaSession，实际生效的是 `AUDIOFOCUS_GAIN_TRANSIENT` 音频焦点；
  媒体键是兜底路径。
- 暂停和恢复由播放器对音频焦点的处理决定；本次实测会自动恢复。
- 只记录包名/状态，不含播放内容。
