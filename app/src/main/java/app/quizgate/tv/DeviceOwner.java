package app.quizgate.tv;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.UserManager;
import android.util.Log;

/**
 * Device Owner 限制。没有 provisioning 时全部是 no-op，不影响普通安装。
 *
 * <p>唯一能阻止「设置 → 恢复出厂设置」和「卸载本应用」的第三方应用方案。
 * 代价是需要一次性 provisioning（电视恢复出厂 + 不登录账号 + adb 命令），
 * 并且 Recovery 双清仍可能绕过。MiTV-MFTR0 / Android 11 实测不支持：
 * 固件没有声明 {@code android.software.device_admin}，该路径只对支持的机型有效。
 *
 * <p>这里只加与「不被绕过」直接相关的限制：
 * <ul>
 *   <li>{@link UserManager#DISALLOW_FACTORY_RESET}</li>
 *   <li>{@link UserManager#DISALLOW_SAFE_BOOT}：安全模式会停用第三方应用</li>
 *   <li>{@link UserManager#DISALLOW_CONFIG_DATE_TIME}：防止改系统时间</li>
 *   <li>{@link DevicePolicyManager#setUninstallBlocked}：禁止卸载本包</li>
 * </ul>
 * 不限制安装未知来源，否则在线升级的「立即安装」会被一起挡掉。
 */
public final class DeviceOwner {
    private static final String TAG = "QuizGate";

    private DeviceOwner() {}

    /** 固件是否声明了 device admin 特性；MiTV-MFTR0 实测为 false，无法 provisioning。 */
    public static boolean supported(Context c) {
        return c.getPackageManager().hasSystemFeature(PackageManager.FEATURE_DEVICE_ADMIN);
    }

    public static boolean isOwner(Context c) {
        DevicePolicyManager dpm = dpm(c);
        return dpm != null && dpm.isDeviceOwnerApp(c.getPackageName());
    }

    /** 只在已经是 Device Owner 时生效；每次进程启动调用一次即可。 */
    public static void apply(Context c) {
        DevicePolicyManager dpm = dpm(c);
        if (dpm == null || !dpm.isDeviceOwnerApp(c.getPackageName())) return;

        ComponentName admin = new ComponentName(c, AdminReceiver.class);
        restrict(dpm, admin, UserManager.DISALLOW_FACTORY_RESET);
        restrict(dpm, admin, UserManager.DISALLOW_SAFE_BOOT);
        restrict(dpm, admin, UserManager.DISALLOW_CONFIG_DATE_TIME);
        try {
            dpm.setUninstallBlocked(admin, c.getPackageName(), true);
        } catch (Throwable t) {
            Log.w(TAG, "device_owner uninstall_block failed: " + t);
        }
        Log.i(TAG, "device_owner applied");
    }

    private static void restrict(DevicePolicyManager dpm, ComponentName admin, String key) {
        try {
            dpm.addUserRestriction(admin, key);
        } catch (Throwable t) {
            Log.w(TAG, "device_owner restrict " + key + " failed: " + t);
        }
    }

    private static DevicePolicyManager dpm(Context c) {
        return (DevicePolicyManager) c.getSystemService(Context.DEVICE_POLICY_SERVICE);
    }
}
