/*
 * Artifactural
 * Copyright (c) 2018.
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

package com.amadornes.artifactural.gradle;

import com.amadornes.artifactural.api.artifact.Artifact;
import com.amadornes.artifactural.api.artifact.ArtifactIdentifier;
import com.amadornes.artifactural.api.artifact.MissingArtifactException;
import com.amadornes.artifactural.api.repository.Repository;
import com.amadornes.artifactural.base.artifact.SimpleArtifactIdentifier;
import com.amadornes.artifactural.base.cache.LocatedArtifactCache;

import org.gradle.api.artifacts.ComponentMetadataSupplierDetails;
import org.gradle.api.artifacts.component.ComponentArtifactIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.internal.artifacts.BaseRepositoryFactory;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.ComponentResolvers;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.ConfiguredModuleComponentRepository;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.ModuleComponentRepositoryAccess;
import org.gradle.api.internal.artifacts.ivyservice.resolveengine.artifact.ResolvableArtifact;
import org.gradle.api.internal.artifacts.repositories.AbstractArtifactRepository;
import org.gradle.api.internal.artifacts.repositories.DefaultMavenLocalArtifactRepository;
import org.gradle.api.internal.artifacts.repositories.ResolutionAwareRepository;
import org.gradle.api.internal.artifacts.repositories.descriptor.FlatDirRepositoryDescriptor;
import org.gradle.api.internal.artifacts.repositories.descriptor.RepositoryDescriptor;
import org.gradle.api.internal.artifacts.repositories.resolver.ExternalResourceArtifactResolver;
import org.gradle.api.internal.artifacts.repositories.resolver.ExternalResourceResolver;
import org.gradle.api.internal.artifacts.repositories.resolver.MavenResolver;
import org.gradle.api.internal.artifacts.repositories.resolver.MetadataFetchingCost;
import org.gradle.api.internal.component.ArtifactType;
import org.gradle.api.model.ObjectFactory;
import org.gradle.internal.action.InstantiatingAction;
import org.gradle.internal.component.external.model.ModuleComponentResolveMetadata;
import org.gradle.internal.component.external.model.ModuleDependencyMetadata;
import org.gradle.internal.component.external.model.MutableModuleComponentResolveMetadata;
import org.gradle.internal.component.model.ComponentArtifactMetadata;
import org.gradle.internal.component.model.ComponentOverrideMetadata;
import org.gradle.internal.component.model.ComponentResolveMetadata;
import org.gradle.internal.component.model.ConfigurationMetadata;
import org.gradle.internal.component.model.ModuleSource;
import org.gradle.internal.component.model.ModuleSources;
import org.gradle.internal.nativeintegration.filesystem.FileSystem;
import org.gradle.internal.nativeintegration.services.FileSystems;
import org.gradle.internal.reflect.DirectInstantiator;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.internal.resolve.result.BuildableArtifactResolveResult;
import org.gradle.internal.resolve.result.BuildableArtifactSetResolveResult;
import org.gradle.internal.resolve.result.BuildableComponentArtifactsResolveResult;
import org.gradle.internal.resolve.result.BuildableModuleComponentMetaDataResolveResult;
import org.gradle.internal.resolve.result.BuildableModuleVersionListingResolveResult;
import org.gradle.internal.resource.ExternalResourceName;
import org.gradle.internal.resource.ExternalResourceRepository;
import org.gradle.internal.resource.LocalBinaryResource;
import org.gradle.internal.resource.local.FileResourceRepository;
import org.gradle.internal.resource.local.LocalFileStandInExternalResource;
import org.gradle.internal.resource.local.LocallyAvailableExternalResource;
import org.gradle.internal.resource.metadata.ExternalResourceMetaData;
import org.gradle.internal.resource.transfer.DefaultCacheAwareExternalResourceAccessor;
import org.gradle.util.GradleVersion;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GradleRepositoryAdapter extends AbstractArtifactRepository implements ResolutionAwareRepository {

    private static final Pattern URL_PATTERN = Pattern.compile(
            "^(?<group>\\S+(?:/\\S+)*)/(?<name>\\S+)/(?<version>\\S+)/" +
                    "\\2-\\3(?:-(?<classifier>[^.\\s]+))?\\.(?<extension>\\S+)$");

    public static GradleRepositoryAdapter add(RepositoryHandler handler, String name, File local, Repository repository) {
        BaseRepositoryFactory factory = ReflectionUtils.get(handler, "repositoryFactory"); // We reflect here and create it manually so it DOESN'T get attached.
        DefaultMavenLocalArtifactRepository maven = (DefaultMavenLocalArtifactRepository)factory.createMavenLocalRepository(); // We use maven local because it bypasses the caching and coping to .m2
        maven.setUrl(local);
        maven.setName(name);

        GradleRepositoryAdapter repo;

        // On Gradle 4.10 above, we need to use the constructor with the 'ObjectFactory' parameter
        // (which can be safely passed as null - see BaseMavenInstaller).
        // We use Gradle410RepositoryAdapter, which actually overrides 'getDescriptor'
        if (GradleVersion.current().compareTo(GradleVersion.version("4.10")) >= 0) {
             repo = new Gradle410RepositoryAdapter(null, repository, maven);
        } else {
            // On versions of gradle older than 4.10, we use the no-arg super constructor
            repo = new GradleRepositoryAdapter(repository, maven);
        }

        repo.setName(name);
        handler.add(repo);
        return repo;
    }

    private final Repository repository;
    private final DefaultMavenLocalArtifactRepository local;
    private final String root;
    private final LocatedArtifactCache cache;


    // This constructor is modified via bytecode manipulation in 'build.gradle'
    // DO NOT change this without modifying 'build.gradle'
    // This contructor is used on Gradle 4.9 and below
    private GradleRepositoryAdapter(Repository repository, DefaultMavenLocalArtifactRepository local) {
        // This is replaced with a call to 'super()', with no arguments
        super(null);
        this.repository = repository;
        this.local = local;
        this.root = cleanRoot(local.getUrl());
        this.cache = new LocatedArtifactCache(new File(root));
    }


    // This constructor is used on Gradle 4.10 and above
    GradleRepositoryAdapter(ObjectFactory objectFactory, Repository repository, DefaultMavenLocalArtifactRepository local) {
        super(objectFactory);
        // This duplication from the above two-argument constructor is unfortunate,
        // but unavoidable
        this.repository = repository;
        this.local = local;
        this.root = cleanRoot(local.getUrl());
        this.cache = new LocatedArtifactCache(new File(root));
    }

    @Override
    public String getDisplayName() {
        return local.getDisplayName();
    }

    @Override
    public ConfiguredModuleComponentRepository createResolver() {
        MavenResolver resolver = (MavenResolver)local.createResolver();

        GeneratingFileResourceRepository repo = new GeneratingFileResourceRepository();
        ReflectionUtils.alter(resolver, "repository", prev -> repo);  // ExternalResourceResolver.repository
        //ReflectionUtils.alter(resolver, "metadataSources", ); //ExternalResourceResolver.metadataSources We need to fix these from returning 'missing'
        // MavenResolver -> MavenMetadataLoader -> FileCacheAwareExternalResourceAccessor -> DefaultCacheAwareExternalResourceAccessor
        DefaultCacheAwareExternalResourceAccessor accessor = ReflectionUtils.get(resolver, "mavenMetaDataLoader.cacheAwareExternalResourceAccessor.delegate");
        ReflectionUtils.alter(accessor, "delegate", prev -> repo); // DefaultCacheAwareExternalResourceAccessor.delegate
        ReflectionUtils.alter(accessor, "fileResourceRepository", prev -> repo); // DefaultCacheAwareExternalResourceAccessor.fileResourceRepository
        ExternalResourceArtifactResolver extResolver = ReflectionUtils.invoke(resolver, ExternalResourceResolver.class, "createArtifactResolver"); //Makes the resolver and caches it.
        ReflectionUtils.alter(extResolver, "repository", prev -> repo);
        //File transport references, Would be better to get a reference to the transport and work from there, but don't see it stored anywhere.
        ReflectionUtils.alter(resolver, "cachingResourceAccessor.this$0.repository", prev -> repo);
        ReflectionUtils.alter(resolver, "cachingResourceAccessor.delegate.delegate", prev -> repo);

        return new ConfiguredModuleComponentRepository() {
            private final ModuleComponentRepositoryAccess local = wrap(resolver.getLocalAccess());
            private final ModuleComponentRepositoryAccess remote = wrap(resolver.getRemoteAccess());
            @Override public String getId() { return resolver.getId(); }
            @Override public String getName() { return resolver.getName(); }
            @Override public ModuleComponentRepositoryAccess getLocalAccess() { return local; }
            @Override public ModuleComponentRepositoryAccess getRemoteAccess() { return remote; }
            @Override public Map<ComponentArtifactIdentifier, ResolvableArtifact> getArtifactCache() { return resolver.getArtifactCache(); }
            @Override public InstantiatingAction<ComponentMetadataSupplierDetails> getComponentMetadataSupplier() { return resolver.getComponentMetadataSupplier(); }
            @Override public boolean isDynamicResolveMode() { return resolver.isDynamicResolveMode(); }
            @Override public boolean isLocal() { return resolver.isLocal(); }
            // Bytecode to implement added changes in Gradle 6.6.0 that expressly adds two methods
            // to the interface without a default implementation because #internals
            public void setComponentResolvers(ComponentResolvers resolver) { }
            public Instantiator getComponentMetadataInstantiator() {
                return resolver.getComponentMetadataInstantiator();
            }

            private ModuleComponentRepositoryAccess wrap(ModuleComponentRepositoryAccess delegate) {
                return new ModuleComponentRepositoryAccess() {
                    @Override
                    public void resolveComponentMetaData(ModuleComponentIdentifier moduleComponentIdentifier, ComponentOverrideMetadata requestMetaData, BuildableModuleComponentMetaDataResolveResult result) {
                        delegate.resolveComponentMetaData(moduleComponentIdentifier, requestMetaData, result);
                        if (result.getState() == BuildableModuleComponentMetaDataResolveResult.State.Resolved) {
                            ModuleComponentResolveMetadata meta = result.getMetaData();
                            if (meta.isMissing()) {
                                MutableModuleComponentResolveMetadata mutable = meta.asMutable();
                                mutable.setChanging(true);
                                mutable.setMissing(false);
                                result.resolved(mutable.asImmutable());
                            }
                        }
                    }

                    @Override
                    public void listModuleVersions(ModuleDependencyMetadata dependency, BuildableModuleVersionListingResolveResult result) {
                        delegate.listModuleVersions(dependency, result);
                    }

                    @Override
                    public void resolveArtifacts(ComponentResolveMetadata component, BuildableComponentArtifactsResolveResult result) {
                        delegate.resolveArtifacts(component, result);
                    }

                    @Override
                    public void resolveArtifactsWithType(ComponentResolveMetadata component, ArtifactType artifactType, BuildableArtifactSetResolveResult result) {
                        delegate.resolveArtifactsWithType(component, artifactType, result);
                    }

                    @Override
                    public void resolveArtifact(ComponentArtifactMetadata artifact, ModuleSource moduleSource, BuildableArtifactResolveResult result) {
                        delegate.resolveArtifact(artifact, moduleSource, result);
                    }

                    // Method shims for Gradle 6.x
                    public void resolveArtifacts(ComponentResolveMetadata component, ConfigurationMetadata variant, BuildableComponentArtifactsResolveResult result) {
                        try {
                            final Method delegateResolveArtifacts = delegate.getClass().getMethod(
                                "resolveArtifacts", ComponentResolveMetadata.class, ConfigurationMetadata.class,
                                BuildableComponentArtifactsResolveResult.class
                            );
                            delegateResolveArtifacts.invoke(delegate, component, variant, result);
                        } catch (Error | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                            e.printStackTrace();
                        }
                    }

                    public void resolveArtifact(ComponentArtifactMetadata artifact, ModuleSources moduleSource, BuildableArtifactResolveResult result) {
                        try {
                            final Method delegatedResolveArtifact = delegate.getClass().getMethod("resolveArtifact", ComponentArtifactMetadata.class, ModuleSources.class, BuildableArtifactResolveResult.class);
                            delegatedResolveArtifact.invoke(delegate, artifact, moduleSource, result);
                        } catch (Error | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                            e.printStackTrace();
                        }
                    }
                    // End Method Shims for Gradle 6.x

                    @Override
                    public MetadataFetchingCost estimateMetadataFetchingCost(ModuleComponentIdentifier moduleComponentIdentifier) {
                        return delegate.estimateMetadataFetchingCost(moduleComponentIdentifier);
                    }
                };
            }
        };
    }

    // This method will be deleted entirely in build.gradle
    // In order for this class to compile, this method needs to exist
    // at compile-time. However, the class 'RepositoryDescriptor' doesn't
    // exist in Gradle 4.9. If we try to classload a class
    // that contains RepositoryDescriptor as a method return type,
    // the JVM will try to classload RepositoryDescriptor, leading
    // to a NoClassDefFoundError

    // To fix this, we strip out this method at build time.
    // At runtime, we instantiate Gradle410RepositoryAdapter
    // when we're running on Gradle 4.10 on above.
    // This ensures that 'getDescriptor' exists on Gradle 4.10,
    // and doesn't existon Gradle 4.9
    public RepositoryDescriptor getDescriptor() {
        throw new Error("This method should be been stripped at build time!");
    }


    private static String cleanRoot(URI uri) {
        String ret = uri.normalize().getPath().replace('\\', '/');
        if (!ret.endsWith("/")) ret += '/';
        return ret;
    }

    private class GeneratingFileResourceRepository implements FileResourceRepository {
        private final FileSystem fileSystem;

        public GeneratingFileResourceRepository() {
            try {
                if (GradleVersion.current().compareTo(GradleVersion.version("6.0.0")) >= 0) {
                    final Class<?> fileSystemClass = Class.forName("org.gradle.internal.nativeintegration.services.FileSystems");
                    final Method getDefaultFileSystem = fileSystemClass.getMethod("getDefault");
                    this.fileSystem = (FileSystem) getDefaultFileSystem.invoke(null);
                } else {
                    this.fileSystem = FileSystems.getDefault();
                }
            } catch (IllegalAccessException | IllegalArgumentException | Error | InvocationTargetException | NoSuchMethodException | ClassNotFoundException e) {
                throw new UnsupportedOperationException("Could not get the Gradle FileSystem object", e);
            }

        }

        private void debug(String message) {
            //System.out.println(message);
        }
        private void log(String message) {
            System.out.println(message);
        }

        @Override
        public ExternalResourceRepository withProgressLogging() {
            return this;
        }

        @Override
        public LocalBinaryResource localResource(File file) {
            debug("localResource: " + file);
            return null;
        }

        @Override
        public LocallyAvailableExternalResource resource(File file) {
            debug("resource(File): " + file);
            return findArtifact(file.getAbsolutePath().replace('\\', '/'));
        }

        @Override
        public LocallyAvailableExternalResource resource(ExternalResourceName location) {
            return resource(location, false);
        }

        @Override
        public LocallyAvailableExternalResource resource(ExternalResourceName location, boolean revalidate) {
            debug("resource(ExternalResourceName,boolean): " + location + ", " + revalidate);
            return findArtifact(location.getUri().getPath().replace('\\', '/'));
        }

        @Override
        public LocallyAvailableExternalResource resource(File file, URI originUri, ExternalResourceMetaData originMetadata) {
            debug("resource(File,URI,ExternalResourceMetaData): " + file + ", " + originUri + ", " + originMetadata);
            return findArtifact(file.getAbsolutePath().replace('\\', '/'));
        }

        private LocallyAvailableExternalResource findArtifact(String path) {
            if (path.startsWith(root)) {
                String relative = path.substring(root.length());
                debug("  Relative: " + relative);
                Matcher matcher = URL_PATTERN.matcher(relative);
                if (matcher.matches()) {
                    ArtifactIdentifier identifier = new SimpleArtifactIdentifier(
                        matcher.group("group").replace('/', '.'),
                        matcher.group("name"),
                        matcher.group("version"),
                        matcher.group("classifier"),
                        matcher.group("extension"));
                    Artifact artifact = repository.getArtifact(identifier);
                    return wrap(artifact, identifier);
                } else if (relative.endsWith("maven-metadata.xml")) {
                    String tmp = relative.substring(0, relative.length() - "maven-metadata.xml".length() - 1);
                    int idx = tmp.lastIndexOf('/');
                    if (idx != -1) {
                        File ret = repository.getMavenMetadata(tmp.substring(0, idx - 1), tmp.substring(idx));
                        if (ret != null) {
                            return new LocalFileStandInExternalResource(ret, fileSystem);
                        }
                    }
                } else if (relative.endsWith("/")) {
                    debug("    Directory listing not supported");
                } else {
                    log("  Matcher Failed: " + relative);
                }
            } else {
                log("Unknown root: " + path);
            }
            return new LocalFileStandInExternalResource(new File(path), fileSystem);
        }

        private LocallyAvailableExternalResource wrap(Artifact artifact, ArtifactIdentifier id) {
            if (!artifact.isPresent())
                return new LocalFileStandInExternalResource(cache.getPath(artifact), fileSystem);
            Artifact.Cached cached = artifact.optionallyCache(cache);
            try {
                return new LocalFileStandInExternalResource(cached.asFile(), fileSystem);
            } catch (MissingArtifactException | IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    //TODO: Make this a artifact provider interface with a proper API so we dont have direct reference to GradleRepoAdapter in consumers.
    public File getArtifact(ArtifactIdentifier identifier) {
        Artifact art = repository.getArtifact(identifier);
        if (!art.isPresent())
            return null;

        Artifact.Cached cached = art.optionallyCache(cache);
        try {
            return cached.asFile();
        } catch (MissingArtifactException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
