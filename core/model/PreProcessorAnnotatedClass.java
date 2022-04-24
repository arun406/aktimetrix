package com.aktimetrix.core.model;

import com.aktimetrix.core.api.ProcessType;
import com.aktimetrix.core.stereotypes.PreProcessor;
import lombok.Data;
import org.springframework.util.StringUtils;

import javax.lang.model.element.TypeElement;

@Data
public class PreProcessorAnnotatedClass {
    private TypeElement annotatedClassElement;
    private String simpleTypeName;
    private String code;
    private ProcessType processType;

    public PreProcessorAnnotatedClass(TypeElement classElement) throws IllegalArgumentException {
        this.annotatedClassElement = classElement;
        PreProcessor annotation = classElement.getAnnotation(PreProcessor.class);
        code = annotation.code();
        processType = annotation.processType();

        if (StringUtils.isEmpty(code)) {
            throw new IllegalArgumentException(
                    String.format("code() in @%s for class %s is null or empty! that's not allowed",
                            PreProcessor.class.getSimpleName(), classElement.getQualifiedName().toString()));
        }

        if (processType == null) {
            throw new IllegalArgumentException(
                    String.format("processType() in @%s for class %s is null or empty! that's not allowed",
                            PreProcessor.class.getSimpleName(), classElement.getQualifiedName().toString()));
        }
    }

    /**
     * The original element that was annotated with @Factory
     */
    public TypeElement getTypeElement() {
        return annotatedClassElement;
    }

}
