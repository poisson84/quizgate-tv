package app.quizgate.tv;

import android.content.ComponentName;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.util.Log;
import android.view.KeyEvent;

import java.util.List;

/**
 * 进入答题锁定态时，尝试暂停正在播放的媒体（Bilibili / 云视听小电视等）。
 *
 * <p>两条路径：
 * <ol>
 *   <li>通过通知使用权读取活动 MediaSession，对 STATE_PLAYING/BUFFERING 的会话调 pause()；</li>
 *   <li>没有活动会话时，发一次 KEYCODE_MEDIA_PAUSE，给前台播放器兜底。</li>
 * </ol>
 *
 * <p>只暂停、不自动恢复：恢复播放是家长/孩子的明确动作，避免答题结束后电视自己响起来。
 * 没有媒体在播时是 no-op；异常只记日志，不影响遮罩。
 */
public final class MediaPauser {
    private static final String TAG = "QuizGate";
    private static AudioFocusRequest focusRequest;

    private MediaPauser() {}

    public static void pause(Context c) {
        int paused = 0;
        try {
            MediaSessionManager msm =
                    (MediaSessionManager) c.getSystemService(Context.MEDIA_SESSION_SERVICE);
            if (msm != null) {
                ComponentName listener = new ComponentName(c, NotifProbe.class);
                List<MediaController> sessions = msm.getActiveSessions(listener);
                for (MediaController mc : sessions) {
                    PlaybackState st = mc.getPlaybackState();
                    if (st == null) continue;
                    int state = st.getState();
                    if (state == PlaybackState.STATE_PLAYING
                            || state == PlaybackState.STATE_BUFFERING) {
                        mc.getTransportControls().pause();
                        paused++;
                    }
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "media_pause sessions failed: " + t);
        }

        if (paused > 0) {
            Log.i(TAG, "media_pause sessions=" + paused);
            return;
        }

        AudioManager am = (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
        if (am == null) return;

        // 很多播放器（含部分 Bilibili TV 版本）不注册 MediaSession，但会在音频焦点丢失时暂停。
        requestFocus(am);

        // 再补一次媒体键，覆盖只监听媒体键的播放器。
        try {
            am.dispatchMediaKeyEvent(
                    new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE));
            am.dispatchMediaKeyEvent(
                    new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PAUSE));
            Log.i(TAG, "media_pause dispatch key");
        } catch (Throwable t) {
            Log.w(TAG, "media_pause key failed: " + t);
        }
    }

    /** 遮罩移除时释放音频焦点；Bilibili 等播放器如果支持，会自动恢复播放。 */
    public static void release(Context c) {
        if (focusRequest == null) return;
        AudioManager am = (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
        if (am != null) {
            try {
                am.abandonAudioFocusRequest(focusRequest);
            } catch (Throwable t) {
                Log.w(TAG, "media_focus release failed: " + t);
            }
        }
        focusRequest = null;
        Log.i(TAG, "media_focus release");
    }

    private static void requestFocus(AudioManager am) {
        if (focusRequest != null) return;
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
        focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(attrs)
                .setWillPauseWhenDucked(false)
                .setOnAudioFocusChangeListener(
                        change -> Log.i(TAG, "media_focus change=" + change))
                .build();
        try {
            int r = am.requestAudioFocus(focusRequest);
            Log.i(TAG, "media_focus request=" + r);
        } catch (Throwable t) {
            focusRequest = null;
            Log.w(TAG, "media_focus request failed: " + t);
        }
    }
}
