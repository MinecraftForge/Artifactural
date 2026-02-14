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

import net.minecraftforge.artifactural.api.artifact.ArtifactAttributeCollection;
import net.minecraftforge.artifactural.api.artifact.ArtifactIdentifier;

public class SimpleArtifactIdentifier implements ArtifactIdentifier {

    private final String group, name, version, classifier, extension;
    private final ArtifactAttributeCollection attributes;

    public SimpleArtifactIdentifier(String group,
                                    String name,
                                    String version,
                                    String classifier,
                                    String extension,
                                    ArtifactAttributeCollection attributes) {
        this.group = group;
        this.name = name;
        this.version = version;
        this.classifier = classifier;
        this.extension = extension;
        this.attributes = attributes;
    }

    @Override
    public String getGroup() {
        return group;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getClassifier() {
        return classifier;
    }

    @Override
    public String getExtension() {
        return extension;
    }

    @Override
    public ArtifactAttributeCollection getAttributes() {
        return attributes;
    }

    @Override
    public String toString() {
        String ret = getGroup() + ':' + getName() + ':' + getVersion();
        if (classifier != null)
            ret += ':' + getClassifier();
        if ("jar".equals(extension))
            ret += '@' + getExtension();
        return ret;
    }
}
