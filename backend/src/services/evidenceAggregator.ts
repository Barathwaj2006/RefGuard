import { EvidencePack } from '../models/types';

export type EvidenceType = 'ORIGINAL_CONTENT' | 'EXTRACTED_ENTITY' | 'URL' | 'UPI_IDENTIFIER' | 'RISK_SIGNAL';

export type SourceCategory = 'OBSERVED_FACT' | 'EXTRACTED_ENTITY' | 'THREAT_INTELLIGENCE' | 'DETERMINISTIC_RULE' | 'CONTEXTUAL_AI_INTERPRETATION' | 'INFERRED_RELATIONSHIP';

export interface InternalEvidence {
  id: string;
  type: EvidenceType;
  data: string;
  category: string; // e.g., 'SOURCE', 'URGENCY', 'PAYMENT', 'GEMINI', 'TRADING', 'SOCIAL_ENG', 'URL', 'UPI'
  explanation: string;
  source_category: SourceCategory;
}

export class EvidenceAggregator {
  private evidence: InternalEvidence[] = [];
  private incidentId: string;
  private timestamp: string;

  constructor(scanId: string, timestamp: string) {
    this.incidentId = 'inc_' + scanId.slice(0, 8);
    this.timestamp = timestamp;
  }

  public addEvidence(type: EvidenceType, category: string, data: string, explanation: string, source_category: SourceCategory): string {
    const id = `ev_${this.evidence.length + 1}_${Math.random().toString(36).substring(2, 7)}`;
    this.evidence.push({ id, type, data, category, explanation, source_category });
    return id;
  }

  public getEvidenceIdsByCategory(category: string): string[] {
    return this.evidence.filter(e => e.category === category).map(e => e.id);
  }
  
  public getEvidenceIdsByCategories(categories: string[]): string[] {
    return this.evidence.filter(e => categories.includes(e.category)).map(e => e.id);
  }

  public getAllEvidenceIds(): string[] {
    return this.evidence.map(e => e.id);
  }

  public buildEvidencePack(): EvidencePack {
    return {
      incident_id: this.incidentId,
      timestamp: this.timestamp,
      items: this.evidence.map(e => ({
        evidence_id: e.id,
        evidence_type: e.type,
        data: e.data,
        explanation: e.explanation,
        source_category: e.source_category
      }))
    };
  }
  
  public hasEvidence(): boolean {
    return this.evidence.length > 0;
  }
}
