package it.eng.mapping;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class TypeSerializer extends StdSerializer<Type> {
    
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public TypeSerializer() {
        this(null);
    }
   
    public TypeSerializer(Class<Type> t) {
        super(t);
    }
 
    @Override
    public void serialize(
    		Type type, JsonGenerator jgen, SerializerProvider provider) 
      throws IOException, JsonProcessingException {
  
        jgen.writeStartObject();
        //jgen.writeNumberField("pippo", value.id);
        jgen.writeObjectField(type.getName(), type.getTypeDetails());
       // jgen.writeNumberField("owner", value.owner.id);
        jgen.writeEndObject();
    }

}