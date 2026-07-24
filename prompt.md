# AI Usage & Guardrails

This project was built using AI coding assistants (as explicitly invited by the
assignment brief). This file records the rules those tools were expected to
follow, so a reviewer can check process, not just output.

## Ground rules

1. **No unverified numbers or status claims.** Any percentage, count, or
   "complete / production-ready / 100%" statement written into README.md or
   any doc must come from actually running the relevant command in this
   repo (e.g. `mvn clean test jacoco:report`) in the same session it's
   written, not from an AI estimate. If it can't be verified at write time,
   it doesn't get written as fact.
2. **No silencing red tests.** An AI suggestion that "fixes" a failing test
   by commenting out, weakening, or deleting the assertion is rejected. If a
   test genuinely can't be made to pass in scope, that is recorded as an open
   item in `implementation_plan.md`, not hidden.
3. **No parallel/duplicate config to route around a problem.** If an AI
   suggestion adds a second config class/bean to sidestep an issue with an
   existing one, the two are reconciled (one is fixed or removed) rather than
   both being kept.
4. **AI-generated planning/status output is not committed as permanent
   documentation.** Session notes, completion reports, and "final status"
   summaries are working scratch, not deliverables. Only `README.md` and
   `implementation_plan.md` are kept up to date and treated as authoritative.
5. **Every AI-touched file is read and diffed by a human before it's kept.**
   Accepting a suggestion means having read it, not just running it.
6. **Known gaps are recorded, not smoothed over.** If an AI-assisted pass
   doesn't fully close an issue, that is stated explicitly in
   `implementation_plan.md` rather than presented as done.

## Representative prompts used

These are the actual prompts that drove the most recent, verifiable pass over
this repo (an assessment + guardrails cleanup session):

- "Check the requirement.md, analyse the code and give me rating Poor/Medium/Good
  based on: Requirement Coverage, Coding Standards, Multiple Commits, AI Usage
  (Guardrails), Test Coverage (use JaCoCo for actual percentage)."
- "give me the prompt that was used to assess the assignment, make it better"
- "can you fix the ai usage part" → clarified in follow-up to be docs-only,
  no `.java` changes in scope
- "i want a prompt.md and implementation_plan.md that's it, which will give me
  Good in AI usage"

Each of these was answered by actually running the build/tests/coverage tool,
reading the real source files and git history, and only then writing findings
— rather than summarizing what the repo's own docs claimed about itself.

## Status

See `implementation_plan.md` → "Known Gaps" for what is still open. This file
does not claim those gaps are resolved.
