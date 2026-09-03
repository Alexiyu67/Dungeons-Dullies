package app.dulliesanddungeons.ui

import app.dulliesanddungeons.domain.ActionCost
import app.dulliesanddungeons.domain.Recovery
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PrivateContentTest {
    @Test
    fun strictDocumentDecodesTypedMechanics() {
        val document = decodePrivateContent(
            """{
              "schemaVersion":1,"id":"private.test","version":"1.0.0","ruleset":"2024","locale":"en",
              "entries":[
                {"id":"class-stargazer","kind":"class","name":"Stargazer","mechanics":{"hitDie":8,"primaryAbility":"WIS","caster":true}},
                {"id":"feature-focus","kind":"feature","name":"Focus","mechanics":{"parentClassId":"class-stargazer","unlockLevel":1,"actionCost":{"bonusActions":1},"resource":{"maximum":2,"recovery":"LONG_REST"},"grantAutomatically":true}}
              ]
            }""",
        )
        assertEquals(8, document.entries.first().mechanics.hitDie)
        assertEquals(ActionCost(bonusActions = 1), document.entries.last().mechanics.actionCost)
        assertEquals(Recovery.LONG_REST, document.entries.last().mechanics.resource?.recovery)
    }

    @Test
    fun unknownDocumentFieldsAreRejected() {
        assertFailsWith<Exception> {
            decodePrivateContent(
                """{"schemaVersion":1,"id":"private.test","version":"1.0.0","ruleset":"2024","locale":"en","legacy":true,"entries":[]}""",
            )
        }
    }

    @Test
    fun exactDependenciesGateAtomicInstallation() {
        val state = DndAppState()
        state.registerPendingImport(
            PendingImportUi(
                packId = "private.child",
                version = "1.0.0",
                requires = listOf(PrivateContentRequirementUi("private.parent", "1.0.0")),
                sourcePath = "child.dndpack",
                candidates = listOf(PrivateEntryUi("feat-child", "feat", "Child", sourcePackId = "private.child", sourcePackVersion = "1.0.0")),
            ),
        )
        assertFalse(state.approvePendingImport("private.child"))
        state.registerPendingImport(
            PendingImportUi(
                packId = "private.parent",
                version = "1.0.0",
                sourcePath = "parent.dndpack",
                candidates = listOf(PrivateEntryUi("feat-parent", "feat", "Parent", sourcePackId = "private.parent", sourcePackVersion = "1.0.0")),
            ),
        )
        assertTrue(state.approvePendingImport("private.parent"))
        assertTrue(state.approvePendingImport("private.child"))
        assertEquals(setOf("private.parent", "private.child"), state.installedPrivatePacks.mapTo(mutableSetOf()) { it.id })
    }

    @Test
    fun reinstallingPackIdReplacesItsEntriesAndVersion() {
        val state = DndAppState()
        state.registerPendingImport(
            PendingImportUi(
                packId = "private.replace",
                version = "1.0.0",
                sourcePath = "first.dndpack",
                candidates = listOf(PrivateEntryUi("feat-first", "feat", "First", sourcePackId = "private.replace", sourcePackVersion = "1.0.0")),
            ),
        )
        assertTrue(state.approvePendingImport("private.replace"))
        state.registerPendingImport(
            PendingImportUi(
                packId = "private.replace",
                version = "2.0.0",
                sourcePath = "second.dndpack",
                candidates = listOf(PrivateEntryUi("feat-second", "feat", "Second", sourcePackId = "private.replace", sourcePackVersion = "2.0.0")),
            ),
        )

        assertTrue(state.approvePendingImport("private.replace"))
        assertEquals(listOf("feat-second"), state.privateEntries.filter { it.sourcePackId == "private.replace" }.map { it.id })
        assertEquals("2.0.0", state.installedPrivatePacks.single { it.id == "private.replace" }.version)
    }

    @Test
    fun existingCharacterLinksOnlyAfterOptIn() {
        val state = DndAppState()
        state.registerPendingImport(
            PendingImportUi(
                packId = "private.handbook",
                version = "1.0.0",
                sourcePath = "handbook.dndpack",
                candidates = listOf(
                    PrivateEntryUi("class-fighter", "class", "Fighter", mechanics = PrivateMechanicsUi(hitDie = 10), sourcePackId = "private.handbook", sourcePackVersion = "1.0.0"),
                    PrivateEntryUi("feature-local-focus", "feature", "Local Focus", mechanics = PrivateMechanicsUi(parentClassId = "class-fighter", unlockLevel = 1, resource = PrivateResourceMechanicsUi(2, Recovery.LONG_REST), grantAutomatically = true), sourcePackId = "private.handbook", sourcePackVersion = "1.0.0"),
                ),
            ),
        )
        assertTrue(state.approvePendingImport("private.handbook"))
        val character = state.characters.first { it.className == "Fighter" }
        state.beginEdit(section = EditorSection.Build, characterId = character.id)
        val draft = requireNotNull(state.editorDraft)
        assertTrue(state.matchingPrivateContentCount(draft) >= 2)
        state.linkPrivateContent(draft)
        assertTrue(state.saveEdit())
        val linked = state.characters.first { it.id == character.id }
        assertTrue(linked.features.any { it.name == "Local Focus" })
        assertEquals(listOf("private.handbook"), linked.privateContentVersions.map { it.id })
    }
}
