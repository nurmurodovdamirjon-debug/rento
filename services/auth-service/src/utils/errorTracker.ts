import { logger } from './logger';

/**
 * Error Tracker — Xatoliklarni kuzatish va tizimlashtirish
 *
 * Production da Sentry.io ga ulash mumkin.
 * Hozircha structured log + in-memory tracking.
 */

interface ErrorEntry {
  timestamp: Date;
  error: string;
  stack?: string;
  context?: Record<string, unknown>;
  count: number;
}

class ErrorTracker {
  private errors: Map<string, ErrorEntry> = new Map();
  private maxEntries = 1000;

  /**
   * Xatolikni qayd qilish
   */
  capture(error: Error, context?: Record<string, unknown>): void {
    const key = `${error.name}:${error.message}`;

    const existing = this.errors.get(key);
    if (existing) {
      existing.count++;
      existing.timestamp = new Date();
      existing.context = context;
    } else {
      // Agar limit oshgan bo'lsa, eng eski xatolikni o'chirish
      if (this.errors.size >= this.maxEntries) {
        const oldestKey = this.errors.keys().next().value;
        if (oldestKey) this.errors.delete(oldestKey);
      }

      this.errors.set(key, {
        timestamp: new Date(),
        error: error.message,
        stack: error.stack,
        context,
        count: 1,
      });
    }

    logger.error('Error captured', {
      error: error.message,
      stack: error.stack,
      context,
    });
  }

  /**
   * Barcha qayd qilingan xatoliklarni olish
   */
  getAll(): ErrorEntry[] {
    return Array.from(this.errors.values()).sort(
      (a, b) => b.timestamp.getTime() - a.timestamp.getTime()
    );
  }

  /**
   * Oxirgi N daqiqa ichidagi xatoliklar soni
   */
  getRecentCount(minutes: number = 5): number {
    const cutoff = new Date(Date.now() - minutes * 60 * 1000);
    let count = 0;
    for (const entry of this.errors.values()) {
      if (entry.timestamp >= cutoff) {
        count += entry.count;
      }
    }
    return count;
  }

  /**
   * Tozalash
   */
  clear(): void {
    this.errors.clear();
  }

  /**
   * Summary — monitoring endpoint uchun
   */
  getSummary(): {
    total: number;
    recent5m: number;
    recent1h: number;
    topErrors: Array<{ error: string; count: number }>;
  } {
    const recent5m = this.getRecentCount(5);
    const recent1h = this.getRecentCount(60);
    const topErrors = this.getAll()
      .sort((a, b) => b.count - a.count)
      .slice(0, 10)
      .map((e) => ({ error: e.error, count: e.count }));

    return {
      total: this.errors.size,
      recent5m,
      recent1h,
      topErrors,
    };
  }
}

export const errorTracker = new ErrorTracker();
