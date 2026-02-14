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

package net.minecraftforge.artifactural.base.artifact;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import net.minecraftforge.artifactural.api.artifact.Artifact;
import net.minecraftforge.artifactural.api.artifact.ArtifactIdentifier;
import net.minecraftforge.artifactural.api.artifact.ArtifactMetadata;
import net.minecraftforge.artifactural.api.artifact.ArtifactType;
import net.minecraftforge.artifactural.api.artifact.MissingArtifactException;
import net.minecraftforge.artifactural.api.artifact.Streamable;

public class StreamableArtifact extends ArtifactBase {

    public static Artifact ofFile(ArtifactIdentifier identifier, ArtifactType type, File file) {
        return new StreamableFileArtifact(identifier, type, file);
    }

    public static Artifact ofURL(ArtifactIdentifier identifier, ArtifactType type, URL url) {
        return new StreamableArtifact(identifier, type, url::openStream);
    }

    public static Artifact ofBytes(ArtifactIdentifier identifier, ArtifactType type, byte[] bytes) {
        return new StreamableArtifact(identifier, type, () -> new ByteArrayInputStream(bytes));
    }

    public static Artifact ofStreamable(ArtifactIdentifier identifier, ArtifactType type, Streamable streamable) {
        return new StreamableArtifact(identifier, type, streamable);
    }

    private final Streamable streamable;

    private StreamableArtifact(ArtifactIdentifier identifier, ArtifactType type, Streamable streamable) {
        this(identifier, type, new SimpleArtifactMetadata(), streamable);
    }

    private StreamableArtifact(ArtifactIdentifier identifier, ArtifactType type, ArtifactMetadata metadata, Streamable streamable) {
        super(identifier, type, metadata);
        this.streamable = streamable;
    }

    @Override
    public Artifact withMetadata(ArtifactMetadata metadata) {
        return new StreamableArtifact(getIdentifier(), getType(), metadata, streamable);
    }

    @Override
    public boolean isPresent() {
        try (InputStream is = openStream()) {
            is.close();
            return true;
        } catch (IOException ex) {
            return false;
        }
    }

    @Override
    public InputStream openStream() throws IOException {
        return streamable.openStream();
    }

    private static class StreamableFileArtifact extends StreamableArtifact implements Artifact.Cached {

        private final File file;

        private StreamableFileArtifact(ArtifactIdentifier identifier, ArtifactType type, File file) {
            super(identifier, type, () -> new FileInputStream(file));
            this.file = file;
        }

        @Override
        public File asFile() throws MissingArtifactException {
            return file;
        }

        @Override
        public File getFileLocation() throws MissingArtifactException {
            return file;
        }

        @Override
        public String toString() {
            return "StreamableFileArtifact(" + file + ")";
        }

    }

}
