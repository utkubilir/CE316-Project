package com.iae.repository;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public final class RunArgsCodec {

    private static final Gson GSON = new Gson();
    private static final Type LIST_TYPE = new TypeToken<List<String>>() {}.getType();

    private RunArgsCodec() {}

    public static String encode(List<String> args) {
        if (args == null || args.isEmpty()) {
            return "[]";
        }
        return GSON.toJson(args);
    }

    public static List<String> decode(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            List<String> parsed = GSON.fromJson(json, LIST_TYPE);
            return parsed != null ? parsed : new ArrayList<>();
        } catch (RuntimeException e) {
            return new ArrayList<>();
        }
    }
}
