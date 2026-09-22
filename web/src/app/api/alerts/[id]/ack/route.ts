import { NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";
import { requireParentContext } from "@/lib/parentSession";
import { logAccess } from "@/lib/accessLog";

export const dynamic = "force-dynamic";

export async function POST(_req: Request, { params }: { params: { id: string } }) {
  const ctx = await requireParentContext();
  if (!ctx) {
    return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
  }

  const alert = await prisma.alert.findFirst({
    where: { id: params.id, familyId: ctx.familyId },
  });
  if (!alert) {
    return NextResponse.json({ error: "Not found" }, { status: 404 });
  }

  const updated = await prisma.alert.update({
    where: { id: alert.id },
    data: { acknowledgedAt: new Date() },
  });

  await logAccess({
    familyId: ctx.familyId,
    actorType: "USER",
    actorId: ctx.userId,
    action: "ALERT_ACKNOWLEDGED",
    targetType: "Alert",
    targetId: alert.id,
  });

  return NextResponse.json({ alert: updated });
}
