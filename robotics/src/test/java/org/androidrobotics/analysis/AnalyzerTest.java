package org.androidrobotics.analysis;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import org.androidrobotics.analysis.adapter.ASTClassFactory;
import org.androidrobotics.analysis.adapter.ASTType;
import org.androidrobotics.analysis.astAnalyzer.ASTInjectionAspect;
import org.androidrobotics.analysis.astAnalyzer.VirtualProxyAspect;
import org.androidrobotics.config.RoboticsGenerationGuiceModule;
import org.androidrobotics.gen.variableBuilder.VariableInjectionBuilderFactory;
import org.androidrobotics.model.ConstructorInjectionPoint;
import org.androidrobotics.model.FieldInjectionPoint;
import org.androidrobotics.model.InjectionNode;
import org.androidrobotics.model.MethodInjectionPoint;
import org.androidrobotics.util.EmptyProcessingEnvironment;
import org.androidrobotics.util.JavaUtilLogger;
import org.junit.Before;
import org.junit.Test;

import javax.inject.Inject;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * @author John Ericksen
 */
public class AnalyzerTest {

    public static interface B {
    }

    public static class A {
        private B b;
        private E e;

        @Inject
        public void setMultiVariables(B b, E e) {
            this.b = b;
            this.e = e;
        }
    }

    public static class BImpl implements B {
        @Inject
        private C c;
        private F f;

        @Inject
        public BImpl(F f) {
            this.f = f;
        }

    }

    public static class C {
        @Inject
        private D d;
    }

    public static class D {
        //back link
        private B b;

        @Inject
        public D(B b) {
            this.b = b;
        }
    }

    public static class E {
        private F f;

        @Inject
        public E(F f) {
            this.f = f;
        }
    }

    public static class F {
        //empty
    }

    private Analyzer analyzer;
    private ASTClassFactory astClassFactory;
    private AnalysisContext analysisContext;

    @Before
    public void setup() {
        Injector injector = Guice.createInjector(Stage.DEVELOPMENT, new RoboticsGenerationGuiceModule(new JavaUtilLogger(this), new EmptyProcessingEnvironment()));

        VariableInjectionBuilderFactory variableInjectionBuilderFactory = injector.getInstance(VariableInjectionBuilderFactory.class);

        analyzer = injector.getInstance(Analyzer.class);
        astClassFactory = injector.getInstance(ASTClassFactory.class);

        analysisContext = injector.getInstance(SimpleAnalysisContextFactory.class).buildContext();

        analysisContext.getInjectionNodeBuilders().put(B.class.getName(),
                variableInjectionBuilderFactory.buildVariableInjectionNodeBuilder(astClassFactory.buildASTClassType(BImpl.class)));
    }

    @Test
    public void testBackLinkAnalysis() {
        ASTType astType = astClassFactory.buildASTClassType(A.class);

        InjectionNode injectionNode = analyzer.analyze(astType, astType, analysisContext);

        //A -> B && A -> E
        assertEquals(1, injectionNode.getAspect(ASTInjectionAspect.class).getMethodInjectionPoints().size());
        MethodInjectionPoint bInjectionPoint = injectionNode.getAspect(ASTInjectionAspect.class).getMethodInjectionPoints().iterator().next();

        assertEquals(2, bInjectionPoint.getInjectionNodes().size());
        //A -> B
        InjectionNode bInjectionNode = bInjectionPoint.getInjectionNodes().get(0);
        assertTrue(isProxyRequired(bInjectionNode));
        assertEquals(BImpl.class.getName(), bInjectionNode.getClassName());

        //A -> E
        InjectionNode eInjectionNode = bInjectionPoint.getInjectionNodes().get(1);
        assertFalse(isProxyRequired(eInjectionNode));
        assertEquals(E.class.getName(), eInjectionNode.getClassName());

        //B -> C
        assertEquals(1, bInjectionNode.getAspect(ASTInjectionAspect.class).getFieldInjectionPoints().size());
        FieldInjectionPoint cInjectionPoint = bInjectionNode.getAspect(ASTInjectionAspect.class).getFieldInjectionPoints().iterator().next();
        InjectionNode cInjectionNode = cInjectionPoint.getInjectionNode();
        assertFalse(isProxyRequired(cInjectionNode));
        assertEquals(C.class.getName(), cInjectionNode.getClassName());

        //B -> F
        ConstructorInjectionPoint fNonBackLinkInjectionPoint = bInjectionNode.getAspect(ASTInjectionAspect.class).getConstructorInjectionPoint();
        assertEquals(1, fNonBackLinkInjectionPoint.getInjectionNodes().size());
        InjectionNode fInjectionNode = fNonBackLinkInjectionPoint.getInjectionNodes().get(0);
        assertFalse(isProxyRequired(fInjectionNode));
        assertEquals(F.class.getName(), fInjectionNode.getClassName());

        //E -> F
        ConstructorInjectionPoint fNonBackLinkInjectionPoint2 = eInjectionNode.getAspect(ASTInjectionAspect.class).getConstructorInjectionPoint();
        assertEquals(1, fNonBackLinkInjectionPoint2.getInjectionNodes().size());
        InjectionNode fInjectionNode2 = fNonBackLinkInjectionPoint2.getInjectionNodes().get(0);
        assertFalse(isProxyRequired(fInjectionNode2));

        //C -> D
        assertEquals(1, cInjectionNode.getAspect(ASTInjectionAspect.class).getFieldInjectionPoints().size());
        FieldInjectionPoint dInjectionPoint = cInjectionNode.getAspect(ASTInjectionAspect.class).getFieldInjectionPoints().iterator().next();
        InjectionNode dInjectionNode = dInjectionPoint.getInjectionNode();
        assertFalse(isProxyRequired(dInjectionNode));
        assertEquals(D.class.getName(), dInjectionNode.getClassName());

        //D -> B back link
        ConstructorInjectionPoint bBackLinkInjectionPoint = dInjectionNode.getAspect(ASTInjectionAspect.class).getConstructorInjectionPoint();
        assertEquals(1, bBackLinkInjectionPoint.getInjectionNodes().size());
        InjectionNode bBackLinkInjectionNode = bBackLinkInjectionPoint.getInjectionNodes().get(0);
        assertEquals(BImpl.class.getName(), bBackLinkInjectionNode.getClassName());
        assertTrue(isProxyRequired(bBackLinkInjectionNode));
    }

    private boolean isProxyRequired(InjectionNode injectionNode) {
        VirtualProxyAspect proxyAspect = injectionNode.getAspect(VirtualProxyAspect.class);

        return proxyAspect != null && proxyAspect.isProxyRequired();
    }
}
