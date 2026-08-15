# RefGuard Hackathon Demo Script

**Duration:** 3–5 minutes
**Objective:** Showcase the multi-stage threat analysis and payment intent mismatch capabilities of RefGuard.

---

## 1. Introduction (0:00 - 0:45)
**Presenter:**
"Hello everyone. Today we are presenting RefGuard, an AI-powered scam shield. Scammers are getting smarter, using social engineering to trick users into sending money when they think they are receiving it. We built RefGuard to detect these anomalies instantly and advise users *before* they make a mistake."

---

## 2. Scenario A: The Refund Scam (0:45 - 2:30)

**Presenter:**
"Let's look at a common scenario. Our user receives this message on WhatsApp:"

> *"Congratulations! You are eligible for a ₹1000 cashback. Click this link to claim. Pay ₹5 verification fee through UPI."*

**Action:**
1. The user copies the text or clicks the link. RefGuard intercepts the intent.
2. Open the RefGuard Demo UI (or API console) and submit the text.

**Presenter:**
"Watch how RefGuard processes this request:"

- **Extraction & Threat Analysis:** "First, the AI extracts the entities. It notices the keywords 'cashback' and 'verification fee'."
- **Payment Intent Mismatch:** "This is the core of the scam. The *stated intent* is 'receiving money' (cashback). However, the *actual payment action* requested is an 'outbound debit' (paying a fee). RefGuard detects this mismatch instantly."
- **Scam Chain & Risk Score:** "The system links this pattern to known refund scams. The Risk Engine assigns a `CRITICAL` risk severity."
- **Protection Recommendation:** "Finally, the UI displays a massive red warning: `DISCOURAGE_PROCEED`. It explains to the user *why* it matters: 'You are about to send money, not receive it.'"

**Action:** Show the JSON response or the UI mockup highlighting the `DISCOURAGE_PROCEED` action.

---

## 3. Scenario B: A Legitimate Transaction (2:30 - 3:30)

**Presenter:**
"But what about false positives? If the user just wants to pay a friend for dinner, we shouldn't block them."

**Action:**
1. Submit a safe text string or a legitimate VPA:
> *"Hey, can you send the ₹500 for dinner to rohit@upi?"*

**Presenter:**
"Here, the intent is clear and matches the action. The risk severity evaluates to `LOW`, and the system issues an `ALLOW` decision. The user proceeds without unnecessary friction."

---

## 4. Conclusion (3:30 - 4:00)

**Presenter:**
"RefGuard operates via strict API contracts, meaning our AI analysis is completely decoupled from the payment execution layer. It advises, it protects privacy, and it stops social engineering at the source. Thank you."
