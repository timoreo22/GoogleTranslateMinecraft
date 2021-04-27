package fr.timoreo.trans;

import com.google.gson.JsonElement;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;

import java.util.function.Function;

public class TranslateHoverEvent extends HoverEvent.Action<Text> {

    public TranslateHoverEvent(String name, boolean parsable, Function<JsonElement, Text> deserializer, Function<Text, JsonElement> serializer, Function<Text, Text> legacyDeserializer) {
        super(name, parsable, deserializer, serializer, legacyDeserializer);
    }
}
