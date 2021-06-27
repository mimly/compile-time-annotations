package mimly;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.CompilationSubject;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import mimly.processors.OverloadProcessor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.annotation.processing.AbstractProcessor;
import java.util.stream.Stream;

public class OverloadTest {

    private final AbstractProcessor processor;

    public OverloadTest() {
        this.processor = new OverloadProcessor();
    }

    private static Stream<Arguments> testOverloadedMethods() {
        return Stream.of(
                // method in superclass overloads toString, overloads => should compile
                Arguments.of(new String[]{"@Overload", "toString", "int x", "", "foo", "int x"}, Compilation.Status.SUCCESS),
                // method in superclass does not overload anything
                Arguments.of(new String[]{"@Overload", "foo", "int x", "", "foo", "int x"}, Compilation.Status.FAILURE),
                // different method name
                Arguments.of(new String[]{"", "foo", "int x", "@Overload", "bar", "int x"}, Compilation.Status.FAILURE),
                // different parameter type, overloads => should compile
                Arguments.of(new String[]{"", "foo", "int x", "@Overload", "foo", "double x"}, Compilation.Status.SUCCESS),
                // different number of parameters, overloads => should compile
                Arguments.of(new String[]{"", "foo", "int x", "@Overload", "foo", "int x, int y"}, Compilation.Status.SUCCESS),
                // different parameter name, overrides
                Arguments.of(new String[]{"", "foo", "int x", "@Overload", "foo", "int y"}, Compilation.Status.FAILURE)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testOverloadedMethods(Object[] args, Compilation.Status status) {
        Compilation compilation = Compiler.javac()
                .withProcessors(this.processor)
                .compile(JavaFileObjects.forSourceString("Test.java", """
                        import mimly.annotations.Overload;
                                                
                        class A {
                            %s
                            public void %s(%s) {}
                        }

                        class B extends A {                            
                            %s
                            public void %s(%s) {}
                        }""".formatted(args)));

        if (status == Compilation.Status.FAILURE) {
            CompilationSubject.assertThat(compilation).failed();
        } else {
            CompilationSubject.assertThat(compilation).succeeded();
        }
    }
}