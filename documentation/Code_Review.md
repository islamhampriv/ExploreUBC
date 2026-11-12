# Code Review

## 1. Change History

| **Change Date**   | **Modified Sections** | **Rationale** |
| ----------------- | --------------------- | ------------- |
| _Nothing to show_ |

---

## 2. Automated Code Review Results

### 2.1. Commit Hash Where Codacy Ran

`[Insert Commit SHA here]`

### 2.2. Unfixed Issues per Codacy Category

_(Placeholder for screenshots of Codacy's Category Breakdown table in Overview)_

### 2.3. Unfixed Issues per Codacy Code Pattern

_(Placeholder for screenshots of Codacy's Issues page)_

### 2.4. Justifications for Unfixed Issues

- **Code Pattern: [Usage of Deprecated Modules](#)**

  1. **Issue**

     - **Location in Git:** [`src/services/chatService.js#L31`](#)
     - **Justification:** ...

  2. ...

- ...

---

## 3. Manual Code Review Results

### 3.1. Review You Performed

- **Peer group reviewed:** Trailmix
- **Commit SHA reviewed:** `6d12cd1496c569073a144350f8fe535b93f53d54`
- **Date the artifacts were exchanged:** Nov 4, 2026
- **Review report submitted:** [`<CP#>_<YourPeerGroupName>_Review.pdf`](#)

| **Section**          | **Item**                                                                                                                                             | **Score** |
| -------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------- | --------- |
| Manual code review | Code is maintainable, efficient, and handles error cases well | **8/10** |
| Manual test review | Tests are complete, errors and edge cases are thoroughly tested, and correct assertions are used | **8/10** |
| Manual test review | Test code is maintainable and well-structured | **8/10** |
| Manual test review | Test implementation matches the requirements and design | **7/10** |
| Manual test review | Non-functional requirements are tested well | **3/10** |
| Manual test review | Tests achieve high coverage and cases of less than 100% coverage are well-justified | **9/10** |
| Manual test review | All back-end tests can be run automatically | **8/10** |
| Automated code review| Codacy runs with the required setup                                                                                                                    | **10/10**      |
| Automated code review | All remaining Codacy issues are well-justified | **9/10** |
|  | **Total** | **70/90** |

**Major fault reported**

- **Description:** The backend has no request rate limiting or throttling at either the Express or Caddy layer. A single signed-in user can therefore send an unbounded number of authenticated Planner requests. Each accepted `POST /api/routes/plan` request can fan out into multiple Google Route Matrix and route-detail calls, including retries, so an attacker can consume the project's Google API quota, create unexpected API charges, and deny route planning to legitimate users. The same lack of throttling also applies to Google sign-in and all user-data endpoints. Neither `backend/src/app.ts`, `backend/package.json`, nor `deploy/Caddyfile` configures a rate limiter.
- **Steps to reproduce:**
  1. Run the backend and test MongoDB locally, then obtain a valid session token for a test account.
  2. Send a burst of concurrent authenticated requests to a cheap endpoint such as `GET /api/users/me`. This safely verifies the common middleware behavior without consuming Google quota.
  3. Record the response status of every request. The review reproduction sent 100 requests concurrently through `createApp()` and received `{"200":100}`: every request was accepted and none returned `429 Too Many Requests`.
  4. Inspect `backend/src/app.ts` and `deploy/Caddyfile`; there is no per-IP or per-session request limit, burst limit, or backoff response.
  5. The same authenticated path applies to `POST /api/routes/plan`. Do not load-test the deployed endpoint: repeated valid Planner requests would reach the billable Google Routes integration and demonstrate the damaging form of the vulnerability.
- **Screenshots:** The terminal reproduction output was `{"200":100}`, showing that all 100 concurrent requests succeeded with no `429` response. No graphical screenshot is required to establish the server behavior. Supporting code evidence is the absence of rate-limiting middleware in `backend/src/app.ts` and the unrestricted `reverse_proxy backend:3000` configuration in `deploy/Caddyfile`.
- **Severity:** **High.** Exploitation requires only a normal Google account and can affect every user by exhausting shared Google Routes quota or backend capacity. The attack is remotely repeatable, has no enforced request ceiling, and may also create financial impact through billable API calls. Recommended remediation is per-IP and per-session throttling, with a strict limit on `/api/auth/google` and `/api/routes/plan`, `429` responses with `Retry-After`, Google Cloud quota/budget alerts, and automated tests for burst rejection.

### 3.2. Review You Received

- **Reviewing group:** [WRITE_HERE]
- **Commit SHA that was reviewed:** `[Insert Commit SHA here]`
- **Review report received:** [`<CP#>_<YourGroupName>_Review.pdf`](#)

| **Section**          | **Item**                                                                                                                                             | **Score** |
| -------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------- | --------- |
| Manual code review   | Code is maintainable, efficient, handles error cases well                                                                                               | x/10      |
| Manual test review   | Tests are complete, errors and edge cases are thoroughly tested, correct assertions are used                                                            | x/10      |
| Manual test review   | Test code is maintainable and well-structured                                                                                                          | x/10      |
| Manual test review   | Test implementation matches the requirements and design                                                                                                | x/10      |
| Manual test review   | Non-functional requirements are tested well                                                                                                            | x/10      |
| Manual test review   | Tests achieve high coverage and cases of < 100% coverage are well-justified                                                                            | x/10      |
| Manual test review   | All back-end tests can be run automatically                                                                                                            | x/10      |
| Automated code review| Codacy runs with the required setup                                                                                                                    | x/10      |
| Automated code review| All remaining Codacy issues are well-justified                                                                                                          | x/10      |

**Major fault reported against your project**

- **Description:** ...
- **Severity:** ...

### 3.3. Actions Taken in Response to the Review

| **Finding** | **Action Taken** | **Commit / Location in Git** |
| ----------- | ---------------- | ---------------------------- |
| ...         | ...              | [`src/...`](#)               |
| ...         | ...              | ...                          |

- **Findings not acted on.** For each finding you decided not to address, give the reason:
  1. **Finding:** ...
     - **Justification:** ...
