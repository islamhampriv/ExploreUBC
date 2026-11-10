# CP3 TrailMix Review

**Repository revision reviewed:** `6d12cd1496c569073a144350f8fe535b93f53d54`  
**Requirements reviewed:** `documentation/Requirements_and_Design.md`  
**Testing specification reviewed:** `documentation/Testing.md`  
**Automated-review justification reviewed:** `documentation/Code_Review.md`

## Score summary

| Review area | Criterion | Score |
| --- | --- | ---: |
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

## Major fault reported

- **Description:** The backend has no request rate limiting or throttling at either the Express or Caddy layer. A single signed-in user can therefore send an unbounded number of authenticated Planner requests. Each accepted `POST /api/routes/plan` request can fan out into multiple Google Route Matrix and route-detail calls, including retries, so an attacker can consume the project's Google API quota, create unexpected API charges, and deny route planning to legitimate users. The same lack of throttling also applies to Google sign-in and all user-data endpoints. Neither `backend/src/app.ts`, `backend/package.json`, nor `deploy/Caddyfile` configures a rate limiter.
- **Steps to reproduce:**
  1. Run the backend and test MongoDB locally, then obtain a valid session token for a test account.
  2. Send a burst of concurrent authenticated requests to a cheap endpoint such as `GET /api/users/me`. This safely verifies the common middleware behavior without consuming Google quota.
  3. Record the response status of every request. The review reproduction sent 100 requests concurrently through `createApp()` and received `{"200":100}`: every request was accepted and none returned `429 Too Many Requests`.
  4. Inspect `backend/src/app.ts` and `deploy/Caddyfile`; there is no per-IP or per-session request limit, burst limit, or backoff response.
  5. The same authenticated path applies to `POST /api/routes/plan`. Do not load-test the deployed endpoint: repeated valid Planner requests would reach the billable Google Routes integration and demonstrate the damaging form of the vulnerability.
- **Screenshots:** The terminal reproduction output was `{"200":100}`, showing that all 100 concurrent requests succeeded with no `429` response. No graphical screenshot is required to establish the server behavior. Supporting code evidence is the absence of rate-limiting middleware in `backend/src/app.ts` and the unrestricted `reverse_proxy backend:3000` configuration in `deploy/Caddyfile`.
- **Severity:** **High.** Exploitation requires only a normal Google account and can affect every user by exhausting shared Google Routes quota or backend capacity. The attack is remotely repeatable, has no enforced request ceiling, and may also create financial impact through billable API calls. Recommended remediation is per-IP and per-session throttling, with a strict limit on `/api/auth/google` and `/api/routes/plan`, `429` responses with `Retry-After`, Google Cloud quota/budget alerts, and automated tests for burst rejection.
