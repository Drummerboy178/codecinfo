package com.parseus.codecinfo.codecinfo.profilelevels

enum class MPEG4Profiles(val value: Int) {

    MPEG4ProfileSimple(0x01),
    MPEG4ProfileSimpleScalable(0x02),
    MPEG4ProfileCore(0x04),
    MPEG4ProfileMain(0x08),
    MPEG4ProfileNbit(0x10),
    MPEG4ProfileScalableTexture(0x20),
    MPEG4ProfileSimpleFace(0x40),
    MPEG4ProfileSimpleFBA(0x80),
    MPEG4ProfileBasicAnimated(0x100),
    MPEG4ProfileHybrid(0x200),
    MPEG4ProfileAdvancedRealTime(0x400),
    MPEG4ProfileCoreScalable(0x800),
    MPEG4ProfileAdvancedCoding(0x1000),
    MPEG4ProfileAdvancedCore(0x2000),
    MPEG4ProfileAdvancedScalable(0x4000),
    MPEG4ProfileAdvancedSimple(0x8000);

    companion object {
        fun from(findValue: Int): String? = try {
            MPEG4Profiles.values().first { it.value == findValue }.name
        } catch (e: Exception) {
            null
        }
    }

}

enum class MPEG4Levels(val value: Int) {

    MPEG4Level0(0x01),
    MPEG4Level0b(0x02),
    MPEG4Level1(0x04),
    MPEG4Level2(0x08),
    MPEG4Level3(0x10),
    MPEG4Level3b(0x18),
    MPEG4Level4(0x20),
    MPEG4Level4a(0x40),
    MPEG4Level5(0x80),
    MPEG4Level6(0x100);

    companion object {
        fun from(findValue: Int): String? = try {
            MPEG4Levels.values().first { it.value == findValue }.name
        } catch (e: Exception) {
            null
        }
    }

}