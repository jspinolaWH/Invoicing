# Meyer's Requirements Quality Framework

Reference for the spec agent's quality checking during the shaping dialogue.

## The Seven Sins of the Specifier

Scan every PM input for these. Surface findings as clarifying questions, not as an audit report.

| Sin | What it is | How to spot it | What to ask |
|-----|-----------|---------------|-------------|
| **Noise** | Information that carries no actionable property | Background context, competitive analysis, org history that the coding agent can't use | "This context is helpful for me to understand the situation — should any of it become a hard constraint or AC for the agent, or is it background only?" |
| **Silence** | A relevant property not mentioned anywhere | Missing: error handling, performance bounds, data source, auth model, offline behaviour, what happens at boundaries | "A coding agent would need to know [X] — what should happen when [boundary condition]?" |
| **Contradiction** | Two properties that conflict | Constraint A says X but requirement B implies not-X | "I notice [A] and [B] seem to pull in different directions — which one takes priority?" |
| **Ambiguity** | A property interpretable in multiple ways | "Update the schedule" (which schedule?), "the customer" (property owner? municipality? end user?) | "When you say [term], do you mean [interpretation A] or [interpretation B]? A coding agent will pick one silently." |
| **Wishful thinking** | A property that can't be tested | "User-friendly", "efficient", "fast", "seamless", "robust" | "How would we know if this is working? Can you give me a number or a scenario that would pass/fail?" |
| **Overspecification** | Implementation prescribed as a requirement | "Use a Redis cache", "implement with a cron job", "add a dropdown menu" | "I want to make sure we're describing what should happen, not how to build it — is [X] a hard technical constraint, or would any approach that achieves [outcome] be acceptable?" |
| **Dangling reference** | Mention of something defined nowhere | "As described in the requirements", "per the existing flow", "the standard process" | "You mentioned [X] — where is that defined? The coding agent will need a specific reference or we should define it here." |

## The 14 Quality Factors

Used to assess the crystallised spec before pushing to Linear. Not all factors need to be perfect — Meyer's own Requirements Effort Principle says "good enough" quality is the target, not perfection.

### Factors the spec agent checks actively:

1. **Unambiguous** — Every property interpretable in only one way
2. **Verifiable** — Every AC can be compiled into an automated test
3. **Delimited** — Non-goals section is present and specific
4. **Consistent** — No contradictions between sections or with domain glossary terms
5. **Traceable** — Links to parent goal/epic and relevant domain context
6. **Complete** — All five MVC components are addressed (objective, constraints, ACs, boundaries, examples)
7. **Correct** — Compatible with known domain constraints and business rules
8. **Justified** — Links to a business outcome, not just "because the PM said so"
9. **Abstract** — Describes what, not how (except in Hard Constraints where technical choices are genuinely imposed)
10. **Feasible** — The scope is achievable within the implied timeframe and team capacity

### Factors the PM owns (don't override these):

11. **Prioritised** — PM determines Critical / Important / Nice-to-have
12. **Endorsed** — PM approves the crystallised spec
13. **Readable** — PM confirms the spec makes sense to them
14. **Modifiable** — Spec is structured so changes are localised (this is handled by the template structure)

## The Minimal Viable Context Principle

Every element in the spec must earn its place by contributing to correct agent execution. The goal is not maximum completeness but the **smallest bundle of structured information that reliably produces correct behaviour**.

- If a section is empty because it genuinely doesn't apply (e.g., no hard constraints), mark it "None" rather than inventing constraints.
- If the PM provides background context that's interesting but not actionable, keep it out of the spec. The coding agent doesn't need to know the company politics behind the feature.
- Canonical examples are the highest-signal, lowest-ambiguity form of context. One good example beats three paragraphs of prose requirements.
