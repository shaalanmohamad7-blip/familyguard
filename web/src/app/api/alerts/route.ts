import { NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";
import { requireParentContext } from "@/lib/parentSession";

export const dynamic = "force-dynamic";

export async function GET() {
  const ctx = await requireParentContext();
  if (!ctx) {
    return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  }

  const alerts = await prisma.alert.findMany({
    where: { familyId: ctx.familyId },
    orderBy: [{ createdAt: "desc" }],
    take: 200,
    include: { device: { select: { id: true, label: true } } },
  });

  // SOS first (and unacknowledged first within type), regardless of recency.
  const sorted = [...alerts].sort((a, b) => {
    const aUrgent = a.type === "SOS" && !a.acknowledgedAt;
    const bUrgent = b.type === "SOS" && !b.acknowledgedAt;
    if (aUrgent !== bUrgent) return aUrgent ? -1 : 1;
    const aAck = a.acknowledgedAt ? 1 : 0;
    const bAck = b.acknowledgedAt ? 1 : 0;
    if (aAck !== bAck) return aAck - bAck;
    return b.createdAt.getTime() - a.createdAt.getTime();
  });

  return NextResponse.json({ alerts: sorted });
}
