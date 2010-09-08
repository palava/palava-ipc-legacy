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

import de.cosmocode.palava.bridge.request.HttpRequest;
import de.cosmocode.palava.ipc.Browser;
import de.cosmocode.palava.ipc.Current;

/**
 * Legacy {@link Browser} implementation.
 *
 * @since 1.0
 * @author Willi Schoenborn
 */
@SuppressWarnings("deprecation")
final class LegacyBrowser implements Browser {

    private final HttpRequest request;
    
    @Inject
    public LegacyBrowser(@Current HttpRequest request) {
        this.request = Preconditions.checkNotNull(request, "Request");
    }

    @Override
    public String getHttpHost() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isHttps() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getRequestUri() {
        return request.getRequestUri();
    }

    @Override
    public String getRequestMethod() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getReferer() {
        return request.getReferer();
    }

    @Override
    public String getRemoteAddress() {
        return request.getRemoteAddress();
    }

    @Override
    public String getUserAgent() {
        return request.getUserAgent();
    }

    @Override
    public String getHttpAccept() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getHttpAcceptLanguage() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getHttpAcceptEncoding() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getHttpAcceptCharset() {
        throw new UnsupportedOperationException();
    }

}
