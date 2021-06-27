package mimly.annotations;

import java.lang.annotation.*;

/**
 * Indicates that a field/method declaration is intended to hide another field/method declaration in
 * a supertype. If a field/method is annotated with this annotation type compilers are required
 * to generate an error message if the field/method does not hide any other field/method in a supertype.
 *
 * @author mimly
 */
@Documented
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
public @interface Hide {
}