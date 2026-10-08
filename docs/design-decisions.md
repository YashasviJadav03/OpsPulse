# OpsPulse: Architectural Design Decisions & Interview Guide

This document details the engineering trade-offs, architectural decisions, and operational considerations implemented throughout the OpsPulse SaaS platform.

---

## 1. Multi-Tenancy Architecture: Shared Database & Shared Schema

### Evaluated Alternatives
1. **Database-per-Tenant**: Highest physical isolation, but prohibitive infrastructure cost, connection pool bloat, and operational complexity when scaling past hundreds of small SaaS tenants.
2. **Schema-per-Tenant**: Reasonable logical isolation, but schema migrations (Flyway) must execute sequentially across hundreds of schemas, creating operational drag and high catalog memory overhead.
3. **Shared Database, Shared Schema (Chosen)**: All tenant data resides in shared tables partitioned logically by a mandatory `tenant_id UUID NOT NULL` column.

### Rationale
- Maximizes hardware utilization and simplifies database management (single connection pool, single migration set).
- Matches modern multi-tenant SaaS economics for mid-market and SMB clients.
- Enforces strict isolation programmatically across four independent tiers:
  1. **Gateway**: Replaces/sanitizes all incoming tenant headers with claims verified from cryptographic JWT signatures.
  2. **Servlet Filter**: Sets `TenantContext` (`ThreadLocal<UUID>`) per request and clears it reliably in `finally`.
  3. **Hibernate 6 `@TenantId`**: Automatically injects `tenant_id = ?` into all generated SQL statements.
  4. **PostgreSQL Indexes**: Composite indexes on `(tenant_id, ...)` guarantee query performance and partition pruning.

---

## 2. Multi-Level Tenant Isolation Enforcement & Verification

| Tier | Component | Isolation Mechanism | Testing Strategy |
|---|---|---|---|
| **Boundary** | Spring Cloud Gateway | Rejects forged client `X-Tenant-Id` / `X-User-Role` headers; injects verified claims from JWT | `GatewayFilterTest` asserting header rewrite |
| **Context** | `TenantFilter` | Rejects missing/malformed tenant headers with HTTP 403; binds valid UUID to `ThreadLocal` | Unit test verifying `TenantContext.getTenantId()` and clean `finally` purge |
| **ORM / Data** | Hibernate `@TenantId` | Automatically appends `WHERE tenant_id = ?` to all entity queries | `TenantIsolationIntegrationTest` proving Tenant B receives 404 attempting to access Tenant A entities |
| **Vector Search** | pgvector / `ai-service` | Vector similarity cosine SQL includes explicit `WHERE tenant_id = :tenantId` filter | Vector integration test proving Tenant B cannot retrieve Tenant A runbook embeddings |

---

## 3. Asynchronous Messaging & Event-Driven Architecture (RabbitMQ)

### Topology Design
- **Exchange**: `opspulse.events` (Topic Exchange).
- **Queues**:
  - `opspulse.check-results` bound to routing key `check.result`.
  - `opspulse.events.dlq` configured as the Dead Letter Queue via `x-dead-letter-exchange`.
- **Publisher Confirms & Mandatory Delivery**:
  - Ensures message durability before confirming task completion.

### Idempotency & State Machine
- When processing `CheckResult` messages in `core-service`, the consumer sets `TenantContext` from the message's `tenantId` payload and resets it afterwards.
- Consecutive failures are counted per monitor:
  - **Incident Trigger**: 3 consecutive failures open an incident if none is already in `OPEN` state.
  - **Auto-Resolution**: 2 consecutive successes automatically transition an open incident to `RESOLVED`.
  - Idempotent state transitions prevent duplicate incident creation under at-least-once message delivery.

---

## 4. Concurrency Model: Virtual Threads vs Reactive

### The Checker Service Concurrency Challenge
The `checker-service` must poll and execute hundreds to thousands of outbound HTTP probes every minute, with network latencies varying from 10ms to several seconds.

### Why Virtual Threads (`Project Loom`)?
1. **Simplified Programming Model**: Outbound HTTP checks are inherently I/O bound. Traditional thread pools (e.g. `ThreadPoolExecutor`) exhaust OS threads under high load or network stalls.
2. **Virtual Thread Efficiency**: In Java 21, `Executors.newVirtualThreadPerTaskExecutor()` spawns lightweight virtual threads unmounted from carrier threads during blocking socket I/O.
3. **Semaphore Concurrency Throttling**: Rather than allowing unbounded socket churn, a configurable `Semaphore` (default: 200 permits) limits simultaneous concurrent in-flight probes, preventing socket starvation and host OS descriptor leaks.

---

## 5. RAG Engine Design: Runbook Chunking & Vector Grounding

### Chunking Strategy
- Markdown runbooks are parsed into semantic chunks of ~500 characters with a 50-character sliding overlap.
- Overlap preserves boundary context across operational commands and troubleshooting instructions.

### Vector Search with Tenant Partitioning
- pgvector stores 1536-dimensional embeddings.
- Every vector query executes:
  ```sql
  SELECT content, runbook_title, 1 - (embedding <=> :queryVector) AS similarity
  FROM runbook_chunks
  WHERE tenant_id = :tenantId
  ORDER BY similarity DESC
  LIMIT 5;
  ```
- Filtering by `tenant_id` at the SQL level ensures vector nearest-neighbor calculations never cross tenant boundaries.

### Prompt Grounding & Output Fallback
- System prompt instructs the model to answer **only** from retrieved runbook context.
- If context is empty or relevance is low, the assistant returns `"No relevant runbook found"`.
- LLM outputs are validated against structured JSON schemas (`IncidentSuggestionResponse`), falling back gracefully if model output is malformed.

---

## 6. Production Roadmap & Future Evolution

If scaling OpsPulse to large-scale enterprise production, the following enhancements are planned:
1. **PostgreSQL Row-Level Security (RLS)**: Enforce database-level tenant isolation natively in PostgreSQL as an extra safeguard beneath Hibernate.
2. **Distributed Rate Limiting (Redis)**: Transition from in-memory token buckets to Redis-backed distributed token buckets across multi-replica gateways.
3. **Kafka Partitioning**: Transition from RabbitMQ to Apache Kafka partitioned by `tenant_id` for massive ingestion throughput of check metrics.
4. **Kubernetes Deployment**: Helm charts and Horizontal Pod Autoscaling (HPA) targeting checker CPU/thread metrics and queue backlogs.
