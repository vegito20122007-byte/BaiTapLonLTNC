package com.auction.client.util;

import com.auction.client.network.Message;
import com.auction.model.Auction;
import com.auction.model.BidTransaction;
import com.auction.model.Bidder;
import com.auction.model.Electronics;
import com.auction.model.Item;
import com.auction.model.Seller;
import com.auction.model.User;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Kiểm thử GsonConfig & Serialization (Toàn diện & Ngoại lệ)")
class GsonConfigTest {

    @Test
    @DisplayName("Gson instance không được null và là thể hiện duy nhất")
    void testGsonInstanceNotNull() {
        Gson gson1 = GsonConfig.getGson();
        Gson gson2 = GsonConfig.getGson();

        assertNotNull(gson1);
        assertSame(gson1, gson2, "GsonConfig phải trả về cùng 1 thể hiện Gson");
    }

    @Test
    @DisplayName("Hỗ trợ tuần tự hóa và giải mã LocalDateTime chuẩn xác")
    void testLocalDateTimeSerialization() {
        Gson gson = GsonConfig.getGson();
        LocalDateTime originalTime = LocalDateTime.of(2026, 11, 1, 18, 30, 45);

        String json = gson.toJson(originalTime);
        assertNotNull(json);

        LocalDateTime deserializedTime = gson.fromJson(json, LocalDateTime.class);
        assertEquals(originalTime, deserializedTime);
    }

    @Test
    @DisplayName("Biên: LocalDateTime null tuần tự hóa ra null và giải mã về null an toàn")
    void testNullLocalDateTimeHandling() {
        Gson gson = GsonConfig.getGson();
        String json = gson.toJson((LocalDateTime) null);
        assertEquals("null", json);

        LocalDateTime deserialized = gson.fromJson("null", LocalDateTime.class);
        assertNull(deserialized);
    }

    @Test
    @DisplayName("Ngoại lệ: Giải mã chuỗi ngày giờ sai định dạng ném ngoại lệ parsing")
    void testMalformedLocalDateTimeThrowsException() {
        Gson gson = GsonConfig.getGson();
        String invalidDateJson = "\"invalid-datetime-string-xyz\"";

        assertThrows(Exception.class, () -> gson.fromJson(invalidDateJson, LocalDateTime.class));
    }

    @Test
    @DisplayName("Tuần tự hóa Message và parsePayload đối tượng User thành công")
    void testMessagePayloadSerialization() {
        User user = new Bidder(123, "bidder_hiep", "secure_pass");

        Message request = Message.request(Message.ACTION_LOGIN, user);
        assertNotNull(request.getPayload());

        // Parse ngược lại từ JSON payload
        User parsedUser = request.parsePayload(Bidder.class);
        assertNotNull(parsedUser);
        assertEquals(123, parsedUser.getId());
        assertEquals("bidder_hiep", parsedUser.getUsername());
    }

    @Test
    @DisplayName("Kiểm tra Message factory methods")
    void testMessageFactoryMethods() {
        Message successMsg = Message.success(Message.ACTION_LOGIN, "Đăng nhập thành công");
        assertTrue(successMsg.isSuccess());
        assertEquals(Message.STATUS_SUCCESS, successMsg.getStatus());

        Message errorMsg = Message.error(Message.ACTION_LOGIN, "Sai mật khẩu");
        assertFalse(errorMsg.isSuccess());
        assertEquals(Message.STATUS_ERROR, errorMsg.getStatus());
        assertEquals("Sai mật khẩu", errorMsg.getErrorMessage());
    }

    @Test
    @DisplayName("Biên & Ngoại lệ: Kiểm tra các trường hợp đặc biệt của Message payload")
    void testMessageEdgeCases() {
        Message msg = new Message();

        // 1. parsePayload khi payload rỗng hoặc null -> trả về null
        msg.setPayload(null);
        assertNull(msg.parsePayload(Bidder.class));

        msg.setPayload("   ");
        assertNull(msg.parsePayload(Bidder.class));

        // 2. setPayloadObject với null -> payload thành null
        msg.setPayloadObject(null);
        assertNull(msg.getPayload());

        // 3. setPayloadObject với String thuần -> không bị bọc 2 lần ngoặc kép
        msg.setPayloadObject("Hello Plain Text");
        assertEquals("Hello Plain Text", msg.getPayload());

        // 4. parsePayload với JSON lỗi cú pháp -> ném JsonSyntaxException
        msg.setPayload("{ malformed json string without closing bracket");
        assertThrows(JsonSyntaxException.class, () -> msg.parsePayload(Bidder.class));
    }

    @Test
    @DisplayName("Tích hợp Domain Model phức tạp: Tuần tự hóa Auction chứa Item, Seller, Dates và Bids")
    void testComplexAuctionModelSerialization() {
        Gson gson = GsonConfig.getGson();

        Seller seller = new Seller(10, "seller_vip", "pass");
        Item item = new Electronics(101, "MacBook Pro M3 Max", "16-inch 64GB RAM", seller, "Apple", 24);
        LocalDateTime startTime = LocalDateTime.of(2026, 11, 1, 9, 0, 0);
        LocalDateTime endTime = LocalDateTime.of(2026, 11, 2, 18, 0, 0);

        Auction auction = new Auction(1, "Đấu giá MacBook Pro", item, seller, 50000000.0, 60000000.0, startTime, endTime);

        User bidder = new Bidder(20, "bidder_pro", "pass");
        BidTransaction bid = new BidTransaction(1, 1, 55000000.0, bidder, startTime.plusHours(2));
        auction.getBids().add(bid);

        // Chuyển Auction sang JSON và parse lại
        String json = gson.toJson(auction);
        assertNotNull(json);
        assertTrue(json.contains("MacBook Pro M3 Max"));
        assertTrue(json.contains("bidder_pro"));

        Auction parsedAuction = gson.fromJson(json, Auction.class);
        assertNotNull(parsedAuction);
        assertEquals("Đấu giá MacBook Pro", parsedAuction.getName());
        assertEquals(50000000.0, parsedAuction.getStartingPrice());
        assertEquals(startTime, parsedAuction.getStartTime());
        assertEquals(endTime, parsedAuction.getEndTime());
        assertEquals(1, parsedAuction.getBids().size());
        assertEquals(55000000.0, parsedAuction.getBids().get(0).getAmount());
    }

    @Test
    @DisplayName("Ngoại lệ: GsonConfig ném JsonParseException khi role/category lạ hoặc JSON không phải Object")
    void testGsonConfigInvalidRoleAndNonObjectJson() {
        String weirdRoleJson = "{\"id\":99,\"username\":\"intruder\",\"role\":\"SUPER_HACKER\"}";
        assertThrows(JsonParseException.class,
                () -> GsonConfig.getGson().fromJson(weirdRoleJson, User.class));

        String weirdCategoryJson = "{\"id\":10,\"name\":\"Món lạ\",\"category\":\"FOOD\"}";
        assertThrows(JsonParseException.class,
                () -> GsonConfig.getGson().fromJson(weirdCategoryJson, Item.class));

        assertThrows(JsonParseException.class,
                () -> GsonConfig.getGson().fromJson("\"just_a_string\"", User.class));
        assertThrows(JsonParseException.class,
                () -> GsonConfig.getGson().fromJson("[1,2,3]", Item.class));
    }
}
