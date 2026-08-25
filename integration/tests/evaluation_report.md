# RefGuard Evaluation Report

## Summary
- Total Tests: 12
- Passed: 12
- Failed: 0
- Errors: 0
- False Positives (Legitimate flagged as bad): 0
- False Negatives (Malicious flagged as good): 0

## Detailed Results
| Type | Passed | Action (Actual/Expected) | Risk (Actual/Expected) | Content |
|---|---|---|---|---|
| malicious | ✅ | DISCOURAGE_PROCEED / REQUIRE_CONFIRMATION,DISCOURAGE_PROCEED | CRITICAL / HIGH,CRITICAL | http://free-cashback-loot.xyz/claim?ref=998877... |
| malicious | ✅ | DISCOURAGE_PROCEED / REQUIRE_CONFIRMATION,DISCOURAGE_PROCEED | CRITICAL / HIGH,CRITICAL | CBI Alert: Your Aadhaar is linked to illegal money... |
| legitimate | ✅ | ALLOW / ALLOW | LOW / LOW | upi://pay?pa=swiggy@icici&pn=SwiggyOrders&am=350&c... |
| legitimate | ✅ | ALLOW / ALLOW | LOW / LOW | Hey, are we still meeting for lunch at 1 PM?... |
| ambiguous | ✅ | ALLOW / ALLOW,REQUIRE_CONFIRMATION | LOW / LOW,MEDIUM | Your electricity bill of Rs.1200 is due on 15-Sep.... |
| intent_mismatch | ✅ | DISCOURAGE_PROCEED / DISCOURAGE_PROCEED | CRITICAL / CRITICAL | Congratulations! You won 5000 cashback scratch car... |
| qr | ✅ | DISCOURAGE_PROCEED / DISCOURAGE_PROCEED | CRITICAL / CRITICAL | upi://pay?pa=scammer@oksbi&pn=RewardClaim&am=2500&... |
| ocr | ✅ | REQUIRE_CONFIRMATION / REQUIRE_CONFIRMATION,DISCOURAGE_PROCEED | HIGH / HIGH,CRITICAL | VGVsZWdyYW0gVGFzayBFYXJuaW5nIFZJUDogRWFybiA1MDAwIG... |
| url_referral | ✅ | REQUIRE_CONFIRMATION / REQUIRE_CONFIRMATION,DISCOURAGE_PROCEED | HIGH / HIGH,CRITICAL | https://tinyurl.com/free-money-now... |
| vpa | ✅ | DISCOURAGE_PROCEED / DISCOURAGE_PROCEED | CRITICAL / CRITICAL | lottery.winner@paytm... |
| malformed | ✅ | ERROR (Caught) / ERROR | N/A /  | N/A |
| degraded | ✅ | DISCOURAGE_PROCEED / REQUIRE_CONFIRMATION,DISCOURAGE_PROCEED | CRITICAL / HIGH,CRITICAL | Join my Telegram VIP group for guaranteed crypto r... |
