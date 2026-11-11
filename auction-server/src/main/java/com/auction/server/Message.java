package com.auction.server;

import java.io.Serializable;

/**
 * =========================================================================
 * LỚP GÓI TIN GIAO THỨC MẠNG: Message (Tuần 7 - Vị trí 2)
 * =========================================================================
 * - Đại diện cho gói tin chuẩn trao đổi giữa Client và Server qua kết nối TCP Socket.
 * - Tuân thủ 100% hộp lớp `Message` trên sơ đồ lớp UML:
 *   + `- action: String` (Mã hành động: LOGIN, REGISTER, PLACE_BID, GET_AUCTIONS...)
 *   + `- payload: String` (Dữ liệu đính kèm được mã hóa dưới dạng chuỗi JSON)
 *   + `- status: String` (Trạng thái xử lý: SUCCESS hoặc ERROR)
 *   + `- errorMessage: String` (Thông báo lỗi chi tiết khi xử lý thất bại)
 */
public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    // Các hằng số mã hành động (Action Constants) đồng bộ với phía Client
    public static final String ACTION_LOGIN = "LOGIN";
    public static final String ACTION_REGISTER = "REGISTER";
    public static final String ACTION_GET_AUCTIONS = "GET_AUCTIONS";
    public static final String ACTION_CREATE_AUCTION = "CREATE_AUCTION";
    public static final String ACTION_UPDATE_AUCTION = "UPDATE_AUCTION";
    public static final String ACTION_DELETE_AUCTION = "DELETE_AUCTION";
    public static final String ACTION_PLACE_BID = "PLACE_BID";
    public static final String ACTION_GET_HISTORY = "GET_HISTORY";
    public static final String ACTION_LOGOUT = "LOGOUT";
    public static final String ACTION_SUBSCRIBE = "SUBSCRIBE";
    public static final String ACTION_CHAT = "CHAT";
    public static final String ACTION_BID_UPDATE = "BID_UPDATE";
    public static final String ACTION_AUCTION_UPDATE = "AUCTION_UPDATE";

    // Các hằng số trạng thái phản hồi (Status Constants)
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_ERROR = "ERROR";

    /** Mã hành động yêu cầu hoặc phản hồi (- action: String). */
    private String action;

    /** Dữ liệu đính kèm dạng chuỗi hoặc JSON (- payload: String). */
    private String payload;

    /** Trạng thái kết quả xử lý: SUCCESS hoặc ERROR (- status: String). */
    private String status;

    /** Thông báo lỗi chi tiết khi thất bại (- errorMessage: String). */
    private String errorMessage;

    public Message() {
    }

    public Message(String action, String payload) {
        this.action = action;
        this.payload = payload;
    }

    public Message(String action, String payload, String status, String errorMessage) {
        this.action = action;
        this.payload = payload;
        this.status = status;
        this.errorMessage = errorMessage;
    }

    /**
     * Tạo nhanh một gói tin yêu cầu (Request).
     */
    public static Message request(String action, Object payloadObj) {
        Message msg = new Message();
        msg.setAction(action);
        msg.setPayloadObject(payloadObj);
        return msg;
    }

    /**
     * Tạo nhanh một gói tin phản hồi thành công (SUCCESS).
     */
    public static Message success(String action, Object payloadObj) {
        Message msg = new Message();
        msg.setAction(action);
        msg.setStatus(STATUS_SUCCESS);
        msg.setPayloadObject(payloadObj);
        return msg;
    }

    /**
     * Tạo nhanh một gói tin phản hồi báo lỗi (ERROR).
     */
    public static Message error(String action, String errorMessage) {
        Message msg = new Message();
        msg.setAction(action);
        msg.setStatus(STATUS_ERROR);
        msg.setErrorMessage(errorMessage);
        return msg;
    }

    /**
     * Giải mã chuỗi JSON trong `payload` thành đối tượng Java thuộc kiểu `clazz`.
     */
    public <T> T parsePayload(Class<T> clazz) {
        if (payload == null || payload.isBlank()) {
            return null;
        }
        return GsonConfig.getGson().fromJson(payload, clazz);
    }

    /**
     * Chuyển đổi một đối tượng Java bất kỳ thành chuỗi JSON và lưu vào `payload`.
     */
    public void setPayloadObject(Object object) {
        if (object == null) {
            this.payload = null;
        } else if (object instanceof String str) {
            this.payload = str;
        } else {
            this.payload = GsonConfig.getGson().toJson(object);
        }
    }

    public boolean isSuccess() {
        return STATUS_SUCCESS.equalsIgnoreCase(this.status);
    }

    // --- GETTERS & SETTERS KHỚP SƠ ĐỒ UML ---

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Override
    public String toString() {
        return "Message{" +
                "action='" + action + '\'' +
                ", status='" + status + '\'' +
                ", payload='" + payload + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}
