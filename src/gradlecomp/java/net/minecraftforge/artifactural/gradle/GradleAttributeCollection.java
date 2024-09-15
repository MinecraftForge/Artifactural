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

import net.minecraftforge.artifactural.base.artifact.SimpleArtifactAttribute;
import net.minecraftforge.artifactural.base.artifact.SimpleAttributeCollection;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.attributes.AttributeContainer;

public class GradleAttributeCollection extends SimpleAttributeCollection {
    public GradleAttributeCollection(final AttributeContainer container) {
        for (Attribute<?> attribute : container.keySet()) {
            values.put(new SimpleArtifactAttribute<>(attribute.getName(), attribute.getType()), container.getAttribute(attribute));
        }
    }
}
