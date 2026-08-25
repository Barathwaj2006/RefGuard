/**
 * Gemini Reasoning Service — AI escalation layer for ambiguous fraud signals.
 *
 * Architecture position:
 *   INPUT → PII-safe preprocessing → Deterministic prefilter → Risk analysis →
 *   **Gemini reasoning (when escalation criteria met)** → Validated structured result →
 *   Fallback if Gemini fails → Final verdict
 *
 * Key invariants:
 * - Gemini NEVER replaces deterministic detection
 * - Only invoked when deterministic score is in the ambiguous zone (40-80)
 * - Maximum score adjustment: ±20 points
 * - 8-second hard timeout on API calls
 * - API key sourced from environment variable only — never hardcoded
 * - Only sanitized (PII-free) content is ever sent to Gemini
 * - Complete fallback: if Gemini is unavailable, the deterministic score stands
 */

import { GoogleGenAI } from '@google/genai';

export interface GeminiVerdict {
  /** Score adjustment: positive = more risky, negative = less risky. Bounded ±20. */
  risk_adjustment: number;
  /** Human-readable reasoning from the model */
  reasoning: string;
  /** Model confidence in its assessment (0-1) */
  confidence: number;
  /** Patterns the model detected */
  detected_patterns: string[];
  /** Whether Gemini was actually used (false = fallback) */
  gemini_used: boolean;
}

export interface GeminiEscalationInput {
  /** PII-sanitized content for analysis */
  sanitizedContent: string;
  /** Current deterministic risk score (40-80 range triggers escalation) */
  deterministicScore: number;
  /** Signals already detected by deterministic engine */
  existingSignals: string[];
  /** Content type for context */
  contentType: string;
}

const GEMINI_TIMEOUT_MS = 8000;
const MIN_ESCALATION_SCORE = 40;
const MAX_ESCALATION_SCORE = 80;
const MAX_ADJUSTMENT = 20;

const SYSTEM_PROMPT = `You are RefGuard's fraud analysis reasoning engine. You analyze sanitized message content for financial fraud indicators specific to India's digital payment ecosystem (UPI, NEFT, IMPS).

Your role is to provide a SECOND OPINION on content that deterministic rules flagged as ambiguous. You do NOT replace deterministic detection — you refine it.

For each analysis, focus especially on the semantic narrative and resolving conflicting signals:
1. Contradictions: Does the sender claim to be giving a refund/prize, but include a link that usually initiates a payment or debit?
2. Social engineering context: Is there an intense fabricated urgency or fear tactic (e.g. "account blocked", "arrest warrant") combined with a suspicious request?
3. Linguistic fraud markers: Are there typos mimicking official communication, or manipulative mixed language (e.g., Hinglish)?
4. Context coherence: Does the message narrative make logical sense for a legitimate institution? (e.g. A bank will not ask you to install AnyDesk).

Respond ONLY with valid JSON matching this exact structure:
{
  "risk_adjustment": <integer -20 to 20>,
  "reasoning": "<one concise paragraph explaining your assessment, specifically addressing any conflicting signals or narrative anomalies>",
  "confidence": <float 0.0 to 1.0>,
  "detected_patterns": ["<pattern1>", "<pattern2>"]
}

Rules:
- risk_adjustment MUST be between -20 and 20
- Positive = content is MORE suspicious than deterministic score suggests (e.g. a clear phishing narrative)
- Negative = content is LESS suspicious (e.g. clearly legitimate conversational or informational text)
- Zero = you agree with the deterministic assessment
- confidence MUST be between 0.0 and 1.0
- detected_patterns should list specific fraud indicators found (empty array if none)
- Do NOT hallucinate patterns that aren't in the text
- If the content is clearly benign, use a negative adjustment`;

/**
 * Determine whether the deterministic score warrants Gemini escalation.
 */
export function shouldEscalateToGemini(deterministicScore: number): boolean {
  return deterministicScore >= MIN_ESCALATION_SCORE && deterministicScore <= MAX_ESCALATION_SCORE;
}

/**
 * Call Gemini for fraud reasoning analysis.
 * Returns a GeminiVerdict with gemini_used=true on success, or a neutral
 * fallback verdict with gemini_used=false on any failure (timeout, API error,
 * missing key, parse error).
 */
export async function analyzeWithGemini(input: GeminiEscalationInput): Promise<GeminiVerdict> {
  const neutralFallback: GeminiVerdict = {
    risk_adjustment: 0,
    reasoning: 'Gemini reasoning unavailable; deterministic score retained.',
    confidence: 0,
    detected_patterns: [],
    gemini_used: false,
  };

  // Check for API key presence
  const apiKey = process.env.GEMINI_API_KEY;
  if (!apiKey) {
    return neutralFallback;
  }

  try {
    const ai = new GoogleGenAI({ apiKey });

    const userPrompt = `Analyze this content for fraud signals. Current deterministic risk score: ${input.deterministicScore}/100. Existing signals: [${input.existingSignals.join(', ')}]. Content type: ${input.contentType}.

Content to analyze (PII has been redacted):
---
${input.sanitizedContent.slice(0, 2000)}
---

Respond with JSON only.`;

    // Race between Gemini call and timeout
    let timeoutId: NodeJS.Timeout;
    const timeoutPromise = new Promise<null>((resolve) => {
      timeoutId = setTimeout(() => resolve(null), GEMINI_TIMEOUT_MS);
    });

    const geminiPromise = ai.models.generateContent({
      model: 'gemini-2.0-flash',
      contents: userPrompt,
      config: {
        systemInstruction: SYSTEM_PROMPT,
        temperature: 0.1,
        maxOutputTokens: 512,
      },
    });

    let result: { text?: string } | null;
    try {
      result = (await Promise.race([geminiPromise, timeoutPromise])) as { text?: string } | null;
    } finally {
      clearTimeout(timeoutId!);
    }

    if (result === null) {
      // Timeout
      return { ...neutralFallback, reasoning: 'Gemini reasoning timed out; deterministic score retained.' };
    }

    // Parse response
    const responseText = result.text?.trim();
    if (!responseText) {
      return { ...neutralFallback, reasoning: 'Gemini returned empty response; deterministic score retained.' };
    }

    // Extract JSON from response (handle markdown code blocks)
    let jsonText = responseText;
    const jsonMatch = responseText.match(/```(?:json)?\s*([\s\S]*?)```/);
    if (jsonMatch) {
      jsonText = jsonMatch[1].trim();
    }

    const parsed = JSON.parse(jsonText);

    // Validate and bound the response
    const adjustment = typeof parsed.risk_adjustment === 'number'
      ? Math.max(-MAX_ADJUSTMENT, Math.min(MAX_ADJUSTMENT, Math.round(parsed.risk_adjustment)))
      : 0;

    const confidence = typeof parsed.confidence === 'number'
      ? Math.max(0, Math.min(1, parsed.confidence))
      : 0.5;

    const reasoning = typeof parsed.reasoning === 'string'
      ? parsed.reasoning.slice(0, 500)
      : 'AI analysis completed.';

    const patterns = Array.isArray(parsed.detected_patterns)
      ? parsed.detected_patterns.filter((p: unknown) => typeof p === 'string').slice(0, 10)
      : [];

    return {
      risk_adjustment: adjustment,
      reasoning,
      confidence,
      detected_patterns: patterns,
      gemini_used: true,
    };

  } catch (error) {
    // Any failure (network, parse, SDK) results in neutral fallback
    const errorMsg = error instanceof Error ? error.message : 'Unknown error';
    return {
      ...neutralFallback,
      reasoning: `Gemini reasoning failed (${errorMsg}); deterministic score retained.`,
    };
  }
}
