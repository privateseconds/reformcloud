/*
 * MIT License
 *
 * Copyright (c) ReformCloud-Team
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
package systems.reformcloud.reformcloud2.executor.api.process;

public enum ProcessState {

    CREATED,

    PREPARED,

    STARTED,

    READY,

    FULL,

    INVISIBLE,

    RESTARTING,

    PAUSED,

    STOPPED;

    public boolean isValid() {
        return this.equals(CREATED) || this.equals(STARTED) || this.equals(READY) || this.equals(FULL) || this.equals(INVISIBLE);
    }

    public boolean isReady() {
        return this.equals(READY) || this.equals(FULL) || this.equals(INVISIBLE);
    }

    public boolean isRuntimeState() {
        return this == PREPARED || this == STARTED || this == RESTARTING || this == PAUSED || this == STOPPED;
    }
}
