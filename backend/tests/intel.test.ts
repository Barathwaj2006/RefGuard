import request from 'supertest';
import express from 'express';
import apiRouter from '../src/routes/api';
import { communityStore } from '../src/services/communityStore';

const app = express();
app.use(express.json());
app.use('/api/v1', apiRouter);

describe('Threat Intelligence APIs', () => {
  beforeAll(() => {
    // Add some test reports to ensure data exists
    communityStore.addReport({
      report_id: 'rpt-1',
      reported_indicator: 'test-scam@upi',
      report_category: 'UPI_FRAUD',
      description: 'Asked for money',
      submission_timestamp: new Date().toISOString(),
      moderation_status: 'PENDING',
      confidence: 0.9,
      provenance: 'test'
    });
    communityStore.addReport({
      report_id: 'rpt-2',
      reported_indicator: 'test-scam@upi',
      report_category: 'UPI_FRAUD',
      description: 'Asked for money again',
      submission_timestamp: new Date().toISOString(),
      moderation_status: 'PENDING',
      confidence: 0.9,
      provenance: 'test2'
    });
  });

  it('GET /api/v1/intel/trending should return trending threat indicators', async () => {
    const res = await request(app).get('/api/v1/intel/trending?limit=5');
    
    expect(res.status).toBe(200);
    expect(res.body.status).toBe('SUCCESS');
    expect(res.body.trending_indicators).toBeInstanceOf(Array);
    
    // Seed records should be there
    expect(res.body.trending_indicators.length).toBeGreaterThan(0);
    const hasSeed = res.body.trending_indicators.some((t: any) => t.source === 'VERIFIED_SEED' || t.source === 'COMMUNITY');
    expect(hasSeed).toBe(true);
  });

  it('GET /api/v1/intel/reports should return recent reports', async () => {
    const res = await request(app).get('/api/v1/intel/reports?limit=5');
    
    expect(res.status).toBe(200);
    expect(res.body.status).toBe('SUCCESS');
    expect(res.body.recent_reports).toBeInstanceOf(Array);
    expect(res.body.recent_reports.length).toBeGreaterThanOrEqual(1);
    
    const firstReport = res.body.recent_reports[0];
    expect(firstReport).toHaveProperty('report_id');
    expect(firstReport).toHaveProperty('indicator');
    expect(firstReport).toHaveProperty('category');
  });
});
