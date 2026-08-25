const fs = require('fs');
let app = fs.readFileSync('integration/demo/public/app.js', 'utf8');

const oldMap = `    const friendlySignalMap = {
      'urgency_indicator': { title: 'Urgency', desc: 'The message pressures you to act immediately without thinking.' },
      'authority_impersonation': { title: 'Authority Impersonation', desc: 'The sender claims to represent an official organization.' },
      'sms_authority_impersonation': { title: 'Authority Impersonation', desc: 'The sender claims to represent an official organization.' },
      'digital_arrest_scam': { title: 'Digital Arrest Threat', desc: 'Uses fear tactics claiming illegal activity to demand money.' },
      'financial_reward': { title: 'Fake Reward', desc: 'Promises an unexpected reward or refund to trick you into paying.' },
      'upi_fraud_pattern': { title: 'UPI Collect Fraud', desc: 'Disguises a payment request as a refund or prize receipt.' }`;

const newMap = `    const friendlySignalMap = {
      'urgency_indicator': { title: 'Urgency', desc: 'The message pressures you to act immediately without thinking.' },
      'authority_impersonation': { title: 'Authority Impersonation', desc: 'The sender claims to represent an official organization.' },
      'sms_authority_impersonation': { title: 'Authority Impersonation', desc: 'The sender claims to represent an official organization.' },
      'digital_arrest_scam': { title: 'Digital Arrest Threat', desc: 'Impersonation of law enforcement combined with pressure to act immediately.' },
      'payment_intent_mismatch': { title: 'Payment Intent Mismatch', desc: 'You were told you would receive money, but the payment request would debit your account.' },
      'deceptive_reward_trigger': { title: 'Deceptive Reward', desc: 'Promises an unexpected reward or refund to trick you into paying.' },
      'hinglish_cashback_scam': { title: 'Cashback Trap', desc: 'Common localized language pattern used in cashback scams.' },
      'financial_reward': { title: 'Fake Reward', desc: 'Promises an unexpected reward or refund to trick you into paying.' },
      'upi_fraud_pattern': { title: 'UPI Collect Fraud', desc: 'Disguises a payment request as a refund or prize receipt.' }`;

if (app.includes('Digital Arrest Threat')) {
  app = app.replace(oldMap, newMap);
  fs.writeFileSync('integration/demo/public/app.js', app);
}
