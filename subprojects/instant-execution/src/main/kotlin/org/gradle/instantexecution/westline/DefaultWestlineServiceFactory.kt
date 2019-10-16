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

package org.gradle.instantexecution.westline

import com.google.common.reflect.TypeToken
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.westline.WestlineService
import org.gradle.api.westline.WestlineServiceFactory
import org.gradle.api.westline.WestlineServiceParameters
import org.gradle.api.westline.WestlineServiceSpec
import org.gradle.internal.Cast
import org.gradle.internal.instantiation.InstantiatorFactory
import org.gradle.internal.reflect.Instantiator
import org.gradle.internal.service.DefaultServiceRegistry
import org.gradle.model.internal.type.ModelType
import org.gradle.workers.WorkParameters.None
import java.lang.reflect.ParameterizedType


class DefaultWestlineServiceFactory(
    private val objects: ObjectFactory,
    private val providers: ProviderFactory,
    private val instantiatorFactory: InstantiatorFactory
) : WestlineServiceFactory {

    override fun <T : WestlineService<P>, P : WestlineServiceParameters> createProviderOf(
        serviceType: Class<T>,
        configuration: Action<in WestlineServiceSpec<P>>
    ): Provider<T> {

        val parameterType = extractParametersType(serviceType)
        val parameters = objects.newInstance(parameterType)
        configuration.execute(DefaultWestlineServiceSpec(parameters))

        val serviceRegistry = DefaultServiceRegistry().apply {
            add(parameterType, parameters)
        }
        val instantiator = instantiatorFactory.inject(serviceRegistry)

        return providers.provider {
            instantiator.newInstance(serviceType)
        }
    }

    private
    fun <T : WestlineService<P>, P : WestlineServiceParameters> extractParametersType(implementationClass: Class<T>): Class<P> {
        val superType = TypeToken.of(implementationClass).getSupertype(WestlineService::class.java).type as ParameterizedType
        val parameterType: Class<P> = Cast.uncheckedNonnullCast(TypeToken.of(superType.actualTypeArguments[0]).rawType)
        if (parameterType == WestlineServiceParameters::class.java) {
            throw IllegalArgumentException(String.format("Could not create service parameters: must use a sub-type of %s as parameter type. Use %s for executions without parameters.", ModelType.of(WestlineServiceParameters::class.java).displayName, ModelType.of(None::class.java).displayName))
        }
        return parameterType
    }
}


private
class DefaultWestlineServiceSpec<P : WestlineServiceParameters>(
    private val params: P
) : WestlineServiceSpec<P> {

    override fun getParameters(): P = params

    override fun parameters(action: Action<in P>) {
        action.execute(params)
    }
}
