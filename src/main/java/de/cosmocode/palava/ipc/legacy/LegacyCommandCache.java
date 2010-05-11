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

import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.MapMaker;
import com.google.inject.Inject;
import com.google.inject.Injector;

import de.cosmocode.palava.bridge.command.Alias;

/**
 * Legacy implementation of the {@link CommandCache} interface.
 *
 * @since 1.0
 * @author Willi Schoenborn
 */
final class LegacyCommandCache implements CommandCache {

    private static final Logger LOG = LoggerFactory.getLogger(LegacyCommandCache.class);

    private final ConcurrentMap<String, Class<?>> cache = new MapMaker().softValues().makeMap();

    private final Injector injector;
    
    private final Set<Alias> aliases;

    @Inject
    public LegacyCommandCache(Injector injector, Set<Alias> aliases) {
        this.injector = Preconditions.checkNotNull(injector, "Injector");
        this.aliases = Preconditions.checkNotNull(aliases, "Aliases");
    }

    @Override
    public Object load(String aliasedName) {
        Preconditions.checkNotNull(aliasedName, "AliasedName");
        final String realName = toRealName(aliasedName);
        final Class<?> type = forName(realName);
        return injector.getInstance(type);
    }
    
    private Class<?> forName(String name) {
        final Class<?> cached = cache.get(name);
        if (cached == null) {
            try {
                final Class<?> type = Class.forName(name);
                LOG.trace("Putting {} into cache", type);
                cache.put(name, type);
                return type;
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException(e);
            }
        } else {
            LOG.trace("Retrieved {} from cache", cached);
            return cached;
        }
    }

    private String toRealName(String aliasedName) {
        for (Alias alias : aliases) {
            if (aliasedName.startsWith(alias.getName())) {
                return alias.apply(aliasedName);
            }
        }
        return aliasedName;
    }
    
}
