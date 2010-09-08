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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Injector;

import de.cosmocode.commons.reflect.Reflection;
import de.cosmocode.palava.bridge.SupersededBy;
import de.cosmocode.palava.bridge.command.Alias;

/**
 * Legacy implementation of the {@link CommandLoader} interface.
 *
 * @since 1.0
 * @author Willi Schoenborn
 */
@SuppressWarnings("deprecation")
final class LegacyCommandLoader implements CommandLoader {
    
    private static final Logger LOG = LoggerFactory.getLogger(LegacyCommandLoader.class);

    private final Injector injector;
    
    private final Set<Alias> aliases;

    @Inject
    public LegacyCommandLoader(Injector injector, Set<Alias> aliases) {
        this.injector = Preconditions.checkNotNull(injector, "Injector");
        this.aliases = Preconditions.checkNotNull(aliases, "Aliases");
    }

    @Override
    public Object load(String aliasedName) {
        Preconditions.checkNotNull(aliasedName, "AliasedName");
        final String realName = toRealName(aliasedName);
        final Class<?> type = getTarget(forName(realName));
        return injector.getInstance(type);
    }
    
    private Class<?> forName(String name) {
        try {
            return Reflection.forName(name);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
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
    
    private Class<?> getTarget(Class<?> type) {
        final SupersededBy supersededBy = type.getAnnotation(SupersededBy.class);
        if (supersededBy == null) {
            return type;
        } else {
            final Class<?> superseder = supersededBy.value();
            LOG.debug("Superseding {} with {}", type, superseder);
            return getTarget(superseder);
        }
    }
    
}
