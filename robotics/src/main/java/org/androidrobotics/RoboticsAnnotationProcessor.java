package org.androidrobotics;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.androidrobotics.analysis.adapter.ASTElementConverterFactory;
import org.androidrobotics.analysis.adapter.ASTType;
import org.androidrobotics.annotations.Activity;
import org.androidrobotics.annotations.RoboticsModule;
import org.androidrobotics.config.RoboticsGenerationGuiceModule;
import org.androidrobotics.util.FilerSourceCodeWriter;
import org.androidrobotics.util.ResourceCodeWriter;
import org.androidrobotics.util.TypeCollectionUtil;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

/**
 * @author John Ericksen
 */
@SupportedAnnotationTypes({"org.androidrobotics.annotations.Activity",
        "org.androidrobotics.annotations.RoboticsModule"})
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class RoboticsAnnotationProcessor extends AbstractProcessor {

    private boolean processorRan = false;
    private RoboticsProcessor roboticsProcessor;
    private TypeCollectionUtil typeCollectionUtil;
    private ASTElementConverterFactory astElementConverterFactory;

    @Override
    public void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        try {
            Injector injector = Guice.createInjector(new RoboticsGenerationGuiceModule());
            roboticsProcessor = injector.getInstance(RoboticsProcessor.class);
            typeCollectionUtil = injector.getInstance(TypeCollectionUtil.class);
            astElementConverterFactory = injector.getInstance(ASTElementConverterFactory.class);
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> typeElements, RoundEnvironment roundEnvironment) {

        if (!processorRan) {

            roboticsProcessor.processModuleElements(wrapASTCollection(
                    roundEnvironment.getElementsAnnotatedWith(RoboticsModule.class)
            ));


            for (Class<? extends Annotation> annotationClass : Arrays.asList(Activity.class)) {
                roboticsProcessor.processRootElement(wrapASTCollection(
                        roundEnvironment.getElementsAnnotatedWith(annotationClass)
                ));
            }

            roboticsProcessor.verify();

            Filer filer = processingEnv.getFiler();

            roboticsProcessor.writeSource(new FilerSourceCodeWriter(filer),
                    new ResourceCodeWriter(filer));

            processorRan = true;
            return true;
        }
        return false;
    }

    private Collection<? extends ASTType> wrapASTCollection(Set<? extends Element> elementCollection) {
        return typeCollectionUtil.wrapCollection(elementCollection,
                astElementConverterFactory.buildASTElementConverter(ASTType.class)
        );
    }
}
