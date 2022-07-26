package org.openrs2.protocol.create.downstream

import org.openrs2.protocol.EmptyPacketCodec
import javax.inject.Singleton

@Singleton
public class PasswordSimilarToName2Codec : EmptyPacketCodec<CreateResponse.PasswordSimilarToName2>(
    packet = CreateResponse.PasswordSimilarToName2,
    opcode = 36
)
