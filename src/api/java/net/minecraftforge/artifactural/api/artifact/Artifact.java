/*
 * Artifactural
 * Copyright (c) 2018-2024.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 2.1
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package net.minecraftforge.artifactural.api.artifact;

import net.minecraftforge.artifactural.api.cache.ArtifactCache;
import net.minecraftforge.artifactural.api.transform.ArtifactTransformer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public interface Artifact {

    static Artifact none() {
        return Internal.NO_ARTIFACT;
    }

    ArtifactIdentifier getIdentifier();

    ArtifactMetadata getMetadata();

    ArtifactType getType();

    Artifact withMetadata(ArtifactMetadata metadata);

    Artifact apply(ArtifactTransformer transformer);

    Artifact.Cached cache(ArtifactCache cache);

    default Artifact.Cached optionallyCache(ArtifactCache cache) {
        return this instanceof Artifact.Cached ? (Artifact.Cached) this : cache(cache);
    }

    default ArtifactAttributeCollection getAttributes() {
        return getIdentifier().getAttributes();
    }

    boolean isPresent();

    InputStream openStream() throws IOException, MissingArtifactException;

    interface Cached extends Artifact {

        // Gets the file location, AND writes the file to disc if it hasn't already.
        File asFile() throws IOException, MissingArtifactException;

        // Gets the file location, but doesn't guarantee that it exists. As the wrapped Artifact may not of been written. What's the point of this?
        File getFileLocation() throws IOException, MissingArtifactException;

    }

}
