/**
 * Copyright 2013 John Ericksen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.androidtransfuse;

import org.androidtransfuse.scope.Scopes;
import org.androidtransfuse.util.GeneratedRepositoryProxy;
import org.androidtransfuse.util.TransfuseRuntimeException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Static utility class which maps the {@code @Injector} annotated interface to the generated implementation.
 *
 * @author John Ericksen
 */
public final class Injectors {

    public static final String INJECTORS_NAME = "Injectors";
    public static final String INJECTORS_REPOSITORY_NAME = "Transfuse$Injectors";
    public static final String INJECTORS_PACKAGE = "org.androidtransfuse";

    private static final GeneratedRepositoryProxy<InjectorRepository> PROXY =
            new GeneratedRepositoryProxy<InjectorRepository>(INJECTORS_PACKAGE, INJECTORS_REPOSITORY_NAME);

    private Injectors(){
        // private utility class constructor
    }

    /**
     * Returns an instance of the provided {@code @Injector} interface.
     *
     * @throws org.androidtransfuse.util.TransfuseRuntimeException if there was an error looking up the wrapped
     * Transfuse$Injectors class.
     * @param type Injector type
     * @param <T>
     * @return Generated Injector instance
     */
    public static<T> T get(Class<T> type) {
        return PROXY.get().get(type);
    }

    public static<T> T get(Class<T> type, Scopes scopes){
        Class injector = PROXY.get().getType(type);

        try {
            //todo: clean this up
            if(injector == null){
                //try to find class
                injector = Class.forName(type.getName() + "Impl");
            }

            Constructor constructor = injector.getConstructor(Scopes.class);

            return (T) constructor.newInstance(scopes);

        } catch (NoSuchMethodException e) {
            throw new TransfuseRuntimeException("testing", e);
        } catch (InvocationTargetException e) {
            throw new TransfuseRuntimeException("testing", e);
        } catch (InstantiationException e) {
            throw new TransfuseRuntimeException("testing", e);
        } catch (IllegalAccessException e) {
            throw new TransfuseRuntimeException("testing", e);
        } catch (ClassNotFoundException e) {
            throw new TransfuseRuntimeException("testing", e);
        }
    }

    /**
     * Proxy Interface to be implemented by code generation.
     */
    public static interface InjectorRepository {

        /**
         * Returns an instance of the provided {@code @Injector} interface.
         *
         * @param type Injector type
         * @param <T>
         * @return Generated Injector instance
         */
        <T> T get(Class<T> type);

        <T> Class<? extends T> getType(Class<T> type);
    }
}
