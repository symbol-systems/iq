---
@context:
  schema: "http://schema.org/"
  dct: "http://purl.org/dc/terms/"
  iq: "urn:iq:"
@type: "TechArticle"
title: "Kernel pipeline"
audience: ["user"]
---

# Kernel pipeline (non-technical overview)

This section explains what happens when you ask IQ to do something (run a query, call a tool, or run a script).

## What is a "pipeline"?

Think of a pipeline like a security checkpoint at an airport. Every request that enters IQ has to pass through a set of gates. Each gate checks one thing: "Is the caller allowed?", "Is this tool allowed?", "Has the user used too much quota?", "Should we keep an audit record?".

Each of those gates is a small, reusable step called a **middleware**. The pipeline is simply a list of those steps executed in order.

## Why this matters for a user

- ✅ **Consistent behavior**: Every request (even from the command line) runs through the same checks.
- ✅ **Auditability**: If something happens, IQ can show who did what and when.
- ✅ **Can be extended**: New checks can be added later without changing the core code that executes the request.

## How a user sees it

From a user perspective you do not need to know how the pipeline works, but it explains why:

- Some commands may be rejected even when they look correct (access control).
- Some operations are rate-limited (quota) and will fail after a certain number.
- Things you do are recorded (audit), so owners can see usage history.

## Where to look next

- If you want to change behaviour, look at the RDF configuration in the `mcp:pipeline` graph (see the ontologist view).
- If you want to run a tool without extra checks (for debugging), look for the `--dry-run` or `--debug` flags in the CLI (these often bypass quotas/audit).
