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
package org.androidtransfuse.bootstrap;

import com.google.common.collect.ImmutableSet;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import org.androidtransfuse.AnnotationProcessorBase;
import org.androidtransfuse.SupportedAnnotations;
import org.androidtransfuse.TransfuseAnalysisException;
import org.androidtransfuse.adapter.ASTType;
import org.androidtransfuse.adapter.element.ASTElementConverterFactory;
import org.androidtransfuse.analysis.AnalysisContext;
import org.androidtransfuse.analysis.InjectionPointFactory;
import org.androidtransfuse.analysis.module.ModuleProcessor;
import org.androidtransfuse.annotations.Factory;
import org.androidtransfuse.gen.FactoriesGenerator;
import org.androidtransfuse.gen.FactoryGenerator;
import org.androidtransfuse.model.InjectionNode;
import org.androidtransfuse.util.Providers;

import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.inject.Provider;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Collections2.transform;

/**
 * @author John Ericksen
 */
@SupportedAnnotations({Bootstrap.class, BootstrapModule.class, Namespace.class})
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class BootstrapProcessor extends AnnotationProcessorBase {

    private boolean ran = false;

    @Override
    public boolean process(Set<? extends TypeElement> typeElements, RoundEnvironment roundEnvironment) {

        if(!ran){

            try {
                Set<? extends Element> namespaceElements = roundEnvironment.getElementsAnnotatedWith(Namespace.class);

                String namespace = null;
                if(namespaceElements.size() > 0){
                    Element namespaceElement = namespaceElements.iterator().next();
                    namespace = namespaceElement.getAnnotation(Namespace.class).value();
                }

                CoreFactory coreFactory = new CoreFactory(processingEnv.getElementUtils(), processingEnv.getMessager(), processingEnv.getFiler(), namespace);

                InjectionPointFactory injectionPointFactory = coreFactory.buildInjectionPointFactory();

                Collection<? extends ASTType> moduleTypes =  wrapASTCollection(coreFactory, roundEnvironment.getElementsAnnotatedWith(BootstrapModule.class));

                //configure injections
                ModuleProcessor moduleProcessor = coreFactory.buildModuleProcessor();

                for (ASTType moduleType : moduleTypes) {
                    moduleProcessor.process(moduleType);
                }

                //scopes utility
                coreFactory.buildScopesGenerator().generate();

                ImmutableSet.Builder<ASTType> factoryTypesBuilder = ImmutableSet.builder();
                factoryTypesBuilder.addAll(wrapASTCollection(coreFactory, roundEnvironment.getElementsAnnotatedWith(Factory.class)));
                factoryTypesBuilder.addAll(coreFactory.getModuleRepository().getInstalledAnnotatedWith(Factory.class));

                ImmutableSet<ASTType> factoryTypes = factoryTypesBuilder.build();

                coreFactory.registerFactories(factoryTypes);

                FactoryGenerator factoryGenerator = coreFactory.buildFactoryGenerator();
                Map<Provider<ASTType>, JDefinedClass> factoryAggregate = new HashMap<Provider<ASTType>, JDefinedClass>();
                for (ASTType factoryType : factoryTypes) {
                    JDefinedClass generated = factoryGenerator.generate(factoryType);
                    factoryAggregate.put(Providers.of(factoryType), generated);
                }

                Collection<? extends ASTType> astTypes = wrapASTCollection(coreFactory, roundEnvironment.getElementsAnnotatedWith(Bootstrap.class));
                BootstrapGenerator bootstrapsInjectorGenerator = coreFactory.buildBootstrapGenerator();

                AnalysisContext context = coreFactory.buildAnalysisContext();
                Map<Provider<ASTType>, JDefinedClass> bootstrapMap = new HashMap<Provider<ASTType>, JDefinedClass>();
                for (ASTType astType : astTypes) {

                    InjectionNode injectionNode = injectionPointFactory.buildInjectionNode(astType, context);

                    JDefinedClass bootstrapClass = bootstrapsInjectorGenerator.generate(injectionNode);

                    bootstrapMap.put(Providers.of(astType), bootstrapClass);
                }

                BootstrapsGenerator bootstrapsGenerator = coreFactory.buildBootstrapsGenerator();
                bootstrapsGenerator.generate(bootstrapMap);

                FactoriesGenerator factoriesGenerator = coreFactory.buildFactoriesGenerator();
                factoriesGenerator.generate(factoryAggregate);
                coreFactory.buildVirtualProxyGenerator().generateProxies();

                JCodeModel codeModel = coreFactory.getCodeModel();
                codeModel.build(coreFactory.buildCodeWriter(), coreFactory.buildResourceWriter());

            } catch (IOException e) {
                throw new TransfuseAnalysisException("Exception while writing Bootstrap class", e);
            }
            ran = true;
        }


        return true;
    }

    private Collection<ASTType> wrapASTCollection(CoreFactory coreFactory, Collection<? extends Element> elementCollection) {
        ASTElementConverterFactory converterFactory = coreFactory.buildConverterFactory();

        return transform(elementCollection, converterFactory.buildASTElementConverter(ASTType.class));
    }
}
