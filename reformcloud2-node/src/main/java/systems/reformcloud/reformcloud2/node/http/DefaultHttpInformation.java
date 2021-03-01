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
package systems.reformcloud.reformcloud2.node.http;

import org.jetbrains.annotations.NotNull;
import systems.reformcloud.reformcloud2.executor.api.http.Headers;
import systems.reformcloud.reformcloud2.executor.api.http.HttpInformation;
import systems.reformcloud.reformcloud2.executor.api.http.HttpVersion;
import systems.reformcloud.reformcloud2.executor.api.http.decode.DecodeResult;
import systems.reformcloud.reformcloud2.node.http.decode.DefaultDecodeResultHolder;

public abstract class DefaultHttpInformation<T extends HttpInformation<T>> extends DefaultDecodeResultHolder<T> implements HttpInformation<T>, InstanceHolder<T> {

    protected final Headers headers;
    protected HttpVersion httpVersion;

    protected DefaultHttpInformation(HttpVersion httpVersion, Headers headers, DecodeResult decodeResult) {
        super(decodeResult);
        this.headers = headers;
        this.httpVersion = httpVersion;
    }

    @Override
    public @NotNull HttpVersion httpVersion() {
        return this.httpVersion;
    }

    @Override
    public @NotNull T httpVersion(@NotNull HttpVersion httpVersion) {
        this.httpVersion = httpVersion;
        return this.self();
    }

    @Override
    public @NotNull Headers headers() {
        return this.headers;
    }
}