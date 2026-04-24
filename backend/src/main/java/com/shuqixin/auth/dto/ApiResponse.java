package com.shuqixin.auth.dto;

public class ApiResponse {

    private int code;
    private String message;
    private Object data;

    public ApiResponse(int code, String message, Object data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static ApiResponse ok(String message, Object data) {
        return new ApiResponse(200, message, data);
    }

    public static ApiResponse error(int code, String message) {
        return new ApiResponse(code, message, null);
    }

    public int getCode() { return code; }
    public String getMessage() { return message; }
    public Object getData() { return data; }
}
