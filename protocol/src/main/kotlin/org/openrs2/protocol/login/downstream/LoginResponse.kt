package org.openrs2.protocol.login.downstream

import org.openrs2.protocol.Packet

public sealed class LoginResponse : Packet {
    public data class ExchangeSessionKey(val key: Long) : LoginResponse()
    public object ShowVideoAd : LoginResponse()
    public data class Ok(
        val staffModelLevel: Int,
        val playerModLevel: Int,
        val playerUnderage: Boolean,
        val parentalChatConsent: Boolean,
        val parentalAdvertConsent: Boolean,
        val mapQuickChat: Boolean,
        val recordMouseMovement: Boolean,
        val playerId: Int,
        val playerMember: Boolean,
        val mapMembers: Boolean
    ) : LoginResponse()
    public object InvalidUsernameOrPassword : LoginResponse()
    public object Banned : LoginResponse()
    public object Duplicate : LoginResponse()
    public object ClientOutOfDate : LoginResponse()
    public object ServerFull : LoginResponse()
    public object LoginServerOffline : LoginResponse()
    public object IpLimit : LoginResponse()
    public object BadSessionId : LoginResponse()
    public object ForcePasswordChange : LoginResponse()
    public object NeedMembersAccount : LoginResponse()
    public object InvalidSave : LoginResponse()
    public object UpdateInProgress : LoginResponse()
    public object ReconnectOk : LoginResponse()
    public object TooManyAttempts : LoginResponse()
    public object MapMembersOnly : LoginResponse()
    public object Locked : LoginResponse()
    public object FullscreenMembersOnly : LoginResponse()
    public object InvalidLoginServer : LoginResponse()
    public data class HopBlocked(val time: Int) : LoginResponse()
    public object InvalidLoginPacket : LoginResponse()
    public object NoReplyFromLoginServer : LoginResponse()
    public object LoginServerLoadError : LoginResponse()
    public object UnknownReplyFromLoginServer : LoginResponse()
    public object IpBlocked : LoginResponse()
    public object ServiceUnavailable : LoginResponse()
    public data class DisallowedByScript(val reason: Int) : LoginResponse()
    public object ClientMembersOnly : LoginResponse()
    public data class SwitchWorld(val id: Int) : LoginResponse()
}
