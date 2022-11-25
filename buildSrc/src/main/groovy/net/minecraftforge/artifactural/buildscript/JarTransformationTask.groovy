/*
 * Artifactural
 * Copyright (c) 2018-2021.
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

package net.minecraftforge.artifactural.buildscript

import groovy.transform.CompileStatic
import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.tree.ClassNode

import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@CompileStatic
abstract class JarTransformationTask extends DefaultTask {
    @InputFile
    abstract RegularFileProperty getFile()
    @Input
    abstract MapProperty<String, ClassTransformer> getTransformers()

    JarTransformationTask() {
        transformers.convention([:])
    }

    void addTransformer(String className, @ClosureParams(value = SimpleType, options = 'org.objectweb.asm.tree.ClassNode') Closure transformer) {
        transformers.put(className, transformer as ClassTransformer)
    }

    @TaskAction
    void run() {
        final bos = new ByteArrayOutputStream()
        final zipOut = new ZipOutputStream(bos)
        try (final zipIn = new ZipInputStream(file.get().asFile.newInputStream())) {
            ZipEntry entry
            while ((entry = zipIn.nextEntry) !== null) {
                if (entry.name.endsWith('.class')) {
                    final ClassTransformer transformer = transformers.get()[entry.name.dropRight(6)]
                    if (transformer !== null) {
                        final node = new ClassNode()
                        final reader = new ClassReader(zipIn)
                        reader.accept(node, 0)
                        transformer.transform(node)
                        final writer = new ClassWriter(ClassWriter.COMPUTE_MAXS)
                        node.accept(writer)

                        zipOut.putNextEntry(copy(entry))
                        zipOut.write(writer.toByteArray())
                        zipOut.closeEntry()
                        continue
                    }
                }

                zipOut.putNextEntry(copy(entry))
                copy(zipIn, zipOut)
                zipOut.closeEntry()
            }
        }
        zipOut.close()

        Files.write(file.asFile.get().toPath(), bos.toByteArray())
    }

    private static ZipEntry copy(ZipEntry entry) {
        return new ZipEntry(entry.name)
    }

    private static void copy(InputStream source, OutputStream target) throws IOException {
        byte[] buf = new byte[8192]
        int length
        while ((length = source.read(buf)) !== -1) {
            target.write(buf, 0, length)
        }
    }
}
