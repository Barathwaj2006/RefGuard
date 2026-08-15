import * as fs from 'fs';
import * as path from 'path';
import { ScamReport } from '../models/types';

class CommunityReportStore {
  private reports: ScamReport[] = [];
  private blockedIndicators: Set<string> = new Set();
  private storageFile: string;

  constructor() {
    this.storageFile = path.resolve(__dirname, '../../data/community_reports.json');
    
    // Seed initial known threat signatures
    this.addSeedIndicators([
      'fake-cashback-reward@paytm',
      'lottery-prize-winner@upi',
      'scammer@upi',
      'phishing.evil-site.tk',
      'free-gift-card.co',
      'rewards-claim@icici',
      'kyc-verification@sbi'
    ]);

    this.loadFromDisk();
  }

  private addSeedIndicators(indicators: string[]) {
    indicators.forEach(ind => this.blockedIndicators.add(ind.toLowerCase()));
  }

  private loadFromDisk(): void {
    try {
      if (fs.existsSync(this.storageFile)) {
        const raw = fs.readFileSync(this.storageFile, 'utf8');
        const data = JSON.parse(raw);
        if (Array.isArray(data.reports)) {
          data.reports.forEach((rep: ScamReport) => {
            this.reports.push(rep);
            if (rep.reported_indicator) {
              this.blockedIndicators.add(rep.reported_indicator.toLowerCase());
            }
          });
        }
      }
    } catch (e) {
      // Fall back safely to memory if file read fails
    }
  }

  private saveToDisk(): void {
    try {
      const dir = path.dirname(this.storageFile);
      if (!fs.existsSync(dir)) {
        fs.mkdirSync(dir, { recursive: true });
      }
      fs.writeFileSync(this.storageFile, JSON.stringify({ reports: this.reports }, null, 2), 'utf8');
    } catch (e) {
      // Non-critical persistence failure
    }
  }

  public addReport(report: ScamReport): void {
    this.reports.push(report);
    if (report.reported_indicator) {
      this.blockedIndicators.add(report.reported_indicator.toLowerCase());
    }
    this.saveToDisk();
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
