# AI Audit Log - Tým xrepcim00

**Datum poslední aktualizace:** 13.03.2026

---

## 1. Structure, architecture and overview
* **Nástroj:** Claude Opus 4.6
* **Datum:** 10.03.2026
* **Prompt:**
  > Suggest multiple different options for: structure, architecture, libraries and build tools. List their pros and cons and why are they relevant for this project.
* **Úprava studentem:**
  > Choose the most appropriate tools/libraries from the suggested options, rewrote structure and architecture to make it simpler/less nested.
* **Míra generování:** 50%

---

## 2. pom.xml
* **Nástroj:** GPT 5.2
* **Datum:** 13.03.2026
* **Prompt (nebo způsob použití):**
  > Generate me pom.xml for this type of project, this is the context: 'context'
* **Úprava studentem:**
  > Rename group id, artifacts, class names, etc. (I did not like the proposed directory structure)
* **Míra generování:** 90%

---

## 3. Data Loader
* **Nástroj:** Claude Sonnet 4.6
* **Datum:** 14.03.2026
* **Prompt (nebo způsob použití):**
  > Find bugs and fix errors in this code: 'damage loader code'
* **Úprava studentem:**
  > Apply bug fixes and suggestions to improve the code (wrap in try catch, use .values().length instead of hardcoding them, ...)
* **Míra generování:** 30%

---

## 4. Map Json
* **Nástroj:** Claude Sonnet 4.6
* **Datum:** 14.03.2026
* **Prompt (nebo způsob použití):**
  > Write me a 10x10 map in this formate: 'formate' with these fields 'fields'
* **Úprava studentem:**
  > Added metadata, buildings and units
* **Míra generování:** 50%

---

## 5. PathFinder
* **Nástroj:** Claude Sonnet 4.6
* **Datum:** 14.03.2026
* **Prompt (nebo způsob použití):**
  > Find errors and debug this code - originally implemented for different projec: 'hw2 algo'
* **Úprava studentem:**
  > Implemented improvements and suggestions - friendly unit passthrough/enemy unit blocking logic, path reconstructrion
* **Míra generování:** 20%

---

## 6. GameView
* **Nástroj:** GPT 5.2
* **Datum:** 30.03.2026
* **Prompt (nebo způsob použití):**
  > Create me basic Game View template that uses these placeholder images 'images', adjust perspective for isometric view, add pan and zoom.
* **Úprava studentem:**
  > Adjusted to use my data loaders and methods, adapted for my current architecture, fixed visual bugs, errors and compilation issues. Fixed runtime exception throws, replaced placeholder methods with my own sprites. (never using chatgpt again, the absolute worst llm for programming)
* **Míra generování:** 50%

---

## 7. GameView Debugging
* **Nástroj:** Claude Sonnet 4.6
* **Datum:** 30.03.2026
* **Prompt (nebo způsob použití):**
  > Here is the code 'code' and here are the errors 'errors', explain the issues and suggest fixs/fix it
* **Úprava studentem:**
  > Applied suggested bug fixes, tweaked the code to align with my specific use case.
* **Míra generování:** 70%

---

## 8. Refactoring and Cleanup
* **Nástroj:** Claude Sonnet 4.6
* **Datum:** 31.03.2026
* **Prompt (nebo způsob použití):**
  > Here is the full repository. In depth analyzte the following: architecture, file/directory structure, modularity, extensibility, possible bugs/errors. Then output extensive analysis and possible improvements according to best coding practices and modern Java standards.
* **Úprava studentem:**
  > Selected appropriate improvements and applied the changes, fixed bugs, modularized the code.
* **Míra generování:** 80%

---

## 9. UI Rendering debugging
* **Nástroj:** GPT 5.3 Codex
* **Datum:** 31.03.2026
* **Prompt (nebo způsob použití):**
  > I have these visual bugs and issues: 'described visual bugs', fix them
* **Úprava studentem:**
  > Applied suggested fixes, readjusted values to match my actual sprite dimensions.
* **Míra generování:** 90%

---

## 10. Logging and Replay
* **Nástroj:** GPT 5.3 Codex
* **Datum:** 31.03.2026
* **Prompt (nebo způsob použití):**
  > I want to implement logger and replay for this game, these are my requirements: 'requirements'
* **Úprava studentem:**
  > Added common Game Persistence class that is shared by both for better modularity and moved some of the implemented methods there.
* **Míra generování:** 80%

---

## 11. BotService
* **Nástroj:** GPT 5.3 Codex
* **Datum:** 31.03.2026
* **Prompt (nebo způsob použití):**
  > I want to implement a new Bot Service in the style of my other services: 'context'. Implement the simple bot that can be used by the engine.
* **Úprava studentem:**
  > Almost none (just added some comments, adjusted small logic errros and cleaned up the code)
* **Míra generování:** 95%

---

## 12. Final Code Refactor and Clean Up
* **Nástroj:** Copilot
* **Datum:** 02.04.2026
* **Prompt (nebo způsob použití):**
  > Inspect the whole codebase, propose changes and adjustments that need to be made in order to: clean up, modularize and extend the code into a production ready formate. What I want - naming standardization according to best java practices, code modularizationa, method and class refactoring so that the naming and style is consistent with production ready game project. After that EXPLAIN IN DEPTH every decision and change you made - why? what is it good for? when to use it in future projects?
* **Úprava studentem:**
  > Applied only selected changes to minize bloat and confusing code. Adjusted and rewrote comments to fit my style and preference.
* **Míra generování:** 80%

---