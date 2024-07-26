package io.github.nikanique.springrestframework.serializer;

import lombok.Data;

import java.lang.invoke.MethodHandle;
import java.util.HashMap;

@Data
public class SerializerConfig {
    private HashMap<String, FieldDescriptor> fields;
    private MethodHandle toRepresentMethod = null;

    public SerializerConfig() {
        fields = new HashMap<>();
    }

    public SerializerConfig(HashMap<String, FieldDescriptor> fields, MethodHandle toRepresentMethod) {
        this.fields = fields;
        this.toRepresentMethod = toRepresentMethod;
    }

    public static SerializerConfig fromDTO(Class<?> DTOClass) {
        HashMap<String, FieldDescriptor> fields = ClassStructureExtractor.extractStructure(DTOClass);
        MethodHandle toRepresentMethod = ClassStructureExtractor.findToRepresentMethod(DTOClass);
        return new SerializerConfig(fields, toRepresentMethod);
    }

    public SerializerConfig addField(String fieldName) {
        fields.put(fieldName, null);
        return this;
    }

    public SerializerConfig addField(String fieldName, FieldDescriptor fieldDescriptor) {
        fields.put(fieldName, fieldDescriptor);
        return this;
    }


}
