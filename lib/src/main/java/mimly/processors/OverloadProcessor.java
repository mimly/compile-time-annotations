package mimly.processors;

import mimly.annotations.Overload;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

//@SupportedAnnotationTypes("Overload")
//@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class OverloadProcessor extends AbstractProcessor {
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(Overload.class.getName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) { // executes only once, one annotation "Overload"
            processOverload(annotation, roundEnv);
        }
        return true;
    }

    private void processOverload(TypeElement annotation, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
            Predicate<Element> isNotThis = e -> !e.equals(element);
            Predicate<Element> isMethod = e -> e.getKind().equals(ElementKind.METHOD);
            Predicate<Element> isConstructor = e -> e.getKind().equals(ElementKind.CONSTRUCTOR);
            Predicate<Element> isMethodOrConstructor = e -> isMethod.test(e) || isConstructor.test(e);
            Predicate<Element> hasSameName = e -> element.getSimpleName().contentEquals(e.getSimpleName());
            Predicate<Element> hasDifferentSignature = e -> !element.toString().equals(e.toString());

            List<? extends Element> elementsInClass = element.getEnclosingElement().getEnclosedElements();
            List<? extends TypeMirror> supertypes = processingEnv.getTypeUtils().directSupertypes(element.getEnclosingElement().asType());
            List<? extends Element> elementsInSupertypes = supertypes.stream()
                    .map(typeMirror -> (DeclaredType) typeMirror)
                    .map(DeclaredType::asElement)
                    .flatMap(element1 -> element1.getEnclosedElements().stream())
                    .collect(Collectors.toList());

//            elementsInClass.forEach(System.out::println); // debug
//            elementsInSupertypes.forEach(System.out::println); // debug

            Optional<? extends Element> anyOverloadedMethod = Stream.concat(elementsInClass.stream(), elementsInSupertypes.stream())
                    .filter(isNotThis) // exclude annotated element being processed
                    .filter(isMethodOrConstructor)
                    .filter(hasSameName)
                    .filter(hasDifferentSignature)
                    .findAny();

            if (anyOverloadedMethod.isEmpty()) {
                displayError(element);
            }
        }
    }

    private void displayError(Element element) {
        String clazz = element.getEnclosingElement().getSimpleName().toString();
        processingEnv.getMessager().printMessage(
                Diagnostic.Kind.ERROR,
                "method does not overload any other method in " + clazz + " class or one of its supertypes",
                element
        );
    }
}