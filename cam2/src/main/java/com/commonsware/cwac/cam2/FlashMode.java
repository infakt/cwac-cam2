/***
 Copyright (c) 2015 CommonsWare, LLC

 Licensed under the Apache License, Version 2.0 (the "License"); you may
 not use this file except in compliance with the License. You may obtain
 a copy of the License at http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package com.commonsware.cwac.cam2;

import android.hardware.Camera;

import java.util.ArrayList;
import java.util.List;

public enum FlashMode {
  OFF,
  ON,
  AUTO,
  REDEYE,
  TORCH;

  public static List<FlashMode> fromClassic(List<String> classicModes) {
    List<FlashMode> matchedModes = new ArrayList<FlashMode>();
    if (classicModes != null) {
      for (FlashMode mode : FlashMode.values()) {
        for (String name : classicModes) {
          if (name.equals(mode.toClassic())) {
            matchedModes.add(mode);
            break;
          }
        }
      }
    }
    return matchedModes;
  }

  public String toClassic() {
    switch (this) {
      case OFF:
        return Camera.Parameters.FLASH_MODE_OFF;
      case ON:
        return Camera.Parameters.FLASH_MODE_ON;
      case AUTO:
        return Camera.Parameters.FLASH_MODE_AUTO;
      case REDEYE:
        return Camera.Parameters.FLASH_MODE_RED_EYE;
      case TORCH:
        return Camera.Parameters.FLASH_MODE_TORCH;
    }
    return null;
  }
}
