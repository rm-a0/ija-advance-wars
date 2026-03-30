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

## 6. GameView Debugging
* **Nástroj:** Claude Sonnet 4.6
* **Datum:** 30.03.2026
* **Prompt (nebo způsob použití):**
  > Here is the code 'code' and here are the errors 'errors', explain the issues and suggest fixs/fix it
* **Úprava studentem:**
  > Applied suggested bug fixes, tweaked the code to align with my specific use case.
* **Míra generování:** 70%

---

