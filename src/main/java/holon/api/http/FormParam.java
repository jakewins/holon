package holon.api.http;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * A parameter provided through an HTTP form ({@link java.lang.String} or {@link holon.api.http.UploadedFile}), or
 * all POST parameters as a Map if no parameter name is set.
 */
@Retention(RetentionPolicy.RUNTIME )
public @interface FormParam
{
    String value() default "";
    String defaultVal() default "";
}
