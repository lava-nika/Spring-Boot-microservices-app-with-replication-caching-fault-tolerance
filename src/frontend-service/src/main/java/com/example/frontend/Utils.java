//package com.example.frontend;
//
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import java.util.Map;
//
//public class Utils {
//
//    public static ResponseEntity<Map<String, Object>> errorResponse(int code, String message) {
//        return ResponseEntity.status(code).body(Map.of(
//                "error", Map.of(
//                        "code", code,
//                        "message", message
//                )
//        ));
//    }
//
//    public static ResponseEntity<Map<String, Object>> successResponse(Object data) {
//        return ResponseEntity.status(HttpStatus.OK).body(Map.of("data", data));
//    }
//}

package com.example.frontend;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

public class Utils {

    public static ResponseEntity<Map<String, Object>> errorResponse(int code, String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("code", code);
        error.put("message", message);

        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put("error", error);

        return ResponseEntity.status(code).body(wrapper);
    }

    public static ResponseEntity<Map<String, Object>> successResponse(Object data) {
        Map<String, Object> wrapper = new HashMap<>();
        wrapper.put("data", data);
        return ResponseEntity.status(HttpStatus.OK).body(wrapper);
    }
}

