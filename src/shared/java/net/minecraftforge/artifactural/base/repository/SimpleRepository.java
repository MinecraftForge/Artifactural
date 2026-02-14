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

package net.minecraftforge.artifactural.base.repository;

import net.minecraftforge.artifactural.api.artifact.Artifact;
import net.minecraftforge.artifactural.api.artifact.ArtifactIdentifier;
import net.minecraftforge.artifactural.api.repository.ArtifactProvider;
import net.minecraftforge.artifactural.api.repository.Repository;

public class SimpleRepository implements Repository {

    public static Repository of(ArtifactProvider<ArtifactIdentifier> provider) {
        return new SimpleRepository(provider);
    }

    private final ArtifactProvider<ArtifactIdentifier> provider;

    private SimpleRepository(ArtifactProvider<ArtifactIdentifier> provider) {
        this.provider = provider;
    }

    @Override
    public Artifact getArtifact(ArtifactIdentifier identifier) {
        return provider.getArtifact(identifier);
    }

}
