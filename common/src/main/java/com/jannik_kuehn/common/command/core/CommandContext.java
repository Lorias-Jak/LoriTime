package com.jannik_kuehn.common.command.core;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.common.CommonSender;

/**
 * Runtime command invocation context.
 *
 * @param plugin LoriTime plugin
 * @param sender command sender
 * @param args command arguments
 */
public record CommandContext(LoriTimePlugin plugin, CommonSender sender, String[] args) {
}
