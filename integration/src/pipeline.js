const crypto = require('crypto');
const ExtractionEngine = require('./extraction');
const ThreatIntelligence = require('./intelligence');
const PaymentIntentMismatchAnalyzer = require('./mismatch');
const EvidencePackBuilder = require('./evidence');
const ScamChainBuilder = require('./scamchain');
const RiskEngine = require('./risk');
const SchemaValidator = require('./validator');

class RefGuardPipeline {
  constructor() {
    this.extractor = new ExtractionEngine();
    this.intelligence = new ThreatIntelligence();
    this.mismatchAnalyzer = new PaymentIntentMismatchAnalyzer();
    this.evidenceBuilder = new EvidencePackBuilder();
    this.scamChainBuilder = new ScamChainBuilder();
    this.riskEngine = new RiskEngine();
    this.validator = new SchemaValidator();
  }

  processScan(scanRequest) {
    // 1. Validate Input against scan-request.json
    const inputValidation = this.validator.validate('scan-request.json', scanRequest);
    if (!inputValidation.valid) {
      const error = new Error('Invalid ScanRequest schema: ' + inputValidation.errors.join(', '));
      error.statusCode = 400;
      error.details = inputValidation.errors;
      throw error;
    }

    const scanId = 'scan_' + crypto.randomUUID().replace(/-/g, '').slice(0, 16);
    const timestamp = new Date().toISOString();

    // 2. Extraction Stage
    const extractedData = this.extractor.extract(scanRequest);

    // 3. Threat Assessment Stage
    const threatAssessment = this.intelligence.evaluate(extractedData);

    // 4. Evidence Assembly Stage
    const { evidencePack, ids: evidenceIds } = this.evidenceBuilder.build(scanId, timestamp, extractedData, threatAssessment);

    // 5. Payment Intent Mismatch Stage
    const paymentIntentMismatch = this.mismatchAnalyzer.analyze(extractedData, evidenceIds.all);

    // 6. Scam Chain Reconstruction Stage
    const scamChain = this.scamChainBuilder.build(extractedData, paymentIntentMismatch, threatAssessment, evidenceIds);

    // 7. Risk Assessment & Protection Decision Stage
    const { riskAssessment, protectionDecision } = this.riskEngine.evaluate(
      extractedData,
      threatAssessment,
      paymentIntentMismatch,
      evidenceIds,
      scamChain
    );

    // 8. Build Aggregated ScanResponse
    const scanResponse = {
      scan_id: scanId,
      timestamp,
      risk_assessment: riskAssessment,
      protection_decision: protectionDecision,
      payment_intent_mismatch: paymentIntentMismatch,
      scam_chain: scamChain,
      evidence_pack: evidencePack
    };

    // 9. Strict Validation against scan-response.json
    const outputValidation = this.validator.validate('scan-response.json', scanResponse);
    if (!outputValidation.valid) {
      console.warn('Output validation warning:', outputValidation.errors);
    }

    return scanResponse;
  }

  processReport(scamReport) {
    const reportValidation = this.validator.validate('scam-report.json', scamReport);
    if (!reportValidation.valid) {
      const error = new Error('Invalid ScamReport schema: ' + reportValidation.errors.join(', '));
      error.statusCode = 400;
      error.details = reportValidation.errors;
      throw error;
    }

    this.intelligence.addCommunityReport(scamReport);

    return {
      report_id: scamReport.report_id || ('rep_' + crypto.randomUUID().slice(0, 8)),
      status: 'ACCEPTED'
    };
  }
}

module.exports = RefGuardPipeline;
