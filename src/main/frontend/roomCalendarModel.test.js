import { describe, expect, it } from "vitest";
import {
  addDays,
  bandPlacement,
  calendarEvents,
  daysForMonth,
  intervalHasConflict,
  intervalOverlaps,
  normalizedRange,
  releasePreview,
  shiftMonth,
} from "./roomCalendarModel.js";

describe("room calendar date model", () => {
  it("builds complete natural months including leap years", () => {
    expect(daysForMonth("2028-02")).toHaveLength(29);
    expect(daysForMonth("2027-02")).toHaveLength(28);
    expect(daysForMonth("2026-09")[0].iso).toBe("2026-09-01");
  });

  it("moves safely over year boundaries", () => {
    expect(shiftMonth("2026-12", 1)).toBe("2027-01");
    expect(shiftMonth("2026-01", -1)).toBe("2025-12");
    expect(addDays("2028-02-28", 1)).toBe("2028-02-29");
  });

  it("uses half-open interval overlap semantics", () => {
    expect(intervalOverlaps("2026-09-01", "2026-09-03", "2026-09-03", "2026-09-04")).toBe(false);
    expect(intervalOverlaps("2026-09-01", "2026-09-03", "2026-09-02", "2026-09-04")).toBe(true);
  });

  it("clips cross-month bands to grid columns", () => {
    expect(bandPlacement("2026-08-30", "2026-09-03", "2026-09")).toMatchObject({
      gridColumnStart: 2,
      gridColumnEnd: 4,
      clippedAtStart: true,
      clippedAtEnd: false,
    });
    expect(bandPlacement("2026-09-30", "2026-10-03", "2026-09")).toMatchObject({
      gridColumnStart: 31,
      gridColumnEnd: 32,
      clippedAtEnd: true,
    });
  });

  it("treats the second click as the exclusive checkout or reopen date", () => {
    expect(normalizedRange("2026-09-03", "2026-09-05")).toEqual({
      startDate: "2026-09-03",
      endDateExclusive: "2026-09-05",
    });
    expect(normalizedRange("2026-09-05", "2026-09-05")).toBeNull();
    expect(normalizedRange("2026-09-05", "2026-09-03")).toBeNull();
  });

  it("detects conflicts within one room while allowing same-day turnover", () => {
    const events = [{ kind: "stay", id: 1, roomId: 10, startDate: "2026-09-02", endDateExclusive: "2026-09-04" }];
    expect(intervalHasConflict(events, 10, "2026-09-03", "2026-09-05")).toBe(true);
    expect(intervalHasConflict(events, 10, "2026-09-04", "2026-09-05")).toBe(false);
    expect(intervalHasConflict(events, 11, "2026-09-03", "2026-09-05")).toBe(false);
  });

  it("converts API records into one collision model", () => {
    const events = calendarEvents({
      stays: [{ id: 1, roomId: 2, checkInDate: "2026-09-01", checkOutDate: "2026-09-02" }],
      blocks: [{ id: 3, roomId: 2, startDate: "2026-09-03", endDateExclusive: "2026-09-05" }],
    });
    expect(events.map((event) => event.kind)).toEqual(["stay", "block"]);
  });

  it("previews whole, leading, trailing and middle block release", () => {
    const block = { startDate: "2026-09-01", endDateExclusive: "2026-09-10" };
    expect(releasePreview(block, "2026-09-01", "2026-09-10")).toMatchObject({ removesEntireBlock: true });
    expect(releasePreview(block, "2026-09-01", "2026-09-04").remaining).toEqual([
      { startDate: "2026-09-04", endDateExclusive: "2026-09-10" },
    ]);
    expect(releasePreview(block, "2026-09-04", "2026-09-10").remaining).toEqual([
      { startDate: "2026-09-01", endDateExclusive: "2026-09-04" },
    ]);
    expect(releasePreview(block, "2026-09-04", "2026-09-06").remaining).toEqual([
      { startDate: "2026-09-01", endDateExclusive: "2026-09-04" },
      { startDate: "2026-09-06", endDateExclusive: "2026-09-10" },
    ]);
  });
});
