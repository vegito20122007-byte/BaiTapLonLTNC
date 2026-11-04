package com.auction.client.network;

import com.auction.client.util.GsonConfig;

import java.io.Serializable;

/**
 * Gói tin chuẩn trao đổi dữ liệu giữa Client và Server qua giao thức Socket TCP.
 * Định dạng tuần tự hóa: JSON.
 */
public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    // Các hằng số mã hành động (Action Constants)
    public static final String ACTION_LOGIN = "LOGIN";
    public static final String ACTION_REGISTER = "REGISTER";
    public static final String ACTION_GET_AUCTIONS = "GET_AUCTIONS";
    public static final String ACTION_CREATE_AUCTION = "CREATE_AUCTION";
    public static final String ACTION_UPDATE_AUCTION = "UPDATE_AUCTION";
    public static final String ACTION_DELETE_AUCTION = "DELETE_AUCTION";
    public static final String ACTION_PLACE_BID = "PLACE_BID";
    public static final String ACTION_GET_HISTORY = "GET_HISTORY";
    public static final String ACTION_LOGOUT = "LOGOUT";

    // Các hằng số trạng thái (Status Constants)
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_ERROR = "ERROR";

    /** Mã hành động yêu cầu hoặc phản hồi. */
    private String action;

    /** Dữ liệu đính kèm (chuỗi ký tự thuần hoặc chuỗi định dạng JSON). */
    private String payload;

    /** Trạng thái kết quả trả về từ Server ("SUCCESS", "ERROR"). */
    private String status;

    /** Thông điệp lỗi chi tiết khi xử lý thất bại. */
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

    // ==========================================
    // FACTORY METHODS TIỆN ÍCH
    // ==========================================

    /**
     * Tạo một request gửi đi với action và payload là một đối tượng Java bất kỳ.
     */
    public static Message request(String action, Object payloadObj) {
        Message msg = new Message();
        msg.setAction(action);
        msg.setPayloadObject(payloadObj);
        return msg;
    }

    /**
     * Tạo một phản hồi thành công (SUCCESS).
     */
    public static Message success(String action, Object payloadObj) {
        Message msg = new Message();
        msg.setAction(action);
        msg.setStatus(STATUS_SUCCESS);
        msg.setPayloadObject(payloadObj);
        return msg;
    }

    /**
     * Tạo một phản hồi thất bại (ERROR).
     */
    public static Message error(String action, String errorMessage) {
        Message msg = new Message();
        msg.setAction(action);
        msg.setStatus(STATUS_ERROR);
        msg.setErrorMessage(errorMessage);
        return msg;
    }

    // ==========================================
    // HELPER PARSE DỮ LIỆU JSON
    // ==========================================

    /**
     * Chuyển đổi payload dạng JSON sang đối tượng Java kiểu chỉ định.
     *
     * @param clazz kiểu đối tượng cần chuyển
     * @param <T> loại kiểu
     * @return đối tượng Java đã parse, hoặc null nếu payload trống
     */
    public <T> T parsePayload(Class<T> clazz) {
        if (payload == null || payload.isBlank()) {
            return null;
        }
        return GsonConfig.getGson().fromJson(payload, clazz);
    }

    /**
     * Chuyển đối tượng Java thành chuỗi JSON và gán vào thuộc tính payload.
     *
     * @param object đối tượng cần tuần tự hóa
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

    // ==========================================
    // GETTERS & SETTERS
    // ==========================================

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
