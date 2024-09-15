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

import net.minecraftforge.artifactural.api.artifact.ArtifactAttribute;
import net.minecraftforge.artifactural.api.artifact.ArtifactAttributeCollection;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;

public class SimpleAttributeCollection implements ArtifactAttributeCollection {
    protected final HashMap<ArtifactAttribute<?>, Object> values = new HashMap<>();

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(ArtifactAttribute<T> key) {
        return (T) values.get(key);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T with(ArtifactAttribute<T> attribute, T value) {
        return (T) values.put(attribute, value);
    }

    @Override
    public Set<ArtifactAttribute<?>> keys() {
        return values.keySet();
    }

    @Override
    public Iterator<Entry<ArtifactAttribute<?>, Object>> iterator() {
        return values.entrySet().iterator();
    }
}
