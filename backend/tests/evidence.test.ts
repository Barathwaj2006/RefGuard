import request from 'supertest';
import app from '../src/app';

describe('Adaptive Risk-Evidence Aggregation', () => {
  it('should capture evidence for benign message', async () => {
    const payload = {
      content_type: 'TEXT',
      content_value: 'Hey, are we still meeting for lunch at 1 PM?',
      source_context: 'com.whatsapp',
      timestamp: new Date().toISOString()
    };

    const res = await request(app)
      .post('/api/v1/scan')
      .send(payload)
      .expect(200);

    expect(res.body.risk_assessment.risk_severity).toBe('LOW');
    expect(res.body.evidence_pack.items.length).toBeGreaterThan(0);
    
    const originalEvidence = res.body.evidence_pack.items.find((i: any) => i.evidence_type === 'ORIGINAL_CONTENT');
    expect(originalEvidence).toBeDefined();

    const sourceEvidence = res.body.evidence_pack.items.find((i: any) => i.data.includes('Source Context: WhatsApp'));
    expect(sourceEvidence).toBeDefined();
    
    // Check references in risk assessment
    expect(res.body.risk_assessment.evidence_references).toContain(originalEvidence.evidence_id);
    expect(res.body.risk_assessment.evidence_references).toContain(sourceEvidence.evidence_id);
  });

  it('should capture evidence for UPI scam and link to scam chain', async () => {
    const payload = {
      content_type: 'TEXT',
      content_value: 'Urgent: Please pay electricity bill to avoid disconnection. upi://pay?pa=scammer@ybl&am=5000',
      source_context: 'com.android.mms',
      timestamp: new Date().toISOString()
    };

    const res = await request(app)
      .post('/api/v1/scan')
      .send(payload)
      .expect(200);

    const pack = res.body.evidence_pack.items;
    
    const upiEvidence = pack.find((i: any) => i.evidence_type === 'UPI_IDENTIFIER' && i.data === 'scammer@ybl');
    expect(upiEvidence).toBeDefined();
    
    const urgencyEvidence = pack.find((i: any) => i.data.includes('Urgent'));
    expect(urgencyEvidence).toBeDefined();
    
    // Check references in scam chain nodes
    const upiNode = res.body.scam_chain.nodes.find((n: any) => n.node_type === 'UPI_REQUEST');
    expect(upiNode.evidence_references).toContain(upiEvidence.evidence_id);
  });

  it('should capture evidence for trading scam', async () => {
    const payload = {
      content_type: 'TEXT',
      content_value: 'Join my Telegram VIP group for 500% guaranteed returns on crypto! Send BTC to 1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa',
      source_context: 'org.telegram.messenger',
      timestamp: new Date().toISOString()
    };

    const res = await request(app)
      .post('/api/v1/scan')
      .send(payload)
      .expect(200);

    const pack = res.body.evidence_pack.items;
    const tradingSignalEv = pack.find((i: any) => i.evidence_type === 'RISK_SIGNAL' && i.data.includes('Trading Fraud'));
    expect(tradingSignalEv).toBeDefined();

    const cryptoEv = pack.find((i: any) => i.data.includes('Crypto addresses:'));
    expect(cryptoEv).toBeDefined();
  });
  
  it('should handle no-evidence case safely without crashing', async () => {
    const payload = {
      content_type: 'TEXT',
      content_value: 'a', // short content to test no-evidence case
      source_context: '',
      timestamp: new Date().toISOString()
    };

    const res = await request(app)
      .post('/api/v1/scan')
      .send(payload)
      .expect(200);
      
    // original content and source context will still generate evidence (empty or unknown)
    expect(res.body.evidence_pack).toBeDefined();
    expect(res.body.evidence_pack.items.length).toBeGreaterThanOrEqual(1); // At least ORIGINAL_CONTENT
  });

  it('should guarantee evidence graph integrity (no dangling references)', async () => {
    const payload = {
      content_type: 'TEXT',
      content_value: 'Urgent: Please pay electricity bill to avoid disconnection. upi://pay?pa=scammer@ybl&am=5000',
      source_context: 'com.android.mms',
      timestamp: new Date().toISOString()
    };

    const res = await request(app)
      .post('/api/v1/scan')
      .send(payload)
      .expect(200);

    const evidenceIds = new Set(res.body.evidence_pack.items.map((i: any) => i.evidence_id));
    
    // Check risk assessment
    res.body.risk_assessment.evidence_references?.forEach((ref: string) => {
      expect(evidenceIds.has(ref)).toBe(true);
    });

    // Check scam chain nodes
    res.body.scam_chain?.nodes?.forEach((node: any) => {
      node.evidence_references?.forEach((ref: string) => {
        expect(evidenceIds.has(ref)).toBe(true);
      });
    });

    // Check scam chain edges
    res.body.scam_chain?.edges?.forEach((edge: any) => {
      edge.evidence_references?.forEach((ref: string) => {
        expect(evidenceIds.has(ref)).toBe(true);
      });
    });

    // Check adaptive scam intelligence
    res.body.adaptive_scam_intelligence?.observed_evidence?.forEach((ref: string) => {
      expect(evidenceIds.has(ref)).toBe(true);
    });
  });
});
