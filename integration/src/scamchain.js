/**
 * RefGuard Scam Chain Graph Reconstructor
 * Reconstructs the multi-step scam flow (Message -> Shortlink -> Landing Page -> UPI Collect -> Debit Action).
 */

class ScamChainBuilder {
  build(extractedData, mismatch, threatAssessment, evidenceIds) {
    const nodes = [];
    const edges = [];
    let nodeIdCounter = 1;

    const makeNode = (type, entityRef, evRefs) => {
      const node_id = 'node_' + (nodeIdCounter++);
      nodes.push({
        node_id,
        state: "OBSERVED",
        confidence: 0.9,
        provenance: "CONTEXT_ENGINE_DERIVATION",
        node_type: type,
        entity_reference: entityRef || undefined,
        evidence_references: evRefs && evRefs.length > 0 ? evRefs : undefined
      });
      return node_id;
    };

    const makeEdge = (from, to, rel, conf = 0.95, prov = 'CONTEXT_ENGINE_DERIVATION', evRefs = []) => {
      edges.push({
        from_node: from,
        to_node: to,
        relationship: rel,
        confidence: conf,
        provenance: prov,
        evidence_references: evRefs.length > 0 ? evRefs : undefined
      });
    };

    // Node 1: Entry Message / Content
    const rootNode = makeNode('MESSAGE', extractedData.contentType, [evidenceIds.original]);
    let prevNode = rootNode;

    // Node 2: Referral Code if present
    if (extractedData.referralCodes.length > 0) {
      const refNode = makeNode('REFERRAL', extractedData.referralCodes[0], evidenceIds.referrals);
      makeEdge(prevNode, refNode, 'CONTAINS_REFERRAL_CODE', 0.98, 'STATIC_EXTRACTION', evidenceIds.referrals);
      prevNode = refNode;
    }

    // Node 3: URLs / Shortlink
    if (extractedData.urls.length > 0) {
      const u = extractedData.urls[0];
      const urlType = u.isShortLink ? 'SHORT_LINK' : 'LANDING_PAGE';
      const urlNode = makeNode(urlType, u.fullUrl, evidenceIds.urls);
      makeEdge(prevNode, urlNode, u.isShortLink ? 'EXPANDS_TO' : 'DIRECTS_USER_TO', 0.95, 'URL_ANALYSIS', evidenceIds.urls);
      prevNode = urlNode;
    }

    // Node 4: UPI Collect / Payment Request
    if (extractedData.vpas.length > 0 || extractedData.upi) {
      const vpa = extractedData.vpas[0] || (extractedData.upi ? extractedData.upi.pa : 'UPI_COLLECT');
      const upiNode = makeNode('UPI_REQUEST', vpa, evidenceIds.vpas);
      makeEdge(prevNode, upiNode, 'TRIGGERS_PAYMENT_COLLECT', 0.92, 'UPI_PROTOCOL_TRACING', evidenceIds.vpas);
      prevNode = upiNode;

      // Node 5: Payment Action (Outbound Debit)
      if (mismatch.status === 'DETECTED' || mismatch.payment_direction === 'OUTBOUND_DEBIT') {
        const actionNode = makeNode('PAYMENT_ACTION', 'OUTBOUND_DEBIT:' + (mismatch.amount || 'VARIABLE'), evidenceIds.threats);
        makeEdge(prevNode, actionNode, 'EXECUTES_UNAUTHORIZED_DEBIT', 0.96, 'INTENT_MISMATCH_DETECTION', evidenceIds.threats);
      }
    }

    return {
      nodes,
      edges
    };
  }
}

module.exports = ScamChainBuilder;
