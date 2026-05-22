package com.jannik_kuehn.common.api.common;

import com.jannik_kuehn.common.api.LoriTimePlayer;

@SuppressWarnings("PMD.CommentRequired")
public interface CommonPlayerSender extends CommonSender, LoriTimePlayer {

    boolean isOnline();
}
