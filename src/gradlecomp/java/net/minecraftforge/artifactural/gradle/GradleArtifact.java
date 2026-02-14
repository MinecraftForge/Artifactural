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

package net.minecraftforge.artifactural.gradle;

import net.minecraftforge.artifactural.api.artifact.Artifact;
import net.minecraftforge.artifactural.api.artifact.ArtifactIdentifier;
import net.minecraftforge.artifactural.api.artifact.ArtifactType;
import net.minecraftforge.artifactural.base.artifact.StreamableArtifact;

import java.io.File;
import java.util.Set;

public class GradleArtifact {

    public static Artifact maven(DependencyResolver resolver, ArtifactIdentifier identifier, ArtifactType type) {
        Set<File> files = resolver.resolveDependency(
                identifier.getGroup()
                        + ":" + identifier.getName()
                        + ":" + identifier.getVersion()
                        + (identifier.getClassifier().isEmpty() ? "" : ":" + identifier.getClassifier())
                        + (identifier.getExtension().isEmpty() ? "" : "@" + identifier.getExtension()),
                false
        );
        if (files.isEmpty()) return Artifact.none();
        return StreamableArtifact.ofFile(identifier, type, files.iterator().next());
    }

}
