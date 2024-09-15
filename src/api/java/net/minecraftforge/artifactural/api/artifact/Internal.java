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

import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import net.minecraftforge.artifactural.api.cache.ArtifactCache;
import net.minecraftforge.artifactural.api.transform.ArtifactTransformer;

final class Internal {

    static final ArtifactAttributeCollection NO_ATTRIBUTES = new ArtifactAttributeCollection() {
        @Override
        public <T> T with(ArtifactAttribute<T> attribute, T value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> T get(ArtifactAttribute<T> key) {
            return null;
        }

        @Override
        public Set<ArtifactAttribute<?>> keys() {
            return Collections.emptySet();
        }

        @Override
        public Iterator<Entry<ArtifactAttribute<?>, Object>> iterator() {
            return Collections.<Entry<ArtifactAttribute<?>, Object>>emptyList().iterator();
        }
    };

    static final ArtifactIdentifier NO_IDENTIFIER = new ArtifactIdentifier() {

        @Override
        public String getGroup() {
            return "missing";
        }

        @Override
        public String getName() {
            return "missing";
        }

        @Override
        public String getVersion() {
            return "0.0.0";
        }

        @Override
        public String getClassifier() {
            return "";
        }

        @Override
        public String getExtension() {
            return "missing";
        }

        @Override
        public String toString() {
            return "NO_IDENTIFIER";
        }

        @Override
        public ArtifactAttributeCollection getAttributes() {
            return NO_ATTRIBUTES;
        }
    };

    static final Artifact NO_ARTIFACT = new Artifact.Cached() {
        @Override
        public String toString() {
            return "NO_ARTIFACT";
        }

        @Override
        public ArtifactIdentifier getIdentifier() {
            return ArtifactIdentifier.none();
        }

        @Override
        public ArtifactMetadata getMetadata() {
            return new ArtifactMetadata() {
                @Override
                public ArtifactMetadata with(String key, String value) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public String getHash() {
                    return "ERROR";
                }

                @Override
                public String toString() {
                    return "NO_METADATA";
                }
            };
        }

        @Override
        public ArtifactType getType() {
            return ArtifactType.OTHER;
        }

        @Override
        public Artifact withMetadata(ArtifactMetadata metadata) {
            return this;
        }

        @Override
        public Artifact apply(ArtifactTransformer transformer) {
            return this;
        }

        @Override
        public Artifact.Cached cache(ArtifactCache cache) {
            return this;
        }

        @Override
        public boolean isPresent() {
            return false;
        }

        @Override
        public InputStream openStream() throws MissingArtifactException {
            throw new MissingArtifactException(getIdentifier());
        }

        @Override
        public File asFile() throws MissingArtifactException {
            throw new MissingArtifactException(getIdentifier());
        }

        @Override
        public File getFileLocation() throws MissingArtifactException {
            throw new MissingArtifactException(getIdentifier());
        }

    };

}
