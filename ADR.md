# StockPulse — Architecture Decision Records (ADR)

This document captures the key architectural decisions for **StockPulse**, an AI-powered inventory and dynamic pricing engine. Each record follows the **Context → Options → Decision → Tradeoffs** format.

---

## ADR-01: Where does commerce logic live?

### Context
In a reactive commerce system, decisions regarding price adjustments and inventory replenishment depend on real-time stock levels, demand velocity, category-wide statistics, and operational constraints. Without clear architectural boundaries, the `ProductService` class risks becoming a "god class" that handles database transactions, event publishing, algorithmic rules, external LLM calls, and state transitions. We need a clean boundary that enforces the Single Responsibility Principle and keeps components testable in isolation.

### Options
1. **Rich Domain Model (`Product` Entity):** Embed pricing algorithms and reorder calculations directly inside the `Product` entity.
   - *Pros:* Keeps data and behavior tightly encapsulated.
   - *Cons:* Domain entities must remain free of external dependencies. Injecting repositories (to compute category average velocity) or HTTP clients (to call LLMs) into JPA entities violates clean architecture and makes persistence brittle.
2. **Service Layer (`ProductService`):** Place all evaluation rules, threshold checks, and LLM integrations directly inside `ProductService`.
   - *Pros:* Fast to implement initially; direct access to Spring repositories.
   - *Cons:* Violates Single Responsibility. Blurs the boundary between core inventory data management and commerce decision intelligence. Unit-testing business rules requires heavy mocking of Spring data layers and web clients.
3. **Dedicated Advisor Strategy Layer (`CommerceAdvisor` Boundary):** Separate concerns into three distinct layers:
   - *Domain Model (`Product`):* Maintains state machine invariants (`ACTIVE`, `PRICE_REVIEW_PENDING`, `OUT_OF_STOCK`) and atomic mutations (`recordSale`, `replenishStock`, `completePricingReview`).
   - *Application Service (`ProductService` / `SuggestionService`):* Coordinates transactions, persistence, and event dispatch.
   - *Advisor Layer (`CommerceAdvisor` interface):* Pure decision engine that receives product context and category statistics, returning advisory recommendations without mutating state directly.

### Decision
We chose **Option 3 (Dedicated Advisor Strategy Layer)**.  
The core domain model ([Product.java](file:///c:/Users/poweroot/StockPulse_Zycus/backend/src/main/java/com/stockpulse/domain/Product.java)) enforces lifecycle transitions and protects invariants. Recommendation intelligence is fully decoupled behind the [CommerceAdvisor.java](file:///c:/Users/poweroot/StockPulse_Zycus/backend/src/main/java/com/stockpulse/service/CommerceAdvisor.java) interface. Both HTTP on-demand endpoints and async event listeners interact with this contract.

### Tradeoffs
- **Cost:** Requires additional DTOs ([AdvisoryResult.java](file:///c:/Users/poweroot/StockPulse_Zycus/backend/src/main/java/com/stockpulse/domain/AdvisoryResult.java)) and routing components compared to inline procedural code.
- **Benefit:** High cohesion and separation of concerns. `RuleBasedCommerceAdvisor` can be 100% unit-tested with zero Spring context or database mocks. The AI strategy can be refactored or swapped out without any risk of breaking inventory persistence.

---

## ADR-02: Unified AI call vs separate pricing/reorder calls?

### Context
When an inventory threshold is crossed or demand spikes, the system must generate two recommendations:
1. **Pricing Adjustment:** Protect depleted stock, capitalize on demand velocity, or run a clearance.
2. **Replenishment Reorder:** Determine the target reorder quantity and lead time days.

We must decide whether to execute two independent AI calls or a single unified AI advisory call.

### Options
1. **Separate Calls (`PricingAdvisor` & `ReorderAdvisor`):** Two discrete prompts and endpoints for pricing and replenishment.
   - *Pros:* Independent failure domains; pricing can succeed even if reorder calculation times out. Smaller, single-purpose prompt templates.
   - *Cons:* Doubles network latency (two serial HTTP roundtrips to the LLM) and doubles token overhead. More critically, it separates decisions that are economically codependent: a merchandiser cannot responsibly set a price without knowing supplier lead times and reorder quantities (e.g., if a reorder takes 30 days, prices should rise to protect stock; if a shipment arrives in 2 days, prices should hold).
2. **Unified Call (`CommerceAdvisor` returning `AdvisoryResult`):** A single prompt supplying product context (current price, stock, threshold, velocity, category average, and trigger reason) and requesting structured JSON containing both pricing and reorder recommendations.
   - *Pros:* Provides the model with holistic context so pricing and replenishment decisions reinforce each other. Cuts LLM latency and API token costs by ~50%. Standardizes the advisory pipeline across the entire system.
   - *Cons:* Requires resilient JSON parsing to handle composite payloads, and an error in the response structure could potentially affect both suggestions if not guarded.

### Decision
We chose **Option 2 (Unified Call)**.  
The [CommerceAdvisor.java](file:///c:/Users/poweroot/StockPulse_Zycus/backend/src/main/java/com/stockpulse/service/CommerceAdvisor.java) contract exposes `AdvisoryResult generateRecommendations(Product product)`. In [AICommerceAdvisor.java](file:///c:/Users/poweroot/StockPulse_Zycus/backend/src/main/java/com/stockpulse/service/AICommerceAdvisor.java), the model receives full situational context and outputs both suggestions in one turn. To mitigate all-or-nothing failure risk, the parsing logic extracts pricing and reorder fields independently, falling back to rule-based defaults if any individual section is invalid.

### Tradeoffs
- **Cost:** Requires a more comprehensive prompt and composite JSON parsing.
- **Benefit:** Halves LLM latency, reduces quota/rate-limit consumption, and produces economically coherent suggestions where pricing and replenishment acknowledge each other.

---

## ADR-03: How does runtime strategy switching work?

### Context
The commerce engine must support both deterministic rule-based logic and AI-powered recommendations. Operators must be able to switch strategies at runtime without modifying code or restarting the Spring Boot JVM. Furthermore, the architecture must allow adding new strategies in Sprint 2 (e.g., `CompetitorAwareStrategy`) as a simple plug-and-play extension.

### Options
1. **Spring Profiles (`@Profile("rule")` vs `@Profile("ai")`):** Strategy determined at startup via active profile.
   - *Pros:* Idiomatic Spring Boot configuration.
   - *Cons:* Inflexible. Switching strategies requires changing JVM arguments or environment variables and restarting the entire application, violating the zero-downtime requirement.
2. **Conditional Bean Registration (`@ConditionalOnProperty`):** Strategy selected via `application.properties`.
   - *Pros:* Clean declarative configuration.
   - *Cons:* Still requires an application restart or context refresh to take effect.
3. **Strategy Registry with Dynamic Router (`CommerceAdvisorSelector`):** Register all `CommerceAdvisor` beans into a Spring-managed map (`Map<String, CommerceAdvisor>`). A routing proxy maintains the active strategy key in a thread-safe variable and exposes an API endpoint to toggle it on the fly.
   - *Pros:* Zero-downtime runtime switching. Both HTTP endpoints and async background workers immediately route to the new strategy. New strategies (like `CompetitorAwareStrategy`) are automatically discovered by Spring's component scan upon class creation.
   - *Cons:* In a multi-instance distributed cluster, state must be stored in a shared store (e.g., Redis or database) rather than an in-memory variable.

### Decision
We chose **Option 3 (Dynamic Strategy Registry)**.  
[CommerceAdvisorSelector.java](file:///c:/Users/poweroot/StockPulse_Zycus/backend/src/main/java/com/stockpulse/service/CommerceAdvisorSelector.java) implements `CommerceAdvisor` and acts as the runtime delegate. It injects available advisor beans and dynamically routes calls based on a configurable strategy identifier (defaulting to `rule-based`). An administrative REST endpoint (`POST /api/commerce-advisor/strategy`) allows toggling between `rule-based` and `ai` at runtime without restarting the server.

### Tradeoffs
- **Cost:** Slightly more routing infrastructure than a direct `@Autowired` binding.
- **Benefit:** Full runtime flexibility, instant strategy switching for live demos, and an open seam for Sprint 2's competitor strategy.

---

## ADR-04: LLM failure handling

### Context
External LLM APIs (Gemini, Groq, Ollama) are inherently non-deterministic. They are susceptible to network latency, connection timeouts, rate-limiting (HTTP 429), malformed JSON responses, and hallucinations (e.g., negative prices, $0, or $999,999). In an asynchronous agentic loop, an unhandled exception or silent drop would leave depleted inventory unnoticed without alerting merchandising.

### Options
1. **Fail-Fast / Drop Trigger:** Throw an exception and terminate processing when the LLM call fails.
   - *Pros:* Simple code path.
   - *Cons:* Unacceptable for an autonomous commerce loop. Silent drops cause stockouts and lost revenue while no one is watching the console.
2. **Retry Loop with Exponential Backoff:** Retry the LLM request up to 3 times on failure.
   - *Pros:* Recovers from transient network blips.
   - *Cons:* Does not resolve quota exhaustion (HTTP 429) or prompt syntax errors. Ties up worker threads and introduces multi-second delays on the async queue.
3. **Strict Bounds Validation with Instantaneous Rule-Based Fallback:**
   - Enforce a 5-second socket timeout on LLM calls.
   - Sanitize and parse JSON output.
   - Validate numerical bounds:
     - Recommended price must be strictly positive and bounded: $0.5 \times \text{currentPrice} \le \text{price} \le 2.5 \times \text{currentPrice}$.
     - Recommended reorder quantity must be an integer: $1 \le \text{quantity} \le 10 \times \text{reorderThreshold}$.
   - If the LLM call fails, times out, outputs unparseable markdown, or produces out-of-bounds numbers, catch the error, log a warning, and immediately invoke `RuleBasedCommerceAdvisor`.
   - Append an audit trail to the reasoning field: `"[Fallback: Rule Engine applied due to AI gateway failure]"`.

### Decision
We chose **Option 3 (Strict Bounds Validation & Rule Fallback)**.  
The [AICommerceAdvisor.java](file:///c:/Users/poweroot/StockPulse_Zycus/backend/src/main/java/com/stockpulse/service/AICommerceAdvisor.java) wraps all LLM interactions in a guarded execution block. Validated outputs are saved with their generated confidence; any anomaly triggers the deterministic rule baseline. The agentic loop guarantees that suggestions are always created and queued for human review.

### Tradeoffs
- **Cost:** Requires writing and maintaining dual-execution paths (AI parser + bounds checker + rule fallback invocation).
- **Benefit:** Zero silent failures. The system is 100% resilient against network outages, provider rate limits, and model hallucinations.

---

## ADR-05: Agentic loop trigger and decoupling

### Context
Recommendations must be generated when stock drops below threshold (`INVENTORY_LOW`) or when sales velocity spikes (`DEMAND_SPIKE`). Sales transactions (`POST /products/{id}/orders`) and stock updates (`PATCH /products/{id}/stock`) represent high-throughput operational paths. If recommendation generation runs synchronously within these requests, the latency of LLM calls (1–3 seconds) will stall checkout requests. Additionally, rapid successive sales could generate duplicate pending suggestions for the same product and trigger.

### Options
1. **Synchronous In-Line Execution:** Execute recommendation logic directly inside `ProductService.recordSale()` before returning the HTTP response.
   - *Pros:* Simple linear execution within a single database transaction.
   - *Cons:* Degrades checkout performance. If the LLM times out or errors, the order request blocks or fails. Violates agentic separation of concerns.
2. **Scheduled Poller (Cron Job):** A periodic background scheduler scanning the database for low stock or high velocity every $N$ minutes.
   - *Pros:* Completely decoupled from API requests.
   - *Cons:* Non-reactive. If inventory depletes immediately after a cron run, the system lags behind by up to $N$ minutes before generating suggestions. Violates the hackathon brief: *"the loop should fire because something changed, not because a timer ticked."*
3. **Event-Driven Asynchronous Decoupling via Spring Events:**
   - `ProductService` updates product state, commits the transaction, and publishes an internal `InventorySignalEvent` via `ApplicationEventPublisher`.
   - The HTTP response returns to the client immediately (<20ms).
   - An `@Async @EventListener` picks up the event in a background thread pool.
   - The event handler checks:
     - **Trigger A (`INVENTORY_LOW`):** Stock level < reorder threshold.
     - **Trigger B (`DEMAND_SPIKE`):** Velocity > 2× category average.
   - **Idempotency Guard:** Checks the repositories for existing `PENDING` suggestions matching the product ID and `TriggerReason`. If an unapproved suggestion already exists, duplicate generation is skipped.

### Decision
We chose **Option 3 (Event-Driven Async with Idempotency Guard)**.  
Stock mutations and sales return immediately. The background listener independently evaluates the triggers, enforces idempotency, calls the active advisor, and queues pending suggestions for human review.

### Tradeoffs
- **Cost:** Introduces eventual consistency. Suggestions appear in the UI on the next polling cycle (or refresh) rather than synchronously in the checkout response. Requires background thread pool configuration.
- **Benefit:** Sub-20ms point-of-sale response times, complete decoupling of transactions from external AI latency, and automated deduplication.

---

## ADR-06: Extensibility and exclusions

### Context
A core goal of the hackathon architecture is demonstrating that the foundation is ready for future commercial extensions (Sprint 2 & 3) while making deliberate, well-justified scoping decisions for the initial 5-hour sprint.

### Decision & Seams

#### 1. Sprint 2 Seams in Code
- **Entity Seams:** [Product.java](file:///c:/Users/poweroot/StockPulse_Zycus/backend/src/main/java/com/stockpulse/domain/Product.java#L45-L54) includes explicitly mapped extension attributes:
  - `costPrice (BigDecimal)`: Establishes an absolute pricing floor so dynamic pricing algorithms never recommend selling below cost.
  - `marginFloor (BigDecimal)`: Enforces minimum gross margin constraints per category.
  - `supplierId (String)`: Foreign reference to supplier catalogs for automated lead times and reorders.
- **Strategy Seams:** The `CommerceAdvisor` interface allows plugging in a `CompetitorAwareStrategy` (ingesting scraped competitor prices) simply by creating the class and registering it in the selector map—with zero modifications to controllers or event handlers.

#### 2. Deliberate Exclusions and Priority Decisions
- **Automated Price Publishing (No Direct Writes):** Excluded by design. StockPulse is an *advisor*, not an unsupervised bot. Prices never go live without human approval in the merchandising console (`Human-in-the-Loop`).
- **Storefront & Payment Cart:** Excluded. ShopStream already operates an e-commerce storefront; StockPulse is exclusively the reactive decision engine behind it.
- **Automated Supplier Purchase Orders:** Reorder suggestions calculate recommended quantities and simulate inbound stock upon approval; direct integration with third-party EDI/supplier APIs is deferred to Sprint 3.
- **SSE Token Streaming Bonus:** Deferred to prioritize rock-solid async event decoupling, dual-trigger idempotency, and resilient rule fallbacks, which carry higher architectural weight.

---

## ADR-07: Human-in-the-Loop (HITL) Checkpoints & Pricing Ambiguity

### Context
When stock drops critically low, merchandising faces a fundamental commercial dilemma:
- **Price Increase (Margin Maximization & Stock Rationing):** Raise prices by 10–20% to slow sales velocity, extract higher margins from remaining inventory, and avoid immediate stockouts.
- **Clearance Discount (Liquidation):** If an item is seasonal, perishable, or declining in demand, discount it to clear warehouse space and eliminate carrying costs.

An automated rule engine cannot know high-level brand strategy or upcoming catalog discontinuations without human oversight.

### Decision
1. **Agentic Checkpoint:** The loop proposes; human merchandisers approve. Live prices and inventory are only mutated when an operator clicks **Accept** on a pending suggestion in the console.
2. **Context-Rich AI Reasoning:** Prompt design explicitly instructs the model to evaluate stock depth against category demand velocity and explain *why* it recommended a price increase, hold, or discount.
3. **Atomic Side-Effects on Accept:**
   - Accepting a `PricingSuggestion` updates `Product.currentPrice` and transitions status from `PRICE_REVIEW_PENDING` back to `ACTIVE`.
   - Accepting a `ReorderSuggestion` simulates an inbound shipment receipt by incrementing `Product.stockLevel` and clearing any `OUT_OF_STOCK` flags.