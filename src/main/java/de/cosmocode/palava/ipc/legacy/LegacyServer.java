/**
 * Copyright 2010 CosmoCode GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.cosmocode.palava.ipc.legacy;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Injector;

import de.cosmocode.palava.bridge.Server;
import de.cosmocode.palava.bridge.ServiceManager;

/**
 * Default implementation of the {@link Server} interface.
 *
 * @author Willi Schoenborn
 */
final class LegacyServer implements Server, ServiceManager {

    private final Injector injector;
    
    @Inject
    public LegacyServer(Injector injector) {
        this.injector = Preconditions.checkNotNull(injector, "Injector");
    }

    @Override
    public ServiceManager getServiceManager() {
        return this;
    }
    
    @Override
    public <T> T lookup(Class<T> spec) {
        Preconditions.checkNotNull(spec, "Spec");
        return injector.getInstance(spec);
    }
    
}
