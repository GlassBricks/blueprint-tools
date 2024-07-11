package glassbricks.factorio.blueprint

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class BoundingBox(
    @SerialName("left_top")
    public val leftTop: Position,
    @SerialName("right_bottom")
    public val rightBottom: Position
)
