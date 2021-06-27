package mimly.processors;

import mimly.annotations.Hide;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
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

//@SupportedAnnotationTypes("Hide")
//@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class HideProcessor extends AbstractProcessor {
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(Hide.class.getName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (TypeElement annotation : annotations) { // executes only once, one annotation "Hide"
            processHide(annotation, roundEnv);
        }
        return true;
    }

    private void processHide(TypeElement annotation, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
            Predicate<Element> isNotThis = e -> !e.equals(element);
            Predicate<Element> isMethod = e -> e.getKind().equals(ElementKind.METHOD);
            Predicate<Element> isField = e -> e.getKind().equals(ElementKind.FIELD);
            Predicate<Element> isMethodOrField = e -> isMethod.test(e) || isField.test(e);
            Predicate<Element> hasSameSignature = e -> element.toString().equals(e.toString());
            Predicate<Element> isStatic = e -> e.getModifiers().contains(Modifier.STATIC);
            Predicate<Element> bothStatic = e -> isStatic.test(element) && isStatic.test(e);
            Predicate<Element> bothStaticIfMethods = e -> isMethod.test(e) && bothStatic.test(e) || isField.test(e);

            List<? extends Element> elementsInClass = element.getEnclosingElement().getEnclosedElements();
            List<? extends TypeMirror> supertypes = processingEnv.getTypeUtils().directSupertypes(element.getEnclosingElement().asType());
            List<? extends Element> elementsInSupertypes = supertypes.stream()
                    .map(typeMirror -> (DeclaredType) typeMirror)
                    .map(DeclaredType::asElement)
                    .flatMap(element1 -> element1.getEnclosedElements().stream())
                    .collect(Collectors.toList());

//            elementsInClass.forEach(System.out::println); // debug
//            elementsInSupertypes.forEach(System.out::println); // debug
//            processingEnv.getElementUtils().hides() // verify

            Optional<? extends Element> anyHiddenMethodOrField = Stream.concat(elementsInClass.stream(), elementsInSupertypes.stream())
                    .filter(isNotThis) // exclude annotated element being processed
                    .filter(isMethodOrField)
                    .filter(hasSameSignature)
                    .filter(bothStaticIfMethods)
                    .findAny();

            if (anyHiddenMethodOrField.isEmpty()) {
                displayError(element);
            }
        }
    }

    private void displayError(Element element) {
        String methodOrField = element.getKind().isField() ? "field" : "method";
        String clazz = element.getEnclosingElement().getSimpleName().toString();
        processingEnv.getMessager().printMessage(
                Diagnostic.Kind.ERROR,
                methodOrField + " does not hide any other " + methodOrField + " in " + clazz + " class or one of its supertypes",
                element
        );
    }
}