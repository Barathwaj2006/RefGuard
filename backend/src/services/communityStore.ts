import { ScamReport } from '../models/types';

class CommunityReportStore {
  private reports: ScamReport[] = [];
  private blockedIndicators: Set<string> = new Set();

  constructor() {
    // Seed initial known threat signatures
    this.addSeedIndicators([
      'fake-cashback-reward@paytm',
      'lottery-prize-winner@upi',
      'scammer@upi',
      'phishing.evil-site.tk',
      'free-gift-card.co'
    ]);
  }

  private addSeedIndicators(indicators: string[]) {
    indicators.forEach(ind => this.blockedIndicators.add(ind.toLowerCase()));
  }

  public addReport(report: ScamReport): void {
    this.reports.push(report);
    if (report.reported_indicator) {
      this.blockedIndicators.add(report.reported_indicator.toLowerCase());
    }
  }

  public hasIndicator(indicator: string): boolean {
    if (!indicator) return false;
    const lower = indicator.toLowerCase();
    for (const blocked of this.blockedIndicators) {
      if (lower.includes(blocked) || blocked.includes(lower)) {
        return true;
      }
    }
    return false;
  }

  public getCount(): number {
    return this.reports.length;
  }
}

export const communityStore = new CommunityReportStore();
