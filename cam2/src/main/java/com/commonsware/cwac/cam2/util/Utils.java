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

package com.commonsware.cwac.cam2.util;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.hardware.Camera;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Surface;

import com.commonsware.cwac.cam2.CameraActivity;
import com.commonsware.cwac.cam2.CameraDescriptor;
import com.commonsware.cwac.cam2.VideoRecorderActivity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Home of static utility methods used by the library and
 * offered to developers in the spirit of togetherness.
 */
public class Utils {
  /**
   * Tests the app and the device to confirm that the code
   * in this library should work. This is called automatically
   * by other classes (e.g., CameraActivity), and so you probably
   * do not need to call it yourself. But, hey, it's a public
   * method, so call it if you feel like it.
   *
   * The method will throw an IllegalStateException if the
   * environment is unsatisfactory, where the exception message
   * will tell you what is wrong.
   *
   * @param ctxt any Context will do
   */
  public static void validateEnvironment(Context ctxt) {
    if (Build.VERSION.SDK_INT<Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
      throw new IllegalStateException("App is running on device older than API Level 14");
    }

    PackageManager pm=ctxt.getPackageManager();

    if (!pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) &&
        !pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
      throw new IllegalStateException("App is running on device that lacks a camera");
    }

    if (ctxt instanceof CameraActivity) {
      try {
        ActivityInfo info=pm.getActivityInfo(((CameraActivity)ctxt).getComponentName(), 0);

        if (info.exported) {
          throw new IllegalStateException("A CameraActivity cannot be exported!");
        }
      }
      catch (PackageManager.NameNotFoundException e) {
        throw new IllegalStateException("Cannot find this activity!", e);
      }
    }

    if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.M) {
      if (ctxt.checkSelfPermission(Manifest.permission.CAMERA)!=
        PackageManager.PERMISSION_GRANTED) {
        throw new IllegalStateException("We do not have the CAMERA permission");
      }

      if (ctxt instanceof VideoRecorderActivity) {
        if (ctxt.checkSelfPermission(Manifest.permission.RECORD_AUDIO)!=
          PackageManager.PERMISSION_GRANTED) {
          throw new IllegalStateException("We do not have the RECORD_AUDIO permission");
        }
      }
    }
  }

  /**
   * Algorithm for determining if the system bar is on the
   * bottom or right. Based on implementation of PhoneWindowManager.
   * Pray that it holds up.
   *
   * @param ctxt any Context will do
   * @return true if the system bar should be on the bottom in
   * the current configuration, false otherwise
   */
  public static boolean isSystemBarOnBottom(Context ctxt) {
    Resources res=ctxt.getResources();
    Configuration cfg=res.getConfiguration();
    DisplayMetrics dm=res.getDisplayMetrics();
    boolean canMove=(dm.widthPixels != dm.heightPixels &&
        cfg.smallestScreenWidthDp < 600);

    return(!canMove || dm.widthPixels < dm.heightPixels);
  }

  public static Size getLargestPictureSize(CameraDescriptor descriptor) {
    Size result=null;

    for (Size size : descriptor.getPictureSizes()) {
      if (result == null) {
        result=size;
      }
      else {
        int resultArea=result.getWidth() * result.getHeight();
        int newArea=size.getWidth() * size.getHeight();

        if (newArea > resultArea) {
          result=size;
        }
      }
    }

    return(result);
  }

  public static Size getSmallestPictureSize(CameraDescriptor descriptor) {
    Size result=null;

    for (Size size : descriptor.getPictureSizes()) {
      if (result == null) {
        result=size;
      }
      else {
        int resultArea=result.getWidth() * result.getHeight();
        int newArea=size.getWidth() * size.getHeight();

        if (newArea < resultArea) {
          result=size;
        }
      }
    }

    return(result);
  }

  public static Size getOptimalPictureSize(CameraDescriptor descriptor, int width, int height) {
    Size result=null;
    List<Size> sorted = new ArrayList<>(descriptor.getPictureSizes());
    Collections.sort(sorted, Collections.reverseOrder(new CompareSizesByArea()));
    float ratio = (float)width/(float)height;

    for (Size size : sorted) {
      if (result == null) {
        result = size;
      }
      float ratioDifference = Math.abs((float) size.getWidth() / (float) size.getHeight() - ratio);
      if (ratioDifference < 0.2) {
        return size;
      }
    }
    return (result);
  }

  // based on https://github.com/googlesamples/android-Camera2Basic/blob/master/Application/src/main/java/com/example/android/camera2basic/Camera2BasicFragment.java

  /**
   * Given {@code choices} of {@code Size}s supported by a camera, chooses the smallest one whose
   * width and height are at least as large as the respective requested values, and whose aspect
   * ratio matches with the specified value.
   *
   *
   * @param currentCamera
   * @param choices     The list of sizes that the camera supports for the intended output class
   * @param _width       The minimum desired width
   * @param _height      The minimum desired height
   * @param defaultDisplay
   * @return The optimal {@code Size}, or an arbitrary one if none were big enough
   */
  public static Size chooseOptimalSize(int currentCamera, List<Size> choices, int _width, int _height, Display defaultDisplay) {
    // Collect the supported resolutions that are at least as big as the preview Surface
    List<Size> bigEnough = new ArrayList<Size>();
    int width;
    int height;

    int rotation = getCameraRotationRelativeToDisplay(currentCamera, defaultDisplay);
    if (rotation % 180 == 0) {
      width = _width;
      height = _height;
    } else {
      width = _height;
      height = _width;
    }

    for (Size option : choices) {
      if (option.getWidth() >= width && option.getHeight() >= height) {
        bigEnough.add(option);
      }
    }

    // Pick the smallest of those, assuming we found any
    if (bigEnough.size() > 0) {
      return Collections.min(bigEnough, new CompareSizesByArea());
    } else {
//      Log.e(TAG, "Couldn't find any suitable preview size");
      return Collections.max(choices, new CompareSizesByArea());
    }
  }

  static class CompareSizesByArea implements Comparator<Size> {

    @Override
    public int compare(Size lhs, Size rhs) {
      // We cast here to ensure the multiplications won't overflow
      return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
          (long) rhs.getWidth() * rhs.getHeight());
    }

  }
  /**

   * @return camera rotation in degrees, relative to device's normal position.
   * @param cameraId
   * @param display
   */
  static int getCameraRotationRelativeToDisplay(int cameraId, Display display) {
    // TODO CONSIDER USING CAM2 API. Maybe steal from CWAC-CAM2's OrientationPlugin. Revisit when CWAC-CAM2 supports 3-rd party OrientationPlugins
    Camera.CameraInfo info = new Camera.CameraInfo();
    Camera.getCameraInfo(cameraId, info);
    int degrees = getDisplayRotation(display);

    int result;
    if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
      result = (info.orientation + degrees) % 360;
      result = (360 - result) % 360;  // compensate the mirror
    } else {
      // back-facing
      result = (info.orientation - degrees + 360) % 360;
    }
    return result;
  }

  static private int getDisplayRotation(Display display) {
    int rotation = display.getRotation();
    int degrees = 0;
    switch (rotation) {
      case Surface.ROTATION_0:
      degrees = 0;
        break;
      case Surface.ROTATION_90:
        degrees = 90;
        break;
      case Surface.ROTATION_180:
        degrees = 180;
        break;
      case Surface.ROTATION_270:
        degrees = 270;
        break;
    }
    return degrees;
  }
}
