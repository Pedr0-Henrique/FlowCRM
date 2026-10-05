import { describe, expect, it } from "vitest";
import { isTaskOverdue } from "./tasks";

const now = Date.parse("2026-10-05T12:00:00.000Z");

describe("isTaskOverdue", () => {
  it("marks unfinished tasks with a past due date as overdue", () => {
    expect(isTaskOverdue({ dueDate: "2026-10-05T11:59:59.000Z", status: "TODO" }, now)).toBe(true);
    expect(isTaskOverdue({ dueDate: "2026-10-05T11:59:59.000Z", status: "IN_PROGRESS" }, now)).toBe(true);
  });

  it("does not mark tasks due now or in the future as overdue", () => {
    expect(isTaskOverdue({ dueDate: "2026-10-05T12:00:00.000Z", status: "TODO" }, now)).toBe(false);
    expect(isTaskOverdue({ dueDate: "2026-10-05T12:00:01.000Z", status: "TODO" }, now)).toBe(false);
  });

  it("does not mark tasks without a due date or tasks that are finished or cancelled", () => {
    expect(isTaskOverdue({ dueDate: null, status: "TODO" }, now)).toBe(false);
    expect(isTaskOverdue({ dueDate: "2026-10-05T11:00:00.000Z", status: "COMPLETED" }, now)).toBe(false);
    expect(isTaskOverdue({ dueDate: "2026-10-05T11:00:00.000Z", status: "CANCELLED" }, now)).toBe(false);
  });
});
