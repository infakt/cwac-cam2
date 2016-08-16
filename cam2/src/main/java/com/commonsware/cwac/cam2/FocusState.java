package com.commonsware.cwac.cam2;

import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;

/**
 * Copyright Infakt Sp. z o.o.
 * Created by Dariusz Skrzypoń (dariusz.skrzypon@infakt.pl) on 16.08.2016
 *
 * @author dariuszskrzypon
 */
public enum FocusState {
    UNFOCUSED(CaptureResult.CONTROL_AF_STATE_PASSIVE_UNFOCUSED), FOCUSING(CaptureRequest.CONTROL_AF_STATE_PASSIVE_SCAN), FOCUSED(CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED), UNKNOWN(-1);

    int cam2Mode;
    FocusState(int cam2Mode) {
        this.cam2Mode = cam2Mode;
    }

    public static FocusState lookupCamera2Mode(int cam2Mode) {
        for (FocusState state: values()) {
            if (state.cam2Mode == cam2Mode) {
                return state;
            }
        }
        return UNKNOWN;
    }
}
