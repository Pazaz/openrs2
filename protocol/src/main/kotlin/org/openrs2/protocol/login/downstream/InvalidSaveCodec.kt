package org.openrs2.protocol.login.downstream

import org.openrs2.protocol.EmptyPacketCodec
import javax.inject.Singleton

@Singleton
public class InvalidSaveCodec : EmptyPacketCodec<LoginResponse.InvalidSave>(
    packet = LoginResponse.InvalidSave,
    opcode = 13
)