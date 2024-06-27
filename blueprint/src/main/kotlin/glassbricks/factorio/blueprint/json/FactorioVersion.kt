package glassbricks.factorio.blueprint.json

import kotlinx.serialization.Serializable

@Serializable
@JvmInline
public value class FactorioVersion(public val version: Long) {
    public val major: Short get() = (version shr 48).toShort()
    public val minor: Short get() = (version shr 32).toShort()
    public val patch: Short get() = (version shr 16).toShort()
    public val build: Short get() = version.toShort()
    override fun toString(): String = "$major.$minor.$patch.$build"
    public companion object {
        public fun fromParts(major: Short, minor: Short, patch: Short, build: Short): FactorioVersion =
            FactorioVersion((major.toLong() shl 48) or (minor.toLong() shl 32) or (patch.toLong() shl 16) or build.toLong())
        public fun fromString(version: String): FactorioVersion {
            val parts = version.split('.')
            return fromParts(parts[0].toShort(), parts[1].toShort(), parts[2].toShort(), parts.getOrNull(3)?.toShort() ?: 0)
        }
        
        public val DEFAULT: FactorioVersion = fromParts(1, 1, 0, 0)
        
    }
}
