# Product UI rules

- Keep visible screens minimal: show only elements and text that help the user complete the current task.
- Treat additional explanations, calculations, provenance, and design rationale as internal information by default.
- Reveal additional information only after an explicit user action such as **Info**, **Details**, or expanding a calculation.
- Prefer direct, whole-surface interactions and short in-place flows over extra screens or repeated selection dialogs.
- Preserve required validation, errors, accessibility labels, and safety-critical state even when reducing copy.
- Keep the Turn Guide optional. Character actions, rolls, resources, rests, and persistence must work without opening or completing it.
- Do not block structurally valid character edits merely because their timing or limits differ from the usual rules. Allow the edit and show a concise advisory hint instead, while preserving validation, safety, accessibility, and data integrity.

## Git worktrees

- Use a separate Git worktree for each task or agent session because multiple CLIs and agents may operate on the repository concurrently.
- Do not make task changes in another session's worktree.
- After a branch has been merged, remove its worktree and prune stale worktree metadata.

## Mockup storage

- Save every generated or exported mockup under the fixed repository-root folder `mockups/`, using a descriptive feature subfolder and filename.
- Prefer self-contained HTML/CSS mockups when the design can be represented faithfully and opened locally without external dependencies.
- Use raster mockups only for artwork, animation concepts, or designs that HTML cannot represent adequately.
- Never leave the only inspectable copy in a temporary directory or generated-image cache; copy the final preview into `mockups/` before presenting it.
- Always provide a clickable workspace path to each mockup. Do not rely only on inline rendering or messages such as "Viewed image".
- The entire `mockups/` tree is local-only and ignored by Git. Never force-add its contents.

## UI mockup approval

- For UI changes with unresolved placement, hierarchy, or interaction choices, create inspectable mockups during planning before finalizing the implementation plan.
- Treat mockup creation as a separate pass: do not modify application source while producing the options.
- Present the mockups with clickable workspace paths and wait for the user to explicitly select or approve a direction.
- Finalize the implementation plan and begin application changes only after that approval.

## Android release flow

- Publish public Android releases only from tags matching `vMAJOR.MINOR.PATCH`.
- Keep the release assets named `DulliesAndDungeons.apk` and
  `DulliesAndDungeons.apk.sha256`; the permanent README download link depends on
  these fixed names.
- Store signing credentials only in GitHub environment secrets. Never commit a
  keystore or its credentials.
- When changing release packaging, verify the permanent download link, checksum,
  and APK signature.
