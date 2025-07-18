/*
 * Copyright (c) 2025 Beniven/Flux Project
 * A parser for fabric.mod.json, strictly modeled on the FML ModFileParser.
 * This is a self-contained parser.
 * SPDX-License-Identifier: LGPL-2.1-only
 */

package net.neoforged.fml.loading.moddiscovery;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.mojang.logging.LogUtils;
import net.neoforged.fml.ModLoadingException;
import net.neoforged.fml.ModLoadingIssue;
import net.neoforged.fml.loading.LogMarkers;
import net.neoforged.neoforgespi.language.IModFileInfo;
import net.neoforged.neoforgespi.locating.IModFile;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

/**
 * A parser for fabric.mod.json files, mirroring the structure and functionality of ModFileParser.
 */
public class FluxFileParser {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    // We define the constant here because we are no longer using the FileReader.
    public static final String FABRIC_MOD_JSON = "fabric.mod.json";

    // This is the main parser function, a direct equivalent to modsTomlParser.
    public static IModFileInfo fabricModJsonParser(final IModFile iModFile) {
        ModFile modFile = (ModFile) iModFile;
        LOGGER.debug(LogMarkers.LOADING, "Considering Fabric mod file candidate {}", modFile.getFilePath());

        // Find fabric.mod.json instead of neoforge.mods.toml
        final Optional<Path> fabricJsonPathOpt = Optional.ofNullable(modFile.findResource(FABRIC_MOD_JSON));
        if (fabricJsonPathOpt.isEmpty()) {
            LOGGER.warn(LogMarkers.LOADING, "Mod file {} is missing {} file", modFile.getFilePath(), FABRIC_MOD_JSON);
            return null;
        }

        try (var reader = Files.newBufferedReader(fabricJsonPathOpt.get())) {
            // Instead of parsing a TOML, we parse a JSON.
            @SuppressWarnings("unchecked")
            final Map<String, Object> jsonData = GSON.fromJson(reader, Map.class);
            LOGGER.info(">>>>> FLUX: Successfully parsed {}: Mod ID '{}', Version '{}' <<<<<", FABRIC_MOD_JSON, jsonData.get("id"), jsonData.get("version"));

            // Instead of creating a NightConfigWrapper, we use our own simple wrapper.
            final JsonConfigWrapper configWrapper = new JsonConfigWrapper(jsonData);

            // We now create the ModFileInfo, passing our wrapper.
            return new ModFileInfo(modFile, configWrapper, configWrapper::setFile); // Mirrored the original constructor

        } catch (IOException | JsonSyntaxException e) {
            LOGGER.error(LogMarkers.LOADING, "Failed to parse fabric.mod.json for mod file {}", modFile.getFilePath(), e);
            var issue = ModLoadingIssue.error("fml.modloadingissue.flux.json_read_error", e.getMessage());
            throw new ModLoadingException(issue.withAffectedPath(modFile.getFilePath()));
        }
    }
}