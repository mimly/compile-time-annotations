package mimly;

import com.google.testing.compile.Compilation;
import com.google.testing.compile.CompilationSubject;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import mimly.processors.HideProcessor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import javax.annotation.processing.AbstractProcessor;
import java.util.stream.Stream;

public class HideTest {

    private final AbstractProcessor processor;

    public HideTest() {
        this.processor = new HideProcessor();
    }

    private static Stream<Arguments> testHiddenMethods() {
        return Stream.of(
                // method in superclass does not hide anything
                Arguments.of(new String[]{"@Hide", "static", "foo", "int x", "", "static", "foo", "int x"}, Compilation.Status.FAILURE),
                // different method name
                Arguments.of(new String[]{"", "static", "foo", "int x", "@Hide", "static", "bar", "int x"}, Compilation.Status.FAILURE),
                // different parameter type
                Arguments.of(new String[]{"", "static", "foo", "int x", "@Hide", "static", "foo", "double x"}, Compilation.Status.FAILURE),
                // different number of parameters
                Arguments.of(new String[]{"", "static", "foo", "int x", "@Hide", "static", "foo", "int x, int y"}, Compilation.Status.FAILURE),
                // different parameter name, hides => should compile
                Arguments.of(new String[]{"", "static", "foo", "int x", "@Hide", "static", "foo", "int y"}, Compilation.Status.SUCCESS),
                // static mismatches
                Arguments.of(new String[]{"", "static", "foo", "int x", "@Hide", "", "foo", "int x"}, Compilation.Status.FAILURE),
                Arguments.of(new String[]{"", "", "foo", "int x", "@Hide", "static", "foo", "int x"}, Compilation.Status.FAILURE),
                // overrides, not hides
                Arguments.of(new String[]{"", "", "foo", "int x", "@Hide", "", "foo", "int x"}, Compilation.Status.FAILURE)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testHiddenMethods(Object[] args, Compilation.Status status) {
        Compilation compilation = Compiler.javac()
                .withProcessors(this.processor)
                .compile(JavaFileObjects.forSourceString("Test.java", """
                        import mimly.annotations.Hide;
                                                
                        class A {
                            %s
                            public %s void %s(%s) {}
                        }

                        class B extends A {                            
                            %s
                            public %s void %s(%s) {}
                        }""".formatted(args)));

        if (status == Compilation.Status.FAILURE) {
            CompilationSubject.assertThat(compilation).failed();
        } else {
            CompilationSubject.assertThat(compilation).succeeded();
        }
    }

    private static Stream<Arguments> testHiddenFields() {
        return Stream.of(
                // field in superclass does not hide anything
                Arguments.of(new String[]{"@Hide", "static int foo", "", "static int foo"}, Compilation.Status.FAILURE),
                // different field name
                Arguments.of(new String[]{"", "static int foo", "@Hide", "static int bar"}, Compilation.Status.FAILURE),
                // different field type, hides => should compile
                Arguments.of(new String[]{"", "static int foo", "@Hide", "static double foo"}, Compilation.Status.SUCCESS),
                // static mismatches, hides => should compile
                Arguments.of(new String[]{"", "static int foo", "@Hide", "int foo"}, Compilation.Status.SUCCESS),
                Arguments.of(new String[]{"", "int foo", "@Hide", "static int foo"}, Compilation.Status.SUCCESS),
                Arguments.of(new String[]{"", "int foo", "@Hide", "int foo"}, Compilation.Status.SUCCESS)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testHiddenFields(Object[] args, Compilation.Status status) {
        Compilation compilation = Compiler.javac()
                .withProcessors(this.processor)
                .compile(JavaFileObjects.forSourceString("Test.java", """
                        import mimly.annotations.Hide;
                                                
                        class A {
                            %s
                            public %s;
                        }

                        class B extends A {                            
                            %s
                            public %s;
                        }""".formatted(args)));

        if (status == Compilation.Status.FAILURE) {
            CompilationSubject.assertThat(compilation).failed();
        } else {
            CompilationSubject.assertThat(compilation).succeeded();
        }
    }

    private static Stream<Arguments> testHiddenMethodsAndFields() {
        return Stream.of(
                // field in superclass does not hide anything
                Arguments.of(new String[]{"@Hide", "@Hide", "", ""}, Compilation.Status.FAILURE),
                Arguments.of(new String[]{"@Hide", "", "", ""}, Compilation.Status.FAILURE),
                Arguments.of(new String[]{"", "@Hide", "", ""}, Compilation.Status.FAILURE),
                // field does not hide method and vice versa
                Arguments.of(new String[]{"", "", "@Hide", ""}, Compilation.Status.FAILURE),
                Arguments.of(new String[]{"", "", "", "@Hide"}, Compilation.Status.FAILURE),
                Arguments.of(new String[]{"", "", "@Hide", "@Hide"}, Compilation.Status.FAILURE)
        );
    }

    @ParameterizedTest
    @MethodSource
    public void testHiddenMethodsAndFields(Object[] args, Compilation.Status status) {
        Compilation compilation = Compiler.javac()
                .withProcessors(this.processor)
                .compile(JavaFileObjects.forSourceString("Test.java", """
                        import mimly.annotations.Hide;
                                                
                        class A {
                            %s
                            public static int foo;
                            
                            %s
                            public static void bar(int x) {}
                        }

                        class B extends A {                            
                            %s
                            public static int bar;
                            
                            %s
                            public static void foo(int x) {}
                        }""".formatted(args)));

        if (status == Compilation.Status.FAILURE) {
            CompilationSubject.assertThat(compilation).failed();
        } else {
            CompilationSubject.assertThat(compilation).succeeded();
        }
    }
}