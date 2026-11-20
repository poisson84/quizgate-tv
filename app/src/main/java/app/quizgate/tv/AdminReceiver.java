package app.quizgate.tv;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * 设备管理员入口。只有被 {@code dpm set-device-owner} 设为 Device Owner 后，
 * {@link DeviceOwner#apply(Context)} 才能真正落地「禁止恢复出厂 / 禁止卸载」。
 *
 * <p>普通安装时这个 receiver 只是存在，不产生任何限制；家长可以随时忽略。
 */
public class AdminReceiver extends DeviceAdminReceiver {
    private static final String TAG = "QuizGate";

    @Override
    public void onEnabled(Context context, Intent intent) {
        Log.i(TAG, "device_admin enabled");
        DeviceOwner.apply(context);
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        Log.i(TAG, "device_admin disabled");
    }

    @Override
    public CharSequence onDisableRequested(Context context, Intent intent) {
        return "禁用后「防止恢复出厂设置」和「禁止卸载」会立即失效。";
    }
}
