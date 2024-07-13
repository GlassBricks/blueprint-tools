package glassbricks.factorio.blueprint

import kotlinx.serialization.Serializable

@Serializable
public data class SignalID(
    public val name: String,
    public val type: SignalType,
)

@Serializable
@Suppress("EnumEntryName")
public enum class SignalType {
    item,
    fluid,
    virtual
}
