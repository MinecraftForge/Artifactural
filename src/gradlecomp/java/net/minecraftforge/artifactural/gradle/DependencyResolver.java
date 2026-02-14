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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ClientModule;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencyArtifact;
import org.gradle.api.artifacts.FileCollectionDependency;
import org.gradle.api.artifacts.ModuleDependency;

import java.io.File;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class DependencyResolver {

    private final Project project;
    private final AtomicInteger counter = new AtomicInteger(0);
    private final Cache<String, CompletableFuture<Set<File>>> resolved = CacheBuilder.newBuilder().expireAfterWrite(30, TimeUnit.MINUTES).build();

    public DependencyResolver(Project project) {
        this.project = project;
    }

    /**
     * Resolves a dependency, downloading the file and its transitives
     * if not cached and returns the set of files.
     */
    public Set<File> resolveDependency(Dependency dependency) {
        if (dependency instanceof FileCollectionDependency) {
            return ((FileCollectionDependency) dependency).getFiles().getFiles();
        }
        String name = dependency.getGroup() + ":" + dependency.getName() + ":" + dependency.getVersion();
        if (dependency instanceof ModuleDependency) {
            Set<DependencyArtifact> artifacts = ((ModuleDependency) dependency).getArtifacts();
            if (!artifacts.isEmpty()) {
                DependencyArtifact artifact = artifacts.iterator().next();
                name += ":" + artifact.getClassifier() + "@" + artifact.getExtension();
            }
        }

        // If this dep is being resolved on another thread, let it do it
        CompletableFuture<Set<File>> future;
        boolean found = true;
        synchronized (resolved) {
            future = resolved.getIfPresent(name);
            if (future == null) {
                resolved.put(name, future = new CompletableFuture<>());
                found = false;
            }
        }

        if (found) {
            try {
                return future.get();
            } catch (InterruptedException | ExecutionException ex) {
                throw new RuntimeException(ex);
            }
        }

        // No other thread is resolving this dep and we've claimed it, so let's go!
        int currentID = counter.getAndIncrement();
        Configuration cfg = project.getConfigurations().maybeCreate("resolve_dep_" + currentID);
        cfg.getDependencies().add(dependency);
        Set<File> files = cfg.resolve();
        project.getConfigurations().remove(cfg);
        future.complete(files);
        return files;
    }

    /**
     * Resolves a dependency, downloading the file and its transitives
     * if not cached and returns the set of files.
     */
    public Set<File> resolveDependency(Object dependency) {
        Dependency dep = project.getDependencies().create(dependency);
        return resolveDependency(dep);
    }

    /**
     * Resolves a dependency, downloading the file and its transitives
     * if not cached and returns the set of files.
     */
    public Set<File> resolveDependency(Object dependency, boolean transitive) {
        Dependency dep = project.getDependencies().create(dependency);
        if (dep instanceof ClientModule) {
            dep = ((ClientModule) dep).copy().setTransitive(transitive);
        }
        return resolveDependency(dep);
    }

    /**
     * Resolves a single dependency without any of its transitives
     * if not cached and returns the file.
     */
    public File resolveSingleDependency(Object dependency) {
        return resolveDependency(dependency, false).iterator().next();
    }

}
