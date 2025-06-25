package com.emiforcreatestock;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Objects;

public class EMIForCreateStockConfig {
    private static EMIForCreateStockConfig INSTANCE;

    private final ModConfigSpec.BooleanValue SEND_IT;

    private EMIForCreateStockConfig(ModConfigSpec.Builder builder) {
        SEND_IT = builder.comment("Send package automatically when you click transfer craft recipe")
                .translation("auto_send_package")
                .define("SendPackage", true);
    }

    public static void register(ModContainer modContainer){
        Pair<EMIForCreateStockConfig, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(EMIForCreateStockConfig::new);
        INSTANCE = pair.getLeft();
        ModConfigSpec CONFIG_SPEC = pair.getRight();
        Objects.requireNonNull(INSTANCE);
        Objects.requireNonNull(CONFIG_SPEC);
        modContainer.registerConfig(ModConfig.Type.CLIENT, CONFIG_SPEC);
    }

    public static String getKey(String key){
        return "emiforcreatestock.config"+key;
    }

    public static boolean sendIt(){
        return INSTANCE.SEND_IT.get();
    }
}
