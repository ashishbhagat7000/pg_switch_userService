package com.vol.pgswitch.dto;

import java.util.Map;

public class ApiResponse<T> {
    private String status;
    private String code;
    private String message;
    private T data;
    private Map<String, Object> meta;

    public ApiResponse() {}

    public ApiResponse(String status, String code, String message, T data, Map<String, Object> meta) {
        this.status = status;
        this.code = code;
        this.message = message;
        this.data = data;
        this.meta = meta;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
    public Map<String, Object> getMeta() { return meta; }
    public void setMeta(Map<String, Object> meta) { this.meta = meta; }
}
