/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.plugins.scala;

import org.gradle.api.Incubating;
import org.gradle.api.provider.Property;

/**
 * Common configuration for Scala based projects. This is added by the {@link ScalaBasePlugin}.
 *
 * @since 6.0
 */
@Incubating
public interface ScalaPluginExtension {
    /**
     * The version of the Zinc compiler to use for compiling Scala code.
     * <p>
     *     Default version is Zinc 1.3.0.
     * </p>
     *
     * Gradle supports Zinc 1.2.0 to 1.3.0.
     *
     * @return zinc compiler version
     * @since 6.0
     */
    Property<String> getZincVersion();
}
