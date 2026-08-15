/**
 * RefGuard Evidence Pack Builder
 * Organizes structured, traceable evidence items with unique IDs.
 */

class EvidencePackBuilder {
  build(scanId, timestamp, extractedData, threatAssessment) {
    const items = [];
    let counter = 1;

    const add = (type, data) => {
      const evidence_id = 'ev_' + scanId.slice(0, 8) + '_' + (counter++);
      items.push({
        evidence_id,
        evidence_type: type,
        data: String(data)
      });
      return evidence_id;
    };

    // Original content
    const originalEvId = add('ORIGINAL_CONTENT', extractedData.rawContent.slice(0, 500));

    // Extracted URLs
    const urlEvIds = extractedData.urls.map(u => add('URL', u.fullUrl));

    // Extracted VPAs
    const vpaEvIds = extractedData.vpas.map(v => add('UPI_IDENTIFIER', v));

    // Extracted Referral Codes
    const refEvIds = extractedData.referralCodes.map(r => add('EXTRACTED_ENTITY', 'REFERRAL_CODE:' + r));

    // Threat Signals
    const threatEvIds = threatAssessment.threats.map(t => add('RISK_SIGNAL', t.type + ':' + t.description));

    return {
      evidencePack: {
        incident_id: 'inc_' + scanId,
        timestamp,
        items
      },
      ids: {
        original: originalEvId,
        urls: urlEvIds,
        vpas: vpaEvIds,
        referrals: refEvIds,
        threats: threatEvIds,
        all: items.map(i => i.evidence_id)
      }
    };
  }
}

module.exports = EvidencePackBuilder;
