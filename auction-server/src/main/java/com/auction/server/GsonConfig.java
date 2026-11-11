package com.auction.server;

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
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * =========================================================================
 * LỚP TIỆN ÍCH CẤU HÌNH JSON: GsonConfig (Tuần 7 - Vị trí 2)
 * =========================================================================
 * - Cung cấp thể hiện {@link Gson} dùng chung phía Server (khớp hộp lớp `GsonConfig` trong UML).
 * - Giải quyết 3 bài toán kỹ thuật khi chuyển đổi đối tượng Java <-> JSON qua Socket:
 *   1. Tuần tự hóa / Giải tuần tự hóa kiểu thời gian {@link LocalDateTime} theo chuẩn ISO-8601.
 *   2. Giải mã đa hình (Polymorphic Deserialization) cho lớp trừu tượng {@link User}
 *      thành {@link Bidder}, {@link Seller}, hoặc {@link Admin} dựa vào trường `"role"`.
 *   3. Giải mã đa hình cho lớp trừu tượng {@link Item}
 *      thành {@link Art}, {@link Electronics}, hoặc {@link Vehicle} dựa vào trường `"category"`.
 */
public class GsonConfig {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private static final Gson GSON = new GsonBuilder()
            // 1. Bộ chuyển đổi (TypeAdapter) cho LocalDateTime
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
            // 2. Bộ giải mã đa hình cho abstract class User
            .registerTypeAdapter(User.class, (JsonDeserializer<User>) (json, typeOfT, context) -> {
                if (json == null || json.isJsonNull()) {
                    return null;
                }
                JsonObject obj = json.getAsJsonObject();
                String role = obj.has("role") && !obj.get("role").isJsonNull()
                        ? obj.get("role").getAsString() : "BIDDER";
                if ("ADMIN".equalsIgnoreCase(role)) {
                    return context.deserialize(json, Admin.class);
                } else if ("SELLER".equalsIgnoreCase(role)) {
                    return context.deserialize(json, Seller.class);
                } else {
                    return context.deserialize(json, Bidder.class);
                }
            })
            // 3. Bộ giải mã đa hình cho abstract class Item
            .registerTypeAdapter(Item.class, (JsonDeserializer<Item>) (json, typeOfT, context) -> {
                if (json == null || json.isJsonNull()) {
                    return null;
                }
                JsonObject obj = json.getAsJsonObject();
                String category = obj.has("category") && !obj.get("category").isJsonNull()
                        ? obj.get("category").getAsString() : "ELECTRONICS";
                if ("ART".equalsIgnoreCase(category)) {
                    return context.deserialize(json, Art.class);
                } else if ("VEHICLE".equalsIgnoreCase(category)) {
                    return context.deserialize(json, Vehicle.class);
                } else {
                    return context.deserialize(json, Electronics.class);
                }
            })
            .serializeNulls()
            .create();

    private GsonConfig() {
    }

    /**
     * Lấy thể hiện Gson chuẩn dùng chung (khớp `+ getGson(): Gson` trong sơ đồ UML).
     *
     * @return Đối tượng {@link Gson}
     */
    public static Gson getGson() {
        return GSON;
    }
}
