package com.rnchartswrapper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.events.Event;

/**
 * Generic Fabric direct-event carrier: `name` must already be the codegen
 * "top"-prefixed internal name (e.g. "topChange" for the `onChange` prop),
 * not the JS-facing prop name.
 */
class RnChartsWrapperEvent extends Event<RnChartsWrapperEvent> {
    private final String mName;
    private final WritableMap mPayload;

    RnChartsWrapperEvent(int surfaceId, int viewId, String name, @Nullable WritableMap payload) {
        super(surfaceId, viewId);
        mName = name;
        mPayload = payload;
    }

    @NonNull
    @Override
    public String getEventName() {
        return mName;
    }

    @Override
    public WritableMap getEventData() {
        return mPayload;
    }
}
