package com.omarea.gesture;

import android.content.Context;
import com.omarea.gesture.core.shizuku.ShizukuManager;
import com.omarea.gesture.util.GlobalState;

public class AdbProcessExtractor {
    public String extract(Context context) {
        return null;
    }

    public boolean updateAdbProcessState(Context context, boolean useRootStartService) {
        GlobalState.enhancedMode = ShizukuManager.Companion.getInstance(context).isAvailable();
        return GlobalState.enhancedMode;
    }
}
