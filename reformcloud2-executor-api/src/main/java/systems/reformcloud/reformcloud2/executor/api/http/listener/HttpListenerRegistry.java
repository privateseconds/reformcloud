/*
 * This file is part of reformcloud2, licensed under the MIT License (MIT).
 *
 * Copyright (c) ReformCloud <https://github.com/ReformCloud>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package systems.reformcloud.reformcloud2.executor.api.http.listener;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;
import systems.reformcloud.reformcloud2.executor.api.http.server.HttpServer;

import java.util.List;
import java.util.Map;

/**
 * Represents a registry of {@link HttpListener}s which are known.
 *
 * @author derklaro
 * @see HttpServer#getListenerRegistry()
 * @since 25. October 2020
 */
public interface HttpListenerRegistry {

    /**
     * Registers all given listeners as listeners for the given path.
     *
     * @param path          the path in the format {@code /a/b/c}. For more information see {@link HttpListener}.
     * @param httpListeners the listeners to register.
     * @return the same instance of this class, for chaining
     */
    @NotNull
    HttpListenerRegistry registerListeners(@NotNull String path, @NotNull HttpListener... httpListeners);

    /**
     * Unregisters all given listeners.
     *
     * @param httpListener the listeners to unregister.
     * @return the same instance of this class, for chaining
     */
    @NotNull
    HttpListenerRegistry unregisterListeners(@NotNull HttpListener... httpListener);

    /**
     * Unregisters all listeners listening to the {@code path}.
     *
     * @param path the path of listeners to unregister.
     * @return the same instance of this class, for chaining
     */
    @NotNull
    HttpListenerRegistry unregisterListeners(@NotNull String path);

    /**
     * Unregisters all known listeners.
     *
     * @return the same instance of this class, for chaining
     */
    @NotNull
    HttpListenerRegistry clearListeners();

    /**
     * Get all registered listeners. The keys of the map are the listening paths, the values
     * all listeners listening to the path.
     *
     * @return all known listeners to this registry
     */
    @NotNull
    @UnmodifiableView
    Map<String, List<HttpListenerRegistryEntry>> getListeners();
}