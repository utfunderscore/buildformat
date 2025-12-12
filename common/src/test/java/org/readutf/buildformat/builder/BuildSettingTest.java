package org.readutf.buildformat.builder;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.readutf.buildformat.settings.BuildSetting;

import java.util.Map;

public class BuildSettingTest {

    @Test
    public void testSerialization() throws JsonProcessingException {

        BuildSetting<String> stringSetting = new BuildSetting<>("test");
        BuildSetting<Integer> intSetting = new BuildSetting<>(123);

        ObjectMapper mapper = new ObjectMapper();
        
        // Configure ObjectMapper to preserve type information for BuildSetting values
        BasicPolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfBaseType(BuildSetting.class)
                .allowIfBaseType(Object.class)
                .build();
        mapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.OBJECT_AND_NON_CONCRETE, JsonTypeInfo.As.PROPERTY);

        // Test String setting serialization and deserialization
        String stringJson = mapper.writeValueAsString(stringSetting);
        System.out.println("Serialized String Setting: " + stringJson);
        
        BuildSetting<?> deserializedStringSetting = mapper.readValue(stringJson, BuildSetting.class);
        Assertions.assertNotNull(deserializedStringSetting);
        Assertions.assertEquals("test", deserializedStringSetting.data());
        Assertions.assertTrue(deserializedStringSetting.data() instanceof String, 
                "String setting data should be a String");

        // Test Integer setting serialization and deserialization
        String intJson = mapper.writeValueAsString(intSetting);
        System.out.println("Serialized Integer Setting: " + intJson);
        
        BuildSetting<?> deserializedIntSetting = mapper.readValue(intJson, BuildSetting.class);
        Assertions.assertNotNull(deserializedIntSetting);
        Assertions.assertEquals(123, deserializedIntSetting.data());
        Assertions.assertTrue(deserializedIntSetting.data() instanceof Integer, 
                "Integer setting data should be an Integer");

        // Test Map serialization and deserialization directly
        Map<String, BuildSetting<?>> settings = Map.of(
                "1", stringSetting, 
                "2", intSetting
        );

        // Use writerFor to explicitly handle the Map type during serialization
        String mapJson = mapper.writerFor(new TypeReference<Map<String, BuildSetting<?>>>() {})
                .writeValueAsString(settings);
        System.out.println("Serialized Map (direct): " + mapJson);

        // Use readerFor to explicitly handle the Map type during deserialization
        Map<String, BuildSetting<?>> deserializedMap = mapper.readerFor(new TypeReference<Map<String, BuildSetting<?>>>() {})
                .readValue(mapJson);

        Assertions.assertNotNull(deserializedMap);
        Assertions.assertEquals(2, deserializedMap.size());

        BuildSetting<?> setting1 = deserializedMap.get("1");
        Assertions.assertNotNull(setting1);
        Assertions.assertEquals("test", setting1.data());
        Assertions.assertTrue(setting1.data() instanceof String, "Setting 1 data should be a String");

        BuildSetting<?> setting2 = deserializedMap.get("2");
        Assertions.assertNotNull(setting2);
        Assertions.assertEquals(123, setting2.data());
        Assertions.assertTrue(setting2.data() instanceof Integer, "Setting 2 data should be an Integer");
    }


}
