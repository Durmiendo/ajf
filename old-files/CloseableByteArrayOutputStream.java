/*
 * Copyright 2014 Higher Frequency Trading
 *
 *       https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.durmiendo.ajf.utils.memorycompiler;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.CompletableFuture;

public class CloseableByteArrayOutputStream extends ByteArrayOutputStream {
    private final CompletableFuture<?> closeFuture = new CompletableFuture<>();

    @Override
    public void close() {
        closeFuture.complete(null);
    }

    public CompletableFuture<?> closeFuture() {
        return closeFuture;
    }
}
