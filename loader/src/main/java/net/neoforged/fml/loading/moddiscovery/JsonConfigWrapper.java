/*
 * Copyright (c) 2024 Your Name/Flux Project
 * The smarter wrapper that can translate property names.
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.loading.moddiscovery;

import com.mojang.logging.LogUtils;
import net.neoforged.neoforgespi.language.IConfigurable;
import net.neoforged.neoforgespi.language.IModFileInfo;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class JsonConfigWrapper implements IConfigurable {
    private static final Logger LOGGER = LogUtils.getLogger();
    private final Map<String, Object> data;
    private IModFileInfo modFileInfo;

    public JsonConfigWrapper(Map<String, Object> data) {
        this.data = data;
    }

    public void setFile(IModFileInfo modFileInfo) {
        this.modFileInfo = modFileInfo;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getConfigElement(String... path) {
        // We only handle simple, one-level keys for now.
        if (path.length != 1) {
            return Optional.empty();
        }

        String key = path[0];

        // ======================================================================
        // ===== START OF THE NEW, SMARTER DECEPTION LOGIC ======================
        // ======================================================================
        // This is our translation layer.
        switch (key) {
            // If FML asks for "modId", we give it the value of "id" from the JSON.
            case "modId":
                LOGGER.info(">>>>> FLUX: Translating request for 'modId' to 'id'. <<<<<");
                return Optional.ofNullable((T) data.get("id"));

            // If FML asks for "displayName", we give it "name".
            case "displayName":
                LOGGER.info(">>>>> FLUX: Translating request for 'displayName' to 'name'. <<<<<");
                return Optional.ofNullable((T) data.get("name"));

            // If FML asks for "loader", we provide our fake value.
            case "loader":
                LOGGER.info(">>>>> FLUX: Deceiving FML with fake 'loader' property. <<<<<");
                return Optional.of((T) "javafml");

            // For any other key, we just look it up normally.
            default:
                return Optional.ofNullable((T) data.get(key));
        }
        // ======================================================================
        // ===== END OF THE NEW, SMARTER DECEPTION LOGIC ========================
        // ======================================================================
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<? extends IConfigurable> getConfigList(String... path) {
        if (path.length == 1 && "mods".equals(path[0])) {
            LOGGER.info(">>>>> FLUX: Deceiving FML with fake 'mods' table. <<<<<");
            return List.of(this);
        }

        // This part remains the same.
        if (path.length == 1) {
            Object value = data.get(path[0]);
            if (value instanceof List<?> list) {
                List<IConfigurable> result = new ArrayList<>();
                for (Object item : list) {
                    if (item instanceof Map<?, ?> map) {
                        result.add(new JsonConfigWrapper((Map<String, Object>) map));
                    }
                }
                return result;
            }
        }
        return Collections.emptyList();
    }
}