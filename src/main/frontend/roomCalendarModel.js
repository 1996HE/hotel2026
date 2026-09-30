const ISO_DATE_PATTERN = /^(\d{4})-(\d{2})-(\d{2})$/;
const MONTH_PATTERN = /^(\d{4})-(\d{2})$/;
const DAY_MILLISECONDS = 86_400_000;

function utcDate(year, monthIndex, day) {
  return new Date(Date.UTC(year, monthIndex, day));
}

function formatUtcDate(date) {
  const year = String(date.getUTCFullYear()).padStart(4, "0");
  const month = String(date.getUTCMonth() + 1).padStart(2, "0");
  const day = String(date.getUTCDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

export function isIsoDate(value) {
  if (typeof value !== "string") return false;
  const match = value.match(ISO_DATE_PATTERN);
  if (!match) return false;
  const parsed = utcDate(Number(match[1]), Number(match[2]) - 1, Number(match[3]));
  return formatUtcDate(parsed) === value;
}

export function todayIso(now = new Date()) {
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, "0");
  const day = String(now.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

export function normalizeMonth(value, fallback = todayIso().slice(0, 7)) {
  if (typeof value !== "string") return fallback;
  const match = value.match(MONTH_PATTERN);
  if (!match) return fallback;
  const month = Number(match[2]);
  return month >= 1 && month <= 12 ? value : fallback;
}

export function addDays(date, amount) {
  if (!isIsoDate(date)) return "";
  const [year, month, day] = date.split("-").map(Number);
  return formatUtcDate(new Date(utcDate(year, month - 1, day).getTime() + amount * DAY_MILLISECONDS));
}

export function shiftMonth(month, amount) {
  const normalized = normalizeMonth(month);
  const [year, monthNumber] = normalized.split("-").map(Number);
  const shifted = utcDate(year, monthNumber - 1 + amount, 1);
  return formatUtcDate(shifted).slice(0, 7);
}

export function daysForMonth(month) {
  const normalized = normalizeMonth(month);
  const [year, monthNumber] = normalized.split("-").map(Number);
  const finalDay = utcDate(year, monthNumber, 0).getUTCDate();
  return Array.from({ length: finalDay }, (_, index) => {
    const iso = `${normalized}-${String(index + 1).padStart(2, "0")}`;
    const weekday = utcDate(year, monthNumber - 1, index + 1).getUTCDay();
    return { iso, day: index + 1, weekday, weekend: weekday === 0 || weekday === 6 };
  });
}

export function monthBounds(month) {
  const normalized = normalizeMonth(month);
  return {
    start: `${normalized}-01`,
    endExclusive: `${shiftMonth(normalized, 1)}-01`,
  };
}

export function compareIso(left, right) {
  if (!isIsoDate(left) || !isIsoDate(right)) return 0;
  return left.localeCompare(right);
}

export function intervalOverlaps(leftStart, leftEndExclusive, rightStart, rightEndExclusive) {
  if (![leftStart, leftEndExclusive, rightStart, rightEndExclusive].every(isIsoDate)) return false;
  return leftStart < rightEndExclusive && rightStart < leftEndExclusive;
}

export function clampInterval(start, endExclusive, visibleStart, visibleEndExclusive) {
  if (![start, endExclusive, visibleStart, visibleEndExclusive].every(isIsoDate)) return null;
  const clippedStart = start < visibleStart ? visibleStart : start;
  const clippedEnd = endExclusive > visibleEndExclusive ? visibleEndExclusive : endExclusive;
  if (clippedStart >= clippedEnd) return null;
  return {
    start: clippedStart,
    endExclusive: clippedEnd,
    clippedAtStart: start < visibleStart,
    clippedAtEnd: endExclusive > visibleEndExclusive,
  };
}

export function dayDistance(start, endExclusive) {
  if (!isIsoDate(start) || !isIsoDate(endExclusive)) return 0;
  const [startYear, startMonth, startDay] = start.split("-").map(Number);
  const [endYear, endMonth, endDay] = endExclusive.split("-").map(Number);
  return Math.round(
    (utcDate(endYear, endMonth - 1, endDay).getTime() - utcDate(startYear, startMonth - 1, startDay).getTime()) /
      DAY_MILLISECONDS
  );
}

export function bandPlacement(start, endExclusive, month) {
  const bounds = monthBounds(month);
  const clipped = clampInterval(start, endExclusive, bounds.start, bounds.endExclusive);
  if (!clipped) return null;
  const startIndex = dayDistance(bounds.start, clipped.start);
  const endIndex = dayDistance(bounds.start, clipped.endExclusive);
  return {
    ...clipped,
    // Grid column 1 is the sticky room heading, so day one begins at column 2.
    gridColumnStart: startIndex + 2,
    gridColumnEnd: endIndex + 2,
  };
}

export function normalizedRange(firstDate, secondDate) {
  if (!isIsoDate(firstDate) || !isIsoDate(secondDate)) return null;
  if (secondDate <= firstDate) return null;
  return { startDate: firstDate, endDateExclusive: secondDate };
}

export function intervalHasConflict(events, roomId, startDate, endDateExclusive, ignoredEvent = null) {
  return events.some((event) => {
    if (String(event.roomId) !== String(roomId)) return false;
    if (ignoredEvent && event.kind === ignoredEvent.kind && String(event.id) === String(ignoredEvent.id)) return false;
    return intervalOverlaps(event.startDate, event.endDateExclusive, startDate, endDateExclusive);
  });
}

export function calendarEvents(data) {
  const stays = Array.isArray(data?.stays) ? data.stays : [];
  const blocks = Array.isArray(data?.blocks) ? data.blocks : [];
  return [
    ...stays.map((stay) => ({
      ...stay,
      kind: "stay",
      startDate: stay.checkInDate,
      endDateExclusive: stay.checkOutDate,
    })),
    ...blocks.map((block) => ({
      ...block,
      kind: "block",
      startDate: block.startDate,
      endDateExclusive: block.endDateExclusive || block.endDate,
    })),
  ].filter((event) => isIsoDate(event.startDate) && isIsoDate(event.endDateExclusive));
}

export function releasePreview(block, releaseStart, releaseEndExclusive) {
  if (!block || !isIsoDate(block.startDate) || !isIsoDate(block.endDateExclusive)) return null;
  const release = clampInterval(releaseStart, releaseEndExclusive, block.startDate, block.endDateExclusive);
  if (!release || release.start !== releaseStart || release.endExclusive !== releaseEndExclusive) return null;
  const remaining = [];
  if (block.startDate < release.start) {
    remaining.push({ startDate: block.startDate, endDateExclusive: release.start });
  }
  if (release.endExclusive < block.endDateExclusive) {
    remaining.push({ startDate: release.endExclusive, endDateExclusive: block.endDateExclusive });
  }
  return {
    release: { startDate: release.start, endDateExclusive: release.endExclusive },
    remaining,
    removesEntireBlock: remaining.length === 0,
  };
}

export function roomLabel(room) {
  const number = room?.roomNumber || room?.number || `#${room?.id ?? "—"}`;
  return room?.roomName ? `${number} ${room.roomName}` : String(number);
}
