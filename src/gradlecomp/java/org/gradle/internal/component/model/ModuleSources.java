package org.gradle.internal.component.model;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

public interface ModuleSources {

    <T extends ModuleSource> Optional<T> getSource(Class<T> sourceType);

    void withSources(Consumer<ModuleSource> consumer);

    default <T extends ModuleSource, R> R withSource(Class<T> sourceType, Function<Optional<T>, R> action) {
        return action.apply(getSource(sourceType));
    }

    int size();
}