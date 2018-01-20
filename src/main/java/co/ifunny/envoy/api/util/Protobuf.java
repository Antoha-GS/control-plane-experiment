package co.ifunny.envoy.api.util;

import com.google.common.base.Preconditions;
import com.google.protobuf.*;
import com.google.protobuf.util.JsonFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class Protobuf {

    final private static Logger logger = LoggerFactory.getLogger(Protobuf.class);

    /**
     * Encodes a protobuf Message into a Struct. Hilariously, it uses JSON as the intermediary.
     *
     * @param message Protobuf Message
     *
     * @return Protobuf Struct
     * @throws InvalidProtocolBufferException if unable to convert Message to Struct
     */
    public static Struct messageToStructThroughJson(Message message) throws InvalidProtocolBufferException {
        Preconditions.checkNotNull(message, "message may not be null");

        Struct.Builder structBuilder = Struct.newBuilder();
        String json = JsonFormat.printer().print(message);
        JsonFormat.parser().merge(json, structBuilder);

        return structBuilder.build();
    }

    public static Struct messageToStruct(Message message) {
        Preconditions.checkNotNull(message, "message may not be null");

        Struct.Builder structBuilder = Struct.newBuilder();

        for (Map.Entry<Descriptors.FieldDescriptor, Object> field : message.getAllFields().entrySet()) {
            //logger.info(String.format("%s: %s", field.getKey().getName(), field.getValue() == null ? "NULL" : field.getValue().getClass().getName()));
            structBuilder.putFields(field.getKey().getName(), objectToValue(field.getValue()));
        }

        return structBuilder.build();
    }

    private static Value objectToValue(Object object) {
        final Value.Builder valueBuilder = Value.newBuilder();

        if (object == null) {
            return valueBuilder.setNullValue(NullValue.NULL_VALUE).build();
        }

        if (object instanceof Number) {
            return valueBuilder.setNumberValue(((Number) object).doubleValue()).build();
        }

        if (object instanceof String) {
            return valueBuilder.setStringValue((String) object).build();
        }

        if (object instanceof Boolean) {
            return valueBuilder.setBoolValue((Boolean) object).build();
        }

        if (object instanceof Descriptors.EnumValueDescriptor) {
            return valueBuilder.mergeFrom(objectToValue(((Descriptors.EnumValueDescriptor) object).getName())).build();
        }

        if (object instanceof Message) {
            return valueBuilder.setStructValue(messageToStruct((Message) object)).build();
        }

        if (object instanceof Iterable) {
            ListValue.Builder listValueBuilder = ListValue.newBuilder();

            for (Object o : (Iterable) object) {
                listValueBuilder.addValues(objectToValue(o));
            }

            return valueBuilder.setListValue(listValueBuilder).build();
        }

        //logger.error("Unable to convert {} to Value", object.getClass().getSimpleName());
        throw new IllegalArgumentException(String.format("Unable to convert %s to Value", object.getClass().getSimpleName()));
        //return valueBuilder.setNullValue(NullValue.NULL_VALUE).build();
    }
}
