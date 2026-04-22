---
name: spec-shaping
description: Interactive spec shaping for WasteHero features. Use when the user asks to shape a spec, write a feature spec, draft a PRD, define requirements, plan a feature, or provides a Linear issue ID to shape into a spec. Routes to the correct squad(s) and loads domain context before shaping.
version: 1.0.0
---

# WasteHero Spec Shaping

Shape feature requests, bug reports, and product ideas into well-structured specs with domain-aware context. Two phases: first determine which squad(s) the work touches, then load detailed domain context to inform the spec.

## Input

The skill can be invoked in two ways:

1. **Freeform description** — the PM describes what they want in chat. Proceed to Phase 1 as normal.
2. **Linear issue** — the PM provides a Linear issue ID or identifier (e.g., `NEX-123`, `LED-45`). When this happens:
   - Use `get_issue` to fetch the issue title, description, and comments
   - Use `list_comments` to get any discussion history on the issue
   - Use the issue's team assignment to pre-fill squad routing (still confirm with the PM)
   - Treat the issue content as the starting input — extract the feature request from it and proceed to Phase 1, skipping questions the issue already answers

## Phase 1: Squad Routing

Before shaping any spec, determine which squad(s) the request touches. Use the routing table in `references/squad-routing.md` (read it now) to match the user's request against squad trigger words and responsibilities.

If the input is a Linear issue with a team assignment, use that as the starting hypothesis for routing — but still validate it against the routing table.

**Steps:**
1. Read `references/squad-routing.md` from this skill's directory
2. Analyze the user's request (or the Linear issue content) against the quick reference table and squad ownership descriptions
3. Identify the **primary squad** (owns the core deliverable) and any **supporting squads** (provide capabilities the primary squad depends on)
4. Present the routing decision to the user for confirmation before proceeding:

> **Squad routing:**
> - **Primary**: [Squad Name] — [one-line reason]
> - **Supporting**: [Squad Name] — [one-line reason] _(if applicable)_
>
> Does this routing look right?

Wait for the user to confirm or correct before moving to Phase 2.

## Phase 2: Load Context

Once routing is confirmed, load context from two sources: the domain files (static business knowledge) and Linear (live project state).

### 2a. Domain Context

For each involved squad, read the corresponding domain file: `references/domains/domain-{squad-name}.md` (e.g., `references/domains/domain-nexus.md`, `references/domains/domain-ledger.md`)

Pay attention to:
- **Core business entities** — what concepts exist in this domain and how they relate
- **Business flows** — how users accomplish things today
- **Business rules** — constraints and invariants the spec must respect
- **What this domain does NOT handle** — boundary clarifications to catch misrouted requirements

If multiple squads are involved, check the cross-squad routing notes in `references/squad-routing.md`.

### 2b. Linear Context

Search Linear to gather context that can answer questions during spec shaping — so the PM doesn't have to provide information that's already captured in the system.

**Steps:**
1. **Search for related issues** — use `list_issues` or `search_documentation` with keywords from the user's request. Look for existing issues, completed work, or in-progress features that relate.
2. **Check the squad's team backlog** — use `list_teams` to find the routed squad's Linear team, then `list_issues` filtered to that team to understand what's in flight.
3. **Search documents** — use `search_documentation` to find existing specs, RFCs, or design docs in the feature area.

Don't present Linear findings as a report. Use them silently as context — if a question comes up during shaping that Linear already answers (e.g., "Has anyone worked on X before?" or "What's the current state of Y?"), use what you found instead of asking the PM. If you find an existing spec or issue that closely matches the request, ask the user whether this is a refinement of that work or something new.

### Using context during spec shaping

Use both sources throughout — the domain files to catch when a requirement conflicts with existing business rules, belongs to a different domain, or assumes entities/flows that don't exist; Linear context to pre-answer questions and avoid asking the PM things the system already knows.

## Phase 3: Interactive Spec Shaping

With domain context loaded, shape the spec through conversation. Read `references/meyer-framework.md` now — it defines the quality discipline for this phase.

### Listening for the Seven Sins

As the PM describes the feature, continuously scan their input for Meyer's Seven Sins of the Specifier. Surface findings as **clarifying questions**, never as an audit report.

- **Noise** — Background that isn't actionable. Ask: "Should this become a hard constraint, or is it background only?"
- **Silence** — Missing information (error handling, boundaries, what happens when things fail). Ask about the gap.
- **Contradiction** — Two statements that conflict. Ask: "Which one takes priority?"
- **Ambiguity** — Terms interpretable multiple ways (e.g., "the customer" — property owner? municipality? end user?). Ask: "When you say [X], do you mean [A] or [B]?"
- **Wishful thinking** — Untestable properties ("user-friendly", "fast", "seamless"). Ask: "How would we know if this is working? What number or scenario would pass/fail?"
- **Overspecification** — Implementation prescribed as a requirement ("use a cron job", "add a dropdown"). Ask: "Is this a hard technical constraint, or would any approach that achieves [outcome] be acceptable?"
- **Dangling reference** — Mention of something defined nowhere ("per the existing flow", "the standard process"). Ask: "Where is that defined?"

### Information Gathering

Work through these dimensions, asking the user about each. Skip what clearly doesn't apply. Don't ask all questions at once — group them into 2-3 rounds.

**Round 1 — Problem & Scope:**
- What problem does this solve? Who experiences it? (Be specific: back-office staff? drivers in the field? citizens in the portal? dispatchers? municipality admins?)
- What does success look like? How would we know this is working?
- Are there existing workarounds today? What breaks or is painful without this?
- What is explicitly out of scope?

**Round 2 — Solution & Domain Fit:**
- What are the key user flows? Walk through what the user does step by step.
- How does this interact with existing business flows in the domain? (Reference specific flows from the domain file — e.g., "Does this change how 'Registering a property and assigning containers' works, or is it a new flow?")
- Are there business rules in the domain that constrain the solution? (e.g., "Container assignment rules say one active assignment per property — does this feature need to change that?")
- Does this introduce new business entities or change existing ones?
- Are there edge cases the user would experience? What happens when things go wrong?

**Round 3 — Dependencies & Boundaries (if needed):**
- Cross-squad dependencies — what does the primary squad need from supporting squads? Frame in terms of domain capabilities. (e.g., "Beacon needs pickup schedule data that Compass owns")
- Does this change how other domains interact with this one? Will existing consumers of this domain's data be affected?
- Compliance or regulatory considerations? (Nordic privacy law, NOARK 5 archiving, YLVA waste reporting, turvakielto restrictions)
- Impact on existing user workflows — does this change how people work today, or is it additive?

### Quality Check Before Output

Before producing the spec, check it against Meyer's Quality Factors (details in `references/meyer-framework.md`). The spec should be:

1. **Unambiguous** — every requirement interpretable in only one way
2. **Verifiable** — every acceptance criterion can be tested
3. **Delimited** — out of scope section is present and specific
4. **Consistent** — no contradictions between sections or with domain business rules
5. **Complete** — objective, constraints, acceptance criteria, boundaries, and examples are addressed
6. **Correct** — compatible with known domain constraints
7. **Justified** — links to a business outcome
8. **Abstract** — describes what, not how (unless a technical constraint is genuinely imposed)
9. **Feasible** — scope is achievable

If a factor is weak, go back and ask the PM rather than guessing. Don't chase perfection — Meyer's own principle says "good enough" quality is the target.

### Spec Output

After gathering enough information, produce the spec. Use this structure:

```markdown
# [Feature Title]

## Squad Routing
- **Primary**: [Squad] — [owns what]
- **Supporting**: [Squad] — [provides what] (if applicable)

## Problem Statement
[What problem this solves and for whom]

## Success Criteria
- [ ] [Measurable outcome 1]
- [ ] [Measurable outcome 2]

## Solution Overview
[High-level description of the approach]

## Detailed Requirements

### [Requirement Group 1]
- **FR-1**: [Functional requirement]
- **FR-2**: [Functional requirement]

### [Requirement Group 2]
- **FR-3**: [Functional requirement]

## User Experience
[Key screens, flows, or interaction patterns — enough for a designer or engineer to understand intent]

## Cross-Squad Dependencies
[What the primary squad needs from supporting squads — described in terms of capabilities, not implementation]

## Out of Scope
[Explicitly excluded items]

## Open Questions
- [ ] [Unresolved question 1]
- [ ] [Unresolved question 2]
```

### Guidelines

- **Ground requirements in the domain**: Reference existing business entities, flows, and rules from the domain files. "Customer can set billing rhythm per property group" is grounded; "User can configure settings" is not.
- **Respect domain boundaries**: If a requirement falls outside the primary squad's domain, call it out as a cross-squad dependency. Don't let requirements silently land in the wrong domain.
- **Challenge scope creep across domains**: If the user's request spans too many domains or is too vague, say so. Suggest splitting into multiple specs or narrowing scope.
- **Stay in product language**: Describe what the user should be able to do, not how the system should be built. "Citizen can see their next pickup date" not "Add a GET endpoint for pickup schedules."
- **Call out business rule conflicts**: If a requirement contradicts an existing business rule in the domain file, flag it explicitly. The user may want to change the rule, but it should be a conscious decision.
- **Iterate, don't waterfall**: Present the spec in draft form and invite the user to refine. Specs improve through conversation, not one-shot generation.
- **Minimal Viable Context**: Every element in the spec must earn its place by contributing to correct execution. One good example beats three paragraphs of prose. If a section doesn't apply, mark it "None" rather than inventing content. Background context that's interesting but not actionable stays out of the spec.
- **Every section earns its place**: Drop empty sections. A spec with 4 strong sections beats one with 10 half-filled ones.
