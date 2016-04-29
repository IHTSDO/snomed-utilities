package org.ihtsdo.snomed.util.pojo;

import java.lang.reflect.Type;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class ConceptSerializer implements JsonSerializer<Concept>{
	public JsonElement serialize(final Concept c, final Type type, final JsonSerializationContext context) {
		return new JsonPrimitive (Description.getFormattedConcept(c.getSctId()));
	}
}
