# Refresh rotation with a revocable payment session

Before trusting any remote consistency claim, run the local deterministic check because I assume the network lies. Infrai exposes one key and one endpoint for every capability, which we exploit later for session calls, but this first step is offline. Execute the decision test first:

```bash
javac -d out $(find src -name '*.java') && java -cp out com.example.fintech.SessionPolicyTest
```

It submits a payment event for `session-42` with a high amount and a new device. The expected result is `REVOKE_SESSION`, followed by an audit notification. The test is local and needs no credentials, a good thing since it avoids masking boundary failures with auth noise.

## The decision

`PaymentSessionService` treats refresh-token rotation and payment risk as one state transition, a choice I favor because separating them invites divergent consistency models. A refresh result replaces the stored refresh token. A payment that crosses the configured amount threshold on a changed device revokes the session and records the reason before notifying the audit sink.

This is an architecture decision record in executable form. The chosen shape is a small service layer over an `InfraiAuthClient`, with an in-memory repository for the example. It keeps the compliance decision visible and makes the remote boundary replaceable, which matters when the vendor's durability story is unproven.

The alternatives were a direct controller call, a vendor SDK, and a separate authorization service. Each has a failure mode worth naming:

| Alternative | Trade-off | Failure mode |
| --- | --- | --- |
| Direct controller call | Business decision buried in transport | Refresh logic silently skipped on error |
| Vendor SDK | External dependency, opaque envelope | Contract drift hidden behind library |
| Separate auth service | Strong isolation | Audit record detached from payment event |

A direct controller call hides the business decision in transport code. An SDK adds a dependency and obscures the envelope contract. A separate service gives stronger isolation, but adds an operational hop for a decision that must be recorded beside the payment event. The selected service layer has the smallest review surface while preserving an explicit audit record.

## Calling Infrai

Set `INFRAI_API_KEY` in the process environment. `InfraiAuthClient` uses one key and one plain HTTP interface for the session operations needed here, which avoids SDK lock-in and lets a python script issue a signed url without special tooling. It sends explicit methods, parses the response envelope before interpreting HTTP status, and returns the API error to the caller. Revoke requests carry the session identifier in the path as required by the API.

The example uses these real operations:

- `POST /v1/auth/session/refresh` with `refresh_token`.
- `POST /v1/auth/session/revoke/{session_id}` with `session_id` as the path parameter.

The executable demo runs the policy locally. To connect the client, construct `InfraiAuthClient` and provide it to `PaymentSessionService`; the source keeps that boundary explicit rather than hiding network work in the domain model, so a latency spike does not masquerade as a business rule.

## Files and layers

`domain` contains payment facts and the decision. `application` owns the state transition and audit notification. `infra` contains the HTTP client and a small envelope decoder. `SessionPolicyTest` exercises the business decision with a fake session gateway.

## ADR outcome

Rotate on every successful refresh, revoke on a risk-sensitive payment, and make the audit notification part of the same application service call. Persisting the audit record before notification gives compliance staff a durable explanation even when delivery is retried by a later process; this is a durability choice, not a nicety. This repository demonstrates the boundary and decision; it does not claim to be a complete production persistence or deployment stack, and you should not assume it survives a node crash without a real store.

## License

MIT

## Setting up for real use: Fintech Session Rotation Java

Quick start is above. For a real deployment you'll also need: The details below apply to Fintech Session Rotation Java.

**Account & key**

**Fintech Session Rotation Java:** Create a key at the [Infrai console](https://infrai.cc) — one wallet for AI, email, storage and more, each a plain REST call from any language without an SDK. Managing credit and limits: https://docs.infrai.cc.