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

package net.minecraftforge.artifactural.gradle;

import net.minecraftforge.artifactural.api.artifact.Artifact;
import net.minecraftforge.artifactural.api.artifact.ArtifactIdentifier;
import net.minecraftforge.artifactural.api.artifact.MissingArtifactException;
import net.minecraftforge.artifactural.api.repository.Repository;
import net.minecraftforge.artifactural.base.artifact.SimpleArtifactIdentifier;
import net.minecraftforge.artifactural.base.cache.LocatedArtifactCache;

import org.gradle.api.artifacts.ComponentMetadataSupplierDetails;
import org.gradle.api.artifacts.component.ComponentArtifactIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.dsl.RepositoryHandler;
import org.gradle.api.internal.artifacts.BaseRepositoryFactory;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.ComponentResolvers;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.ConfiguredModuleComponentRepository;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.ModuleComponentRepositoryAccess;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.strategy.VersionParser;
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
import org.gradle.internal.component.model.ModuleSources;
import org.gradle.internal.nativeintegration.filesystem.FileSystem;
import org.gradle.internal.nativeintegration.services.FileSystems;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.internal.resolve.result.BuildableArtifactFileResolveResult;
import org.gradle.internal.resolve.result.BuildableArtifactSetResolveResult;
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
        maven.metadataSources(m -> {
            m.mavenPom();
            m.artifact();
        });

        final GradleRepositoryAdapter repo;

        if (GradleVersion.current().compareTo(GradleVersion.version("7.6")) >= 0) {
            // If we are on gradle 7.6+ we want to use the super constructor with 2 arguments (with the VersionParser)
            repo = new GradleRepositoryAdapter(repository, maven, getVersionParser(maven));
        } else {
            // If we are on gradle 4.10 - 7.5 we use the super constructor with only the ObjectFactory parameter
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
    // This constructor is used on Gradle 7.5.* and below
    @Deprecated // TODO - remove this constructor when we can break ABI compatibility
    private GradleRepositoryAdapter(Repository repository, DefaultMavenLocalArtifactRepository local) {
        // This is replaced with a call to 'super(getObjectFactory(local))'
        super(getObjectFactory(local), null);
        this.repository = repository;
        this.local = local;
        this.root = cleanRoot(local.getUrl());
        this.cache = new LocatedArtifactCache(new File(root));
    }

    // This constructor is used on Gradle 7.6 and above
    private GradleRepositoryAdapter(Repository repository, DefaultMavenLocalArtifactRepository local, VersionParser versionParser) {
        super(getObjectFactory(local), versionParser);
        this.repository = repository;
        this.local = local;
        this.root = cleanRoot(local.getUrl());
        this.cache = new LocatedArtifactCache(new File(root));
    }

    private static ObjectFactory getObjectFactory(DefaultMavenLocalArtifactRepository maven) {
        return ReflectionUtils.get(maven, "objectFactory");
    }

    private static VersionParser getVersionParser(DefaultMavenLocalArtifactRepository maven) {
        return ReflectionUtils.get(maven, "versionParser");
    }

    @Override
    public String getDisplayName() {
        return local.getDisplayName();
    }

    @Override
    public ConfiguredModuleComponentRepository createResolver() {
        MavenResolver resolver = (MavenResolver)local.createResolver();

        GeneratingFileResourceRepository  repo = new GeneratingFileResourceRepository();
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

            @Override
            public void setComponentResolvers(ComponentResolvers resolver) { }
            @Override
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
                    public void resolveArtifactsWithType(ComponentResolveMetadata component, ArtifactType artifactType, BuildableArtifactSetResolveResult result) {
                        delegate.resolveArtifactsWithType(component, artifactType, result);
                    }

                    @Override
                    public void resolveArtifact(ComponentArtifactMetadata componentArtifactMetadata, ModuleSources moduleSources, BuildableArtifactFileResolveResult buildableArtifactFileResolveResult) {
                        delegate.resolveArtifact(componentArtifactMetadata, moduleSources, buildableArtifactFileResolveResult);
                    }

                    @Override
                    public MetadataFetchingCost estimateMetadataFetchingCost(ModuleComponentIdentifier moduleComponentIdentifier) {
                        return delegate.estimateMetadataFetchingCost(moduleComponentIdentifier);
                    }
                };
            }
        };
    }

    public RepositoryDescriptor getDescriptor() {
        return new FlatDirRepositoryDescriptor("ArtifacturalRepository", new ArrayList<>());
    }


    private static String cleanRoot(URI uri) {
        String ret = uri.normalize().getPath().replace('\\', '/');
        if (!ret.endsWith("/")) ret += '/';
        return ret;
    }

    private class GeneratingFileResourceRepository implements FileResourceRepository {
        private final FileSystem fileSystem = FileSystems.getDefault();
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
