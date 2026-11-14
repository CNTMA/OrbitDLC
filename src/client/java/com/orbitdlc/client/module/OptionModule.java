package com.orbitdlc.client.module;

import net.minecraft.client.OptionInstance;

public final class OptionModule<T> extends Module {
    private final OptionInstance<T> option;
    private final T activeValue;
    private final String description;
    private T previousValue;

    public OptionModule(String id, Category category, String description, OptionInstance<T> option, T activeValue) {
        super(id, category);
        this.description = description;
        this.option = option;
        this.activeValue = activeValue;
    }

    @Override
    public String description() { return description; }

    @Override
    protected void onEnable() {
        previousValue = option.get();
        option.set(activeValue);
    }

    @Override
    protected void onDisable() {
        option.set(previousValue);
    }
}
