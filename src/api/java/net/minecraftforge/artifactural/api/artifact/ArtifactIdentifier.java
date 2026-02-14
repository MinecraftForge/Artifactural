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

import java.util.function.Predicate;

public interface ArtifactIdentifier {

    static ArtifactIdentifier none() {
        return Internal.NO_IDENTIFIER;
    }

    String getGroup();

    String getName();

    String getVersion();

    String getClassifier();

    String getExtension();

    ArtifactAttributeCollection getAttributes();

    static Predicate<ArtifactIdentifier> groupMatches(String group) {
        return identifier -> identifier.getGroup().matches(group);
    }

    static Predicate<ArtifactIdentifier> nameMatches(String name) {
        return identifier -> identifier.getName().matches(name);
    }

    static Predicate<ArtifactIdentifier> versionMatches(String version) {
        return identifier -> identifier.getVersion().matches(version);
    }

    static Predicate<ArtifactIdentifier> classifierMatches(String classifier) {
        return identifier -> identifier.getClassifier().matches(classifier);
    }

    static Predicate<ArtifactIdentifier> extensionMatches(String extension) {
        return identifier -> identifier.getExtension().matches(extension);
    }

    static Predicate<ArtifactIdentifier> groupEquals(String group) {
        return identifier -> identifier.getGroup().equals(group);
    }

    static Predicate<ArtifactIdentifier> nameEquals(String name) {
        return identifier -> identifier.getName().equals(name);
    }

    static Predicate<ArtifactIdentifier> versionEquals(String version) {
        return identifier -> identifier.getVersion().equals(version);
    }

    static Predicate<ArtifactIdentifier> classifierEquals(String classifier) {
        return identifier -> identifier.getClassifier().equals(classifier);
    }

    static Predicate<ArtifactIdentifier> extensionEquals(String extension) {
        return identifier -> identifier.getExtension().equals(extension);
    }

}
