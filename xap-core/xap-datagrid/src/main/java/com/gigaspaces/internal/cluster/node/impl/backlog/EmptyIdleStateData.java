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

package com.gigaspaces.internal.cluster.node.impl.backlog;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;


/**
 * @author idan
 * @author eitan
 * @since 9.0
 */
@com.gigaspaces.api.InternalApi
public class EmptyIdleStateData implements IIdleStateData {
    private static final long serialVersionUID = 1L;

    public static final IIdleStateData INSTANCE = new EmptyIdleStateData();

    private EmptyIdleStateData() {
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException,
            ClassNotFoundException {
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public long size() {
        return 0;
    }

    @Override
    public String toString() {
        return "Idle state data [empty]";
    }

}
