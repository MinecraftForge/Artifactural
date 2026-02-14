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

package net.minecraftforge.artifactural.base.util;

import java.util.Map;

public class PatternReplace {
    /*
     * Replaces a patterened string, with support for optional groups.
     * Example:
     *   Values:
     *     group: net/minecraftforge
     *     name: forge
     *     version: 1.0
     *     ext: jar
     *
     *   Example: [group]/[name]/[version]/[name]-[version](-[classifier]).[ext]
     *     {classifier: test} net/minecraftforge/forge/1.0/forge-1.0-test.jar
     *     {classifier: null} net/minecraftforge/forge/1.0/forge-1.0.jar
     *
     *   Nested Optionals are supported:
     *     Example: [group]/[name]/[version]/[name]-[version](-[classifier](-[suffix])).[ext]
     *     {classifier: test, suffix: foo} net/minecraftforge/forge/1.0/forge-1.0-test-foo.jar
     *     {classifier: test, suffix: foo} net/minecraftforge/forge/1.0/forge-1.0-test.jar
     *     {classifier: null, suffix: foo} net/minecraftforge/forge/1.0/forge-1.0.jar
     *
     *   Compound optionals are supported:
     *     Example: [group]/[name]/[version]/[name]-[version](-[classifier]-[suffix]).[ext]
     *     {classifier: test, suffix: foo} net/minecraftforge/forge/1.0/forge-1.0-test-foo.jar
     *     {classifier: test, suffix: null} net/minecraftforge/forge/1.0/forge-1.0.jar
     *     {classifier: null, suffix: foo} net/minecraftforge/forge/1.0/forge-1.0.jar
     *
     *
     *   TODO: Support nested names?
     *     Example: [group]/[name]/[version]/[name]-[version](-[classifier[suffix]]).[ext]
     *     {classifierFoo: test, suffix: Foo} net/minecraftforge/forge/1.0/forge-1.0-test.jar
     *     {classifierFoo: null, suffix: Foo} net/minecraftforge/forge/1.0/forge-1.0.jar
     */
    public static String replace(String pattern, Map<String, String> values) {
        if (pattern == null) return null;
        if (pattern.isEmpty()) return "";

        Optional optional = null;
        StringBuffer name = null;
        StringBuffer ret = new StringBuffer();

        char[] chars = pattern.toCharArray();
        for (int x = 0; x < chars.length; x++) {
            char c = chars[x];
            if (c == '\\') {
                if (x == chars.length -1)
                    throw new IllegalArgumentException("Escape character can not be end of pattern: " + pattern);
                x++;
                ret.append(chars[x]);
                continue;
            }
            switch (c) {
                case '[':
                    if (name != null)
                        throw new IllegalArgumentException("Nested names are not supported @ " + x + " : " + pattern);
                    name = new StringBuffer();
                    break;
                case ']':
                    if (name == null)
                        throw new IllegalArgumentException("Name closing found without opening @ " + x + " : " + pattern);
                    String key = name.toString();
                    if (key.isEmpty())
                        throw new IllegalArgumentException("Name can not be empty @ " + x + ": " + pattern);
                    if (optional != null)
                        optional.setKey(key, values);
                    else
                        ret.append(values.get(key)); // appends 'null' if missing, if you want "" then use ([name])
                        // Should we have this default to not replacing at all if value is not set to allow chaining?
                        //   Meaning: '[key]' == '[key]' if 'key' is not set.
                        //   Current: '[key]' == 'null'
                    name = null;
                    break;
                case '(':
                    optional = new Optional(optional);
                    break;
                case ')':
                    if (optional == null)
                        throw new IllegalArgumentException("Optional closing found without opening @ " + x + ": " + pattern);
                    optional = optional.finish(x, pattern, ret);
                    break;
                default:
                    if (name != null)
                        name.append(c);
                    else if (optional != null)
                        optional.append(c);
                    else
                        ret.append(c);
            }
        }
        if (optional != null)
            throw new IllegalArgumentException("Missing closing of optional value: " + pattern);
        if (name != null)
            throw new IllegalArgumentException("Missing closing of name entry: " + pattern);
        return ret.toString();
    }

    public static String quote(String value) {
        return value.replaceAll("\\", "\\\\")
                .replaceAll("(", "\\(")
                .replaceAll(")", "\\)")
                .replaceAll("[", "\\[")
                .replaceAll("]", "\\]");
    }

    private static class Optional {
        private final Optional parent;
        private final StringBuffer buf = new StringBuffer();
        private boolean hadAll = true;
        private boolean hadValue = false;

        private Optional(Optional parent) {
            this.parent = parent;
        }

        public void append(char c) {
            buf.append(c);
        }

        private void setKey(String key, Map<String, String> values) {
            hadValue = true;
            String value = values.get(key);
            if (value != null && !value.isEmpty()) {
                hadAll &= true;
                buf.append(value);
            } else
                hadAll = false;
        }

        public Optional finish(int position, String pattern, StringBuffer ret) {
            if (!hadValue)
                throw new IllegalArgumentException("Invalid optional, missing inner name @ " + position  +": " + pattern);
            if (hadAll)
                (parent == null ? ret : parent.buf).append(buf);
            return parent;
        }
    }
}
