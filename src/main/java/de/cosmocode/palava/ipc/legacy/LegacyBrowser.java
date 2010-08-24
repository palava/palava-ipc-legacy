package de.cosmocode.palava.ipc.legacy;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import de.cosmocode.palava.bridge.request.HttpRequest;
import de.cosmocode.palava.ipc.Browser;

/**
 * Legacy {@link Browser} implementation.
 *
 * @since 1.0
 * @author Willi Schoenborn
 */
final class LegacyBrowser implements Browser {

    private final HttpRequest request;
    
    @Inject
    public LegacyBrowser(HttpRequest request) {
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
