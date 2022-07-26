package org.openrs2.protocol.login.downstream

import org.openrs2.protocol.EmptyPacketCodec
import javax.inject.Singleton

@Singleton
public class NoReplyFromLoginServerCodec : EmptyPacketCodec<LoginResponse.NoReplyFromLoginServer>(
    packet = LoginResponse.NoReplyFromLoginServer,
    opcode = 23
)
