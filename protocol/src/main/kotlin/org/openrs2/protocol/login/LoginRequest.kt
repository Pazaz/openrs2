package org.openrs2.protocol.login

import org.openrs2.protocol.Packet

public sealed class LoginRequest : Packet {
    public class InitJs5RemoteConnection(public val version: Int) : LoginRequest()
}
