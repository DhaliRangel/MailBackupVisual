package com.mbv.config;

import com.mbv.MbvApplication;
import javafx.scene.Scene;

import java.util.HashMap;
import java.util.Map;

public class ThemeManager {

    private static final Map<String, String> THEMES = new HashMap<>();
    private String currentTheme = "dark-blue";

    static {
        THEMES.put("dark-blue", "/css/theme-blue.css");
        THEMES.put("dark-green", "/css/theme-green.css");
        THEMES.put("dark-orange", "/css/theme-orange.css");
    }

    public void applyTheme(String themeName) {
        Scene scene = MbvApplication.getPrimaryStage().getScene();
        if (scene == null) return;

        scene.getStylesheets().clear();
        String cssPath = THEMES.getOrDefault(themeName, THEMES.get("dark-blue"));
        scene.getStylesheets().add(getClass().getResource(cssPath).toExternalForm());
        this.currentTheme = themeName;
    }

    public String getCurrentTheme() {
        return currentTheme;
    }

    public Map<String, String> getAvailableThemes() {
        return THEMES;
    }
}
