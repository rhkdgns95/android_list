package com.example.listactivity.model;

import java.util.HashMap;
import java.util.Map;

public class ChatModel {
    public Map<String, Comment> comments = new HashMap<>();

    public static class Comment {
        public String message;
        public Object timestamp;
    }
}
