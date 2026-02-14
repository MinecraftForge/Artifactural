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

package net.minecraftforge.artifactural.base.cache;

import net.minecraftforge.artifactural.api.artifact.Artifact;
import net.minecraftforge.artifactural.api.artifact.ArtifactIdentifier;
import net.minecraftforge.artifactural.api.artifact.ArtifactMetadata;
import net.minecraftforge.artifactural.api.artifact.ArtifactType;
import net.minecraftforge.artifactural.api.artifact.MissingArtifactException;
import net.minecraftforge.artifactural.api.cache.ArtifactCache;
import net.minecraftforge.artifactural.api.transform.ArtifactTransformer;
import net.minecraftforge.artifactural.base.artifact.StreamableArtifact;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public abstract class ArtifactCacheBase implements ArtifactCache {

    Artifact.Cached doStore(File path, Artifact artifact) {
        return wrap(
                StreamableArtifact.ofStreamable(
                        artifact.getIdentifier(),
                        artifact.getType(),
                        () -> stream(path, artifact)
                ).withMetadata(artifact.getMetadata()),
                path
        );
    }

    private InputStream stream(File path, Artifact artifact) throws IOException {
        if (!path.exists()) {
            path.getParentFile().mkdirs();
            path.createNewFile();
            FileOutputStream fos = new FileOutputStream(path);
            InputStream is = artifact.openStream();
            int read;
            byte[] bytes = new byte[256];
            while ((read = is.read(bytes)) > 0) {
                fos.write(bytes, 0, read);
            }
            fos.close();
            is.close();
        }
        return new FileInputStream(path);
    }

    public static Artifact.Cached wrap(Artifact artifact, File file) {
        return new Artifact.Cached() {

            @Override
            public ArtifactIdentifier getIdentifier() {
                return artifact.getIdentifier();
            }

            @Override
            public ArtifactMetadata getMetadata() {
                return artifact.getMetadata();
            }

            @Override
            public ArtifactType getType() {
                return artifact.getType();
            }

            @Override
            public Artifact withMetadata(ArtifactMetadata metadata) {
                return artifact.withMetadata(metadata);
            }

            @Override
            public Artifact apply(ArtifactTransformer transformer) {
                return artifact.apply(transformer);
            }

            @Override
            public Artifact.Cached cache(ArtifactCache cache) {
                return artifact.cache(cache);
            }

            @Override
            public boolean isPresent() {
                return artifact.isPresent();
            }

            @Override
            public InputStream openStream() throws IOException, MissingArtifactException {
                return artifact.openStream();
            }

            @Override
            public File asFile() throws IOException, MissingArtifactException {
                if(!file.exists()) {
                    artifact.openStream().close();
                }
                return file;
            }

            @Override
            public File getFileLocation() throws MissingArtifactException {
                return file;
            }
            @Override
            public String toString() {
                return "wrapped(" + artifact + ", " + file + ")";
            }
        };
    }

}
