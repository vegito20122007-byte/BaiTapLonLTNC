package com.auction.client.util;

import com.auction.model.Admin;
import com.auction.model.Art;
import com.auction.model.Bidder;
import com.auction.model.Electronics;
import com.auction.model.Item;
import com.auction.model.Seller;
import com.auction.model.User;
import com.auction.model.Vehicle;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Cung cấp thể hiện Gson duy nhất được cấu hình chuẩn cho toàn bộ ứng dụng Client.
 * Tích hợp sẵn:
 * 1. TypeAdapter xử lý kiểu java.time.LocalDateTime (ISO-8601).
 * 2. Polymorphic Deserializer cho abstract class User (Bidder, Seller, Admin) dựa vào thuộc tính "role".
 * 3. Polymorphic Deserializer cho abstract class Item (Electronics, Art, Vehicle) dựa vào thuộc tính "category".
 */
public class GsonConfig {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private static final Gson GSON = new GsonBuilder()
            // 1. Adapter cho LocalDateTime
            .registerTypeAdapter(LocalDateTime.class, new TypeAdapter<LocalDateTime>() {
                @Override
                public void write(JsonWriter out, LocalDateTime value) throws IOException {
                    if (value == null) {
                        out.nullValue();
                    } else {
                        out.value(value.format(FORMATTER));
                    }
                }

                @Override
                public LocalDateTime read(JsonReader in) throws IOException {
                    if (in.peek() == JsonToken.NULL) {
                        in.nextNull();
                        return null;
                    }
                    return LocalDateTime.parse(in.nextString(), FORMATTER);
                }
            })
            // 2. Deserializer đa hình cho User (Lớp trừu tượng)
            .registerTypeAdapter(User.class, (JsonDeserializer<User>) (json, typeOfT, context) -> {
                if (json == null || json.isJsonNull()) {
                    return null;
                }
                if (!json.isJsonObject()) {
                    throw new JsonParseException("Dữ liệu User phải là JSON Object hợp lệ.");
                }
                JsonObject obj = json.getAsJsonObject();
                String role = obj.has("role") && !obj.get("role").isJsonNull() ? obj.get("role").getAsString() : "BIDDER";
                if ("ADMIN".equalsIgnoreCase(role)) {
                    return context.deserialize(json, Admin.class);
                } else if ("SELLER".equalsIgnoreCase(role)) {
                    return context.deserialize(json, Seller.class);
                } else if ("BIDDER".equalsIgnoreCase(role)) {
                    return context.deserialize(json, Bidder.class);
                }
                throw new JsonParseException("Vai trò User không hợp lệ: " + role);
            })
            // 3. Deserializer đa hình cho Item (Lớp trừu tượng)
            .registerTypeAdapter(Item.class, (JsonDeserializer<Item>) (json, typeOfT, context) -> {
                if (json == null || json.isJsonNull()) {
                    return null;
                }
                if (!json.isJsonObject()) {
                    throw new JsonParseException("Dữ liệu Item phải là JSON Object hợp lệ.");
                }
                JsonObject obj = json.getAsJsonObject();
                String category = obj.has("category") && !obj.get("category").isJsonNull() ? obj.get("category").getAsString() : "ELECTRONICS";
                if ("ART".equalsIgnoreCase(category)) {
                    return context.deserialize(json, Art.class);
                } else if ("VEHICLE".equalsIgnoreCase(category)) {
                    return context.deserialize(json, Vehicle.class);
                } else if ("ELECTRONICS".equalsIgnoreCase(category)) {
                    return context.deserialize(json, Electronics.class);
                }
                throw new JsonParseException("Danh mục Item không hợp lệ: " + category);
            })
            .serializeNulls()
            .create();

    private GsonConfig() {
        // Chặn khởi tạo trực tiếp
    }

    /**
     * Lấy thể hiện Gson dùng chung.
     *
     * @return đối tượng Gson
     */
    public static Gson getGson() {
        return GSON;
    }
}
