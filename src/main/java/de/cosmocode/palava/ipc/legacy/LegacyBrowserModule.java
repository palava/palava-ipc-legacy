package de.cosmocode.palava.ipc.legacy;

import com.google.inject.Binder;
import com.google.inject.Module;

import de.cosmocode.palava.ipc.Browser;
import de.cosmocode.palava.ipc.Current;
import de.cosmocode.palava.ipc.IpcConnectionScoped;

/**
 * Binds {@link Browser} to {@link LegacyBrowser}.
 *
 * @since 1.0
 * @author Willi Schoenborn
 */
public final class LegacyBrowserModule implements Module {

    @Override
    public void configure(Binder binder) {
        binder.bind(Browser.class).annotatedWith(Current.class).to(LegacyBrowser.class).in(IpcConnectionScoped.class);
    }

}
