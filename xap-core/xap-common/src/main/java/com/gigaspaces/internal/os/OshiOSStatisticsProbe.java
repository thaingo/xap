package com.gigaspaces.internal.os;

import com.gigaspaces.internal.oshi.OshiChecker;
import com.gigaspaces.internal.oshi.OshiUtils;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;

import java.util.concurrent.TimeUnit;


public class OshiOSStatisticsProbe implements OSStatisticsProbe {

    @Override
    public OSStatistics probeStatistics() throws Exception {
        SystemInfo oshiSystemInfo = OshiChecker.getSystemInfo();
        HardwareAbstractionLayer hardwareAbstractionLayer = oshiSystemInfo.getHardware();

        GlobalMemory memory = hardwareAbstractionLayer.getMemory();
        CentralProcessor processor = oshiSystemInfo.getHardware().getProcessor();

        long[] oldCpuTicks = processor.getSystemCpuLoadTicks();
        Thread.sleep(TimeUnit.SECONDS.toMillis(1));
        return new OSStatistics(System.currentTimeMillis(),
                OshiUtils.calcFreeSwapMemory(memory),
                memory.getAvailable(),
                memory.getAvailable(),
                processor.getSystemCpuLoadBetweenTicks(oldCpuTicks),
                OshiUtils.getActualUsedMemory(memory),
                OshiUtils.getUsedMemoryPerc(memory),
                OshiUtils.calcNetStats());
    }

}
