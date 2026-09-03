package app.dulliesanddungeons.rules

import app.dulliesanddungeons.domain.Ability
import app.dulliesanddungeons.domain.CharacterDocument
import app.dulliesanddungeons.domain.CharacterNote
import app.dulliesanddungeons.domain.CharacterProfile
import app.dulliesanddungeons.domain.CharacterSheetData
import app.dulliesanddungeons.domain.CharacterState
import app.dulliesanddungeons.domain.ClassLevel
import app.dulliesanddungeons.domain.CoinDenomination
import app.dulliesanddungeons.domain.ContentDistribution
import app.dulliesanddungeons.domain.ContentPackManifest
import app.dulliesanddungeons.domain.CombatProfile
import app.dulliesanddungeons.domain.DiceExpression
import app.dulliesanddungeons.domain.CurrencyPurse
import app.dulliesanddungeons.domain.FiveEBuildData
import app.dulliesanddungeons.domain.FiveEHealthState
import app.dulliesanddungeons.domain.MovementMode
import app.dulliesanddungeons.domain.RulesetId
import app.dulliesanddungeons.domain.SpellRecord
import app.dulliesanddungeons.domain.WeaponRecord
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CharacterDocumentTest {
    @Test
    fun canonicalDocumentRoundTripsRulesStateAndSheetFacts() {
        val build = sampleBuild(level = 5)
        val document = CharacterDocument(
            build = build,
            state = CharacterState(
                characterId = build.id,
                currentHitPoints = 31,
                maximumHitPoints = 40,
                temporaryHitPoints = 3,
                maximumHitPointReduction = 5,
                health = FiveEHealthState(exhaustionLevel = 2),
                currency = CurrencyPurse(platinum = 1, gold = 23, silver = 4, copper = 8),
            ),
            sheet = CharacterSheetData(
                portraitSeed = 7,
                notes = "Keeps a silver token.",
                noteEntries = listOf(CharacterNote("token", "Keepsake", "Keeps a silver token.")),
                profile = CharacterProfile("Quiet but stubborn", "Protect the village", "Neutral good"),
                combat = CombatProfile(baseSpeedsFeet = mapOf(MovementMode.WALK to 30, MovementMode.FLY to 40)),
                weapons = listOf(
                    WeaponRecord(
                        id = "longbow",
                        name = "Longbow",
                        attackAbility = Ability.DEXTERITY,
                        damage = DiceExpression(1, 8),
                        damageType = "piercing",
                    ),
                ),
                spells = listOf(SpellRecord("misty_step", "Misty Step", level = 2)),
            ),
        )

        val json = Json { encodeDefaults = true; classDiscriminator = "type" }
        val restored = json.decodeFromString<CharacterDocument>(json.encodeToString(document))

        assertEquals(document, restored)
        assertEquals(5, restored.build.level)
        assertEquals(40, restored.sheet.combat.baseSpeedsFeet.getValue(MovementMode.FLY))
        assertEquals(23, restored.state.currency.balance(CoinDenomination.GOLD))
    }

    @Test
    fun documentsWithoutMaximumHitPointReductionRemainCompatible() {
        val build = sampleBuild()
        val document = CharacterDocument(
            build = build,
            state = CharacterState(build.id, currentHitPoints = 10, maximumHitPoints = 10),
        )
        val json = Json { encodeDefaults = false; classDiscriminator = "type" }
        val encoded = json.encodeToString(document)

        assertFalse("maximumHitPointReduction" in encoded)
        assertEquals(0, json.decodeFromString<CharacterDocument>(encoded).state.maximumHitPointReduction)
        assertFalse("currency" in encoded)
        assertEquals(CurrencyPurse(), json.decodeFromString<CharacterDocument>(encoded).state.currency)
    }

    @Test
    fun negativeCurrencyIsRejectedByDocumentValidation() {
        val build = sampleBuild()
        val document = CharacterDocument(
            build = build,
            state = CharacterState(
                characterId = build.id,
                currentHitPoints = 10,
                maximumHitPoints = 10,
                currency = CurrencyPurse(silver = -1),
            ),
        )

        assertTrue(CharacterDocumentValidator.validate(document).any { it.code == "currency.negative" })
    }

    @Test
    fun duplicateNoteIdsAreRejectedByDocumentValidation() {
        val build = sampleBuild(level = 1)
        val note = CharacterNote("duplicate", "First")
        val document = CharacterDocument(
            build = build,
            state = CharacterState(
                characterId = build.id,
                currentHitPoints = 10,
                maximumHitPoints = 10,
                health = FiveEHealthState(),
            ),
            sheet = CharacterSheetData(noteEntries = listOf(note, note.copy(title = "Second"))),
        )

        assertTrue(CharacterDocumentValidator.validate(document).any { it.code == "note.duplicate" })
    }

    @Test
    fun totalLevelIsOwnedByTheRulesPayload() {
        val build = sampleBuild(level = 7, classLevels = 7).copy(
            rules = FiveEBuildData(
                ancestryId = "human",
                backgroundId = "guard",
                classes = listOf(ClassLevel("fighter", 5), ClassLevel("wizard", 2)),
                abilities = Ability.entries.associateWith { 10 },
            ),
        )

        assertEquals(7, build.level)
        assertEquals(5, build.classes.first { it.classId == "fighter" }.levels)
    }

    @Test
    fun restrictedContentCannotBeMarkedForBundledDistribution() {
        assertFailsWith<IllegalArgumentException> {
            ContentPackManifest(
                id = "restricted",
                version = "1",
                ruleset = RulesetId.PF2E_REMASTER,
                locales = setOf(app.dulliesanddungeons.domain.LocaleId.EN),
                sources = emptyList(),
                distribution = ContentDistribution.BUNDLED_OPEN,
                containsRestrictedMaterial = true,
            )
        }
    }
}
