const assert = require('assert');
const path = require('path');
const RefGuardPipeline = require('../src/pipeline');
const SchemaValidator = require('../src/validator');

describe('RefGuard Full-System End-to-End Integration Suite', () => {
  let pipeline;
  let validator;

  beforeEach(() => {
    pipeline = new RefGuardPipeline();
    validator = new SchemaValidator(path.resolve(__dirname, '../../contracts/schemas'));
  });

  function validateResponse(res, scenarioName) {
    const v = validator.validate('scan-response.json', res);
    assert.strictEqual(v.valid, true, '[' + scenarioName + '] Schema validation failed: ' + (v.errors ? v.errors.join('; ') : ''));
  }

  // 1. URL / Referral Scan Scenario
  it('E2E-1: Should analyze malicious viral referral URL and detect high risk', () => {
    const request = {
      content_type: 'URL',
      content_value: 'http://free-cashback-loot.xyz/claim?ref=998877',
      source_context: 'com.whatsapp',
      timestamp: new Date().toISOString()
    };

    const response = pipeline.processScan(request);
    validateResponse(response, 'E2E-1: URL/Referral');

    assert.strictEqual(typeof response.scan_id, 'string');
    assert.ok(response.risk_assessment.risk_score >= 60, 'Risk score should be >= 60, got ' + response.risk_assessment.risk_score);
    assert.ok(['HIGH', 'CRITICAL'].includes(response.risk_assessment.risk_severity));
    assert.ok(response.scam_chain.nodes.length >= 2, 'Scam chain should have nodes');
    assert.ok(response.evidence_pack.items.length >= 2, 'Evidence pack should have items');
  });

  // 2. Tampered QR Code Scan Scenario
  it('E2E-2: Should analyze tampered QR UPI payload and flag fraudulent collect', () => {
    const request = {
      content_type: 'QR',
      content_value: 'upi://pay?pa=scammer@oksbi&pn=RewardClaim&am=2500&cu=INR',
      source_context: 'com.google.android.apps.nbu.paisa.user',
      timestamp: new Date().toISOString()
    };

    const response = pipeline.processScan(request);
    validateResponse(response, 'E2E-2: QR Scan');

    assert.ok(response.risk_assessment.risk_score >= 80, 'Risk score for known bad VPA should be >= 80');
    assert.strictEqual(response.risk_assessment.risk_severity, 'CRITICAL');
    assert.strictEqual(response.protection_decision.action, 'DISCOURAGE_PROCEED');
    assert.ok(response.payment_intent_mismatch.status !== 'UNKNOWN');
    assert.strictEqual(response.payment_intent_mismatch.payment_direction, 'OUTBOUND_DEBIT');
  });

  // 3. Screenshot / Share Input Scenario
  it('E2E-3: Should parse base64 simulated screenshot and extract phishing triggers', () => {
    const simulatedOcrText = 'Telegram Task Earning VIP: Earn 5000 daily by liking videos. Contact wa.me/919876543210 ref=TASK99';
    const b64 = Buffer.from(simulatedOcrText).toString('base64');

    const request = {
      content_type: 'IMAGE',
      content_value: b64,
      source_context: 'com.android.gallery',
      timestamp: new Date().toISOString()
    };

    const response = pipeline.processScan(request);
    validateResponse(response, 'E2E-3: Screenshot');

    assert.ok(response.risk_assessment.risk_score >= 60);
    assert.ok(response.risk_assessment.signals.some(s => s.toLowerCase().includes('task') || s.toLowerCase().includes('telegram') || s.toLowerCase().includes('referral')));
  });

  // 4. High-Risk UPI VPA Scenario
  it('E2E-4: Should detect reported high-risk UPI VPA and advise protection', () => {
    const request = {
      content_type: 'UPI_VPA',
      content_value: 'lottery.winner@paytm',
      source_context: 'com.refguard.manual',
      timestamp: new Date().toISOString()
    };

    const response = pipeline.processScan(request);
    validateResponse(response, 'E2E-4: High Risk UPI');

    assert.ok(response.risk_assessment.risk_score >= 80);
    assert.strictEqual(response.protection_decision.action, 'DISCOURAGE_PROCEED');
  });

  // 5. Payment-Intent Mismatch Scenario
  it('E2E-5: Should detect critical mismatch when user is told they are receiving prize but UPI is debit', () => {
    const request = {
      content_type: 'TEXT',
      content_value: 'Congratulations! You won 5000 cashback scratch card. Enter UPI PIN to claim: upi://pay?pa=rewards.collect@ybl&am=5000',
      source_context: 'com.whatsapp',
      timestamp: new Date().toISOString()
    };

    const response = pipeline.processScan(request);
    validateResponse(response, 'E2E-5: Intent Mismatch');

    assert.strictEqual(response.payment_intent_mismatch.status, 'DETECTED');
    assert.strictEqual(response.payment_intent_mismatch.payment_direction, 'OUTBOUND_DEBIT');
    assert.strictEqual(response.payment_intent_mismatch.amount, 5000);
    assert.strictEqual(response.risk_assessment.risk_severity, 'CRITICAL');
    assert.strictEqual(response.protection_decision.action, 'DISCOURAGE_PROCEED');
    assert.ok(response.protection_decision.user_instruction.includes('UPI PIN'));
  });

  // 6. Legitimate Input Scenario
  it('E2E-6: Should allow verified merchant transaction with LOW risk', () => {
    const request = {
      content_type: 'QR',
      content_value: 'upi://pay?pa=swiggy@icici&pn=SwiggyOrders&am=350&cu=INR',
      source_context: 'com.swiggy.consumer',
      timestamp: new Date().toISOString()
    };

    const response = pipeline.processScan(request);
    validateResponse(response, 'E2E-6: Legit Merchant');

    assert.strictEqual(response.risk_assessment.risk_severity, 'LOW');
    assert.strictEqual(response.protection_decision.action, 'ALLOW');
    assert.ok(response.risk_assessment.risk_score < 30, 'Risk score should be < 30, got ' + response.risk_assessment.risk_score);
  });

  // 7. Malformed / Invalid Input Scenario
  it('E2E-7: Should reject malformed request with structured 400 error', () => {
    const invalidRequest = {
      content_type: 'INVALID_TYPE_XYZ',
      timestamp: new Date().toISOString()
    };

    assert.throws(() => {
      pipeline.processScan(invalidRequest);
    }, (err) => {
      assert.strictEqual(err.statusCode, 400);
      assert.ok(err.message.includes('Invalid ScanRequest schema'));
      return true;
    });
  });

  // 8. Offline State Support Scenario
  it('E2E-8: Should support offline pipeline queuing and local community report ingestion', () => {
    const report = {
      report_id: 'rep_test_001',
      reported_indicator: 'newly.discovered.scammer@paytm',
      report_category: 'UNAUTHORIZED_COLLECT',
      description: 'Scammer requesting collect requests on Telegram group for newly.discovered.scammer@paytm',
      submission_timestamp: new Date().toISOString(),
      moderation_status: 'VERIFIED',
      confidence: 0.95,
      provenance: 'COMMUNITY_REPORT'
    };

    const reportResult = pipeline.processReport(report);
    assert.strictEqual(reportResult.status, 'ACCEPTED');

    const scanReq = {
      content_type: 'UPI_VPA',
      content_value: 'newly.discovered.scammer@paytm',
      timestamp: new Date().toISOString()
    };

    const scanRes = pipeline.processScan(scanReq);
    validateResponse(scanRes, 'E2E-8: Offline Community Ingestion');

    assert.ok(scanRes.risk_assessment.risk_score >= 80);
    assert.strictEqual(scanRes.risk_assessment.risk_severity, 'CRITICAL');
  });
});
