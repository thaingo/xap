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
package com.gigaspaces.internal.client.spaceproxy.executors;

import com.gigaspaces.async.AsyncResult;
import com.gigaspaces.internal.client.RenewIteratorLeaseSpaceResponseInfo;
import com.gigaspaces.internal.space.requests.RenewIteratorLeaseSpaceRequestInfo;
import com.gigaspaces.internal.space.requests.SpaceRequestInfo;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.List;
import java.util.UUID;

/**
 * @author Alon Shoham
 * @since 15.2.0
 */
@com.gigaspaces.api.InternalApi
public class RenewIteratorLeaseDistributedSpaceTask extends SystemDistributedTask<RenewIteratorLeaseSpaceResponseInfo> {
    private RenewIteratorLeaseSpaceRequestInfo _renewIteratorLeaseSpaceRequestInfo;

    public RenewIteratorLeaseDistributedSpaceTask() {
    }

    public RenewIteratorLeaseDistributedSpaceTask(UUID uuid) {
        this._renewIteratorLeaseSpaceRequestInfo = new RenewIteratorLeaseSpaceRequestInfo(uuid);
    }

    @Override
    public SpaceRequestInfo getSpaceRequestInfo() {
        return _renewIteratorLeaseSpaceRequestInfo;
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        super.writeExternal(out);
        out.writeObject(_renewIteratorLeaseSpaceRequestInfo);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        super.readExternal(in);
        this._renewIteratorLeaseSpaceRequestInfo = (RenewIteratorLeaseSpaceRequestInfo) in.readObject();
    }

    @Override
    public RenewIteratorLeaseSpaceResponseInfo reduce(List<AsyncResult<RenewIteratorLeaseSpaceResponseInfo>> asyncResults) throws Exception {
        return null;
    }
}