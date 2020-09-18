package com.stirante.runechanger.model.app;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class SettingsConfiguration {

    public List<FieldConfiguration<?>> getFields() {
        return fields;
    }

    private final List<FieldConfiguration<?>> fields = new ArrayList<>();

    public FieldConfiguration<String> textField(String key) {
        return new FieldConfiguration<>(key, String.class);
    }

    public FieldConfiguration<Boolean> checkbox(String key) {
        return new FieldConfiguration<>(key, Boolean.class);
    }

    public class FieldConfiguration<T> {

        public String getPrefKey(String sourceKey) {
            return "source_" + sourceKey + "_" + getKey();
        }

        public String getTitleKey(String sourceKey) {
            return "source_" + sourceKey + "_" + getKey();
        }

        public String getDescKey(String sourceKey) {
            return "source_" + sourceKey + "_" + getKey() + "_description";
        }

        public String getKey() {
            return key;
        }

        public Class<T> getType() {
            return type;
        }

        public Predicate<T> getValidator() {
            return validator;
        }

        public T getDefaultValue() {
            return defaultValue;
        }

        private final String key;
        private final Class<T> type;
        private Predicate<T> validator;
        private T defaultValue;

        public FieldConfiguration(String key, Class<T> type) {
            this.key = key;
            this.type = type;
        }

        public FieldConfiguration<T> defaultValue(T defaultValue) {
            this.defaultValue = defaultValue;
            return this;
        }

        public FieldConfiguration<T> validation(Predicate<T> validator) {
            this.validator = validator;
            return this;
        }

        public SettingsConfiguration add() {
            fields.add(this);
            return SettingsConfiguration.this;
        }

    }

}
