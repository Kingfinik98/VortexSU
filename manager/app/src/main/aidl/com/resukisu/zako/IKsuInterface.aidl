// IKsuInterface.aidl
package com.vortexsu.zako;

import android.content.pm.PackageInfo;
import java.util.List;

interface IKsuInterface {
    int getPackageCount();
    List<PackageInfo> getPackages(int start, int maxCount);
}