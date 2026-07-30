package com.github.camotoy.geyserskinmanager.common.skinretriever;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.camotoy.geyserskinmanager.common.RawCape;
import com.github.camotoy.geyserskinmanager.common.RawSkin;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

public class GeyserSkinRetriever implements BedrockSkinRetriever {
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final Object geyser;

    public GeyserSkinRetriever() {
        try {
            Class<?> geyserImpl = Class.forName("org.geysermc.geyser.GeyserImpl");
            this.geyser = geyserImpl.getMethod("getInstance").invoke(null);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not access the installed Geyser instance", e);
        }
    }

    @Override
    public RawCape getBedrockCape(UUID uuid) {
        Object session = connectionByUuid(uuid);
        if (session == null) {
            return null;
        }

        Object clientData = clientData(session);
        int width = intValue(clientData, "getCapeImageWidth", "capeImageWidth");
        int height = intValue(clientData, "getCapeImageHeight", "capeImageHeight");
        byte[] capeData = bytesValue(clientData, "getCapeData", "capeData");
        if (width == 0 || height == 0 || capeData.length == 0) {
            return null;
        }
        return new RawCape(width, height, stringValue(clientData, "getCapeId", "capeId"), capeData);
    }

    @Override
    public RawSkin getBedrockSkin(String name) {
        Object sessionManager = invokeNoArgs(geyser, "getSessionManager", "sessionManager");
        Object sessions = invokeNoArgs(sessionManager, "getSessions", "sessions");
        Iterable<?> sessionValues = sessions instanceof Map
                ? ((Map<?, ?>) sessions).values()
                : (Iterable<?>) sessions;

        for (Object session : sessionValues) {
            if (name.equals(stringValue(session, "name", "getName"))) {
                return getImage(clientData(session));
            }
        }
        return null;
    }

    @Override
    public RawSkin getBedrockSkin(UUID uuid) {
        Object session = connectionByUuid(uuid);
        if (session == null) {
            return null;
        }

        return getImage(clientData(session));
    }

    @Override
    public boolean isBedrockPlayer(UUID uuid) {
        return connectionByUuid(uuid) != null;
    }

    /**
     * Taken from https://github.com/NukkitX/Nukkit/blob/master/src/main/java/cn/nukkit/network/protocol/LoginPacket.java
     */
    private RawSkin getImage(Object clientData) {
        Object skinDataValue = invokeNoArgs(clientData, "getSkinData", "skinData");
        byte[] image = binaryValue(skinDataValue, "skin data");
        String rawSkinData = base64Value(skinDataValue, "skin data");
        if (image.length > (128 * 128 * 4) || booleanValue(clientData, "isPersonaSkin", "getPersonaSkin", "personaSkin")) {
            //System.out.println("Persona skins are not yet supported, sorry!");
            return null;
        }
        String geometryName = new String(bytesValue(clientData, "getGeometryName", "geometryName"), StandardCharsets.UTF_8);
        boolean alex = isAlex(geometryName);
        return new RawSkin(
                intValue(clientData, "getSkinImageWidth", "skinImageWidth"),
                intValue(clientData, "getSkinImageHeight", "skinImageHeight"),
                image, alex, geometryName,
                new String(bytesValue(clientData, "getGeometryData", "geometryData"), StandardCharsets.UTF_8),
                rawSkinData
        );
    }

    private Object connectionByUuid(UUID uuid) {
        try {
            Method method = geyser.getClass().getMethod("connectionByUuid", UUID.class);
            return method.invoke(geyser, uuid);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Could not retrieve a Geyser connection", e);
        }
    }

    private Object clientData(Object session) {
        return invokeNoArgs(session, "getClientData", "clientData");
    }

    private int intValue(Object target, String... methodNames) {
        return ((Number) invokeNoArgs(target, methodNames)).intValue();
    }

    private boolean booleanValue(Object target, String... methodNames) {
        return (Boolean) invokeNoArgs(target, methodNames);
    }

    private byte[] bytesValue(Object target, String... methodNames) {
        return binaryValue(invokeNoArgs(target, methodNames), methodNames[0]);
    }

    private String stringValue(Object target, String... methodNames) {
        return (String) invokeNoArgs(target, methodNames);
    }

    private byte[] binaryValue(Object value, String valueName) {
        if (value instanceof byte[]) {
            return (byte[]) value;
        }
        if (value instanceof String) {
            return Base64.getDecoder().decode((String) value);
        }
        throw new IllegalStateException("Geyser returned an unsupported " + valueName + " type: " + value.getClass().getName());
    }

    private String base64Value(Object value, String valueName) {
        if (value instanceof String) {
            return (String) value;
        }
        if (value instanceof byte[]) {
            return Base64.getEncoder().encodeToString((byte[]) value);
        }
        throw new IllegalStateException("Geyser returned an unsupported " + valueName + " type: " + value.getClass().getName());
    }

    private Object invokeNoArgs(Object target, String... methodNames) {
        for (String methodName : methodNames) {
            try {
                return target.getClass().getMethod(methodName).invoke(target);
            } catch (NoSuchMethodException ignored) {
                // Try the next accessor name for compatibility with newer Geyser mappings.
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new IllegalStateException("Could not invoke Geyser method " + methodName, e);
            }
        }
        throw new IllegalStateException("No compatible Geyser accessor found on " + target.getClass().getName());
    }

    private boolean isAlex(String geometryName) {
        try {
            String defaultGeometryName = OBJECT_MAPPER.readTree(geometryName).get("geometry").get("default").asText();
            return "geometry.humanoid.customSlim".equals(defaultGeometryName);
        } catch (Exception exception) {
            exception.printStackTrace();
            return false;
        }
    }
}
