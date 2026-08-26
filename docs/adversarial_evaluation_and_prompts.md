# RefGuard — Adversarial Test Set & AI Studio Prompt Sequence

Two things in this doc:
1. **50 adversarial test cases** — run these through RefGuard *before* submission to find failure modes before a judge does.
2. **AI Studio prompt sequence** — a step-by-step set of prompts to generate training data, build/refine the edge classifier, and evaluate it, so you walk into judging with real precision/recall numbers instead of vibes.

---

## PART 1 — Adversarial Test Set (50 cases)

Organized by your existing MVP flows (A–H). For each case: **input**, **expected verdict**, **why it's tricky**. Run all 50, log actual vs expected in a spreadsheet, and compute accuracy per category — that spreadsheet becomes a real evaluation artifact for your pitch.

### A. Fake Viral Referral (Text/URL) — 8 cases

| # | Input | Expected | Why it's tricky |
|---|---|---|---|
| A1 | "🎉 You got ₹500! Claim your referral bonus now: bit.ly/xR7pQz2" | REQUIRE_CONFIRMATION / DISCOURAGE | Shortlink hides destination; classic urgency+reward pattern |
| A2 | "Your friend Priya invited you to earn ₹200 cashback. Download: paytm-rewards.in/claim" | DISCOURAGE | Typosquat domain mimicking a real brand (paytm-rewards.in ≠ paytm.com) |
| A3 | "Refer 3 friends and get a free iPhone! Limited slots: freegift-app.co" | DISCOURAGE | No urgency language, tests if model relies only on keywords vs. structural pattern |
| A4 | "Hey, check out this app my college uses for attendance: college-attend.edu.in" | ALLOW | Legit-sounding .edu.in domain, plain referral — should NOT be flagged (false-positive test) |
| A5 | "You're invited to join our family WhatsApp group for Diwali planning" | ALLOW | Zero payment/URL context — pure negative control |
| A6 | "Last chance! Referral bonus expires in 10 mins. Click now: rewardz-app.xyz/go" | DISCOURAGE | .xyz TLD + countdown urgency — should trigger multiple heuristics at once |
| A7 | "Congratulations, you are selected for cashback of ₹5000 from [well-known bank]" (no link) | REQUIRE_CONFIRMATION | No URL present — tests if model can flag on text alone |
| A8 | Same as A1 but sent in Tamil ("உங்களுக்கு ₹500 கிடைத்தது! இப்போது கிளைம் செய்யுங்கள்: bit.ly/xR7pQz2") | REQUIRE_CONFIRMATION / DISCOURAGE | Regional-language equivalent — tests whether detection logic is language-dependent or pattern-dependent |

### B. Tampered QR Code (`upi://pay`) — 8 cases

| # | Input (decoded UPI URI) | Expected | Why it's tricky |
|---|---|---|---|
| B1 | `upi://pay?pa=scammer123@oksbi&pn=Refund&am=1&cu=INR` | DISCOURAGE (CRITICAL) | Classic "pay ₹1 to receive refund" inversion trap |
| B2 | `upi://pay?pa=merchant.legit@okhdfcbank&pn=Cafe%20Coffee%20Day&am=250&cu=INR` | ALLOW | Legitimate-looking merchant VPA — false-positive control |
| B3 | `upi://pay?pa=cashback.claim@okaxis&pn=CashbackTeam&am=10&cu=INR` | DISCOURAGE | Low amount ("just ₹10") designed to feel harmless — tests if model over-indexes on amount size |
| B4 | `upi://pay?pa=9876543210@ybl&pn=Unknown&am=0&cu=INR` | REQUIRE_CONFIRMATION | Zero-amount collect request — ambiguous, real edge case |
| B5 | Malformed QR text (not valid UPI URI): `upi:/pay?pa=test` (missing slash) | 400 / graceful handling | Tests robustness, not just detection — should not crash |
| B6 | `upi://pay?pa=support@paytm&pn=PaytmSupport&am=1&cu=INR` (VPA impersonating known brand handle) | DISCOURAGE | Brand-impersonation VPA — tests handle-level heuristics, not just domain |
| B7 | Same as B1 but QR image is slightly blurry/rotated (if testing via camera scan) | DISCOURAGE (assuming OCR/scan succeeds) | Tests OCR/QR-read robustness under real-world (non-lab) image conditions |
| B8 | `upi://pay?pa=friend.name@oksbi&pn=Rahul&am=500&cu=INR` (amount matches a plausible real repayment context) | REQUIRE_CONFIRMATION or ALLOW | Genuinely ambiguous — a real friend repayment looks identical structurally to a scam. This is the hardest case in the set; document how RefGuard handles genuine ambiguity |

### C. Screenshot / Image OCR — 6 cases

| # | Input description | Expected | Why it's tricky |
|---|---|---|---|
| C1 | Screenshot of WhatsApp message: "Complete this task and earn ₹300/hour, DM for details" | REQUIRE_CONFIRMATION (HIGH) | Advance-fee/task-scam pattern, text-only via OCR |
| C2 | Screenshot of a real bank SMS: "₹2000 debited from A/C XX1234 on 26-Aug for UPI/merchant" | ALLOW (informational) | Legitimate transaction notification — false-positive control |
| C3 | Screenshot with low contrast / small font (stress-test OCR accuracy itself) | Should still extract text reasonably | Tests OCR failure mode, not fraud logic |
| C4 | Screenshot containing a scam message overlaid on a meme/image (text partially obscured by graphics) | REQUIRE_CONFIRMATION or degraded-confidence flag | Tests OCR robustness against non-plain-text backgrounds |
| C5 | Screenshot of a fake "KYC update" SMS with a link: "Your bank account will be blocked. Update KYC: sbi-kyc-update.info" | DISCOURAGE | Classic fear-based (not reward-based) manipulation — tests if model detects fear triggers, not just greed triggers |
| C6 | Screenshot in Hindi/Devanagari script scam message | REQUIRE_CONFIRMATION / DISCOURAGE | Tests OCR + classifier on non-Latin script |

### D. High-Risk UPI VPA — 6 cases

| # | Input | Expected | Why it's tricky |
|---|---|---|---|
| D1 | VPA present in your community registry as previously reported | DISCOURAGE (CRITICAL) | Baseline registry-match test |
| D2 | VPA with deceptive keyword: `refund.helpdesk@oksbi` | DISCOURAGE | Keyword heuristic test |
| D3 | Brand new VPA, no history, no keywords, but used in a scam-pattern message (from category A) | Should inherit risk from message context, not VPA alone | Tests whether VPA-only and message-only signals are properly fused |
| D4 | Well-known real merchant VPA (e.g. a large e-commerce brand's actual payment handle) | ALLOW | False-positive control against your whitelist |
| D5 | VPA that's a random string of numbers with no keyword: `7823910456@ybl` | REQUIRE_CONFIRMATION (context-dependent) | Ambiguous alone — realistic personal VPA also looks like this |
| D6 | VPA typo-adjacent to a known-bad one in your registry (off by one character) | Ideally still DISCOURAGE if fuzzy-matched, otherwise document as a known gap | Tests fuzzy-matching robustness of the community registry |

### E. Payment-Intent Mismatch — 6 cases

| # | Scenario | Expected | Why it's tricky |
|---|---|---|---|
| E1 | Message says "you're receiving ₹500" but underlying UPI action is an outbound debit request | DISCOURAGE ("DO NOT enter UPI PIN") | Your flagship feature — must be bulletproof |
| E2 | Message says "pay ₹500 for your order" and UPI action is genuinely an outbound debit (correctly aligned) | ALLOW | Negative control — mismatch detector must not fire when intent and action actually match |
| E3 | Message is ambiguous about direction ("₹500 UPI request from Rahul") with a real debit request | REQUIRE_CONFIRMATION | Genuinely unclear natural language — good test of graceful uncertainty handling |
| E4 | Message in Tamil claiming a refund, paired with a debit UPI URI | DISCOURAGE | Cross-language version of E1 |
| E5 | Message claims "processing fee refund" (still a mismatch, but scam script differs from E1's wording) | DISCOURAGE | Tests generalization beyond the exact scam-script wording your model may have been tuned on |
| E6 | Message explicitly states "this will deduct money from your account" and the UPI action matches (transparent, honest debit) | ALLOW | Extreme negative control — an honest debit request should never be flagged as a mismatch |

### F/G/H — Legitimate / Malformed / Offline — 6 cases

| # | Input | Expected | Why it's tricky |
|---|---|---|---|
| F1 | QR from a real, whitelisted large retailer | ALLOW (LOW, score < 30) | Whitelist sanity check |
| G1 | Completely garbage/non-UTF8 input | 400 Bad Request, no crash | Robustness, not detection |
| G2 | Empty string input | 400 Bad Request, no crash | Robustness edge case |
| G3 | Extremely long input (10,000+ characters, stress test) | Should not hang or crash | Performance/robustness under load |
| H1 | Any valid scam input, submitted with device in airplane mode | Should queue as SuccessOffline, classify locally via edge classifier, sync later | Core offline-first claim — this MUST work live in a demo with wifi off |
| H2 | Two conflicting reports on the same VPA arriving during offline queue, synced on reconnect | Should resolve without data corruption | Sync-conflict edge case, low priority but good to know the behavior |

---

## PART 2 — AI Studio Prompt Sequence (Edge Classifier Build & Evaluation)

Use these in sequence inside Google AI Studio. Each prompt builds on the last — copy the **[COPY]** blocks directly.

### Step 1 — Generate labeled synthetic training data

**[COPY into AI Studio]**
```
You are helping build a training dataset for a UPI/referral scam text classifier for an Android app called RefGuard.

Generate 150 labeled examples in JSON array format, each with fields:
- "text": a realistic message a user might receive via SMS/WhatsApp in an Indian context
- "label": "scam" or "legit"
- "subtype": one of ["fake_referral", "payment_intent_mismatch", "kyc_fear", "task_scam", "tampered_qr_context", "legit_transaction", "legit_referral", "legit_personal_message"]

Requirements:
- Roughly balanced between scam and legit (70 scam, 80 legit)
- Vary language style: formal, casual, broken English, Hinglish
- Include some legit messages that superficially resemble scams (e.g. real cashback offers from known brands, real bank notifications) as hard negatives
- Include some scam messages with NO obvious keywords like "urgent" or "free" — subtle social-engineering phrasing only
- Do not repeat sentence structures more than 3 times
- Output ONLY the JSON array, no explanation
```

### Step 2 — Generate regional-language variants

**[COPY into AI Studio]**
```
Take the 70 "scam" examples from the dataset you just generated. For each, produce a natural (not machine-translated-sounding) Tamil and Hindi equivalent, keeping the same label and subtype. Output as JSON with fields: "text_en", "text_ta", "text_hi", "label", "subtype".
```

### Step 3 — Held-out adversarial test set (separate from training data)

**[COPY into AI Studio]**
```
Generate a SEPARATE set of 40 additional examples, structurally different from a typical training set — these are meant to be a held-out test set to evaluate a classifier's generalization, not to train on.

Focus specifically on:
- 10 examples using scam tactics NOT common in most public scam datasets (emerging/novel phrasing)
- 10 "hard negative" legit examples that use urgency or money-related language for legitimate reasons (e.g. "your subscription renews in 2 days", "urgent: submit assignment by 5pm")
- 10 borderline/ambiguous examples where even a human would hesitate
- 10 examples mixing English and regional language in the same message (code-switching, common in real Indian SMS)

Output as JSON: "text", "label", "difficulty" (easy/medium/hard).
```

### Step 4 — Model architecture guidance prompt

**[COPY into AI Studio]**
```
I'm building an on-device (offline-capable) text classifier for an Android app in Kotlin, meant to run fast on-device with no internet dependency (file: LocalEdgeClassifier.kt). It needs to classify short SMS/message text as scam-risk or legit, and ideally output a 0-100 risk score with a short list of contributing factors (e.g. "urgency language +15", "payment mismatch +40") for explainability.

Given these constraints (on-device, low-latency, explainable output, Android/Kotlin), recommend:
1. Whether a lightweight trained model (e.g. TF-Lite text classifier) or a transparent rule/feature-weighted scoring system is more appropriate for a hackathon MVP timeline, and why
2. If a trained model: what feature representation (TF-IDF, small embedding, n-gram) fits on-device constraints
3. If a feature-weighted system: a concrete starter list of 15-20 weighted features (keywords, structural patterns, entity presence) with suggested weights, designed to be explainable in a live demo
4. How to combine both approaches if time allows (model score + rule-based explainability layer on top)

Be concrete and implementation-ready, not theoretical.
```

### Step 5 — Evaluation prompt (after you've run the model/rules against the datasets)

**[COPY into AI Studio, after pasting your actual results]**
```
Here are my classifier's predictions vs. actual labels on a held-out test set: [paste your results as a table or JSON: text, true_label, predicted_label, predicted_score]

Calculate:
1. Overall accuracy, precision, recall, F1 for the "scam" class
2. A confusion matrix
3. Identify the 3-5 most common failure patterns (what kind of inputs does it get wrong most often)
4. Suggest specific feature or threshold adjustments to fix the biggest failure pattern first
```

### Step 6 — Judge Q&A prep prompt

**[COPY into AI Studio]**
```
I'm presenting a UPI fraud detection app at a hackathon. My classifier achieved [INSERT YOUR ACTUAL NUMBERS] precision/recall on a held-out test set of 40 adversarial examples I built specifically to avoid overfitting to my training data.

Generate 10 tough technical questions a skeptical judge might ask about this classifier (e.g. about overfitting, dataset size, generalization, bias, real-world deployment), and draft a concise, honest one-to-two-sentence answer for each that doesn't overclaim.
```
