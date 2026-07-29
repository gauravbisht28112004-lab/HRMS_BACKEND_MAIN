# Finbud HRMS — Go-Live Reset Runbook

**Scope chosen:** Full trial reset — wipes `attendance`, `payroll`, `leave_requests`, `regularization_requests`, and re-seeds clean 2026 `leave_balances`. Keeps employees, users, roles, departments, salary structures, shifts, locations, holidays, config.

**Script to run:** `reset_trial_data_pre_golive.sql` (already in the repo, validated against the live schema on 24 Jun 2026 — no SQL changes needed).

**Run as:** a human, in `psql`, on the **production** database. Do not add this file to `src/main/resources/db/migration` — it is not a Flyway migration and must never run on app boot.

---

## The one thing that breaks "delete today, fresh tomorrow"

Your app has a scheduler `autoMarkAbsent` at **00:30 IST every day** that marks the *previous* day absent for anyone who didn't punch. So:

- If you wipe today (24th), the 00:30 job on the 25th re-inserts ABSENT rows for the 24th. Your table is dirty again on go-live morning.
- Your own data shows staff punching as early as **04:15**, so "run it before staff punch in" can mean before ~04:00 — a very tight window.

**Pick ONE timing strategy:**

- **Option A — Freeze schedulers (recommended).** Temporarily disable the attendance + payroll schedulers, run the reset the evening of the 24th after everyone has left, re-enable on the 25th. Avoids the early-puncher problem entirely. The crons are externalized (`app.scheduler.*`), so you can disable via config/env without a code change — coordinate with whoever owns the deploy.
- **Option B — Tight morning window.** Run on the 25th *after* 00:30 and *before* the earliest punch. Riskier given the 04:15 punchers.

---

## Steps

### 0. Backup — mandatory, no undo after commit
```bash
pg_dump "$DATABASE_URL" -Fc -f finbud_hrms_pre_reset_2026-06-25.dump
ls -lh finbud_hrms_pre_reset_2026-06-25.dump   # confirm it exists and size is sane
```

### 1. Freeze (Option A) or confirm window (Option B)
- Option A: set the attendance + payroll scheduler crons to disabled (or stop the scheduled job), confirm no scheduler will fire during the reset.
- Verify no staff are punching right now.

### 2. Run the script
```bash
psql "$DATABASE_URL" -f reset_trial_data_pre_golive.sql
```
It runs inside a transaction and stops before commit. Read the **pre-counts** it prints.

### 3. Verify the post-counts (printed by the script)
Expect:
- `attendance` = 0
- `regularization_requests` = 0
- `payroll` = 0
- `leave_requests` = 0
- `leave_balances` = your **ACTIVE** employee count
- `active employees (expected)` = the same number

**Check that the active-employee count matches your real headcount.** Only `ACTIVE` employees get a 2026 balance (same as the app's own yearly job). Anyone in `ON_NOTICE` or `SUSPENDED` will have no balance row and will hit errors if they apply for leave — fix their status before go-live if they should be able to take leave.

### 4. Commit or roll back
- Counts right → type `COMMIT;`
- Anything wrong → type `ROLLBACK;` (undoes everything, nothing lost)

### 5. Clear caches
- Prod config suggests Redis may not be enabled on Railway. If you do run Redis: `redis-cli -h <host> -p <port> FLUSHDB`
- Otherwise restart the app so no stale attendance/leave values linger in memory.

### 6. Payslip PDFs (only if the trial generated any)
Deleting `payroll` rows does **not** delete payslip PDFs in object storage (`payroll.payslip_url`). Delete those from your bucket separately.

### 7. Re-enable schedulers + smoke test
- Re-enable the schedulers you froze in step 1.
- One employee punches in/out → row appears.
- One employee applies for leave → balance decrements from 6.00.

---

## Heads-up: June payroll will be a partial month

You're wiping 1–24 June trial attendance and going live on the 25th, so June has real attendance only from the 25th onward. The payroll job runs **01:00 on the 1st of the month**, so on **1 July** it will compute June pay from ~6 days of data.

Decide before 1 July how June is handled — manual, prorated, or skip the auto-run — or it will generate wrong June payslips. This is a process decision, not a script bug.

---

## Rollback (if something is wrong after commit)
Restore the step-0 dump into a fresh/empty database and re-point the app, or restore over the existing DB if you know what you're doing:
```bash
pg_restore --clean --if-exists -d "$DATABASE_URL" finbud_hrms_pre_reset_2026-06-25.dump
```

---

## What was validated (24 Jun 2026)
- Only `regularization_requests` has an FK to `attendance` — script deletes it first. Correct.
- `payroll` and `leave_requests` have no FK children — straight deletes are safe.
- Leave-balance re-seed: all 12 inserted columns exist on the `LeaveBalance` entity, no NOT-NULL column omitted, values (6.00 / 6.00) match the real allocator constants.
- "ACTIVE only" matches `employeeRepository.findAllByStatus(ACTIVE)` in the yearly job.
- Scheduler crons confirmed: auto-absent `0 30 0 * * ?`, missing-punch `0 0 21 * * ?`, attendance-close `0 59 23 * * ?`, payroll `0 0 1 1 * ?`.
