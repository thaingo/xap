/*
 * Copyright (c) 2008-2016, GigaSpaces Technologies, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gigaspaces.internal.oshi;

import com.gigaspaces.internal.os.OSStatistics;
import com.gigaspaces.metrics.Gauge;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.software.os.OSProcess;

public class OshiGaugeUtils {

    public final static SystemInfo oshiSystemInfo = OshiChecker.getSystemInfo();
    public final static CentralProcessor processor = oshiSystemInfo.getHardware().getProcessor();
    public final static GlobalMemory memory = oshiSystemInfo.getHardware().getMemory();
    public final static OSProcess osProcess = oshiSystemInfo.getOperatingSystem().getProcess(oshiSystemInfo.getOperatingSystem().getProcessId());

    public static Gauge<Double> getCpuPercGauge() {
        return new Gauge<Double>() {
            @Override
            public Double getValue() throws Exception {
                return processor.getSystemCpuLoad();
            }
        };
    }

    public static Gauge<Long> getFreeMemoryInBytesGauge() {
        return new Gauge<Long>() {
            @Override
            public Long getValue() throws Exception {
                return memory.getAvailable();
            }
        };
    }

    public static Gauge<Long> getActualFreeMemoryInBytesGauge() {
        return new Gauge<Long>() {
            @Override
            public Long getValue() throws Exception {
                return memory.getAvailable();
            }
        };
    }

    public static Gauge<Long> getUsedMemoryInBytesGauge() {
        return new Gauge<Long>() {
            @Override
            public Long getValue() throws Exception {
                return OshiUtils.getActualUsedMemory(memory);
            }
        };
    }

    public static Gauge<Long> getActualUsedMemoryInBytesGauge() {
        return new Gauge<Long>() {
            @Override
            public Long getValue() throws Exception {
                return OshiUtils.getActualUsedMemory(memory);
            }
        };
    }

    public static Gauge<Double> getUsedMemoryInPercentGauge() {
        return new Gauge<Double>() {
            @Override
            public Double getValue() throws Exception {
                return OshiUtils.getUsedMemoryPerc(memory);
            }
        };
    }

    public static Gauge<Long> getFreeSwapInBytesGauge() {
        return new Gauge<Long>() {
            @Override
            public Long getValue() throws Exception {
                return OshiUtils.calcFreeSwapMemory(memory);
            }
        };
    }

    public static Gauge<Long> getUsedSwapInBytesGauge() {
        return new Gauge<Long>() {
            @Override
            public Long getValue() throws Exception {
                return memory.getSwapUsed();
            }
        };
    }

    public static Gauge<Double> getUsedSwapInPercentGauge() {
        return new Gauge<Double>() {
            @Override
            public Double getValue() throws Exception {
                return (memory.getSwapUsed()/memory.getSwapTotal())*100d;
            }
        };
    }

    public static Gauge<Long> createRxBytesGauge(OSStatistics.OSNetInterfaceStats osNetInterfaceStats) {
        return new Gauge<Long>() {
            @Override
            public Long getValue() throws Exception {
                return osNetInterfaceStats.getRxBytes();
            }
        };
    }

    public static Gauge<Long> createTxBytesGauge(OSStatistics.OSNetInterfaceStats osNetInterfaceStats) {
        return new Gauge<Long>() {
            @Override
            public Long getValue() throws Exception {
                return osNetInterfaceStats.getTxBytes();
            }
        };
    }

    public static Gauge<Long> createRxPacketsGauge(OSStatistics.OSNetInterfaceStats osNetInterfaceStats) {
        return new Gauge<Long>() {
            @Override
            public Long getValue() throws Exception {
                return osNetInterfaceStats.getRxPackets();
            }
        };
    }

    public static Gauge<Long> createTxPacketsGauge(OSStatistics.OSNetInterfaceStats osNetInterfaceStats) {
        return new Gauge<Long>() {
            @Override
            public Long getValue() throws Exception {
                return osNetInterfaceStats.getTxPackets();
            }
        };
    }

    public static Gauge<Long> createRxErrorsGauge(OSStatistics.OSNetInterfaceStats osNetInterfaceStats) {
        return new Gauge<Long>() {
            @Override
            public Long getValue() throws Exception {
                return osNetInterfaceStats.getRxErrors();
            }
        };
    }

    public static Gauge<Long> createTxErrorsGauge(OSStatistics.OSNetInterfaceStats osNetInterfaceStats) {
        return new Gauge<Long>() {
            @Override
            public Long getValue() throws Exception {
                return osNetInterfaceStats.getTxErrors();
            }
        };
    }

    public static Gauge<Long> createRxDroppedGauge(OSStatistics.OSNetInterfaceStats osNetInterfaceStats) {
        return new Gauge<Long>() {
            @Override
            public Long getValue() throws Exception {
                return osNetInterfaceStats.getRxDropped();
            }
        };
    }

    public static Gauge<Long> createTxDroppedGauge(OSStatistics.OSNetInterfaceStats osNetInterfaceStats) {
        return new Gauge<Long>() {
            @Override
            public Long getValue() throws Exception {
                return osNetInterfaceStats.getTxDropped();
            }
        };
    }

    public static Gauge<Long> createProcessCpuTotalTimeGauge() {
        return new Gauge<Long>() {
            @Override
            public Long getValue() throws Exception {
                return osProcess.getKernelTime() + osProcess.getUserTime();
            }
        };
    }

    public static Gauge<Double> createProcessUsedCpuInPercentGauge() {
        return new Gauge<Double>() {
            @Override
            public Double getValue() throws Exception {
                return osProcess.calculateCpuPercent();
            }
        };
    }

}