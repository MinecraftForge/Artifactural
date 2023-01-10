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

import net.minecraftforge.artifactural.api.artifact.ArtifactIdentifier;
import org.gradle.api.Action;
import org.gradle.api.artifacts.ModuleIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.artifacts.repositories.ArtifactRepository;
import org.gradle.api.attributes.AttributeContainer;
import org.gradle.api.internal.artifacts.repositories.ArtifactResolutionDetails;
import org.gradle.api.internal.artifacts.repositories.ContentFilteringRepository;
import org.gradle.api.internal.attributes.ImmutableAttributes;

public class RepositoryContentUtils {

    /**
     * Checks if a repository is filtered to exclude a given artifact.
     *
     * @param repository Repository that may have filtering.
     * @param artifact   Artifact to test.
     *
     * @return {@code true} if the repository has a content filter that excludes the given artifact.
     */
    public static boolean contentFilterExcludes(ArtifactRepository repository, ArtifactIdentifier artifact) {
        if (repository instanceof ContentFilteringRepository) {
            //Check if the repo is configured in such a way as to support this artifact or not
            Action<? super ArtifactResolutionDetails> contentFilter = ((ContentFilteringRepository) repository).getContentFilter();
            ContentResolutionTracker details = new ContentResolutionTracker(artifact);
            contentFilter.execute(details);
            //Return whether the resolution details match the content filter
            return details.wontBeFound;
        }
        //If it can't be filtered then it can't be excluded
        return false;
    }

    private static class ContentResolutionTracker implements ArtifactResolutionDetails {

        private final ModuleIdentifier moduleIdentifier;
        private final ModuleComponentIdentifier moduleComponentIdentifier;
        private boolean wontBeFound;

        ContentResolutionTracker(ArtifactIdentifier artifact) {
            this.moduleIdentifier = new ModuleIdentifier() {
                @Override
                public String getGroup() {
                    return artifact.getGroup();
                }

                @Override
                public String getName() {
                    return artifact.getName();
                }
            };
            this.moduleComponentIdentifier = new ModuleComponentIdentifier() {
                @Override
                public String getGroup() {
                    return getModuleIdentifier().getGroup();
                }

                @Override
                public String getModule() {
                    return getModuleIdentifier().getName();
                }

                @Override
                public String getVersion() {
                    return artifact.getVersion();
                }

                @Override
                public ModuleIdentifier getModuleIdentifier() {
                    return getModuleId();
                }

                @Override
                public String getDisplayName() {
                    return artifact.toString();
                }
            };
        }

        @Override
        public ModuleIdentifier getModuleId() {
            return moduleIdentifier;
        }

        @Override
        public ModuleComponentIdentifier getComponentId() {
            return moduleComponentIdentifier;
        }

        @Override
        public AttributeContainer getConsumerAttributes() {
            return ImmutableAttributes.EMPTY;
        }

        @Override
        public String getConsumerName() {
            return "artifacturalContentConsumer";
        }

        @Override
        public boolean isVersionListing() {
            return moduleComponentIdentifier == null;
        }

        @Override
        public void notFound() {
            wontBeFound = true;
        }
    }
}