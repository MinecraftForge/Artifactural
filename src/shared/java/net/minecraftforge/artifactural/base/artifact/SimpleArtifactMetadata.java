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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import net.minecraftforge.artifactural.api.artifact.ArtifactMetadata;

public class SimpleArtifactMetadata implements ArtifactMetadata {

    private final List<Entry> entries = new LinkedList<>();
    private String hash = null;

    public SimpleArtifactMetadata() {
    }

    private SimpleArtifactMetadata(SimpleArtifactMetadata parent, Entry entry) {
        this.entries.addAll(parent.entries);
        this.entries.add(entry);
    }

    @Override
    public ArtifactMetadata with(String key, String value) {
        return new SimpleArtifactMetadata(this, new Entry(key, value));
    }

    @Override
    public String getHash() {
        if (hash != null) return hash;
        try {
            String str = entries.stream().map(Entry::toString).collect(Collectors.joining("\n"));
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] hashBytes = digest.digest(str.getBytes());
            StringBuilder hashBuilder = new StringBuilder();
            for (byte b : hashBytes) {
                hashBuilder.append(String.format("%02x", b));
            }
            return hash = hashBuilder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public String toString() {
        return "SimpleArtifactMetadata(" + entries.toString() + ", " + getHash() + ")";
    }

    private static class Entry {

        private final String key, value;

        private Entry(String key, String value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public String toString() {
            return '[' + key + ',' + value + ']';
        }

    }

}
