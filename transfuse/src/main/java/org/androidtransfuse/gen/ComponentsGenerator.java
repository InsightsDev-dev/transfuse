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
package org.androidtransfuse.gen;

import com.sun.codemodel.JClass;
import com.sun.codemodel.JClassAlreadyExistsException;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpression;
import org.androidtransfuse.Components;
import org.androidtransfuse.adapter.PackageClass;
import org.androidtransfuse.util.Repository;

import javax.inject.Inject;

/**
 * @author John Ericksen
 */
public class ComponentsGenerator extends AbstractRepositoryGenerator {

    private static final PackageClass REPOSITORY_NAME = new PackageClass(Components.COMPONENTS_PACKAGE, Components.COMPONENTS_REPOSITORY_NAME);

    @Inject
    public ComponentsGenerator(ClassGenerationUtil generationUtil, UniqueVariableNamer namer) {
        super(Repository.class, generationUtil, namer, REPOSITORY_NAME, Class.class);
    }

    @Override
    protected JExpression generateInstance(JDefinedClass factoryRepositoryClass, JClass interfaceClass, JClass concreteType) throws JClassAlreadyExistsException {
        return concreteType.dotclass();
    }


}
