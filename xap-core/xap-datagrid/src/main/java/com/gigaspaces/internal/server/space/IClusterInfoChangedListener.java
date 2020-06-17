package com.gigaspaces.internal.server.space;

import com.gigaspaces.internal.cluster.SpaceClusterInfo;

public interface IClusterInfoChangedListener {

    void afterClusterInfoChange(SpaceClusterInfo clusterInfo);
}
