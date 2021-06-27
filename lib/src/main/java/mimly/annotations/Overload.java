package mimly.annotations;

import java.lang.annotation.*;

/**
 * Indicates that a method declaration is intended to overload another method declaration in this class
 * or a supertype. If a method is annotated with this annotation type compilers are required to generate
 * an error message if the method does not overload any other method in this class or a supertype.
 *
 * @author mimly
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface Overload {
}