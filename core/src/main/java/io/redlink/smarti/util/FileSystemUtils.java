/*
 * Copyright 2017 Redlink GmbH
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
 *
 */
package io.redlink.smarti.util;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 */
public class FileSystemUtils {
    private static final Set<URI> jarFileSystems = new HashSet<>();

    /**
     * Convert a local URL (file:// or jar:// protocol) to a {@link Path}
     * @param resource the URL resource
     * @return the Path
     * @throws URISyntaxException
     * @throws IOException
     */
    public static Path toPath(URL resource) throws IOException, URISyntaxException {
        if (resource == null) return null;

        final String protocol = resource.getProtocol();
        if ("file".equals(protocol)) {
            return Paths.get(resource.toURI());
        } else if ("jar".equals(protocol)) {
            final String s = resource.toString();
            final int separator = s.indexOf("!/");
            final String entryName = s.substring(separator + 2);
            final URI fileURI = URI.create(s.substring(0, separator));

            final FileSystem fileSystem;
            if (!jarFileSystems.contains(fileURI))
                synchronized (jarFileSystems) {
                    if (jarFileSystems.add(fileURI)) {
                        fileSystem = FileSystems.newFileSystem(fileURI, Collections.<String, Object>emptyMap());
                    } else {
                        fileSystem = FileSystems.getFileSystem(fileURI);
                    }
                } else {
                fileSystem = FileSystems.getFileSystem(fileURI);
            }
            return fileSystem.getPath(entryName);
        } else {
            throw new IllegalArgumentException("Can't read " + resource + ", unknown protocol '" + protocol + "'");
        }
    }

    private FileSystemUtils() {
        throw new AssertionError("No io.redlink.reisebuddy.util.FileSystemUtils instances for you!");
    }
}
