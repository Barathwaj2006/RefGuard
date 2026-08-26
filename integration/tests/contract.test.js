const assert = require('assert');
const fs = require('fs');
const path = require('path');
const SchemaValidator = require('./validator');

describe('Contracts Schema Conformance Test Suite', () => {
  const schemasDir = path.resolve(__dirname, '../../contracts/schemas');
  const examplesDir = path.resolve(__dirname, '../../contracts/examples');
  const validator = new SchemaValidator(schemasDir);

  it('Should validate all contract example payloads against their respective schemas', () => {
    const exampleFiles = fs.readdirSync(examplesDir).filter(f => f.endsWith('.json'));
    assert.ok(exampleFiles.length > 0, 'Example files must exist');

    const mapping = {
      'error-response-400.json': 'error-response.json',
      'scam-chain-viral-referral.json': 'scam-chain.json',
      'scam-report-tampered-qr.json': 'scam-report.json',
      'scan-request-legit-referral.json': 'scan-request.json',
      'scan-response-fake-referral.json': 'scan-response.json',
      'scan-response-high-risk-upi.json': 'scan-response.json',
      'scan-response-legit-referral.json': 'scan-response.json',
      'scan-response-payment-intent-mismatch.json': 'scan-response.json',
      'scan-response-qr-scam.json': 'scan-response.json'
    };

    for (const [exampleFile, schemaFile] of Object.entries(mapping)) {
      const filePath = path.join(examplesDir, exampleFile);
      if (!fs.existsSync(filePath)) continue;

      const raw = fs.readFileSync(filePath, 'utf8').replace(/^\uFEFF/, '');
      const data = JSON.parse(raw);
      const res = validator.validate(schemaFile, data);
      assert.strictEqual(res.valid, true, 'Example ' + exampleFile + ' failed schema ' + schemaFile + ': ' + (res.errors ? res.errors.join('; ') : ''));
    }
  });
});
