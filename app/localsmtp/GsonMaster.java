package localsmtp;

import java.lang.reflect.Type;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.apache.commons.lang.time.DurationFormatUtils;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.annotations.Expose;

/**
 * Use this whenever serializing/deserializing gson objects. 
 * It register a lot of great type adapters. 
 * 
 * @author hansi
 *
 */
class GsonMaster {

	public static Gson get(){
		return new GsonBuilder()
	        .addSerializationExclusionStrategy(new ExclusionStrategy() {
	            @Override
	            public boolean shouldSkipField(FieldAttributes fieldAttributes) {
	                final Expose expose = fieldAttributes.getAnnotation(Expose.class);
	                return expose != null && !expose.serialize();
	            }
	
	            @Override
	            public boolean shouldSkipClass(Class<?> aClass) {
	                return false;
	            }
	        })
	        .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
	        .registerTypeAdapter(ZonedDateTime.class, new ZonedDateTimeAdapter())
			.create(); 
	}
	
	private static class LocalDateTimeAdapter implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime>{
		@Override
		public LocalDateTime deserialize(JsonElement elem, Type type, JsonDeserializationContext ctx) throws JsonParseException {
			return LocalDateTime.parse(elem.getAsString()); 
		}

		@Override
		public JsonElement serialize(LocalDateTime date, Type type, JsonSerializationContext ctx) {
			return new JsonPrimitive(date.toString()); 
		}
		
	}
	
	private static class ZonedDateTimeAdapter implements JsonSerializer<ZonedDateTime>, JsonDeserializer<ZonedDateTime>{
		@Override
		public ZonedDateTime deserialize(JsonElement elem, Type type, JsonDeserializationContext ctx) throws JsonParseException {
			return ZonedDateTime.parse(elem.getAsString()); 
		}

		@Override
		public JsonElement serialize(ZonedDateTime date, Type type, JsonSerializationContext ctx) {
			return new JsonPrimitive(date.toString()); 
		}
		
	}
	
}
