package org.openrs2.protocol.login.downstream

import org.openrs2.protocol.EmptyPacketCodec
import javax.inject.Singleton

@Singleton
public class FullscreenMembersOnlyCodec : EmptyPacketCodec<LoginResponse.FullscreenMembersOnly>(
    packet = LoginResponse.FullscreenMembersOnly,
    opcode = 19
)
